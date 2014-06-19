/*
 * Copyright (C) 2013 Google Inc. All Rights Reserved. 
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

package com.example.android.leanback;

import java.util.HashMap;
import java.util.List;

import android.content.AsyncTaskLoader;
import android.content.Context;
import android.util.Log;

/*
 * This class asynchronously loads videos from a backend
 */
public class VideoItemLoader extends AsyncTaskLoader<HashMap<String, List<Movie>>> {

    private static final String TAG = "VideoItemLoader";
    private final String mUrl;
    private Context mContext;

    public VideoItemLoader(Context context, String url) {
        super(context);
        mContext = context;
        mUrl = url;
    }

    @Override
    public HashMap<String, List<Movie>> loadInBackground() {
        try {
            return VideoProvider.buildMedia(mContext, mUrl);
        } catch (Exception e) {
            Log.e(TAG, "Failed to fetch media data", e);
            return null;
        }
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        forceLoad();
    }

    /**
     * Handles a request to stop the Loader.
     */
    @Override
    protected void onStopLoading() {
        // Attempt to cancel the current load task if possible.
        cancelLoad();
    }

}
