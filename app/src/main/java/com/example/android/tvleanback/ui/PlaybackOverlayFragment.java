/*
 * Copyright (c) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tvleanback.ui;

import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.support.v4.media.session.MediaSessionCompat.FLAG_HANDLES_TRANSPORT_CONTROLS;

import com.google.android.exoplayer.ExoPlayer;
import com.google.android.exoplayer.util.Util;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.LoaderManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.media.session.MediaController;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Build.VERSION_CODES;
import android.os.Bundle;
import android.os.RemoteException;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.FragmentActivity;
import android.support.v4.media.MediaDescriptionCompat;
import android.support.v4.media.MediaMetadataCompat;
import android.support.v4.media.session.MediaControllerCompat;
import android.support.v4.media.session.MediaSessionCompat;
import android.support.v4.media.session.PlaybackStateCompat;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.tvleanback.R;
import com.example.android.tvleanback.Utils;
import com.example.android.tvleanback.data.VideoContract;
import com.example.android.tvleanback.model.Video;
import com.example.android.tvleanback.model.VideoCursorMapper;
import com.example.android.tvleanback.player.ExtractorRendererBuilder;
import com.example.android.tvleanback.player.VideoPlayer;
import com.example.android.tvleanback.presenter.CardPresenter;

import java.util.ArrayList;
import java.util.List;

/*
 * The PlaybackOverlayFragment class handles the Fragment associated with displaying the UI for the
 * media controls such as play / pause / skip forward / skip backward etc.
 *
 * The UI is updated through events that it receives from its MediaController
 */
