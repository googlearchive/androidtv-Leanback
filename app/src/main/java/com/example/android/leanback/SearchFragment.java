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

import java.util.HashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.os.Bundle;
import android.os.Handler;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.ObjectAdapter;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.Row;
import android.text.TextUtils;
import android.util.Log;

/*
 * This class demonstrates how to do in-app search 
 */
@SuppressLint("DefaultLocale")
public class SearchFragment extends android.support.v17.leanback.app.SearchFragment
        implements android.support.v17.leanback.app.SearchFragment.SearchResultProvider {
    private static final String TAG = "SearchFragment";
    private static final int SEARCH_DELAY_MS = 300;

    private ArrayObjectAdapter mRowsAdapter;
    private Handler mHandler = new Handler();
    private SearchRunnable mDelayedLoad;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mRowsAdapter = new ArrayObjectAdapter(new ListRowPresenter());
        setSearchResultProvider(this);
        setOnItemClickedListener(getDefaultItemClickedListener());
        mDelayedLoad = new SearchRunnable();
    }

    @Override
    public ObjectAdapter getResultsAdapter() {
        return mRowsAdapter;
    }

    private void queryByWords(String words) {
        mRowsAdapter.clear();
        if (!TextUtils.isEmpty(words)) {
            mDelayedLoad.setSearchQuery(words);
            mHandler.removeCallbacks(mDelayedLoad);
            mHandler.postDelayed(mDelayedLoad, SEARCH_DELAY_MS);
        }
    }

    @Override
    public boolean onQueryTextChange(String newQuery) {
        Log.i(TAG, String.format("Search Query Text Change %s", newQuery));
        queryByWords(newQuery);
        return true;
    }

    @Override
    public boolean onQueryTextSubmit(String query) {
        Log.i(TAG, String.format("Search Query Text Submit %s", query));
        queryByWords(query);
        return true;
    }

    private void loadRows(String query) {
        HashMap<String, List<Movie>> movies = VideoProvider.getMovieList();
        ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
        for (Map.Entry<String, List<Movie>> entry : movies.entrySet())
        {
            for (int i = 0; i < entry.getValue().size(); i++) {
                Movie movie = entry.getValue().get(i);
                if (movie.getTitle().toLowerCase(Locale.ENGLISH)
                        .indexOf(query.toLowerCase(Locale.ENGLISH)) >= 0
                        || movie.getDescription().toLowerCase(Locale.ENGLISH)
                                .indexOf(query.toLowerCase(Locale.ENGLISH)) >= 0) {
                    listRowAdapter.add(movie);
                }
            }
        }
        HeaderItem header = new HeaderItem(0, getResources().getString(R.string.search_results),
                null);
        mRowsAdapter.add(new ListRow(header, listRowAdapter));
    }

    protected OnItemClickedListener getDefaultItemClickedListener() {
        return new OnItemClickedListener() {
            @Override
            public void onItemClicked(Object item, Row row) {
                if (item instanceof Movie) {
                    Movie movie = (Movie) item;
                    Intent intent = new Intent(getActivity(), DetailsActivity.class);
                    intent.putExtra(getResources().getString(R.string.movie), movie);
                    startActivity(intent);
                }
            }
        };
    }

    private class SearchRunnable implements Runnable {

        private volatile String searchQuery;

        public SearchRunnable() {
        }

        public void run() {
            loadRows(searchQuery);
        }

        public void setSearchQuery(String value) {
            this.searchQuery = value;
        }
    }
}
