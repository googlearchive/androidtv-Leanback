/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */
package com.example.android.tvleanback.ui;

import android.app.Activity;
import android.content.Intent;
import android.graphics.Bitmap;
import android.media.MediaMetadata;
import android.media.MediaPlayer;
import android.media.session.MediaController;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.Bundle;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.FrameLayout;
import android.widget.VideoView;

import com.bumptech.glide.Glide;
import com.bumptech.glide.request.animation.GlideAnimation;
import com.bumptech.glide.request.target.SimpleTarget;
import com.example.android.tvleanback.R;
import com.example.android.tvleanback.Utils;
import com.example.android.tvleanback.data.VideoProvider;
import com.example.android.tvleanback.model.Movie;

import java.util.Collection;
import java.util.List;

/**
 * PlaybackOverlayActivity for video playback that loads PlaybackOverlayFragment
 */
public class PlaybackActivity extends Activity {
    private static final String TAG = "PlaybackOverlayActivity";

    private static final double MEDIA_HEIGHT = 0.95;
    private static final double MEDIA_WIDTH = 0.95;
    private static final double MEDIA_TOP_MARGIN = 0.025;
    private static final double MEDIA_RIGHT_MARGIN = 0.025;
    private static final double MEDIA_BOTTOM_MARGIN = 0.025;
    private static final double MEDIA_LEFT_MARGIN = 0.025;
    public static final String AUTO_PLAY = "auto_play";
    private VideoView mVideoView;
    private LeanbackPlaybackState mPlaybackState = LeanbackPlaybackState.IDLE;
    private MediaSession mSession;
    private String mCurrentVideoPath;
    private long mDuration = -1;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        createMediaSession();

