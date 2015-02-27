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
import android.app.PendingIntent;
import android.app.TaskStackBuilder;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
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
 * This class builds up to MAX_RECOMMMENDATIONS of recommendations and defines what happens
 * when they're clicked from Recommendations section on Home screen
 */
public class UpdateRecommendationsService extends IntentService {
    private static final String TAG = "RecommendationsService";
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

        RecommendationBuilder builder = new RecommendationBuilder()
                .setContext(getApplicationContext())
                .setSmallIcon(R.drawable.videos_by_google_icon);

        // flatten to list
        List flattenedRecommendations = new ArrayList();
        for (Map.Entry<String, List<Movie>> entry : recommendations.entrySet()) {
            for (Movie movie : entry.getValue()) {
                Log.d(TAG, "Recommendation - " + movie.getTitle());
                flattenedRecommendations.add(movie);
            }
        }

        Collections.shuffle(flattenedRecommendations);
        Movie movie;
        for (int i = 0; i < flattenedRecommendations.size() && i < MAX_RECOMMENDATIONS; i++) {
            movie = (Movie) flattenedRecommendations.get(i);
            final RecommendationBuilder notificationBuilder = builder
                    .setBackground(movie.getCardImageUrl())
                    .setId(i+1)
                    .setPriority(MAX_RECOMMENDATIONS - i - 1)
                    .setTitle(movie.getTitle())
                    .setDescription(getString(R.string.popular_header))
                    .setIntent(buildPendingIntent(movie, i + 1));

            try {
                Bitmap bitmap = Glide.with(getApplicationContext())
                        .load(movie.getCardImageUrl())
                        .asBitmap()
                        .into(CARD_WIDTH, CARD_HEIGHT) // Only use for synchronous .get()
                        .get();
                notificationBuilder.setBitmap(bitmap);
                Notification notification = notificationBuilder.build();
                mNotificationManager.notify(i + 1, notification);
            } catch (InterruptedException | ExecutionException e) {
                Log.e(TAG, "Could not create recommendation: " + e);
            }
        }
    }

    private PendingIntent buildPendingIntent(Movie movie, int id) {
        Intent detailsIntent = new Intent(this, MovieDetailsActivity.class);
        detailsIntent.putExtra(MovieDetailsActivity.MOVIE, movie);
        detailsIntent.putExtra(MovieDetailsActivity.NOTIFICATION_ID, id);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MovieDetailsActivity.class);
        stackBuilder.addNextIntent(detailsIntent);
        // Ensure a unique PendingIntents, otherwise all recommendations end up with the same
        // PendingIntent
        detailsIntent.setAction(movie.getId());

        return stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
    }
}
