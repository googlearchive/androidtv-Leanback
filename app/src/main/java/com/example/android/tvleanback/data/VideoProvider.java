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

import android.content.Context;
import android.util.Log;

import com.example.android.tvleanback.R;
import com.example.android.tvleanback.model.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedInputStream;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;

/*
 * This class loads videos from a backend and saves them into a HashMap
 */
public class VideoProvider {

    private static final String TAG = "VideoProvider";
    private static String TAG_MEDIA = "videos";
    private static String TAG_GOOGLE_VIDEOS = "googlevideos";
    private static String TAG_CATEGORY = "category";
    private static String TAG_STUDIO = "studio";
    private static String TAG_SOURCES = "sources";
    private static String TAG_DESCRIPTION = "description";
    private static String TAG_CARD_THUMB = "card";
    private static String TAG_BACKGROUND = "background";
    private static String TAG_TITLE = "title";

    private static HashMap<String, List<Movie>> sMovieList;
    private static Context sContext;
    private static String sPrefixUrl;

    public static void setContext(Context context) {
        if (sContext == null)
            sContext = context;
    }

    public static HashMap<String, List<Movie>> getMovieList() {
        return sMovieList;
    }

    public static HashMap<String, List<Movie>> buildMedia(Context ctx, String url)
            throws JSONException {
        if (null != sMovieList) {
            return sMovieList;
        }
        sMovieList = new HashMap<String, List<Movie>>();

        JSONObject jsonObj = new VideoProvider().parseUrl(url);
        JSONArray categories = jsonObj.getJSONArray(TAG_GOOGLE_VIDEOS);
        if (null != categories) {
            Log.d(TAG, "category #: " + categories.length());
            String title = new String();
            String videoUrl = new String();
            String bgImageUrl = new String();
            String cardImageUrl = new String();
            String studio = new String();
            for (int i = 0; i < categories.length(); i++) {
                JSONObject category = categories.getJSONObject(i);
                String category_name = category.getString(TAG_CATEGORY);
                JSONArray videos = category.getJSONArray(TAG_MEDIA);
                Log.d(TAG,
                        "category: " + i + " Name:" + category_name + " video length: "
                                + videos.length());
                List<Movie> categoryList = new ArrayList<Movie>();
                if (null != videos) {
                    for (int j = 0; j < videos.length(); j++) {
                        JSONObject video = videos.getJSONObject(j);
                        String description = video.getString(TAG_DESCRIPTION);
                        JSONArray videoUrls = video.getJSONArray(TAG_SOURCES);
                        if (null == videoUrls || videoUrls.length() == 0) {
                            continue;
                        }
                        title = video.getString(TAG_TITLE);
                        videoUrl = getVideoPrefix(category_name, videoUrls.getString(0));
                        bgImageUrl = getThumbPrefix(category_name, title,
                                video.getString(TAG_BACKGROUND));
                        cardImageUrl = getThumbPrefix(category_name, title,
                                video.getString(TAG_CARD_THUMB));
                        studio = video.getString(TAG_STUDIO);
                        categoryList.add(buildMovieInfo(category_name, title, description, studio,
                                videoUrl, cardImageUrl,
                                bgImageUrl));
                    }
                    sMovieList.put(category_name, categoryList);
                }
            }
        }
        return sMovieList;
    }

    private static Movie buildMovieInfo(String category,
                                        String title,
                                        String description,
                                        String studio,
                                        String videoUrl,
                                        String cardImageUrl,
                                        String bgImageUrl) {
        Movie movie = new Movie();
        movie.setId(Movie.getCount());
        Movie.incrementCount();
        movie.setTitle(title);
        movie.setDescription(description);
        movie.setStudio(studio);
        movie.setCategory(category);
        movie.setCardImageUrl(cardImageUrl);
        movie.setBackgroundImageUrl(bgImageUrl);
        movie.setVideoUrl(videoUrl);

        return movie;
    }

    private static String getVideoPrefix(String category, String videoUrl) {
        String ret = "";
        ret = sPrefixUrl + category.replace(" ", "%20") + '/' +
                videoUrl.replace(" ", "%20");
        return ret;
    }

    private static String getThumbPrefix(String category, String title, String imageUrl) {
        String ret = "";

        ret = sPrefixUrl + category.replace(" ", "%20") + '/' +
                title.replace(" ", "%20") + '/' +
                imageUrl.replace(" ", "%20");
        return ret;
    }

    protected JSONObject parseUrl(String urlString) {
        Log.d(TAG, "Parse URL: " + urlString);
        InputStream is = null;

        sPrefixUrl = sContext.getResources().getString(R.string.prefix_url);

        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection urlConnection = url.openConnection();
            is = new BufferedInputStream(urlConnection.getInputStream());
            BufferedReader reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"), 8);
            StringBuilder sb = new StringBuilder();
            String line = null;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.d(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != is) {
                try {
                    is.close();
                } catch (IOException e) {
                    Log.d(TAG, "JSON feed closed", e);
                }
            }
        }
    }
}
