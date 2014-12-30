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

package com.example.android.tvleanback;

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

import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.ExecutionException;

/*
 * This class builds up to MAX_RECOMMMENDATIONS of recommendations and defines what happens
 * when they're clicked from Recommendations section on Home screen
 */
public class UpdateRecommendationsService extends IntentService {
    private static final String TAG = "UpdateRecommendationsService";
    private static final int MAX_RECOMMENDATIONS = 3;

    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;

    private NotificationManager mNotificationManager;

    public UpdateRecommendationsService() {
        super("RecommendationService");
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

        int count = 0;
        for (Map.Entry<String, List<Movie>> entry : recommendations.entrySet()) {
            for (Movie movie : entry.getValue()) {
                Log.d(TAG, "Recommendation - " + movie.getTitle());

                final int id = count + 1;
                final RecommendationBuilder notificationBuilder = builder.setBackground(movie.getCardImageUrl())
                        .setId(id)
                        .setPriority(MAX_RECOMMENDATIONS - count)
                        .setTitle(movie.getTitle())
                        .setDescription(getString(R.string.popular_header))
                        .setIntent(buildPendingIntent(movie));

                try {
                    Bitmap bitmap = Glide.with(getApplicationContext())
                            .load(movie.getCardImageUrl())
                            .asBitmap()
                            .into(CARD_WIDTH, CARD_HEIGHT) // Only use for synchronous .get()
                            .get();
                    notificationBuilder.setBitmap(bitmap);
                    Notification notification = notificationBuilder.build();
                    mNotificationManager.notify(id, notification);
                } catch (InterruptedException e) {
                    Log.e(TAG, "Could not create recommendation: " + e);
                } catch (ExecutionException e) {
                    Log.e(TAG, "Could not create recommendation: " + e);
                }

                if (++count >= MAX_RECOMMENDATIONS) {
                    break;
                }
            }
        }
    }

    private PendingIntent buildPendingIntent(Movie movie) {
        Intent detailsIntent = new Intent(this, DetailsActivity.class);
        detailsIntent.putExtra("Movie", movie);

        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(DetailsActivity.class);
        stackBuilder.addNextIntent(detailsIntent);
        // Ensure a unique PendingIntents, otherwise all recommendations end up with the same
        // PendingIntent
        detailsIntent.setAction(Long.toString(movie.getId()));

        PendingIntent intent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);
        return intent;
    }
}