        setContentView(R.layout.playback_controls);
        loadViews();
        //Example for handling resizing view for overscan
        //overScan();
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        mVideoView.suspend();
        mSession.release();
    }


    private void createMediaSession() {
        if (mSession == null) {
            mSession = new MediaSession(this, "LeanbackSampleApp");
            mSession.setCallback(new MediaSessionCallback());
            mSession.setFlags(MediaSession.FLAG_HANDLES_MEDIA_BUTTONS |
                    MediaSession.FLAG_HANDLES_TRANSPORT_CONTROLS);

            mSession.setActive(true);

            setMediaController(new MediaController(this, mSession.getSessionToken()));
        }
    }

    private void playPause(boolean doPlay) {
        if (mPlaybackState == LeanbackPlaybackState.IDLE) {
            setupCallbacks();
        }

        if (doPlay && mPlaybackState != LeanbackPlaybackState.PLAYING) {
            mPlaybackState = LeanbackPlaybackState.PLAYING;
            mVideoView.start();
        } else {
            mPlaybackState = LeanbackPlaybackState.PAUSED;
            mVideoView.pause();
        }
        updatePlaybackState();
    }


    private void updatePlaybackState() {
        PlaybackState.Builder stateBuilder = new PlaybackState.Builder()
                .setActions(getAvailableActions());
        int state = PlaybackState.STATE_PLAYING;
        if (mPlaybackState == LeanbackPlaybackState.PAUSED || mPlaybackState == LeanbackPlaybackState.IDLE) {
            state = PlaybackState.STATE_PAUSED;
        }
        stateBuilder.setState(state, mVideoView.getCurrentPosition(), 1.0f);
        mSession.setPlaybackState(stateBuilder.build());
    }

    private long getAvailableActions() {
        long actions = PlaybackState.ACTION_PLAY |
                PlaybackState.ACTION_PLAY_FROM_MEDIA_ID |
                PlaybackState.ACTION_PLAY_FROM_SEARCH;

        if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
            actions |= PlaybackState.ACTION_PAUSE;
        }

        return actions;
    }

    private void updateMetadata(final Movie movie) {
        final MediaMetadata.Builder metadataBuilder = new MediaMetadata.Builder();

        String title = movie.getTitle().replace("_", " -");

        metadataBuilder.putString(MediaMetadata.METADATA_KEY_MEDIA_ID, movie.getId());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_TITLE, title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_SUBTITLE,
                movie.getStudio());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_DESCRIPTION,
                movie.getDescription());
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_DISPLAY_ICON_URI,
                movie.getCardImageUrl());
        metadataBuilder.putLong(MediaMetadata.METADATA_KEY_DURATION, mDuration);

        // And at minimum the title and artist for legacy support
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_TITLE, title);
        metadataBuilder.putString(MediaMetadata.METADATA_KEY_ARTIST, movie.getStudio());

        Glide.with(this)
            .load(Uri.parse(movie.getCardImageUrl()))
            .asBitmap()
            .into(new SimpleTarget<Bitmap>(500, 500) {
                @Override
                public void onResourceReady(Bitmap bitmap, GlideAnimation anim) {
                    metadataBuilder.putBitmap(MediaMetadata.METADATA_KEY_ART, bitmap);
                    mSession.setMetadata(metadataBuilder.build());
                }
            });
    }

    private void loadViews() {
        mVideoView = (VideoView) findViewById(R.id.videoView);
        mVideoView.setFocusable(false);
        mVideoView.setFocusableInTouchMode(false);

        Movie movie = (Movie)getIntent().getParcelableExtra(MovieDetailsActivity.MOVIE);
        setVideoPath(movie.getVideoUrl());
        updateMetadata(movie);
    }

    /**
     * Example for handling resizing content for overscan.  Typically you won't need to resize which
     * is why overScan(); is commented out.
     */
    private void overScan() {
        DisplayMetrics metrics = new DisplayMetrics();
        getWindowManager().getDefaultDisplay().getMetrics(metrics);
        int w = (int) (metrics.widthPixels * MEDIA_WIDTH);
        int h = (int) (metrics.heightPixels * MEDIA_HEIGHT);
        int marginLeft = (int) (metrics.widthPixels * MEDIA_LEFT_MARGIN);
        int marginTop = (int) (metrics.heightPixels * MEDIA_TOP_MARGIN);
        int marginRight = (int) (metrics.widthPixels * MEDIA_RIGHT_MARGIN);
        int marginBottom = (int) (metrics.heightPixels * MEDIA_BOTTOM_MARGIN);
        FrameLayout.LayoutParams lp = new FrameLayout.LayoutParams(w, h);
        lp.setMargins(marginLeft, marginTop, marginRight, marginBottom);
        mVideoView.setLayoutParams(lp);
    }

    private void setupCallbacks() {

        mVideoView.setOnErrorListener(new MediaPlayer.OnErrorListener() {

            @Override
            public boolean onError(MediaPlayer mp, int what, int extra) {
                String msg = "";
                if (extra == MediaPlayer.MEDIA_ERROR_TIMED_OUT) {
                    msg = getString(R.string.video_error_media_load_timeout);
                } else if (what == MediaPlayer.MEDIA_ERROR_SERVER_DIED) {
                    msg = getString(R.string.video_error_server_inaccessible);
                } else {
                    msg = getString(R.string.video_error_unknown_error);
                }
                mVideoView.stopPlayback();
                mPlaybackState = LeanbackPlaybackState.IDLE;
                return false;
            }
        });


        mVideoView.setOnPreparedListener(new MediaPlayer.OnPreparedListener() {
            @Override
            public void onPrepared(MediaPlayer mp) {
                if (mPlaybackState == LeanbackPlaybackState.PLAYING) {
                    mVideoView.start();
                }
            }
        });


        mVideoView.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
            @Override
            public void onCompletion(MediaPlayer mp) {
                mPlaybackState = LeanbackPlaybackState.IDLE;
            }
        });

    }

    @Override
    public void onResume() {
        super.onResume();
    }

    @Override
    public void onPause() {
        super.onPause();
        if (mVideoView.isPlaying()) {
            if (!requestVisibleBehind(true)) {
                // Try to play behind launcher, but if it fails, stop playback.
                stopPlayback();
            }
        } else {
            requestVisibleBehind(false);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        Log.d(TAG, "pausing playback in onStop");
        pausePlayback();
    }



    @Override
    public void onVisibleBehindCanceled() {
        Log.d(TAG, "pausing playback in onVisibleBehindCanceled");
        pausePlayback();
        super.onVisibleBehindCanceled();
    }

    private void pausePlayback() {
        mVideoView.pause();
        mPlaybackState = LeanbackPlaybackState.PAUSED;
        updatePlaybackState();
    }

    private void stopPlayback() {
        if (mVideoView != null) {
            mVideoView.stopPlayback();
        }
    }

    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }

    /*
     * List of various states that we can be in
     */
    public static enum LeanbackPlaybackState {
        PLAYING, PAUSED, BUFFERING, IDLE;
    }

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
        public void onPlayFromMediaId(String mediaId, Bundle extras) {
            Movie movie = getMovieById(mediaId);

            setVideoPath(movie.getVideoUrl());

            mPlaybackState = LeanbackPlaybackState.PAUSED;
            updateMetadata(movie);
            playPause(extras.getBoolean(AUTO_PLAY));
        }

        @Override
        public void onSeekTo(long pos) {
            mVideoView.seekTo((int) pos);
            updatePlaybackState();
        }

        @Override
        public void onFastForward() {
            if (mDuration != -1) {
                // Fast forward 10 seconds.
                int newPosition = mVideoView.getCurrentPosition() + (10 * 1000);
                if (newPosition > mDuration) {
                    newPosition = (int) mDuration;
                }
                mVideoView.seekTo(newPosition);
                updatePlaybackState();
            }
        }

        @Override
        public void onRewind() {
            Log.d(TAG, "received fastForward in MediaSession.Callback");
            // rewind 10 seconds
            int newPosition = mVideoView.getCurrentPosition() - (10 * 1000);
            if (newPosition < 0) {
                newPosition = 0;
            }
            mVideoView.seekTo(newPosition);
            updatePlaybackState();
        }
    }

    private void setVideoPath(String videoUrl) {
        mVideoView.setVideoPath(videoUrl);
        mCurrentVideoPath = videoUrl;
        mDuration = Utils.getDuration(videoUrl);
    }

    private Movie getMovieById(String mediaId) {
        Collection<List<Movie>> movies = VideoProvider.getMovieList().values();
        for (List<Movie> category :movies) {
            for (Movie movie: category) {
                if (movie.getId().equals(mediaId)) {
                    return movie;
                }
            }
        }
        return null;
    }
}
