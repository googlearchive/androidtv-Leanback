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

package com.example.android.leanback;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.graphics.Bitmap;
import android.os.Bundle;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.squareup.picasso.Picasso;

import java.io.IOException;

/*
 * This class builds recommendations as notifications with videos as inputs.
 */
public class RecommendationBuilder {
    private static final String TAG = "RecommendationBuilder";

    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;

    public static final String EXTRA_BACKGROUND_IMAGE_URL = "background_image_url";
    private Context mContext;
    private NotificationManager mNotificationManager;

    private int mId;
    private int mPriority;
    private int mSmallIcon;
    private String mTitle;
    private String mDescription;
    private String mImageUri;
    private String mBackgroundUri;
    private PendingIntent mIntent;

    public RecommendationBuilder() {
    }

    public RecommendationBuilder setContext(Context context) {
        mContext = context;
        return this;
    }

    public RecommendationBuilder setId(int id) {
        mId = id;
        return this;
    }

    public RecommendationBuilder setPriority(int priority) {
        mPriority = priority;
        return this;
    }

    public RecommendationBuilder setTitle(String title) {
        mTitle = title;
        return this;
    }

    public RecommendationBuilder setDescription(String description) {
        mDescription = description;
        return this;
    }

    public RecommendationBuilder setImage(String uri) {
        mImageUri = uri;
        return this;
    }

    public RecommendationBuilder setBackground(String uri) {
        mBackgroundUri = uri;
        return this;
    }

    public RecommendationBuilder setIntent(PendingIntent intent) {
        mIntent = intent;
        return this;
    }

    public RecommendationBuilder setSmallIcon(int resourceId) {
        mSmallIcon = resourceId;
        return this;
    }

    public Notification build() throws IOException {

        Log.d(TAG, "Building notification - " + this.toString());

        if (mNotificationManager == null) {
            mNotificationManager = (NotificationManager) mContext
                    .getSystemService(Context.NOTIFICATION_SERVICE);
        }

        Bundle extras = new Bundle();
        if (mBackgroundUri != null) {
            extras.putString(EXTRA_BACKGROUND_IMAGE_URL, mBackgroundUri);
        }

        Bitmap image = Picasso.with(mContext)
                .load(mImageUri)
                .resize(Utils.dpToPx(CARD_WIDTH, mContext), Utils.dpToPx(CARD_HEIGHT, mContext))
                .get();

        Notification notification = new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(mContext)
                        .setContentTitle(mTitle)
                        .setContentText(mDescription)
                        .setPriority(mPriority)
                        .setLocalOnly(true)
                        .setOngoing(true)
                        .setColor(mContext.getResources().getColor(R.color.fastlane_background))
                        // .setCategory(Notification.CATEGORY_RECOMMENDATION)
                        .setCategory("recommendation")
                        .setLargeIcon(image)
                        .setSmallIcon(mSmallIcon)
                        .setContentIntent(mIntent)
                        .setExtras(extras))
                .build();

        mNotificationManager.notify(mId, notification);
        mNotificationManager = null;
        return notification;
    }

    @Override
    public String toString() {
        return "RecommendationBuilder{" +
                ", mId=" + mId +
                ", mPriority=" + mPriority +
                ", mSmallIcon=" + mSmallIcon +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mImageUri='" + mImageUri + '\'' +
                ", mBackgroundUri='" + mBackgroundUri + '\'' +
                ", mIntent=" + mIntent +
                '}';
    }
}
