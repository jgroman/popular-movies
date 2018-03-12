/*
 * Copyright 2018 Jaroslav Groman
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package cz.jtek.popularmovies;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Display;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;

import cz.jtek.popularmovies.utilities.MockDataUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;

public class MainActivity
        extends AppCompatActivity
        implements MovieGridAdapter.MovieGridOnClickHandler,
        LoaderManager.LoaderCallbacks<TmdbData>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TmdbData mTmdbData;

    private RecyclerView mRecyclerView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    // Default number of columns in grid
    private static final int DEFAULT_GRID_COLUMNS = 3;
    private static final int DEFAULT_MOVIE_POSTER_WIDTH = 185;

    private MovieGridAdapter mMovieGridAdapter;

    private static final int MOVIELIST_LOADER_ID = 0;

    // Movie detail activity extras
    public static final String EXTRA_DETAIL_TITLE = "title";
    public static final String EXTRA_DETAIL_RELEASE_DATE = "release";
    public static final String EXTRA_DETAIL_OVERVIEW = "overview";
    public static final String EXTRA_DETAIL_VOTE_AVERAGE = "vote_average";
    public static final String EXTRA_DETAIL_POSTER_URL = "poster_url";


    // AsyncLoader Bundle
    private static final String BUNDLE_KEY_PAGE = "page";
    private int mApiResultsPageToLoad = 1;

    private static boolean WERE_PREFERENCES_UPDATED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerview_movies);
        mErrorMessage = findViewById(R.id.tv_error_message);
        mLoadingIndicator = findViewById(R.id.pb_loading);

        // Our grid layout uses full display width
        int displayWidth = getDisplayWidth(this);

        // Calculate number of columns in grid
        int gridColumns = DEFAULT_GRID_COLUMNS;
        if (displayWidth > 0) {
            gridColumns = displayWidth / DEFAULT_MOVIE_POSTER_WIDTH;
        }

        // Layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, gridColumns);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);


        mMovieGridAdapter = new MovieGridAdapter(this);

        mRecyclerView.setAdapter(mMovieGridAdapter);

        // Loader
        LoaderManager.LoaderCallbacks<TmdbData> loaderCallbacks = MainActivity.this;

        Bundle loaderBundle = new Bundle();
        loaderBundle.putInt(BUNDLE_KEY_PAGE, mApiResultsPageToLoad);

        getSupportLoaderManager().initLoader(MOVIELIST_LOADER_ID, loaderBundle, loaderCallbacks);

        // Preference
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);


    }

    private int getDisplayWidth(Context context) {

        int width;
        Display display;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        try {
            display = wm.getDefaultDisplay();
        } catch (NullPointerException npe) {
            Log.e(TAG, "Null pointer exception on getDefaultDisplay().");
            return 0;
        }

        if (android.os.Build.VERSION.SDK_INT >= 13) {
            Point size = new Point();
            display.getSize(size);
            width = size.x;
        } else {
            width = display.getWidth();  // deprecated
        }

        return width;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.menu_main, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int itemId = item.getItemId();

        if (R.id.action_settings == itemId) {
            Intent startSettingsActivity = new Intent(this, SettingsActivity.class);
            startActivity(startSettingsActivity);
            return true;
        }

        return super.onOptionsItemSelected(item);
    }


    /**
     * This method responds to clicks on movie grid items - opens movie detail activity
     *
     * @param itemId Id of clicked-on item
     */
    @Override
    public void onClick(int itemId) {
        Intent intent = new Intent(this, MovieDetailActivity.class);

        TmdbData.Movie movie = mTmdbData.getMovieList().get(itemId);
        String secureBaseUrl = mTmdbData.getConfig().getSecureBaseUrl();

        intent.putExtra(EXTRA_DETAIL_TITLE, movie.getTitle());
        intent.putExtra(EXTRA_DETAIL_OVERVIEW, movie.getOverview());
        intent.putExtra(EXTRA_DETAIL_POSTER_URL, secureBaseUrl + movie.getPosterPath());
        intent.putExtra(EXTRA_DETAIL_RELEASE_DATE, movie.getReleaseDate());
        intent.putExtra(EXTRA_DETAIL_VOTE_AVERAGE, movie.getVoteAverage());

        startActivity(intent);
    }

    @Override
    public Loader<TmdbData> onCreateLoader(int id, Bundle args) {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        return new TmdbDataLoader(this, args);
    }

    /**
     * Called when a previously created loader has finished its load.
     *
     * @param loader   The Loader that has finished.
     * @param tmdbData The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<TmdbData> loader, TmdbData tmdbData) {
        Log.d(TAG, "Load finished");
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mMovieGridAdapter.setMovieData(tmdbData);
        mTmdbData = tmdbData;

        if (null == tmdbData) {
            showErrorMessage();
        } else {
            showMovieDataView();
        }
    }

    /**
     * Called when a previously created loader is being reset, and thus
     * making its data unavailable.  The application should at this point
     * remove any references it has to the Loader's data.
     *
     * @param loader The Loader that is being reset.
     */
    @Override
    public void onLoaderReset(Loader<TmdbData> loader) {
        // This method is not used but it is required to be overridden
    }

    /**
     * This method is used for resetting movie grid adapter contents.
     */
    private void invalidateData() {
        mMovieGridAdapter.setMovieData(null);
        mTmdbData = null;
    }

    /**
     * This method will make the View for the movie data visible and
     * hide the error message.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showMovieDataView() {
        // Hide error message
        mErrorMessage.setVisibility(View.INVISIBLE);
        // Display movie grid
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the weather
     * View.
     * <p>
     * Since it is okay to redundantly set the visibility of a View, we don't
     * need to check whether each view is currently visible or invisible.
     */
    private void showErrorMessage() {
        // Hide movie grid
        mRecyclerView.setVisibility(View.INVISIBLE);
        // Display error message
        mErrorMessage.setVisibility(View.VISIBLE);
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }



    public static class TmdbDataLoader extends AsyncTaskLoader<TmdbData> {

        final PackageManager mPackageManager;
        TmdbData mTmdbData = new TmdbData();
        Bundle mArgs;


        public TmdbDataLoader(Context context, Bundle args) {
            super(context);
            mPackageManager = getContext().getPackageManager();
            mArgs = args;
        }

        @Override
        protected void onStartLoading() {
            if (mTmdbData.getMovieList().size() != 0) {
                // If there are already data available, deliver them
                deliverResult(mTmdbData);
            } else {
                // Start background task
                forceLoad();
            }
        }

        /**
         * Handles a request to stop the Loader.
         */
        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public TmdbData loadInBackground() {

            // Get API results page to load from bundle
            int movieResultPage = mArgs.getInt(BUNDLE_KEY_PAGE, 1);

            Log.d(TAG, "Load in background, page: " + movieResultPage);

            try {
                // On loading first result page also load current API configuration
                if (movieResultPage == 1) {
                    // Load current API configuration
                    URL configUrl = NetworkUtils.buildConfigurationUrl();
                    //String jsonConfig = NetworkUtils.getResponseFromHttpUrl(configUrl);
                    String jsonConfig = MockDataUtils.getMockJson(getContext(), "mock_configuration");

                    TmdbJsonUtils.getConfigFromJson(mTmdbData, jsonConfig);
                }

                // Load movie result page
                URL movieUrl = NetworkUtils.buildMovieUrl(getContext(), null, movieResultPage);
                //String jsonMovies = NetworkUtils.getResponseFromHttpUrl(movieUrl);
                String jsonMovies = MockDataUtils.getMockJson(getContext(), "mock_popular");

                TmdbJsonUtils.getMovieListFromJson(mTmdbData, jsonMovies);

                return mTmdbData;

            } catch (IOException iex) {
                Log.e(TAG, "IOException ");
                iex.printStackTrace();
                return null;
            }
        }

    }
}
