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

import android.app.Activity;
import android.app.LoaderManager;
import android.content.Context;
import android.content.CursorLoader;
import android.content.Intent;
import android.content.Loader;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.media.MediaDescription;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.NonNull;
import android.support.v17.leanback.widget.AbstractDetailsDescriptionPresenter;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.ControlButtonPresenterSelector;
import android.support.v17.leanback.widget.CursorObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.OnItemViewSelectedListener;
import android.support.v17.leanback.widget.PlaybackControlsRow;
import android.support.v17.leanback.widget.PlaybackControlsRow.ClosedCaptioningAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.FastForwardAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.HighQualityAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.MoreActions;
import android.support.v17.leanback.widget.PlaybackControlsRow.PlayPauseAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RepeatAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.RewindAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ShuffleAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipNextAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.SkipPreviousAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsDownAction;
import android.support.v17.leanback.widget.PlaybackControlsRow.ThumbsUpAction;
import android.support.v17.leanback.widget.PlaybackControlsRowPresenter;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
import android.util.Log;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.load.resource.drawable.GlideDrawable;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.tvleanback.R;
import com.example.android.tvleanback.Utils;
import com.example.android.tvleanback.data.VideoContract;
import com.example.android.tvleanback.model.Video;
import com.example.android.tvleanback.model.VideoCursorMapper;
import com.example.android.tvleanback.presenter.CardPresenter;

import java.util.ArrayList;
import java.util.List;
import java.util.Timer;
import java.util.TimerTask;

import static android.media.session.MediaSession.FLAG_HANDLES_MEDIA_BUTTONS;
import static android.media.session.MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS;

/*
 * The PlaybackOverlayFragment class handles the Fragment associated with displaying the UI for the
 * media controls such as play / pause / skip forward / skip backward etc.
 *
 * The UI is updated through events that it receives from its MediaController
 */
public class PlaybackOverlayFragment extends android.support.v17.leanback.app.PlaybackOverlayFragment implements LoaderManager.LoaderCallbacks<Cursor> {
    private static final String TAG = "PlaybackOverlayFragment";
    private static final boolean SHOW_DETAIL = true;
    private static final boolean HIDE_MORE_ACTIONS = false;
    private static final int BACKGROUND_TYPE = PlaybackOverlayFragment.BG_LIGHT;
    private static final int CARD_WIDTH = 150;
    private static final int CARD_HEIGHT = 240;
    private static final int DEFAULT_UPDATE_PERIOD = 1000;
    private static final int UPDATE_PERIOD = 16;
    private static final int SIMULATED_BUFFERED_TIME = 10000;
    private static final int CLICK_TRACKING_DELAY = 1000;
    private static final int INITIAL_SPEED = 10000;

    private final Handler mClickTrackingHandler = new Handler();

    private VideoView mVideoView; // VideoView is used to play the video (media) in a view.
    private Video mSelectedVideo; // Video is the currently playing Video and its metadata.
    private ArrayObjectAdapter mRowsAdapter;
    private ArrayObjectAdapter mPrimaryActionsAdapter;
    private ArrayObjectAdapter mSecondaryActionsAdapter;
    private PlayPauseAction mPlayPauseAction;
    private ClosedCaptioningAction mClosedCaptioningAction;
    private FastForwardAction mFastForwardAction;
    private HighQualityAction mHighQualityAction;
    private MoreActions mMoreActions;
    private RepeatAction mRepeatAction;
    private RewindAction mRewindAction;
    private ShuffleAction mShuffleAction;
    private SkipNextAction mSkipNextAction;
    private SkipPreviousAction mSkipPreviousAction;
    private ThumbsDownAction mThumbsDownAction;
    private ThumbsUpAction mThumbsUpAction;
    private PlaybackControlsRow mPlaybackControlsRow;
    private Handler mHandler;
    private Runnable mRunnable;
    private long mDuration = -1;
    private int mFfwRwdSpeed = INITIAL_SPEED;
    private Timer mClickTrackingTimer;
    private int mClickCount;
    private int mQueueIndex = -1;
    private List<MediaSession.QueueItem> mQueue;
    private CursorObjectAdapter mVideoCursorAdapter;
    private int mPosition = 0;
    private long mStartTimeMillis;
    private MediaSession mSession; // MediaSession is used to hold the state of our media playback.
    private final static int RECOMMENDED_VIDEOS_LOADER = 1;
    private final static int QUEUE_VIDEOS_LOADER = 2;
    private int mSpecificVideoLoaderId = 3;
    private final VideoCursorMapper mVideoCursorMapper = new VideoCursorMapper();
    private LoaderManager.LoaderCallbacks<Cursor> mCallbacks;