public class PlaybackOverlayFragment
        extends android.support.v17.leanback.app.PlaybackOverlayFragment
        implements LoaderManager.LoaderCallbacks<Cursor>, TextureView.SurfaceTextureListener,
        VideoPlayer.Listener {
    private static final String TAG = "PlaybackOverlayFragment";
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final String AUTO_PLAY = "auto_play";
    private static final Bundle mAutoPlayExtras = new Bundle();
    private static final int RECOMMENDED_VIDEOS_LOADER = 1;
    private static final int QUEUE_VIDEOS_LOADER = 2;

    static {
        mAutoPlayExtras.putBoolean(AUTO_PLAY, true);
    }

    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();
    private int mSpecificVideoLoaderId = 3;
    private int mQueueIndex = -1;
    private Video mSelectedVideo; // Video is the currently playing Video and its metadata.
    private ArrayObjectAdapter mRowsAdapter;
    private List<MediaSessionCompat.QueueItem> mQueue = new ArrayList<>();
    private CursorObjectAdapter mVideoCursorAdapter;
    private MediaSessionCompat mSession; // MediaSession is used to hold the state of our media playback.
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;
    private MediaController mMediaController;
    private PlaybackControlHelper mGlue;
    private MediaController.Callback mMediaControllerCallback;
    private VideoPlayer mPlayer;
    private boolean mIsMetadataSet = false;
    private AudioManager mAudioManager;
    private boolean mHasAudioFocus;
    private boolean mPauseTransient;
    private final AudioManager.OnAudioFocusChangeListener mOnAudioFocusChangeListener =
            new AudioManager.OnAudioFocusChangeListener() {
                @Override
                public void onAudioFocusChange(int focusChange) {
                    switch (focusChange) {
                        case AudioManager.AUDIOFOCUS_LOSS:
                            abandonAudioFocus();
                            pause();
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT:
                            if (mGlue.isMediaPlaying()) {
                                pause();
                                mPauseTransient = true;
                            }
                            break;
                        case AudioManager.AUDIOFOCUS_LOSS_TRANSIENT_CAN_DUCK:
                            mPlayer.mute(true);
                            break;
                        case AudioManager.AUDIOFOCUS_GAIN:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT:
                        case AudioManager.AUDIOFOCUS_GAIN_TRANSIENT_MAY_DUCK:
                            if (mPauseTransient) {
                                play();
                            }
                            mPlayer.mute(false);
                            break;
                    }
                }
            };

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);
        mCallbacks = this;

        createMediaSession();
    }

    @Override
    public void onStop() {
        super.onStop();

        mSession.release();
        releasePlayer();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();

        if (mMediaController != null) {
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        mSession.release();
        releasePlayer();
    }

    @Override
    public void onStart() {
        super.onStart();

        // Set up UI
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        if (!updateSelectedVideo(video)) {
            return;
        }

        mGlue = new PlaybackControlHelper(getActivity(), this, mSelectedVideo);
        PlaybackControlsRowPresenter controlsRowPresenter = mGlue.createControlsRowAndPresenter();
        PlaybackControlsRow controlsRow = mGlue.getControlsRow();
        mMediaControllerCallback = mGlue.createMediaControllerCallback();

        mMediaController = getActivity().getMediaController();
        mMediaController.registerCallback(mMediaControllerCallback);

        ClassPresenterSelector ps = new ClassPresenterSelector();
        ps.addClassPresenter(PlaybackControlsRow.class, controlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);
        mRowsAdapter.add(controlsRow);
        addOtherRows();
        updatePlaybackRow();
        setAdapter(mRowsAdapter);

        startPlaying();
    }

    @Override
    public void onResume() {
        super.onResume();
        Video video = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        if (!updateSelectedVideo(video)) {
            return;
        }

        startPlaying();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mAudioManager = (AudioManager) getActivity().getSystemService(Context.AUDIO_SERVICE);

        // Initialize instance variables.
        TextureView textureView = (TextureView) getActivity().findViewById(R.id.texture_view);
        textureView.setSurfaceTextureListener(this);

        setBackgroundType(BACKGROUND_TYPE);

        // Set up listener.
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    private boolean updateSelectedVideo(Video video) {
        Intent intent = new Intent(getActivity().getIntent());
        intent.putExtra(VideoDetailsActivity.VIDEO, video);
        if (mSelectedVideo != null && mSelectedVideo.equals(video)) {
            return false;
        }
        mSelectedVideo = video;

        PendingIntent pi = PendingIntent.getActivity(
                getActivity(), 0, intent, PendingIntent.FLAG_UPDATE_CURRENT);
        mSession.setSessionActivity(pi);

        return true;
    }

    @TargetApi(VERSION_CODES.N)
    @Override
    public void onPause() {
        super.onPause();
        if (mGlue.isMediaPlaying()) {
            boolean isVisibleBehind = getActivity().requestVisibleBehind(true);
            boolean isInPictureInPictureMode =
                   PlaybackOverlayActivity.supportsPictureInPicture(getActivity())
                            && getActivity().isInPictureInPictureMode();
            if (!isVisibleBehind && !isInPictureInPictureMode) {
                pause();
            }
        } else {
            getActivity().requestVisibleBehind(false);
        }
    }

    @Override
    public void onPictureInPictureModeChanged(boolean pictureInPictureMode) {
        if (pictureInPictureMode) {
            mGlue.setFadingEnabled(false);
            setFadingEnabled(true);
            fadeOut();
        } else {
            mGlue.setFadingEnabled(true);
        }
    }

    @Override
    public Loader<Cursor> onCreateLoader(int id, Bundle args) {
        switch (id) {
            case RECOMMENDED_VIDEOS_LOADER: // Fall through.
            case QUEUE_VIDEOS_LOADER: {
                String category = args.getString(VideoContract.VideoEntry.COLUMN_CATEGORY);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null, // Projection to return - null means return all fields.
                        VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?",
                        // Selection clause is category.
                        new String[]{category}, // Select based on the category.
                        null // Default sort order
                );
            }
            default: {
                // Loading a specific video.
                String videoId = args.getString(VideoContract.VideoEntry._ID);
                return new CursorLoader(
                        getActivity(),
                        VideoContract.VideoEntry.CONTENT_URI,
                        null, // Projection to return - null means return all fields.
                        VideoContract.VideoEntry._ID + " = ?", // Selection clause is id.
                        new String[]{videoId}, // Select based on the id.
                        null // Default sort order
                );
            }
        }
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor cursor) {
        if (cursor != null && cursor.moveToFirst()) {
            switch (loader.getId()) {
                case QUEUE_VIDEOS_LOADER: {
                    mQueue.clear();
                    while (!cursor.isAfterLast()) {
                        Video v = (Video) mVideoCursorMapper.convert(cursor);

                        // Set the queue index to the selected video.
                        if (v.id == mSelectedVideo.id) {
                            mQueueIndex = mQueue.size();
                        }

                        // Add the video to the queue.
                        MediaSessionCompat.QueueItem item = getQueueItem(v);
                        mQueue.add(item);

                        cursor.moveToNext();
                    }

                    mSession.setQueue(mQueue);
                    mSession.setQueueTitle(getString(R.string.queue_name));
                    break;
                }
                case RECOMMENDED_VIDEOS_LOADER: {
                    mVideoCursorAdapter.changeCursor(cursor);
                    break;
                }
                default: {
                    // Playing a specific video.
                    Video video = (Video) mVideoCursorMapper.convert(cursor);
                    playVideo(video, mAutoPlayExtras);
                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private void setPosition(long position) {
        if (position > mPlayer.getDuration()) {
            mPlayer.seekTo(mPlayer.getDuration());
        } else if (position < 0) {
            mPlayer.seekTo(0L);
        } else {
            mPlayer.seekTo(position);
        }
    }

    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSessionCompat(getActivity(), "LeanbackSampleApp");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS | FLAG_HANDLES_TRANSPORT_CONTROLS);
            mSession.setActive(true);

            // Set the Activity's MediaController used to invoke transport controls / adjust volume.
            try {
                ((FragmentActivity) getActivity()).setSupportMediaController(
                        new MediaControllerCompat(getActivity(), mSession.getSessionToken()));
                setPlaybackState(PlaybackState.STATE_NONE);
            } catch (RemoteException e) {
                e.printStackTrace();
            }
        }
    }

    private MediaSessionCompat.QueueItem getQueueItem(Video v) {
        MediaDescriptionCompat desc = new MediaDescriptionCompat.Builder()
                .setDescription(v.description)
                .setMediaId(v.id + "")
                .setIconUri(Uri.parse(v.cardImageUrl))
                .setMediaUri(Uri.parse(v.videoUrl))
                .setSubtitle(v.studio)
                .setTitle(v.title)
                .build();
        return new MediaSessionCompat.QueueItem(desc, v.id);
    }

    public long getBufferedPosition() {
        if (mPlayer != null) {
            return mPlayer.getBufferedPosition();
        }
        return 0L;
    }

    public long getCurrentPosition() {
        if (mPlayer != null) {
            return mPlayer.getCurrentPosition();
        }
        return 0L;
    }

    public long getDuration() {
        if (mPlayer != null) {
            return mPlayer.getDuration();
        }
        return ExoPlayer.UNKNOWN_TIME;
    }

    private long getAvailableActions(int nextState) {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                PlaybackState.ACTION_FAST_FORWARD |
                PlaybackState.ACTION_REWIND |
                PlaybackState.ACTION_PAUSE;

        if (nextState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void play() {
        // Request audio focus whenever we resume playback
        // because the app might have abandoned audio focus due to the AUDIOFOCUS_LOSS.
        requestAudioFocus();

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }
        if (!mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().start();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        }
    }

    private void pause() {
        mPauseTransient = false;

        if (mPlayer == null) {
            setPlaybackState(PlaybackState.STATE_NONE);
            return;
        }
        if (mGlue.isMediaPlaying()) {
            mPlayer.getPlayerControl().pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private void requestAudioFocus() {
        if (mHasAudioFocus) {
            return;
        }
        int result = mAudioManager.requestAudioFocus(mOnAudioFocusChangeListener,
                AudioManager.STREAM_MUSIC, AudioManager.AUDIOFOCUS_GAIN);
        if (result == AudioManager.AUDIOFOCUS_REQUEST_GRANTED) {
            mHasAudioFocus = true;
        } else {
            pause();
        }
    }

    private void abandonAudioFocus() {
        mHasAudioFocus = false;
        mAudioManager.abandonAudioFocus(mOnAudioFocusChangeListener);
    }

    void updatePlaybackRow() {
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    /**
     * Creates a ListRow for related videos.
     */
    private void addOtherRows() {
        mVideoCursorAdapter = new CursorObjectAdapter(new CardPresenter());
        mVideoCursorAdapter.setMapper(new VideoCursorMapper());

        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mSelectedVideo.category);
        getLoaderManager().initLoader(RECOMMENDED_VIDEOS_LOADER, args, this);

        HeaderItem header = new HeaderItem(getString(R.string.related_movies));
        mRowsAdapter.add(new ListRow(header, mVideoCursorAdapter));
    }

    private VideoPlayer.RendererBuilder getRendererBuilder() {
        String userAgent = Util.getUserAgent(getActivity(), "ExoVideoPlayer");
        Uri contentUri = Uri.parse(mSelectedVideo.videoUrl);
        int contentType = Util.inferContentType(contentUri.getLastPathSegment());

        switch (contentType) {
            case Util.TYPE_OTHER: {
                return new ExtractorRendererBuilder(getActivity(), userAgent, contentUri);
            }
            default: {
                throw new IllegalStateException("Unsupported type: " + contentType);
            }
        }
    }

    private void preparePlayer() {
        if (mPlayer == null) {
            mPlayer = new VideoPlayer(getRendererBuilder());
            mPlayer.addListener(this);
            mPlayer.seekTo(0L);
            mPlayer.prepare();
        } else {
            mPlayer.stop();
            mPlayer.seekTo(0L);
            mPlayer.setRendererBuilder(getRendererBuilder());
            mPlayer.prepare();
        }
        mPlayer.setPlayWhenReady(true);

        requestAudioFocus();
    }

    private void releasePlayer() {
        if (mPlayer != null) {
            mPlayer.release();
            mPlayer = null;
        }
        abandonAudioFocus();
    }

    @Override
    public void onStateChanged(boolean playWhenReady, int playbackState) {
        switch (playbackState) {
            case ExoPlayer.STATE_BUFFERING:
                // Do nothing.
                break;
            case ExoPlayer.STATE_ENDED:
                mIsMetadataSet = false;
                mMediaController.getTransportControls().skipToNext();
                break;
            case ExoPlayer.STATE_IDLE:
                // Do nothing.
                break;
            case ExoPlayer.STATE_PREPARING:
                mIsMetadataSet = false;
                break;
            case ExoPlayer.STATE_READY:
                // Duration is set here.
                if (!mIsMetadataSet) {
                    updateMetadata(mSelectedVideo);
                    mIsMetadataSet = true;
                }
                break;
            default:
                // Do nothing.
                break;
        }
    }

    @Override
    public void onError(Exception e) {
        Log.e(TAG, "An error occurred: " + e);
    }

    @Override
    public void onVideoSizeChanged(int width, int height, int unappliedRotationDegrees,
            float pixelWidthHeightRatio) {
        // Do nothing.
    }

    @Override
    public void onSurfaceTextureAvailable(SurfaceTexture surfaceTexture, int width, int height) {
        if (mPlayer != null) {
            mPlayer.setSurface(new Surface(surfaceTexture));
        }
    }

    @Override
    public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
        // Do nothing.
    }

    @Override
    public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
        if (mPlayer != null) {
            mPlayer.blockingClearSurface();
        }
        return true;
    }

    @Override
    public void onSurfaceTextureUpdated(SurfaceTexture surface) {
        // Do nothing.
    }

    private int getPlaybackState() {
        Activity activity = getActivity();

        if (activity != null) {
            PlaybackState state = activity.getMediaController().getPlaybackState();
            if (state != null) {
                return state.getState();
            } else {
                return PlaybackState.STATE_NONE;
            }
        }
        return PlaybackState.STATE_NONE;
    }

    private void setPlaybackState(int state) {
        long currPosition = getCurrentPosition();

        PlaybackStateCompat.Builder stateBuilder = new PlaybackStateCompat.Builder()
                .setActions(getAvailableActions(state));
        stateBuilder.setState(state, currPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private void updateMetadata(final Video video) {
        final MediaMetadataCompat.Builder metadataBuilder = new MediaMetadataCompat.Builder();

        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_MEDIA_ID, video.id + "");
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_SUBTITLE, video.studio);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_DISPLAY_DESCRIPTION,
                video.description);

        long duration = Utils.getDuration(video.videoUrl);
        metadataBuilder.putLong(MediaMetadataCompat.METADATA_KEY_DURATION, duration);

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadataCompat.METADATA_KEY_ARTIST, video.studio);

        Resources res = getResources();
        int cardWidth = res.getDimensionPixelSize(R.dimen.playback_overlay_width);
        int cardHeight = res.getDimensionPixelSize(R.dimen.playback_overlay_height);

        Glide.with(this)
                .load(Uri.parse(video.cardImageUrl))
                .asBitmap()
                .centerCrop()
                .into(new SimpleTarget<Bitmap>(cardWidth, cardHeight) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        metadataBuilder.putBitmap(MediaMetadataCompat.METADATA_KEY_ART, bitmap);
                        mSession.setMetadata(metadataBuilder.build());
                    }
                });
    }

    private void playVideo(Video video, Bundle extras) {
        updateSelectedVideo(video);
        preparePlayer();
        setPlaybackState(PlaybackState.STATE_PAUSED);
        if (extras.getBoolean(AUTO_PLAY)) {
            play();
        } else {
            pause();
        }
    }

    private void startPlaying() {
        // Prepare the player and start playing the selected video
        playVideo(mSelectedVideo, mAutoPlayExtras);

        // Start loading videos for the queue
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mSelectedVideo.category);
        getLoaderManager().initLoader(QUEUE_VIDEOS_LOADER, args, mCallbacks);
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                intent.putExtra(VideoDetailsActivity.VIDEO, video);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        VideoDetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    // An event was triggered by MediaController.TransportControls and must be handled here.
    // Here we update the media itself to act on the event that was triggered.
    private class MediaSessionCallback extends MediaSessionCompat.Callback {

        @Override
        public void onPlay() {
            play();
        }

        @Override
        // This method should play any media item regardless of the Queue.
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Bundle args = new Bundle();
            args.putString(VideoContract.VideoEntry._ID, mediaId);
            getLoaderManager().initLoader(mSpecificVideoLoaderId++, args, mCallbacks);
        }

        @Override
        public void onPause() {
            pause();
        }

        @Override
        public void onSkipToNext() {
            // Update the media to skip to the next video.
            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int nextIndex = ++mQueueIndex;
            if (nextIndex < mQueue.size()) {
                MediaSessionCompat.QueueItem item = mQueue.get(nextIndex);
                String mediaId = item.getDescription().getMediaId();
                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onSkipToPrevious() {
            // Update the media to skip to the previous video.
            setPlaybackState(PlaybackState.STATE_SKIPPING_TO_PREVIOUS);

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int prevIndex = --mQueueIndex;
            if (prevIndex >= 0) {
                MediaSessionCompat.QueueItem item = mQueue.get(prevIndex);
                String mediaId = item.getDescription().getMediaId();

                getActivity().getMediaController()
                        .getTransportControls()
                        .playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onFastForward() {
            if (mPlayer.getDuration() != ExoPlayer.UNKNOWN_TIME) {
                // Fast forward 10 seconds.
                int prevState = getPlaybackState();
                setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                setPosition(mPlayer.getCurrentPosition() + (10 * 1000));
                setPlaybackState(prevState);
            }
        }

        @Override
        public void onRewind() {
            // Rewind 10 seconds.
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_REWINDING);
            setPosition(mPlayer.getCurrentPosition() - (10 * 1000));
            setPlaybackState(prevState);
        }

        @Override
        public void onSeekTo(long position) {
            setPosition(position);
        }
    }
}
