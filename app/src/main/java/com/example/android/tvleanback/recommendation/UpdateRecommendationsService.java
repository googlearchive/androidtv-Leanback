/*
 * Copyright (C) 2015 The Android Open Source Project
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

package com.example.android.tvleanback.recommendation;

import android.app.IntentService;
import android.app.Notification;
import android.app.NotificationManager;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.support.app.recommendation.ContentRecommendation;
import android.util.Log;

import com.bumptech.glide.Glide;
import com.example.android.tvleanback.R;
import com.example.android.tvleanback.data.VideoProvider;
import com.example.android.tvleanback.model.Movie;
import com.example.android.tvleanback.ui.MovieDetailsActivity;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/*
 * This class builds up to MAX_RECOMMMENDATIONS of ContentRecommendations and defines what happens
 * when they're selected from Recommendations section on the Home screen by creating an Intent.
 */
public class UpdateRecommendationsService extends IntentService {
    private static final String TAG = "RecommendationService";
    private static final int MAX_RECOMMENDATIONS = 3;

    private static final int CARD_WIDTH = 313;
    private static final int CARD_HEIGHT = 176;

    private NotificationManager mNotificationManager;

    public UpdateRecommendationsService() {
        super(TAG);
    }

    @Override
    protected void onHandleIntent(Intent intent) {
        Log.d(TAG, "Updating recommendation cards");
        HashMap<String, List<Movie>> recommendations = VideoProvider.getMovieList();
        if (recommendations == null) {
            return;
        }

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) getApplicationContext()
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        // This will be used to build up an object for your content recommendation that will be
        // shown on the TV home page along with other provider's recommendations.
        ContentRecommendation.Builder builder = new ContentRecommendation.Builder()
                .setBadgeIcon(R.drawable.videos_by_google_icon);

        // flatten to list
        List<Movie> flattenedRecommendations = new ArrayList<>();
        for (Map.Entry<String, List<Movie>> entry : recommendations.entrySet()) {
            for (Movie movie : entry.getValue()) {
                flattenedRecommendations.add(movie);
            }
        }

        // Our naive approach to deciding what content to recommend involves simply shuffling all
        // the videos in our app and picking up to MAX_RECOMMENDATIONS of them.
        Collections.shuffle(flattenedRecommendations);

        Movie movie;
        for (int i = 0; i < flattenedRecommendations.size() && i < MAX_RECOMMENDATIONS; i++) {
            movie = flattenedRecommendations.get(i);
            builder.setIdTag("Video" + i + 1)
                    .setTitle(movie.getTitle())
                    .setText(getString(R.string.popular_header))
                    .setContentIntentData(ContentRecommendation.INTENT_TYPE_ACTIVITY,
                            buildPendingIntent(movie, i + 1), 0, null);

            try {
                // No ContentRecommendation is complete without an image.
                Bitmap bitmap = Glide.with(getApplicationContext())
                        .load(movie.getCardImageUrl())
                        .asBitmap()
                        .into(CARD_WIDTH, CARD_HEIGHT) // Only use for synchronous .get()
                        .get();
                builder.setContentImage(bitmap);

                // Create an object holding all the information used to recommend the content.
                ContentRecommendation rec = builder.build();
                Notification notification = rec.getNotificationObject(getApplicationContext());

                Log.d(TAG, "Recommending video");

                // Recommend the content by publishing the notification.
                mNotificationManager.notify(i + 1, notification);
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Could not create recommendation: " + e);
            }
        }
    }

    private Intent buildPendingIntent(Movie movie, int id) {
        Intent detailsIntent = new Intent(this, MovieDetailsActivity.class);
        detailsIntent.putExtra(MovieDetailsActivity.MOVIE, movie);
        detailsIntent.putExtra(MovieDetailsActivity.NOTIFICATION_ID, id);
        detailsIntent.setAction(movie.getId());

        return detailsIntent;
    }
}