    private MediaController mMediaController;
    private final MediaController.Callback mMediaControllerCallback = new MediaControllerCallback();

    private static final String AUTO_PLAY = "auto_play";

    @Override
    public void onAttach(Context context) {
        Log.d(TAG, "onAttach");
        super.onAttach(context);
        mCallbacks = this;

        createMediaSession();

        mMediaController = getActivity().getMediaController();
        Log.d(TAG, "register callback of mediaController");
        mMediaController.registerCallback(mMediaControllerCallback);
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate");
        super.onCreate(savedInstanceState);

        // Initialize instance variables.
        mVideoView = (VideoView) getActivity().findViewById(R.id.videoView);
        mSelectedVideo = getActivity().getIntent().getParcelableExtra(VideoDetailsActivity.VIDEO);
        mHandler = new Handler();
        mQueue = new ArrayList<>();

        // Hack to get playback controls to be in focus right away.
        mVideoView.post(new Runnable() {
            @Override
            public void run() {
                mVideoView.setFocusable(false);
                mVideoView.setFocusableInTouchMode(false);
            }
        });

        // Set up UI.
        setBackgroundType(BACKGROUND_TYPE);
        setFadingEnabled(false);
        setupRows();

        // Start playing the selected video.
        setVideoPath(mSelectedVideo.videoUrl);
        updateMetadata(mSelectedVideo);
        playPause(true);

        // Start loading videos for the queue.
        Bundle args = new Bundle();
        args.putString(VideoContract.VideoEntry.COLUMN_CATEGORY, mSelectedVideo.category);
        getLoaderManager().initLoader(QUEUE_VIDEOS_LOADER, args, mCallbacks);

        // Set up listeners.
        setOnItemViewSelectedListener(new OnItemViewSelectedListener() {
            @Override
            public void onItemSelected(Presenter.ViewHolder itemViewHolder, Object item,
                                       RowPresenter.ViewHolder rowViewHolder, Row row) {
            }
        });
        setOnItemViewClickedListener(new ItemViewClickedListener());
    }

    @Override
    public void onStop() {
        Log.d(TAG, "onStop");
        stopProgressAutomation();
        mRowsAdapter = null;
        super.onStop();
    }

    @Override
    public void onDetach() {
        Log.d(TAG, "onDetach");
        if (mMediaController != null) {
            Log.d(TAG, "Unregister callback of MediaController");
            mMediaController.unregisterCallback(mMediaControllerCallback);
        }
        super.onDetach();
    }

    @Override
    public void onDestroy() {
        Log.d(TAG, "onDestroy");
        super.onDestroy();

        mSession.release();

        if (mVideoView != null) {
            mVideoView.stopPlayback();
            mVideoView.suspend();
            mVideoView.setVideoURI(null);
        }
    }

    private void setPosition(int position) {
        if (position > mDuration) {
            mPosition = (int) mDuration;
        } else if (position < 0) {
            mPosition = 0;
            mStartTimeMillis = System.currentTimeMillis();
        } else {
            mPosition = position;
        }
        mStartTimeMillis = System.currentTimeMillis();
        Log.d(TAG, "position set to " + mPosition);
    }

    private void createMediaSession() {
        Log.d(TAG, "createMediaSession");
        if (mSession == null) {
            mSession = new MediaSession(getActivity(), "LeanbackSampleApp");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(FLAG_HANDLES_MEDIA_BUTTONS |
                    FLAG_HANDLES_TRANSPORT_CONTROLS);

            mSession.setActive(true);

            // Set the Activity's MediaController used to invoke transport controls / adjust volume.
            getActivity().setMediaController(new MediaController(getActivity(), mSession.getSessionToken()));
            setPlaybackState(PlaybackState.STATE_NONE);
        }
    }

    private MediaSession.QueueItem getQueueItem(Video v) {
        MediaDescription desc = new MediaDescription.Builder()
                .setDescription(v.description)
                .setMediaId(v.id + "")
                .setMediaUri(Uri.parse(v.videoUrl))
                .setIconUri(Uri.parse(v.cardImageUrl))
                .setSubtitle(v.studio)
                .setTitle(v.title)
                .build();

        return new MediaSession.QueueItem(desc, v.id);
    }

