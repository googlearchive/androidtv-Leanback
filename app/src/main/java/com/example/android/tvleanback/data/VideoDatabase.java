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
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.database.sqlite.SQLiteQueryBuilder;
import android.media.Rating;
import android.provider.BaseColumns;
import android.util.Log;

import com.example.android.tvleanback.R;
import com.example.android.tvleanback.model.Movie;

import org.json.JSONException;

import java.io.IOException;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Contains logic to return specific words from the video database, and
 * load the video database table when it needs to be created.
 */
public class VideoDatabase {
    //The columns we'll include in the video database table
    public static final String KEY_NAME = SearchManager.SUGGEST_COLUMN_TEXT_1;
    public static final String KEY_DESCRIPTION = SearchManager.SUGGEST_COLUMN_TEXT_2;
    public static final String KEY_ICON = SearchManager.SUGGEST_COLUMN_RESULT_CARD_IMAGE;
    public static final String KEY_DATA_TYPE = SearchManager.SUGGEST_COLUMN_CONTENT_TYPE;
    public static final String KEY_IS_LIVE = SearchManager.SUGGEST_COLUMN_IS_LIVE;
    public static final String KEY_VIDEO_WIDTH = SearchManager.SUGGEST_COLUMN_VIDEO_WIDTH;
    public static final String KEY_VIDEO_HEIGHT = SearchManager.SUGGEST_COLUMN_VIDEO_HEIGHT;
    public static final String KEY_AUDIO_CHANNEL_CONFIG =
            SearchManager.SUGGEST_COLUMN_AUDIO_CHANNEL_CONFIG;
    public static final String KEY_PURCHASE_PRICE = SearchManager.SUGGEST_COLUMN_PURCHASE_PRICE;
    public static final String KEY_RENTAL_PRICE = SearchManager.SUGGEST_COLUMN_RENTAL_PRICE;
    public static final String KEY_RATING_STYLE = SearchManager.SUGGEST_COLUMN_RATING_STYLE;
    public static final String KEY_RATING_SCORE = SearchManager.SUGGEST_COLUMN_RATING_SCORE;
    public static final String KEY_PRODUCTION_YEAR = SearchManager.SUGGEST_COLUMN_PRODUCTION_YEAR;
    public static final String KEY_COLUMN_DURATION = SearchManager.SUGGEST_COLUMN_DURATION;
    public static final String KEY_ACTION = SearchManager.SUGGEST_COLUMN_INTENT_ACTION;
    private static final String TAG = "VideoDatabase";
    private static final String DATABASE_NAME = "video_database_leanback";
    private static final String FTS_VIRTUAL_TABLE = "Leanback_table";
    private static final int DATABASE_VERSION = 2;
    private static final HashMap<String, String> COLUMN_MAP = buildColumnMap();
    private static int CARD_WIDTH = 313;
    private static int CARD_HEIGHT = 176;
    private final VideoDatabaseOpenHelper mDatabaseOpenHelper;

    /**
     * Constructor
     *
     * @param context The Context within which to work, used to create the DB
     */
    public VideoDatabase(Context context) {
        mDatabaseOpenHelper = new VideoDatabaseOpenHelper(context);
    }

    /**
     * Builds a map for all columns that may be requested, which will be given to the
     * SQLiteQueryBuilder. This is a good way to define aliases for column names, but must include
     * all columns, even if the value is the key. This allows the ContentProvider to request
     * columns w/o the need to know real column names and create the alias itself.
     */
    private static HashMap<String, String> buildColumnMap() {
        HashMap<String, String> map = new HashMap<String, String>();
        map.put(KEY_NAME, KEY_NAME);
        map.put(KEY_DESCRIPTION, KEY_DESCRIPTION);
        map.put(KEY_ICON, KEY_ICON);
        map.put(KEY_DATA_TYPE, KEY_DATA_TYPE);
        map.put(KEY_IS_LIVE, KEY_IS_LIVE);
        map.put(KEY_VIDEO_WIDTH, KEY_VIDEO_WIDTH);
        map.put(KEY_VIDEO_HEIGHT, KEY_VIDEO_HEIGHT);
        map.put(KEY_AUDIO_CHANNEL_CONFIG, KEY_AUDIO_CHANNEL_CONFIG);
        map.put(KEY_PURCHASE_PRICE, KEY_PURCHASE_PRICE);
        map.put(KEY_RENTAL_PRICE, KEY_RENTAL_PRICE);
        map.put(KEY_RATING_STYLE, KEY_RATING_STYLE);
        map.put(KEY_RATING_SCORE, KEY_RATING_SCORE);
        map.put(KEY_PRODUCTION_YEAR, KEY_PRODUCTION_YEAR);
        map.put(KEY_COLUMN_DURATION, KEY_COLUMN_DURATION);
        map.put(KEY_ACTION, KEY_ACTION);
        map.put(BaseColumns._ID, "rowid AS " +
                BaseColumns._ID);
        map.put(SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_INTENT_DATA_ID);
        map.put(SearchManager.SUGGEST_COLUMN_SHORTCUT_ID, "rowid AS " +
                SearchManager.SUGGEST_COLUMN_SHORTCUT_ID);
        return map;
    }

