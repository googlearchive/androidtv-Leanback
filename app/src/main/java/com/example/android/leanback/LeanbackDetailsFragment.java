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
import android.net.Uri;
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
import android.support.v17.leanback.widget.ImageCardView;
import android.support.v17.leanback.widget.ListRow;
import android.support.v17.leanback.widget.ListRowPresenter;
import android.support.v17.leanback.widget.OnActionClickedListener;
import android.support.v17.leanback.widget.OnItemViewClickedListener;
import android.support.v17.leanback.widget.Presenter;
import android.support.v17.leanback.widget.Row;
import android.support.v17.leanback.widget.RowPresenter;
import android.support.v4.app.ActivityOptionsCompat;
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

    private Movie mSelectedMovie;

    private Drawable mDefaultBackground;
    private Target mBackgroundTarget;
    private DisplayMetrics mMetrics;
    private DetailsOverviewRowPresenter mDorPresenter;
    private DetailRowBuilderTask mDetailRowBuilderTask;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        Log.d(TAG, "onCreate DetailsFragment");
        super.onCreate(savedInstanceState);

        mDorPresenter =
                new DetailsOverviewRowPresenter(new DetailsDescriptionPresenter());

        BackgroundManager backgroundManager = BackgroundManager.getInstance(getActivity());
        backgroundManager.attach(getActivity().getWindow());
        mBackgroundTarget = new PicassoBackgroundManagerTarget(backgroundManager);

        mDefaultBackground = getResources().getDrawable(R.drawable.default_background);

        mMetrics = new DisplayMetrics();
        getActivity().getWindowManager().getDefaultDisplay().getMetrics(mMetrics);

        mSelectedMovie = (Movie) getActivity().getIntent().getSerializableExtra(DetailsActivity.MOVIE);
        if (null != mSelectedMovie || checkGlobalSearchIntent()) {
            Log.d(TAG, "DetailsActivity movie: " + mSelectedMovie.toString());
            mDetailRowBuilderTask = (DetailRowBuilderTask) new DetailRowBuilderTask().execute(mSelectedMovie);
            mDorPresenter.setSharedElementEnterTransition(getActivity(),
                    DetailsActivity.SHARED_ELEMENT_NAME);
            updateBackground(mSelectedMovie.getBackgroundImageURI());
            setOnItemViewClickedListener(new ItemViewClickedListener());
        }
    }

    @Override
    public void onStop() {
        mDetailRowBuilderTask.cancel(true);
        super.onStop();
    }

    /*
     * Check if there is a global search intent
     */
    private boolean checkGlobalSearchIntent() {
        Intent intent = getActivity().getIntent();
        String intentAction = intent.getAction();
        String globalSearch = getString(R.string.global_search);
        if (globalSearch.equalsIgnoreCase(intentAction)) {
            Uri intentData = intent.getData();
            Log.d(TAG, "action: " + intentAction + " intentData:" + intentData);
            int selectedIndex = Integer.parseInt(intentData.getLastPathSegment());
            HashMap<String, List<Movie>> movies = VideoProvider.getMovieList();
            int movieTally = 0;
            for (Map.Entry<String, List<Movie>> entry : movies.entrySet()) {
                List<Movie> list = entry.getValue();
                for(Movie movie : list) {
                    movieTally++;
                    if (selectedIndex == movieTally) {
                        mSelectedMovie = movie;
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private class DetailRowBuilderTask extends AsyncTask<Movie, Integer, DetailsOverviewRow> {

        private volatile boolean running = true;

        @Override
        protected DetailsOverviewRow doInBackground(Movie... movies) {

            while(running) {
                mSelectedMovie = movies[0];

                Log.d(TAG, "doInBackground: " + mSelectedMovie.toString());
                DetailsOverviewRow row = new DetailsOverviewRow(mSelectedMovie);
                try {
                    Bitmap poster = Picasso.with(getActivity())
                            .load(mSelectedMovie.getCardImageUrl())
                            .resize(Utils.convertDpToPixel(getActivity()
                                            .getApplicationContext(), DETAIL_THUMB_WIDTH),
                                    Utils.convertDpToPixel(getActivity()
                                            .getApplicationContext(), DETAIL_THUMB_HEIGHT))
                            .centerCrop()
                            .get();
                    row.setImageBitmap(getActivity(), poster);
                } catch (IOException e) {
                    Log.e(TAG, e.toString());
                }

                row.addAction(new Action(ACTION_WATCH_TRAILER, getResources().getString(
                        R.string.watch_trailer_1), getResources().getString(R.string.watch_trailer_2)));
                row.addAction(new Action(ACTION_RENT, getResources().getString(R.string.rent_1),
                        getResources().getString(R.string.rent_2)));
                row.addAction(new Action(ACTION_BUY, getResources().getString(R.string.buy_1),
                        getResources().getString(R.string.buy_2)));
                return row;
            }
            return null;
        }

        @Override
        protected void onPostExecute(DetailsOverviewRow detailRow) {
            if (!running) {
                return;
            }
            ClassPresenterSelector presenterSelector = new ClassPresenterSelector();
            // set detail background and style
            mDorPresenter.setBackgroundColor(getResources().getColor(R.color.detail_background));
            mDorPresenter.setStyleLarge(true);
            mDorPresenter.setOnActionClickedListener(new OnActionClickedListener() {
                @Override
                public void onActionClicked(Action action) {
                    if (action.getId() == ACTION_WATCH_TRAILER) {
                        Intent intent = new Intent(getActivity(), PlaybackOverlayActivity.class);
                        intent.putExtra(DetailsActivity.MOVIE, mSelectedMovie);
                        startActivity(intent);
                    } else {
                        Toast.makeText(getActivity(), action.toString(), Toast.LENGTH_SHORT).show();
                    }
                }
            });

            presenterSelector.addClassPresenter(DetailsOverviewRow.class, mDorPresenter);
            presenterSelector.addClassPresenter(ListRow.class, new ListRowPresenter());
            ArrayObjectAdapter adapter = new ArrayObjectAdapter(presenterSelector);
            adapter.add(detailRow);

            String subcategories[] = {getString(R.string.related_movies)};
            HashMap<String, List<Movie>> movies = VideoProvider.getMovieList();

            ArrayObjectAdapter listRowAdapter = new ArrayObjectAdapter(new CardPresenter());
            for (Map.Entry<String, List<Movie>> entry : movies.entrySet()) {
                if (mSelectedMovie.getCategory().indexOf(entry.getKey()) >= 0) {
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

        @Override
        protected void onCancelled() {
            running = false;
        }
    }

    private final class ItemViewClickedListener implements OnItemViewClickedListener {
        @Override
        public void onItemClicked(Presenter.ViewHolder itemViewHolder, Object item,
                                  RowPresenter.ViewHolder rowViewHolder, Row row) {

            if (item instanceof Movie) {
                Movie movie = (Movie) item;
                Log.d(TAG, "Item: " + item.toString());
                Intent intent = new Intent(getActivity(), DetailsActivity.class);
                intent.putExtra(DetailsActivity.MOVIE, movie);

                Bundle bundle = ActivityOptionsCompat.makeSceneTransitionAnimation(
                        getActivity(),
                        ((ImageCardView) itemViewHolder.view).getMainImageView(),
                        DetailsActivity.SHARED_ELEMENT_NAME).toBundle();
                getActivity().startActivity(intent, bundle);
            }
        }
    }

    protected void updateBackground(URI uri) {
        Picasso.with(getActivity())
                .load(uri.toString())
                .resize(mMetrics.widthPixels, mMetrics.heightPixels)
                .error(mDefaultBackground)
                .into(mBackgroundTarget);
    }
}