    private void setPlaybackState(int state) {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions(state));
        stateBuilder.setState(state, mPosition, 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions(int nextState) {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH |
                PlaybackState.ACTION_SKIP_TO_NEXT |
                PlaybackState.ACTION_SKIP_TO_PREVIOUS |
                PlaybackState.ACTION_PAUSE;

        if (nextState == PlaybackState.STATE_PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView.isPlaying()) {
            if (!getActivity().requestVisibleBehind(true)) {
                // Try to play behind launcher, but if it fails, stop playback.
                playPause(false);
            }
        } else {
            getActivity().requestVisibleBehind(false);
        }
    }

    private void playPause(boolean doPlay) {
        if (getPlaybackState() == PlaybackState.STATE_NONE) {
            setupCallbacks();
        }

        if (doPlay && getPlaybackState() != PlaybackState.STATE_PLAYING) {
            if (mPosition > 0) {
                mVideoView.seekTo(mPosition);
            }
            mVideoView.start();
            mStartTimeMillis = System.currentTimeMillis();
            setPlaybackState(PlaybackState.STATE_PLAYING);
        } else {
            int timeElapsedSinceStart = (int) (System.currentTimeMillis() - mStartTimeMillis);
            setPosition(mPosition + timeElapsedSinceStart);
            mVideoView.pause();
            setPlaybackState(PlaybackState.STATE_PAUSED);
        }
    }

    private void setVideoPath(String videoUrl) {
        setPosition(0);
        mVideoView.setVideoPath(videoUrl);
        mStartTimeMillis = 0;
        mDuration = Utils.getDuration(videoUrl);
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                mVideoView.stopPlayback();
                setPlaybackState(PlaybackState.STATE_STOPPED);
                return false;
            }
        });

        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (getPlaybackState() == PlaybackState.STATE_PLAYING) {
                    mVideoView.start();
                }
            }
        });

        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                setPlaybackState(PlaybackState.STATE_STOPPED);
            }
        });

    }

    private void setupRows() {
        ClassPresenterSelector ps = new ClassPresenterSelector();
        PlaybackControlsRowPresenter playbackControlsRowPresenter;
        Activity activity = getActivity();

        if (SHOW_DETAIL) {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter(new DescriptionPresenter());
        } else {
            playbackControlsRowPresenter = new PlaybackControlsRowPresenter();
        }

        ControlButtonPresenterSelector presenterSelector = new ControlButtonPresenterSelector();
        mPrimaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);
        mSecondaryActionsAdapter = new ArrayObjectAdapter(presenterSelector);

        mPlayPauseAction = new PlayPauseAction(getActivity());
        mRepeatAction = new RepeatAction(activity);
        mThumbsUpAction = new ThumbsUpAction(activity);
        mThumbsDownAction = new ThumbsDownAction(activity);
        mShuffleAction = new ShuffleAction(activity);
        mSkipNextAction = new SkipNextAction(activity);
        mSkipPreviousAction = new SkipPreviousAction(activity);
        mFastForwardAction = new FastForwardAction(activity);
        mRewindAction = new RewindAction(activity);
        mHighQualityAction = new HighQualityAction(activity);
        mClosedCaptioningAction = new ClosedCaptioningAction(activity);
        mMoreActions = new MoreActions(activity);

        // Add main controls to primary adapter.
        mPrimaryActionsAdapter.add(mSkipPreviousAction);
        mPrimaryActionsAdapter.add(mRewindAction);
        mPrimaryActionsAdapter.add(mPlayPauseAction);
        mPrimaryActionsAdapter.add(mFastForwardAction);
        mPrimaryActionsAdapter.add(mSkipNextAction);

        // Add rest of controls to secondary adapter.
        mSecondaryActionsAdapter.add(mThumbsUpAction);
        mSecondaryActionsAdapter.add(mRepeatAction);
        mSecondaryActionsAdapter.add(mShuffleAction);
        mSecondaryActionsAdapter.add(mThumbsDownAction);
        mSecondaryActionsAdapter.add(mHighQualityAction);
        mSecondaryActionsAdapter.add(mClosedCaptioningAction);
        mSecondaryActionsAdapter.add(mMoreActions);

        playbackControlsRowPresenter.setOnActionClickedListener(new OnActionClickedListener() {
            public void onActionClicked(Action action) {
                if (action.getId() == mPlayPauseAction.getId()) {
                    togglePlayback(mPlayPauseAction.getIndex() == PlayPauseAction.PLAY);
                } else if (action.getId() == mSkipNextAction.getId()) {
                    next();
                } else if (action.getId() == mSkipPreviousAction.getId()) {
                    prev();
                } else if (action.getId() == mFastForwardAction.getId()) {
                    fastForward();
                } else if (action.getId() == mRewindAction.getId()) {
                    fastRewind();
                }
                if (action instanceof PlaybackControlsRow.MultiAction) {
                    notifyChanged(action);
                }
            }
        });
        playbackControlsRowPresenter.setSecondaryActionsHidden(HIDE_MORE_ACTIONS);

        ps.addClassPresenter(PlaybackControlsRow.class, playbackControlsRowPresenter);
        ps.addClassPresenter(ListRow.class, new ListRowPresenter());
        mRowsAdapter = new ArrayObjectAdapter(ps);

        addPlaybackControlsRow();
        addOtherRows();

        setAdapter(mRowsAdapter);
    }

    private void togglePlayback(boolean playPause) {
        if (playPause) {
            mMediaController.getTransportControls().play();
        } else {
            mMediaController.getTransportControls().pause();
        }
    }

    private void addPlaybackControlsRow() {
        if (SHOW_DETAIL) {
            mPlaybackControlsRow = new PlaybackControlsRow(mSelectedVideo);
        } else {
            mPlaybackControlsRow = new PlaybackControlsRow();
        }
        mRowsAdapter.add(mPlaybackControlsRow);

        updatePlaybackRow();

        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);
    }

    private void notifyChanged(Action action) {
        int index = mPrimaryActionsAdapter.indexOf(action);
        if (index >= 0) {
            mPrimaryActionsAdapter.notifyArrayItemRangeChanged(index, 1);
        } else {
            index = mSecondaryActionsAdapter.indexOf(action);
            if (index >= 0) {
                mSecondaryActionsAdapter.notifyArrayItemRangeChanged(index, 1);
            }
        }
    }

    private void updatePlaybackRow() {
        mPlaybackControlsRow.setCurrentTime(0);
        mPlaybackControlsRow.setBufferedProgress(0);
        mRowsAdapter.notifyArrayItemRangeChanged(0, 1);
    }

    private void updateMovieView(MediaMetadata metadata) {
        Video v = new Video.VideoBuilder().buildFromMediaDesc(metadata.getDescription());
        long dur = metadata.getLong(MediaMetadata.METADATA_KEY_DURATION);

        // PlaybackControlsRow doesn't allow you to set the item, so we must create a new one
        // because our Video class is now immutable.
        // TODO(ryanseys): Implement Playback Glue support so this can be mitigated.
        mPlaybackControlsRow = new PlaybackControlsRow(v);
        mPlaybackControlsRow.setTotalTime((int) dur);

        // Show the video card image if there is enough room in the UI for it.
        // If you have many primary actions, you may not have enough room.
        updateVideoImage(v.cardImageUrl);

        mRowsAdapter.clear();
        mRowsAdapter.add(mPlaybackControlsRow);

        updatePlaybackRow();

        mPlaybackControlsRow.setPrimaryActionsAdapter(mPrimaryActionsAdapter);
        mPlaybackControlsRow.setSecondaryActionsAdapter(mSecondaryActionsAdapter);

        addOtherRows();
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

    private int getUpdatePeriod() {
        if (getView() == null || mPlaybackControlsRow.getTotalTime() <= 0
                || getView().getWidth() == 0) {
            return DEFAULT_UPDATE_PERIOD;
        }
        return Math.max(UPDATE_PERIOD, mPlaybackControlsRow.getTotalTime() / getView().getWidth());
    }

    private void startProgressAutomation() {
        if (mRunnable == null) {
            mRunnable = new Runnable() {
                @Override
                public void run() {
                    int updatePeriod = getUpdatePeriod();
                    int currentTime = mPlaybackControlsRow.getCurrentTime() + updatePeriod;
                    int totalTime = mPlaybackControlsRow.getTotalTime();
                    mPlaybackControlsRow.setCurrentTime(currentTime);
                    mPlaybackControlsRow.setBufferedProgress(currentTime + SIMULATED_BUFFERED_TIME);

                    if (totalTime > 0 && totalTime <= currentTime) {
                        stopProgressAutomation();
                        next();
                    } else {
                        mHandler.postDelayed(this, updatePeriod);
                    }
                }
            };
            mHandler.postDelayed(mRunnable, getUpdatePeriod());
        }
    }

    private void next() {
        mMediaController.getTransportControls().skipToNext();
    }

    private void prev() {
        mMediaController.getTransportControls().skipToPrevious();
    }

    private void fastForward() {
        startClickTrackingTimer();
        mMediaController.getTransportControls().fastForward();
    }

    private void fastRewind() {
        startClickTrackingTimer();
        mMediaController.getTransportControls().rewind();
    }

    private void stopProgressAutomation() {
        if (mHandler != null && mRunnable != null) {
            mHandler.removeCallbacks(mRunnable);
            mRunnable = null;
        }
    }

    private void updateVideoImage(String uri) {
        Glide.with(this)
                .load(uri)
                .centerCrop()
                .into(new SimpleTarget<GlideDrawable>(CARD_WIDTH, CARD_HEIGHT) {
                    @Override
                    public void onResourceReady(GlideDrawable resource, GlideAnimation<? super GlideDrawable> glideAnimation) {
                        mPlaybackControlsRow.setImageDrawable(resource);
                        mRowsAdapter.notifyArrayItemRangeChanged(0, mRowsAdapter.size());
                    }
                });
    }

    private void startClickTrackingTimer() {
        if (null != mClickTrackingTimer) {
            mClickCount++;
            mClickTrackingTimer.cancel();
        } else {
            mClickCount = 0;
            mFfwRwdSpeed = INITIAL_SPEED;
        }
        mClickTrackingTimer = new Timer();
        mClickTrackingTimer.schedule(new UpdateFfwRwdSpeedTask(), CLICK_TRACKING_DELAY);
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
                        VideoContract.VideoEntry.COLUMN_CATEGORY + " = ?", // Selection clause is category.
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
                            Log.d(TAG, "mQueueIndex set to " + mQueueIndex);
                        }

                        // Add the video to the queue.
                        MediaSession.QueueItem item = getQueueItem(v);
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
                    Bundle extras = new Bundle();
                    extras.putBoolean(AUTO_PLAY, true);
                    playVideo(video, extras);
                    break;
                }
            }
        }
    }

    @Override
    public void onLoaderReset(Loader<Cursor> loader) {
        mVideoCursorAdapter.changeCursor(null);
    }

    private static class DescriptionPresenter extends AbstractDetailsDescriptionPresenter {
        @Override
        protected void onBindDescription(ViewHolder viewHolder, Object item) {
            Video video = (Video) item;

            viewHolder.getTitle().setText(video.title);
            viewHolder.getSubtitle().setText(video.studio);
        }
    }

    private class UpdateFfwRwdSpeedTask extends TimerTask {

        @Override
        public void run() {
            mClickTrackingHandler.post(new Runnable() {
                @Override
                public void run() {
                    if (mClickCount == 0) {
                        mFfwRwdSpeed = INITIAL_SPEED;
                    } else if (mClickCount == 1) {
                        mFfwRwdSpeed *= 2;
                    } else if (mClickCount >= 2) {
                        mFfwRwdSpeed *= 4;
                    }
                    mClickCount = 0;
                    mClickTrackingTimer = null;
                }
            });
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Video) {
                Video video = (Video) item;
                Log.d(TAG, "Item: " + item.toString());
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

    private void updateMetadata(final Video video) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, video.id + "");
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE, video.studio);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION, video.description);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI, video.cardImageUrl);
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, mDuration);

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, video.title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, video.studio);

        Glide.with(this)
                .load(Uri.parse(video.cardImageUrl))
                .asBitmap()
                .into(new SimpleTarget<Bitmap>(500, 500) {
                    @Override
                    public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                        metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                        mSession.setMetadata(metadataBuilder.build());
                    }
                });
    }

    private void playVideo(Video v, Bundle extras) {
        Log.d(TAG, "Playing video " + v.title + " with url = " + v.videoUrl);
        Log.d(TAG, "Video: " + v.toString());
        setVideoPath(v.videoUrl);
        setPlaybackState(PlaybackState.STATE_PAUSED);
        updateMetadata(v);
        playPause(extras.getBoolean(AUTO_PLAY));
    }

    // An event was triggered by MediaController.TransportControls and must be handled here.
    // Here we update the media itself to act on the event that was triggered.
    private class MediaSessionCallback extends MediaSession.Callback {

        @Override
        public void onPlay() {
            playPause(true);
        }

        @Override
        public void onPause() {
            playPause(false);
        }

        @Override
        // This method should play any media item regardless of the Queue.
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Bundle args = new Bundle();
            args.putString(VideoContract.VideoEntry._ID, mediaId);
            getLoaderManager().initLoader(mSpecificVideoLoaderId++, args, mCallbacks);
        }

        @Override
        public void onSkipToNext() {
            // Update the media to skip to the next video.
            mPosition = 0;

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int nextIndex = ++mQueueIndex;
            if (nextIndex < mQueue.size()) {
                MediaSession.QueueItem item = mQueue.get(nextIndex);
                String mediaId = item.getDescription().getMediaId();
                getActivity().getMediaController().getTransportControls().playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onSkipToPrevious() {
            // Update the media to skip to the previous video.
            mPosition = 0;
            setPlaybackState(PlaybackState.STATE_SKIPPING_TO_PREVIOUS);

            Bundle bundle = new Bundle();
            bundle.putBoolean(AUTO_PLAY, true);

            int prevIndex = --mQueueIndex;
            if (prevIndex >= 0) {
                MediaSession.QueueItem item = mQueue.get(prevIndex);
                String mediaId = item.getDescription().getMediaId();

                getActivity().getMediaController().getTransportControls().playFromMediaId(mediaId, bundle);
            } else {
                getActivity().onBackPressed(); // Return to details presenter.
            }
        }

        @Override
        public void onSeekTo(long pos) {
            setPosition((int) pos);
            mVideoView.seekTo(mPosition);
        }

        @Override
        public void onFastForward() {
            if (mDuration != -1) {
                // Fast forward 10 seconds.
                int prevState = getPlaybackState();
                setPlaybackState(PlaybackState.STATE_FAST_FORWARDING);
                setPosition(mVideoView.getCurrentPosition() + (10 * 1000));
                mVideoView.seekTo(mPosition);
                setPlaybackState(prevState);
            }
        }

        @Override
        public void onRewind() {
            // Rewind 10 seconds.
            int prevState = getPlaybackState();
            setPlaybackState(PlaybackState.STATE_REWINDING);
            setPosition(mVideoView.getCurrentPosition() - (10 * 1000));
            mVideoView.seekTo(mPosition);
            setPlaybackState(prevState);
        }
    }

    private class MediaControllerCallback extends MediaController.Callback {

        @Override
        public void onPlaybackStateChanged(@NonNull PlaybackState state) {
            // The playback state has changed, so update your UI accordingly.
            // This should not update any media player / state!
            Log.d(TAG, "Playback state changed: " + state.getState());

            int nextState = state.getState();

            if (nextState == PlaybackState.STATE_PLAYING) {
                startProgressAutomation();
                setFadingEnabled(true);
                mPlayPauseAction.setIndex(PlayPauseAction.PAUSE);
                mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PAUSE));
                notifyChanged(mPlayPauseAction);
            } else if (nextState == PlaybackState.STATE_PAUSED) {
                stopProgressAutomation();
                setFadingEnabled(false);
                mPlayPauseAction.setIndex(PlayPauseAction.PLAY);
                mPlayPauseAction.setIcon(mPlayPauseAction.getDrawable(PlayPauseAction.PLAY));
                notifyChanged(mPlayPauseAction);
            } else if (nextState == PlaybackState.STATE_SKIPPING_TO_NEXT) {
                startProgressAutomation();
                setFadingEnabled(true);
                notifyChanged(mSkipNextAction);
            } else if (nextState == PlaybackState.STATE_SKIPPING_TO_PREVIOUS) {
                startProgressAutomation();
                setFadingEnabled(true);
                notifyChanged(mSkipPreviousAction);
            }

            int currentTime = (int) state.getPosition();
            mPlaybackControlsRow.setCurrentTime(currentTime);
            mPlaybackControlsRow.setBufferedProgress(currentTime + SIMULATED_BUFFERED_TIME);
        }

        @Override
        public void onMetadataChanged(MediaMetadata metadata) {
            Log.d(TAG, "onMetadataChanged");
            updateMovieView(metadata);
        }
    }
}