    /**
     * Returns a Cursor positioned at the word specified by rowId
     *
     * @param rowId   id of word to retrieve
     * @param columns The columns to include, if null then all are included
     * @return Cursor positioned to matching word, or null if not found.
     */
    public Cursor getWord(String rowId, String[] columns) {
        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE rowid = <rowId>
         */
        String selection = "rowid = ?";
        String[] selectionArgs = new String[]{rowId};

        return query(selection, selectionArgs, columns);
    }

    /**
     * Returns a Cursor over all words that match the first letter of the given query
     *
     * @param query   The string to search for
     * @param columns The columns to include, if null then all are included
     * @return Cursor over all words that match, or null if none found.
     */
    public Cursor getWordMatch(String query, String[] columns) {
        /* This builds a query that looks like:
         *     SELECT <columns> FROM <table> WHERE <KEY_WORD> MATCH 'query*'
         * which is an FTS3 search for the query text (plus a wildcard) inside the word column.
         *
         * - "rowid" is the unique id for all rows but we need this value for the "_id" column in
         *    order for the Adapters to work, so the columns need to make "_id" an alias for "rowid"
         * - "rowid" also needs to be used by the SUGGEST_COLUMN_INTENT_DATA alias in order
         *   for suggestions to carry the proper intent data.SearchManager
         *   These aliases are defined in the VideoProvider when queries are made.
         * - This can be revised to also search the definition text with FTS3 by changing
         *   the selection clause to use FTS_VIRTUAL_TABLE instead of KEY_WORD (to search across
         *   the entire table, but sorting the relevance could be difficult.
         */
        String selection = KEY_NAME + " MATCH ?";
        String[] selectionArgs = new String[]{query + "*"};

        return query(selection, selectionArgs, columns);
    }

    /**
     * Performs a database query.
     *
     * @param selection     The selection clause
     * @param selectionArgs Selection arguments for "?" components in the selection
     * @param columns       The columns to return
     * @return A Cursor over all rows matching the query
     */
    private Cursor query(String selection, String[] selectionArgs, String[] columns) {
        /* The SQLiteBuilder provides a map for all possible columns requested to
         * actual columns in the database, creating a simple column alias mechanism
         * by which the ContentProvider does not need to know the real column names
         */
        SQLiteQueryBuilder builder = new SQLiteQueryBuilder();
        builder.setTables(FTS_VIRTUAL_TABLE);
        builder.setProjectionMap(COLUMN_MAP);

        Cursor cursor = new PaginatedCursor(builder.query(mDatabaseOpenHelper.getReadableDatabase(),
                columns, selection, selectionArgs, null, null, null));

        if (cursor == null) {
            return null;
        } else if (!cursor.moveToFirst()) {
            cursor.close();
            return null;
        }
        return cursor;
    }

    /**
     * This creates/opens the database.
     */
    private static class VideoDatabaseOpenHelper extends SQLiteOpenHelper {

        private final Context mHelperContext;
        private SQLiteDatabase mDatabase;

        VideoDatabaseOpenHelper(Context context) {
            super(context, DATABASE_NAME, null, DATABASE_VERSION);
            mHelperContext = context;
        }

        /* Note that FTS3 does not support column constraints and thus, you cannot
         * declare a primary key. However, "rowid" is automatically used as a unique
         * identifier, so when making requests, we will use "_id" as an alias for "rowid"
         */
        private static final String FTS_TABLE_CREATE =
                "CREATE VIRTUAL TABLE " + FTS_VIRTUAL_TABLE +
                        " USING fts3 (" +
                        KEY_NAME + ", " +
                        KEY_DESCRIPTION + "," +
                        KEY_ICON + "," +
                        KEY_DATA_TYPE + "," +
                        KEY_IS_LIVE + "," +
                        KEY_VIDEO_WIDTH + "," +
                        KEY_VIDEO_HEIGHT + "," +
                        KEY_AUDIO_CHANNEL_CONFIG + "," +
                        KEY_PURCHASE_PRICE + "," +
                        KEY_RENTAL_PRICE + "," +
                        KEY_RATING_STYLE + "," +
                        KEY_RATING_SCORE + "," +
                        KEY_PRODUCTION_YEAR + "," +
                        KEY_COLUMN_DURATION + "," +
                        KEY_ACTION + ");";

        @Override
        public void onCreate(SQLiteDatabase db) {
            mDatabase = db;
            mDatabase.execSQL(FTS_TABLE_CREATE);
            loadDatabase();
        }

