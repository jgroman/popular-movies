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
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
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
    private static final int DEFAULT_MOVIE_POSTER_HEIGHT = 278;

    private MovieGridAdapter mMovieGridAdapter;

    private static final int MOVIELIST_LOADER_ID = 0;

    // Movie detail activity extras
    public static final String EXTRA_DETAIL_TITLE = "title";
    public static final String EXTRA_DETAIL_RELEASE_DATE = "release";
    public static final String EXTRA_DETAIL_OVERVIEW = "overview";
    public static final String EXTRA_DETAIL_VOTE_AVERAGE = "vote_average";
    public static final String EXTRA_DETAIL_POSTER_URL = "poster_url";

    // Shared preferences
    public static final String PREF_KEY_SORT_ORDER = "pref_key_sort_order_list";


    // AsyncLoader Bundle
    private static final String LOADER_BUNDLE_KEY_PAGE = "page";
    private static final String LOADER_BUNDLE_KEY_SORT_ORDER = "sort-order";
    private int mApiResultsPageToLoad = 1;

    private static boolean prefsWereUpdated = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerview_movies);
        mErrorMessage = findViewById(R.id.tv_error_message);
        mLoadingIndicator = findViewById(R.id.pb_loading);

        // Our grid layout uses full display width
        int displayWidth = getDisplayWidth(this);

        int gridColumns = DEFAULT_GRID_COLUMNS;
        int optimalWidth = DEFAULT_MOVIE_POSTER_WIDTH;
        int optimalHeight = DEFAULT_MOVIE_POSTER_HEIGHT;

        if (displayWidth > 0) {
            // Number of columns which fits into current display width
            gridColumns = displayWidth / DEFAULT_MOVIE_POSTER_WIDTH;
            // Optimal column width to fill all available space
            optimalWidth = displayWidth / gridColumns;
            // Factor to resize original image with
            double resizeFactor = (double) optimalWidth / (double) DEFAULT_MOVIE_POSTER_WIDTH;
            // Resized image height
            optimalHeight = (int) ((double) DEFAULT_MOVIE_POSTER_HEIGHT * resizeFactor);
        }

        // Layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, gridColumns);
        mRecyclerView.setLayoutManager(layoutManager);
        mRecyclerView.setHasFixedSize(true);

        // Sending preferred image size to grid adapter
        mMovieGridAdapter = new MovieGridAdapter(this, optimalWidth, optimalHeight);
        mRecyclerView.setAdapter(mMovieGridAdapter);

        // Shared Preferences and preference change listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        // Check for network availability
        if (!isNetworkAvailable()) {
            // Network is not available
            showErrorMessage(getResources().getString(R.string.error_msg_no_network));
        } else {
            // Loader
            LoaderManager.LoaderCallbacks<TmdbData> loaderCallbacks = MainActivity.this;

            Bundle loaderArgsBundle = new Bundle();
            // Store results page into loader args bundle
            loaderArgsBundle.putInt(LOADER_BUNDLE_KEY_PAGE, mApiResultsPageToLoad);
            // Store results sort order into loader args bundle
            String defaultSortOrder = getResources().getString(R.string.pref_sort_order_top_rated);
            String prefSortOrder = sp.getString(PREF_KEY_SORT_ORDER, defaultSortOrder);
            loaderArgsBundle.putString(LOADER_BUNDLE_KEY_SORT_ORDER, prefSortOrder);
            // Prepare loader
            getSupportLoaderManager().initLoader(MOVIELIST_LOADER_ID, loaderArgsBundle, loaderCallbacks);
        }
    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        prefsWereUpdated = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (prefsWereUpdated) {
            prefsWereUpdated = false;

            // On preference change restart loading results from page 1
            mApiResultsPageToLoad = 1;

            // Check for network availability
            if (!isNetworkAvailable()) {
                // Network is not available
                showErrorMessage(getResources().getString(R.string.error_msg_no_network));
            } else {

                SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
                Bundle loaderArgsBundle = new Bundle();
                // Store results page into loader args bundle
                loaderArgsBundle.putInt(LOADER_BUNDLE_KEY_PAGE, mApiResultsPageToLoad);
                // Store results sort order into loader args bundle
                String defaultSortOrder = getResources().getString(R.string.pref_sort_order_top_rated);
                String prefSortOrder = sp.getString(PREF_KEY_SORT_ORDER, defaultSortOrder);
                loaderArgsBundle.putString(LOADER_BUNDLE_KEY_SORT_ORDER, prefSortOrder);

                getSupportLoaderManager().restartLoader(MOVIELIST_LOADER_ID, loaderArgsBundle, MainActivity.this);
            }
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        // Unregister this activity as shared preference change listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.unregisterOnSharedPreferenceChangeListener(this);
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
     * Called when a previously created loader has finished loading.
     *
     * @param loader   The Loader that has finished.
     * @param tmdbData The data generated by the Loader.
     */
    @Override
    public void onLoadFinished(Loader<TmdbData> loader, TmdbData tmdbData) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        mMovieGridAdapter.setMovieData(tmdbData);
        mTmdbData = tmdbData;

        if (null == tmdbData) {
            showErrorMessage(getResources().getString(R.string.error_msg_no_data));
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
        // This method is not used but it is required to be present and overridden
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
     */
    private void showMovieDataView() {
        // Hide error message
        mErrorMessage.setVisibility(View.INVISIBLE);
        // Display movie grid
        mRecyclerView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the movie grid.
     */
    private void showErrorMessage(String errorMessage) {
        // Hide movie grid
        mRecyclerView.setVisibility(View.INVISIBLE);
        // Display error message
        mErrorMessage.setVisibility(View.VISIBLE);

        if (errorMessage != null && errorMessage.length() > 0) {
            mErrorMessage.setText(errorMessage);
        }
    }

    /**
     * This method returns display width
     *
     * @param context Context
     * @return Display width
     */
    private int getDisplayWidth(Context context) {

        int width = 0;
        Display display;

        WindowManager wm = (WindowManager) context.getSystemService(Context.WINDOW_SERVICE);

        if (wm != null) {
            display = wm.getDefaultDisplay();

            if (android.os.Build.VERSION.SDK_INT >= 13) {
                Point size = new Point();
                display.getSize(size);
                width = size.x;
            } else {
                width = display.getWidth();  // deprecated
            }
        }

        return width;
    }

    public boolean isNetworkAvailable() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }

        return false;
    }


    /**
     *
     */
    public static class TmdbDataLoader extends AsyncTaskLoader<TmdbData> {

        final PackageManager mPackageManager;
        TmdbData mTmdbData = new TmdbData();
        Bundle mArgs;


        private TmdbDataLoader(Context context, Bundle args) {
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

        /**
         * This AsyncTaskLoader method will load and parse the TMDb JSON data in the background
         *
         * @return Movie data from TMDb as TmdbData object
         *         null if an error occurs
         */
        @Override
        public TmdbData loadInBackground() {

            // Get API results page to load from bundle
            int movieResultPage = mArgs.getInt(LOADER_BUNDLE_KEY_PAGE, 1);
            // Get sort order from bundle
            String sortOrder = mArgs.getString(LOADER_BUNDLE_KEY_SORT_ORDER);

            Log.d(TAG, "Load in background, page: " + movieResultPage + ", sort order: " + sortOrder);

            try {
                // On loading first result page also load current API configuration
                if (movieResultPage == 1) {
                    // Load current API configuration
                    URL configUrl = NetworkUtils.buildConfigurationUrl();
                    //String jsonConfig = NetworkUtils.getResponseFromHttpUrl(configUrl);

                    // Mock request used for debugging to avoid sending network queries
                    String jsonConfig = MockDataUtils.getMockJson(getContext(), "mock_configuration");

                    TmdbJsonUtils.getConfigFromJson(mTmdbData, jsonConfig);
                }

                // Load movie result page
                URL movieUrl = NetworkUtils.buildMovieUrl(getContext(), sortOrder, movieResultPage);
                //String jsonMovies = NetworkUtils.getResponseFromHttpUrl(movieUrl);

                // Mock request used for debugging to avoid sending network queries
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
