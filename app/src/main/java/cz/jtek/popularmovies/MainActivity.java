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

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.graphics.Point;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
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
import java.util.List;

import cz.jtek.popularmovies.utilities.MockDataUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils.AsyncTaskResult;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;
import cz.jtek.popularmovies.utilities.UIUtils;

public class MainActivity
        extends AppCompatActivity
        implements MovieGridAdapter.MovieGridOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TmdbData mTmdbData;
    private TmdbData.Config mTmdbConfig;
    private List<TmdbData.Movie> mTmdbMovieList;

    private RecyclerView mRecyclerView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    private Context mContext;

    // Grid Adapter
    private static final int DEFAULT_GRID_COLUMNS = 3;
    private MovieGridAdapter mMovieGridAdapter;

    // Movie detail activity extras
    public static final String EXTRA_MOVIE = "movie";
    public static final String EXTRA_CONFIG = "config";

    // Shared preferences
    private static final String PREF_KEY_SORT_ORDER = "pref_key_sort_order_list";
    private static boolean prefsUpdatedFlag = false;

    // AsyncLoader
    private static final int LOADER_ID_CONFIG     = 0;
    private static final int LOADER_ID_MOVIELIST = 1;
    private static final String LOADER_BUNDLE_KEY_PAGE = "page";
    private static final String LOADER_BUNDLE_KEY_SORT_ORDER = "sort-order";
    private int mApiResultsPageToLoad = 1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerview_movies);
        mErrorMessage = findViewById(R.id.tv_error_message);
        mLoadingIndicator = findViewById(R.id.pb_loading);

        mContext = this;

        // Our grid layout uses full display width
        int displayWidth = UIUtils.getDisplayWidth(this);

        int gridColumns = DEFAULT_GRID_COLUMNS;
        int optimalWidth = TmdbData.Config.getPosterWidth();
        int optimalHeight = TmdbData.Config.getPosterHeight();

        if (displayWidth > 0) {
            // Number of columns which fits into current display width
            gridColumns = displayWidth / TmdbData.Config.getPosterWidth();

            if (gridColumns < 1) {
                gridColumns = 1;
            }

            // Optimal column width to fill all available space
            optimalWidth = displayWidth / gridColumns;
            // Factor to resize original image with
            double resizeFactor = (double) optimalWidth / (double) TmdbData.Config.getPosterWidth();
            // Resized image height
            optimalHeight = (int) ((double) TmdbData.Config.getPosterHeight() * resizeFactor);
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
        if (!NetworkUtils.isNetworkAvailable(this)) {
            // Network is not available
            showErrorMessage(getResources().getString(R.string.error_msg_no_network));
        } else {
            // Initialize config loader (which in turn initializes movie list loader)
            getSupportLoaderManager().initLoader(LOADER_ID_CONFIG, null, configLoaderListener);
        }
    }

    /**
     * Shared preference change listener. On preference change sets global flag.
     *
     * @param sharedPreferences Shared preferences
     * @param s                       Unused string parameter
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {
        prefsUpdatedFlag = true;
    }

    @Override
    protected void onStart() {
        super.onStart();

        if (prefsUpdatedFlag) {
            prefsUpdatedFlag = false;

            // On preference change restart loading results from page 1
            mApiResultsPageToLoad = 1;

            // Check for network availability
            if (!NetworkUtils.isNetworkAvailable(this)) {
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

                getSupportLoaderManager().restartLoader(LOADER_ID_MOVIELIST, loaderArgsBundle, movieListLoaderListener);
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

        intent.putExtra(EXTRA_CONFIG, mTmdbConfig);
        intent.putExtra(EXTRA_MOVIE, mTmdbMovieList.get(itemId));

        startActivity(intent);
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
     * Loader callbacks for config loader
     */
    private LoaderManager.LoaderCallbacks<AsyncTaskResult<TmdbData.Config>> configLoaderListener =
            new LoaderManager.LoaderCallbacks<AsyncTaskResult<TmdbData.Config>>() {

                @NonNull
                @Override
                public Loader<AsyncTaskResult<TmdbData.Config>> onCreateLoader(int id, @Nullable Bundle args) {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    return new TmdbConfigLoader(mContext, args);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<AsyncTaskResult<TmdbData.Config>> loader,
                                           AsyncTaskResult<TmdbData.Config> data) {
                    if (data.hasException()) {
                        mLoadingIndicator.setVisibility(View.INVISIBLE);
                        // There was an error during data loading
                        Exception ex = data.getException();
                        if (ex instanceof TmdbData.TmdbStatusException) {
                            // TMDb API error
                            showErrorMessage(ex.getMessage());
                        } else {
                            showErrorMessage(getResources().getString(R.string.error_msg_no_data));
                        }
                    } else {
                        // Valid results received
                        mTmdbConfig = data.getResult();

                        // Initialize movie list loader
                        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(mContext);
                        Bundle loaderArgsBundle = new Bundle();
                        // Store results page into loader args bundle
                        loaderArgsBundle.putInt(LOADER_BUNDLE_KEY_PAGE, mApiResultsPageToLoad);
                        // Store results sort order into loader args bundle
                        String defaultSortOrder = getResources().getString(R.string.pref_sort_order_most_popular);
                        String prefSortOrder = sp.getString(PREF_KEY_SORT_ORDER, defaultSortOrder);
                        loaderArgsBundle.putString(LOADER_BUNDLE_KEY_SORT_ORDER, prefSortOrder);

                        getSupportLoaderManager().initLoader(LOADER_ID_MOVIELIST, loaderArgsBundle, movieListLoaderListener);
                    }
                }

                @Override
                public void onLoaderReset(@NonNull Loader<AsyncTaskResult<TmdbData.Config>> loader) {
                    // Not used
                }
            };

    /**
     * Loader callbacks for movie list loader
     */
    private LoaderManager.LoaderCallbacks<AsyncTaskResult<List<TmdbData.Movie>>> movieListLoaderListener =
            new LoaderManager.LoaderCallbacks<AsyncTaskResult<List<TmdbData.Movie>>>() {

                @NonNull
                @Override
                public Loader<AsyncTaskResult<List<TmdbData.Movie>>> onCreateLoader(int id, @Nullable Bundle args) {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    return new TmdbMovieListLoader(mContext, args);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<AsyncTaskResult<List<TmdbData.Movie>>> loader,
                                           AsyncTaskResult<List<TmdbData.Movie>> data) {
                    mLoadingIndicator.setVisibility(View.INVISIBLE);

                    if (data.hasException()) {
                        // There was an error during data loading
                        Exception ex = data.getException();
                        if (ex instanceof TmdbData.TmdbStatusException) {
                            // TMDb API error
                            showErrorMessage(ex.getMessage());
                        } else {
                            showErrorMessage(getResources().getString(R.string.error_msg_no_data));
                        }
                    } else {
                        // Valid results received
                        mTmdbMovieList = data.getResult();
                        mMovieGridAdapter.setMovieData(mTmdbConfig, mTmdbMovieList);
                        showMovieDataView();
                    }
                }

                @Override
                public void onLoaderReset(@NonNull Loader<AsyncTaskResult<List<TmdbData.Movie>>> loader) {
                    // Not used
                }
            };

    /**
     * TMDb API data async task loader implementation
     */
    public static class TmdbMovieListLoader
            extends AsyncTaskLoader<AsyncTaskResult<List<TmdbData.Movie>>> {

        final PackageManager mPackageManager;
        AsyncTaskResult<List<TmdbData.Movie>> mResult;
        final Bundle mArgs;

        private TmdbMovieListLoader(Context context, Bundle args) {
            super(context);
            mPackageManager = getContext().getPackageManager();
            mArgs = args;
        }

        @Override
        protected void onStartLoading() {
            if (mResult != null && (mResult.hasResult() || mResult.hasException())) {
                // If there are already data available, deliver them
                deliverResult(mResult);
            } else {
                // Start loader
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
        public AsyncTaskResult<List<TmdbData.Movie>> loadInBackground() {

            // Get API results page to load from bundle
            int movieResultPage = mArgs.getInt(LOADER_BUNDLE_KEY_PAGE, 1);
            // Get sort order from bundle
            String sortOrder = mArgs.getString(LOADER_BUNDLE_KEY_SORT_ORDER);

            try {

                // Load movie result page
                // TODO URL movieUrl = NetworkUtils.buildMovieUrl(getContext(), sortOrder, movieResultPage);
                // TODO String jsonMovies = NetworkUtils.getResponseFromHttpUrl(movieUrl);

                // Example mock request used for debugging to avoid sending network queries
                String jsonMovies = MockDataUtils.getMockJson(getContext(), "mock_popular");

                TmdbJsonUtils.TmdbJsonResult<List<TmdbData.Movie>> movieResult =
                        TmdbJsonUtils.getMovieListFromJson(jsonMovies);
                mResult = new AsyncTaskResult<>(movieResult.getResult(), movieResult.getException());
            } catch (IOException iex) {
                Log.e(TAG, "IOException when fetching API data.");
                iex.printStackTrace();
                mResult = new AsyncTaskResult<>(null, iex);
            }
            return mResult;
        }

    }

    /**
     *
     */
    public static class TmdbConfigLoader
            extends AsyncTaskLoader<AsyncTaskResult<TmdbData.Config>> {

        final PackageManager mPackageManager;
        AsyncTaskResult<TmdbData.Config> mResult;
        final Bundle mArgs;

        private TmdbConfigLoader(Context context, Bundle args) {
            super(context);
            mPackageManager = getContext().getPackageManager();
            mArgs = args;
        }

        @Override
        protected void onStartLoading() {
            if (mResult != null && (mResult.hasResult() || mResult.hasException())) {
                // If there are already data available, deliver them
                deliverResult(mResult);
            } else {
                // Start background task
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() {
            cancelLoad();
        }

        @Override
        public AsyncTaskResult<TmdbData.Config> loadInBackground() {
            try {
                // Load current API configuration
                // TODO URL configUrl = NetworkUtils.buildConfigurationUrl();
                // TODO String jsonConfig = NetworkUtils.getResponseFromHttpUrl(configUrl);

                // Example mock request used for debugging to avoid sending network queries
                String jsonConfig = MockDataUtils.getMockJson(getContext(), "mock_configuration");

                TmdbJsonUtils.TmdbJsonResult<TmdbData.Config> configResult =
                        TmdbJsonUtils.getConfigFromJson(jsonConfig);
                mResult = new AsyncTaskResult<>(configResult.getResult(), configResult.getException());
            } catch (IOException iex) {
                Log.e(TAG, "IOException when fetching API configuration.");
                iex.printStackTrace();
                mResult = new AsyncTaskResult<>(null, iex);
            }
            return mResult;
        }
    }

}