        /**
         * Starts a thread to load the database table with words
         */
        private void loadDatabase() {
            new Thread(new Runnable() {
                public void run() {
                    try {
                        loadMovies();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                }
            }).start();
        }

        private void loadMovies() throws IOException {
            Log.d(TAG, "Loading movies...");

            HashMap<String, List<Movie>> movies = null;
            try {
                VideoProvider.setContext(mHelperContext);
                movies = VideoProvider.buildMedia(mHelperContext,
                        mHelperContext.getResources().getString(R.string.catalog_url));
            } catch (JSONException e) {
                Log.e(TAG, "JSon Exception when loading movie", e);
            }

            for (Map.Entry<String, List<Movie>> entry : movies.entrySet()) {
                List<Movie> list = entry.getValue();
                for (Movie movie : list) {
                    long id = addMovie(movie);
                    if (id < 0) {
                        Log.e(TAG, "unable to add movie: " + movie.toString());
                    }
                }
            }
            // add dummy movies to illustrate action deep link in search detail
            // Android TV Search requires that the media’s title, MIME type, production year,
            // and duration all match exactly to those found from Google’s servers.
            addMovieForDeepLink(mHelperContext.getString(R.string.noah_title),
                    mHelperContext.getString(R.string.noah_description),
                    R.drawable.noah,
                    8280000,
                    "2014");
            addMovieForDeepLink(mHelperContext.getString(R.string.dragon2_title),
                    mHelperContext.getString(R.string.dragon2_description),
                    R.drawable.dragon2,
                    6300000,
                    "2014");
            addMovieForDeepLink(mHelperContext.getString(R.string.maleficent_title),
                    mHelperContext.getString(R.string.maleficent_description),
                    R.drawable.maleficent,
                    5820000,
                    "2014");
        }

        /**
         * Add a movie to the database.
         *
         * @return rowId or -1 if failed
         */
        public long addMovie(Movie movie) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_NAME, movie.getTitle());
            initialValues.put(KEY_DESCRIPTION, movie.getDescription());
            initialValues.put(KEY_ICON, movie.getCardImageUrl());
            initialValues.put(KEY_DATA_TYPE, "video/mp4");
            initialValues.put(KEY_IS_LIVE, false);
            initialValues.put(KEY_VIDEO_WIDTH, CARD_WIDTH);
            initialValues.put(KEY_VIDEO_HEIGHT, CARD_HEIGHT);
            initialValues.put(KEY_AUDIO_CHANNEL_CONFIG, "2.0");
            initialValues.put(KEY_PURCHASE_PRICE, mHelperContext.getString(R.string.buy_2));
            initialValues.put(KEY_RENTAL_PRICE, mHelperContext.getString(R.string.rent_2));
            initialValues.put(KEY_RATING_STYLE, Rating.RATING_5_STARS);
            initialValues.put(KEY_RATING_SCORE, 3.5f);
            initialValues.put(KEY_PRODUCTION_YEAR, 2014);
            initialValues.put(KEY_COLUMN_DURATION, 0);
            initialValues.put(KEY_ACTION, mHelperContext.getString(R.string.global_search));
            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        /**
         * Add an entry to the database for dummy deep link.
         *
         * @return rowId or -1 if failed
         */
        public long addMovieForDeepLink(String title, String description, int icon, long duration, String production_year) {
            ContentValues initialValues = new ContentValues();
            initialValues.put(KEY_NAME, title);
            initialValues.put(KEY_DESCRIPTION, description);
            initialValues.put(KEY_ICON, icon);
            initialValues.put(KEY_DATA_TYPE, "video/mp4");
            initialValues.put(KEY_IS_LIVE, false);
            initialValues.put(KEY_VIDEO_WIDTH, 1280);
            initialValues.put(KEY_VIDEO_HEIGHT, 720);
            initialValues.put(KEY_AUDIO_CHANNEL_CONFIG, "2.0");
            initialValues.put(KEY_PURCHASE_PRICE, "Free");
            initialValues.put(KEY_RENTAL_PRICE, "Free");
            initialValues.put(KEY_RATING_STYLE, Rating.RATING_5_STARS);
            initialValues.put(KEY_RATING_SCORE, 3.5f);
            initialValues.put(KEY_PRODUCTION_YEAR, production_year);
            initialValues.put(KEY_COLUMN_DURATION, duration);
            return mDatabase.insert(FTS_VIRTUAL_TABLE, null, initialValues);
        }

        @Override
        public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
            Log.w(TAG, "Upgrading database from version " + oldVersion + " to "
                    + newVersion + ", which will destroy all old data");
            db.execSQL("DROP TABLE IF EXISTS " + FTS_VIRTUAL_TABLE);
            onCreate(db);
        }


    }

}
