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

import android.app.Notification;
import android.app.PendingIntent;
import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.net.Uri;
import android.os.Bundle;
import android.os.ParcelFileDescriptor;
import android.support.v4.app.NotificationCompat;
import android.util.Log;

import com.example.android.tvleanback.R;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.FileOutputStream;
import java.io.IOException;

/*
 * This class builds recommendations as notifications with videos as inputs.
 */
public class RecommendationBuilder {
    private static final String TAG = "RecommendationBuilder";
    private static final String
            BACKGROUND_URI_PREFIX = "content://com.example.android.tvleanback.recommendation/";

    private Context mContext;

    private int mId;
    private int mPriority;
    private int mSmallIcon;
    private String mTitle;
    private String mDescription;
    private Bitmap mBitmap;
    private String mBackgroundUri;
    private String mGroupKey;
    private String mSort;
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

    public RecommendationBuilder setBitmap(Bitmap bitmap) {
        mBitmap = bitmap;
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

    public Notification build() {

        Bundle extras = new Bundle();
        File bitmapFile = getNotificationBackground(mContext, mId);

        if (mBackgroundUri != null) {
            extras.putString(Notification.EXTRA_BACKGROUND_IMAGE_URI,
                    Uri.parse(BACKGROUND_URI_PREFIX + Integer.toString(mId)).toString());
        }

        // the following simulates group assignment into "Top", "Middle", "Bottom"
        // by checking mId and similarly sort order
        mGroupKey = (mId < 3) ? "Top" : (mId < 5) ? "Middle" : "Bottom";
        mSort = (mId < 3) ? "1.0" : (mId < 5) ? "0.7" : "0.3";

        // save bitmap into files for content provider to serve later
        try {
            bitmapFile.createNewFile();
            FileOutputStream fOut = new FileOutputStream(bitmapFile);
            mBitmap.compress(Bitmap.CompressFormat.PNG, 85, fOut);
            fOut.flush();
            fOut.close();
        } catch (IOException ioe) {
            Log.d(TAG, "Exception caught writing bitmap to file!", ioe);
        }

        Notification notification = new NotificationCompat.BigPictureStyle(
                new NotificationCompat.Builder(mContext)
                        .setAutoCancel(true)
                        .setContentTitle(mTitle)
                        .setContentText(mDescription)
                        .setPriority(mPriority)
                        .setLocalOnly(true)
                        .setOngoing(true)
                        /*
                        groupKey (optional): Can be used to group together recommendations, so
                        they are ranked by the launcher as a separate group. Can be useful if the
                        application has different sources for recommendations, like "trending",
                        "subscriptions", and "new music" categories for YouTube, where the user can
                        be more interested in recommendations from one group than another.
                         */
                        .setGroup(mGroupKey)
                        /*
                        sortKey (optional): A float number between 0.0 and 1.0, used to indicate
                        the relative importance (and sort order) of a single recommendation within
                        its specified group. The recommendations will be ordered in decreasing
                        order of importance within a given group.
                         */
                        .setSortKey(mSort)
                        .setColor(mContext.getResources().getColor(R.color.fastlane_background))
                        .setCategory(Notification.CATEGORY_RECOMMENDATION)
                        .setLargeIcon(mBitmap)
                        .setSmallIcon(mSmallIcon)
                        .setContentIntent(mIntent)
                        .setExtras(extras))
                .build();

        Log.d(TAG, "Building notification - " + this.toString());

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
                ", mBitmap='" + mBitmap + '\'' +
                ", mBackgroundUri='" + mBackgroundUri + '\'' +
                ", mIntent=" + mIntent +
                '}';
    }

    public static class RecommendationBackgroundContentProvider extends ContentProvider {

        @Override
        public boolean onCreate() {
            return true;
        }

        @Override
        public int delete(Uri uri, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        public String getType(Uri uri) {
            return null;
        }

        @Override
        public Uri insert(Uri uri, ContentValues values) {
            return null;
        }

        @Override
        public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                            String sortOrder) {
            return null;
        }

        @Override
        public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
            return 0;
        }

        @Override
        /*
         * content provider serving files that are saved locally when recommendations are built
         */
        public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
            int backgroundId = Integer.parseInt(uri.getLastPathSegment());
            File bitmapFile = getNotificationBackground(getContext(), backgroundId);
            return ParcelFileDescriptor.open(bitmapFile, ParcelFileDescriptor.MODE_READ_ONLY);
        }
    }

    private static File getNotificationBackground(Context context, int notificationId) {
        return new File(context.getCacheDir(), "tmp" + Integer.toString(notificationId) + ".png");
    }

}
