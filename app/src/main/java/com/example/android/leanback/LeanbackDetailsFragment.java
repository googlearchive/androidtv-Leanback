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

import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.Drawable;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v17.leanback.app.BackgroundManager;
import android.support.v17.leanback.app.DetailsFragment;
import android.support.v17.leanback.widget.Action;
import android.support.v17.leanback.widget.ArrayObjectAdapter;
import android.support.v17.leanback.widget.ClassPresenterSelector;
import android.support.v17.leanback.widget.DetailsOverviewRow;
import android.support.v17.leanback.widget.DetailsOverviewRowPresenter;
import android.support.v17.leanback.widget.HeaderItem;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemClickedListener;
import android.support.v17.leanback.widget.Row;
import android.util.DisplayMetrics;
import android.util.Log;
import android.widget.Toast;

import com.squareup.picasso.Picasso;
import com.squareup.picasso.Target;

import java.io.IOException;
import java.net.URI;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/*
 * LeanbackDetailsFragment extends DetailsFragment, a Wrapper fragment for leanback details screens.
 * It shows a detailed view of video and its meta plus related videos.
 */
public class LeanbackDetailsFragment extends DetailsFragment {
    private static final String TAG = "DetailsFragment";

    private static final int ACTION_WATCH_TRAILER = 1;
    private static final int ACTION_RENT = 2;
    private static final int ACTION_BUY = 3;

    private static final int DETAIL_THUMB_WIDTH = 274;
    private static final int DETAIL_THUMB_HEIGHT = 274;

    private Movie selectedMovie;

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.i(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        selectedMovie = (Movie) getActivity().getIntent().getSerializableExtra("Movie");
        Log.d(TAG, "DetailsActivity movie: " + selectedMovie.toString());
        new DetailRowBuilderTask().execute(selectedMovie);

        setOnItemClickedListener(getDefaultItemClickedListener());
        updateBackground(selectedMovie.getBackgroundImageURI());
    }

    private class DetailRowBuilderTask extends AsyncTask<Movie, Integer, DetailsOverviewRow> {
        @Override
        protected DetailsOverviewRow doInBackground(Movie... movies) {
            selectedMovie = movies[0];

            Log.d(TAG, "doInBackground: " + selectedMovie.toString());
            DetailsOverviewRow row = new DetailsOverviewRow(selectedMovie);
            try {
                Bitmap poster = Picasso.with(getActivity())
                        .load(selectedMovie.getCardImageUrl())
                        .resize(Utils.dpToPx(DETAIL_THUMB_WIDTH, getActivity()
                                .getApplicationContext()),
                                Utils.dpToPx(DETAIL_THUMB_HEIGHT, getActivity()
                                        .getApplicationContext()))
                        .centerCrop()
                        .get();
                row.setImageBitmap(getActivity(), poster);
            } catch (IOException e) {
            }

            row.addAction(new Action(ACTION_WATCH_TRAILER, getResources().getString(
                    R.string.watch_trailer_1), getResources().getString(R.string.watch_trailer_2)));
            row.addAction(new Action(ACTION_RENT, getResources().getString(R.string.rent_1),
                    getResources().getString(R.string.rent_2)));
            row.addAction(new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
                    getResources().getString(R.string.buy_2)));
            return row;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            ClassPresenterSelector ps = new ClassPresenterSelector();
            DetailsOverviewRowPresenter dorPresenter =
                    new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());
            // set detail background and style
            dorPresenter.setBackgroundColor(getResources().getColor(R.color.detail_background));
            dorPresenter.setStyleLarge(true);
            dorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if (action.getId() == ACTION_WATCH_TRAILER) {
                        Intent intent = new Intent(getActivity(), PlayerActivity.class);
                        intent.putExtra(getResources().getString(R.string.movie), selectedMovie);
                        intent.putExtra(getResources().getString(R.string.should_start), true);
                        startActivity(intent);
                    }
                    else {
                        Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            ps.addClassPresenter(DetailsOverviewRow.class, dorPresenter);
            ps.addClassPresenter(ListRow.class,
                    new ListRowPresenter());

            ArrayObjectAdapter adapter = new ArrayObjectAdapter(ps);
            adapter.add(detailRow);

            String subcategories[] = {
                    getString(R.string.related_movies)
            };
            HashMap<String, List<Movie>> movies = VideoProvider.getMovieList();

            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            for (Map.Entry<String, List<Movie>> entry : movies.entrySet())
            {
                if (selectedMovie.getCategory().indexOf(entry.getKey()) >= 0) {
                    List<Movie> list = entry.getValue();
                    for (int j = 0; j < list.size(); j++) {
                        listRowAdapter.add(list.get(j));
                    }
                }
            }
            HeaderItem header = new HeaderItem(0, subcategories[0], null);
            adapter.add(new ListRow(header, listRowAdapter));

            setAdapter(adapter);
        }

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

    protected void updateBackground(URI uri) {
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }
}
