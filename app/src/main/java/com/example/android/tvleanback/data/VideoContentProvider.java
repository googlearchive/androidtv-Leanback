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

package com.example.android.tvleanback.data;

import android.app.SearchManager;
import android.content.ContentProvider;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.UriMatcher;
import android.database.Cursor;
import android.net.Uri;
import android.provider.BaseColumns;
import android.util.Log;

/**
 * Provides access to the video database.
 */
public class VideoContentProvider extends ContentProvider {
    private static String TAG = "VideoContentProvider";
    public static String AUTHORITY = "com.example.android.tvleanback";

    // UriMatcher stuff
    private static final int SEARCH_SUGGEST = 0;
    private static final int REFRESH_SHORTCUT = 1;
    private static final UriMatcher URI_MATCHER = buildUriMatcher();

    private VideoDatabase mVideoDatabase;

    /**
     * Builds up a UriMatcher for search suggestion and shortcut refresh queries.
     */
    private static UriMatcher buildUriMatcher() {
        UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        // to get suggestions...
        Log.d(TAG, "suggest_uri_path_query: " + SearchManager.SUGGEST_URI_PATH_QUERY);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY, SEARCH_SUGGEST);
        matcher.addURI(AUTHORITY, SearchManager.SUGGEST_URI_PATH_QUERY + "/*", SEARCH_SUGGEST);
        return matcher;
    }

    @Override
    public boolean onCreate() {
        Log.d(TAG, "onCreate");
        mVideoDatabase = new VideoDatabase(getContext());
        return true;
    }

    /**
     * Handles all the video searches and suggestion queries from the Search Manager.
     * When requesting a specific word, the uri alone is required.
     * When searching all of the video for matches, the selectionArgs argument must carry
     * the search query as the first element.
     * All other arguments are ignored.
     */
    @Override
    public Cursor query(Uri uri, String[] projection, String selection, String[] selectionArgs,
                        String sortOrder) {
        // Use the UriMatcher to see what kind of query we have and format the db query accordingly
        switch (URI_MATCHER.match(uri)) {
            case SEARCH_SUGGEST:
                Log.d(TAG, "search suggest: " + selectionArgs[0] + " URI: " + uri);
                if (selectionArgs == null) {
                    throw new IllegalArgumentException(
                            "selectionArgs must be provided for the Uri: " + uri);
                }
                return getSuggestions(selectionArgs[0]);
            default:
                throw new IllegalArgumentException("Unknown Uri: " + uri);
        }
    }

    private Cursor getSuggestions(String query) {
        query = query.toLowerCase();
        String[] columns = new String[]{
                BaseColumns._ID,
                VideoDatabase.KEY_NAME,
                VideoDatabase.KEY_DESCRIPTION,
                VideoDatabase.KEY_ICON,
                VideoDatabase.KEY_DATA_TYPE,
                VideoDatabase.KEY_IS_LIVE,
                VideoDatabase.KEY_VIDEO_WIDTH,
                VideoDatabase.KEY_VIDEO_HEIGHT,
                VideoDatabase.KEY_AUDIO_CHANNEL_CONFIG,
                VideoDatabase.KEY_PURCHASE_PRICE,
                VideoDatabase.KEY_RENTAL_PRICE,
                VideoDatabase.KEY_RATING_STYLE,
                VideoDatabase.KEY_RATING_SCORE,
                VideoDatabase.KEY_PRODUCTION_YEAR,
                VideoDatabase.KEY_COLUMN_DURATION,
                VideoDatabase.KEY_ACTION,
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID
        };
        return mVideoDatabase.getWordMatch(query, columns);
    }

    /**
     * This method is required in order to query the supported types.
     * It's also useful in our own query() method to determine the type of Uri received.
     */
    @Override
    public String getType(Uri uri) {
        switch (URI_MATCHER.match(uri)) {
            case SEARCH_SUGGEST:
                return SearchManager.SUGGEST_MIME_TYPE;
            case REFRESH_SHORTCUT:
                return SearchManager.SHORTCUT_MIME_TYPE;
            default:
                throw new IllegalArgumentException("Unknown URL " + uri);
        }
    }

    // Other required implementations...

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        throw new UnsupportedOperationException();
    }
}
