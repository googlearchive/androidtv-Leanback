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
import android.content.res.Resources;
import android.net.Uri;
import android.util.Log;

import com.example.android.tvleanback.R;
import com.example.android.tvleanback.model.Movie;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStreamReader;
import java.io.UnsupportedEncodingException;
import java.net.URLConnection;
import java.net.URLDecoder;
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
    private static HashMap<String, Movie> sMovieListById;

    private static Resources sResources;
    private static Uri sPrefixUrl;

    public static void setContext(Context context) {
        if (null == sResources) {
            sResources = context.getResources();
        }
    }

    public static Movie getMovieById(String mediaId) {
        return sMovieListById.get(mediaId);
    }

    public static HashMap<String, List<Movie>> getMovieList() {
        return sMovieList;
    }

    public static HashMap<String, List<Movie>> buildMedia(Context ctx, String url)
            throws JSONException {
        if (null != sMovieList) {
            return sMovieList;
        }
        sMovieList = new HashMap<>();
        sMovieListById = new HashMap<>();

        JSONObject jsonObj = new VideoProvider().parseUrl(url);

        if (null == jsonObj) {
            Log.e(TAG, "An error occurred fetching videos.");
            return sMovieList;
        }

        JSONArray categories = jsonObj.getJSONArray(TAG_GOOGLE_VIDEOS);

        if (null != categories) {
            final int categoryLength = categories.length();
            Log.d(TAG, "category #: " + categoryLength);
            String title;
            String videoUrl;
            String bgImageUrl;
            String cardImageUrl;
            String studio;
            for (int catIdx = 0; catIdx < categoryLength; catIdx++) {
                JSONObject category = categories.getJSONObject(catIdx);
                String categoryName = category.getString(TAG_CATEGORY);
                JSONArray videos = category.getJSONArray(TAG_MEDIA);
                Log.d(TAG,
                        "category: " + catIdx + " Name:" + categoryName + " video length: "
                                + (null != videos ? videos.length() : 0));
                List<Movie> categoryList = new ArrayList<Movie>();
                Movie movie;
                if (null != videos) {
                    for (int vidIdx = 0, vidSize = videos.length(); vidIdx < vidSize; vidIdx++) {
                        JSONObject video = videos.getJSONObject(vidIdx);
                        String description = video.getString(TAG_DESCRIPTION);
                        JSONArray videoUrls = video.getJSONArray(TAG_SOURCES);
                        if (null == videoUrls || videoUrls.length() == 0) {
                            continue;
                        }
                        title = video.getString(TAG_TITLE);
                        videoUrl = getVideoPrefix(categoryName, getVideoSourceUrl(videoUrls));
                        bgImageUrl = getThumbPrefix(categoryName, title,
                                video.getString(TAG_BACKGROUND));
                        cardImageUrl = getThumbPrefix(categoryName, title,
                                video.getString(TAG_CARD_THUMB));
                        studio = video.getString(TAG_STUDIO);

                        movie = buildMovieInfo(categoryName, title, description, studio,
                                videoUrl, cardImageUrl, bgImageUrl);
                        sMovieListById.put(movie.getId(), movie);
                        categoryList.add(movie);
                    }
                    sMovieList.put(categoryName, categoryList);
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

    // workaround for partially pre-encoded sample data
    private static String getVideoSourceUrl(final JSONArray videos) throws JSONException {
        try {
            final String url = videos.getString(0);
            return (-1) == url.indexOf('%') ? url : URLDecoder.decode(url, "UTF-8");
        } catch (UnsupportedEncodingException e) {
            throw new JSONException("Broken VM: no UTF-8");
        }
    }

    private static String getVideoPrefix(String category, String videoUrl) {
        return sPrefixUrl.buildUpon()
                .appendPath(category)
                .appendPath(videoUrl)
                .toString();
    }

    private static String getThumbPrefix(String category, String title, String imageUrl) {
        return sPrefixUrl.buildUpon()
                .appendPath(category)
                .appendPath(title)
                .appendPath(imageUrl)
                .toString();
    }

    protected JSONObject parseUrl(String urlString) {
        Log.d(TAG, "Parse URL: " + urlString);
        BufferedReader reader = null;

        sPrefixUrl = Uri.parse(sResources.getString(R.string.prefix_url));

        try {
            java.net.URL url = new java.net.URL(urlString);
            URLConnection urlConnection = url.openConnection();
            reader = new BufferedReader(new InputStreamReader(
                    urlConnection.getInputStream(), "iso-8859-1"));
            StringBuilder sb = new StringBuilder();
            String line;
            while ((line = reader.readLine()) != null) {
                sb.append(line);
            }
            String json = sb.toString();
            return new JSONObject(json);
        } catch (Exception e) {
            Log.e(TAG, "Failed to parse the json for media list", e);
            return null;
        } finally {
            if (null != reader) {
                try {
                    reader.close();
                } catch (IOException e) {
                    Log.d(TAG, "JSON feed closed", e);
                }
            }
        }
    }
}
