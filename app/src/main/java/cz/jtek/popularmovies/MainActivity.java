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
import android.database.Cursor;
import android.os.Parcelable;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.support.v7.preference.PreferenceManager;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;

import cz.jtek.popularmovies.data.MovieContract;
import cz.jtek.popularmovies.data.MovieContract.MovieEntry;
import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils.AsyncTaskResult;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;
import cz.jtek.popularmovies.utilities.UIUtils;

public class MainActivity
        extends AppCompatActivity
        implements MovieGridAdapter.MovieGridOnClickHandler,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private TmdbData.Config mTmdbConfig;
    private ArrayList<TmdbData.Movie> mTmdbMovieList;

    private RecyclerView mRecyclerView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    private Context mContext;

    // Layout Manager
    private GridLayoutManager mLayoutManager;
    private Parcelable mLayoutManagerSaveState;

    // Grid Adapter
    private static final int DEFAULT_GRID_COLUMNS = 3;
    private MovieGridAdapter mMovieGridAdapter;

    // Movie detail activity extras
    public static final String EXTRA_MOVIE = "movie";

    // Shared preferences
    private static final String PREF_KEY_SORT_ORDER = "pref_key_sort_order_list";
    private String mPrefSortOrder;
    private static boolean sPrefsUpdatedFlag = false;

    // AsyncLoader
    private static final int LOADER_ID_CONFIG     = 0;
    private static final int LOADER_ID_MOVIE_LIST = 1;
    private static final int LOADER_ID_CURSOR     = 2;
    private static final String LOADER_BUNDLE_KEY_PAGE = "page";
    private static final String LOADER_BUNDLE_KEY_SORT_ORDER = "sort-order";
    private int mApiResultsPageToLoad = 1;

    // Instance State bundle keys
    private static final String KEY_CONFIG = "config";
    private static final String KEY_MOVIE_LIST = "movie-list";
    private static final String KEY_LAYOUT_STATE = "layout-state";
    private static final String KEY_PREF_SORT_ORDER  = "sort-order";

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

        // Shared Preferences and preference change listener
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);
        sp.registerOnSharedPreferenceChangeListener(this);

        // Obtain current sort order from shared preferences
        String defaultSortOrder = getResources().getString(R.string.pref_sort_order_top_rated);
        mPrefSortOrder = sp.getString(PREF_KEY_SORT_ORDER, defaultSortOrder);

        // Layout
        mLayoutManager = new GridLayoutManager(this, gridColumns);
        mRecyclerView.setLayoutManager(mLayoutManager);
        mRecyclerView.setHasFixedSize(true);

        // Sending preferred image size to grid adapter
        mMovieGridAdapter = new MovieGridAdapter(this, this, optimalWidth, optimalHeight);
        mRecyclerView.setAdapter(mMovieGridAdapter);

        if (savedInstanceState != null) {
            // Retrieving original sort order
            // In very low memory conditions it might have been changed without
            // triggering this activity change listener (as it could have been destroyed)
            String originalSortOrder = savedInstanceState.getString(KEY_PREF_SORT_ORDER);
            if (!mPrefSortOrder.equals(originalSortOrder)) {
                sPrefsUpdatedFlag = true;
            }

            // Retrieving Config and movie list from saved instance state
            mTmdbConfig = savedInstanceState.getParcelable(KEY_CONFIG);
            mTmdbMovieList = savedInstanceState.getParcelableArrayList(KEY_MOVIE_LIST);

            mMovieGridAdapter.setMovieData(mTmdbMovieList);
            showMovieDataView();
        }
        else {
            // Using loaders to obtain config and movie list
            // Select loaders depending on sort type preference
            if (mPrefSortOrder.equals(getResources().getString(R.string.pref_sort_order_favorite))) {
                getSupportLoaderManager().initLoader(LOADER_ID_CURSOR, null, favoriteLoaderListener);
            }
            else if (mPrefSortOrder.equals(getResources().getString(R.string.pref_sort_order_most_popular)) ||
                        mPrefSortOrder.equals(getResources().getString(R.string.pref_sort_order_top_rated))) {
                // Check for network availability
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    // Network is not available
                    showErrorMessage(getResources().getString(R.string.error_msg_no_network));
                }
                else {
                    // Initialize config loader (which in turn runs movie list loader)
                    getSupportLoaderManager().initLoader(LOADER_ID_CONFIG, null, configLoaderListener);
                }
            }
        }
    }

    /**
     * Shared preference change listener. On preference change sets global flag.
     *
     * @param sharedPreferences Shared preferences
     * @param key                    Changed preference key
     */
    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String key) {
        sPrefsUpdatedFlag = true;
    }

    /**
     *
     */
    @Override
    protected void onStart() {
        super.onStart();

        if (sPrefsUpdatedFlag) {
            // We are returning to this activity after preference change
            sPrefsUpdatedFlag = false;

            // Restart loading results from page 1
            mApiResultsPageToLoad = 1;

            // Shared Preferences and preference change listener
            SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(this);

            // Obtain current sort order from shared preferences
            String defaultSortOrder = getResources().getString(R.string.pref_sort_order_top_rated);
            String prefSortOrder = sp.getString(PREF_KEY_SORT_ORDER, defaultSortOrder);

            // Start loaders depending on sort type preference
            if (prefSortOrder.equals(getResources().getString(R.string.pref_sort_order_favorite))) {
                getSupportLoaderManager().initLoader(LOADER_ID_CURSOR, null, favoriteLoaderListener);
            }
            else if (prefSortOrder.equals(getResources().getString(R.string.pref_sort_order_most_popular)) ||
                    prefSortOrder.equals(getResources().getString(R.string.pref_sort_order_top_rated))) {
                // Check for network availability
                if (!NetworkUtils.isNetworkAvailable(this)) {
                    // Network is not available
                    showErrorMessage(getResources().getString(R.string.error_msg_no_network));
                }
                else {
                    // Initialize config loader (which in turn runs movie list loader)
                    getSupportLoaderManager().initLoader(LOADER_ID_CONFIG, null, configLoaderListener);
                }
            }
        }
    }

    @Override
    public void onResume() {
        super.onResume();

        if (mLayoutManagerSaveState != null) {
            // Restoring layout manager state (e.g. scroll position)
            mLayoutManager.onRestoreInstanceState(mLayoutManagerSaveState);
        }
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        // Store Config
        outState.putParcelable(KEY_CONFIG, mTmdbConfig);

        // Store movie list
        outState.putParcelableArrayList(KEY_MOVIE_LIST, mTmdbMovieList);

        // Store recycler view state
        mLayoutManagerSaveState = mLayoutManager.onSaveInstanceState();
        outState.putParcelable(KEY_LAYOUT_STATE, mLayoutManagerSaveState);

        // Store current sort order
        // In very low memory conditions MainActivity might get destroyed when
        // SettingsActivity is opened  and  shared preference listener will not work
        // This is used for detecting change against live shared preferences
        outState.putString(KEY_PREF_SORT_ORDER, mPrefSortOrder);

        // Calling superclass to save state
        super.onSaveInstanceState(outState);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        // Config and movie list are retrieved in onCreate method

        // Retrieve recycler view state
        mLayoutManagerSaveState = savedInstanceState.getParcelable(KEY_LAYOUT_STATE);
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

                        getSupportLoaderManager().initLoader(LOADER_ID_MOVIE_LIST, loaderArgsBundle, movieListLoaderListener);

                        // Destroy this loader (otherwise is gets called twice for some reason)
                        getSupportLoaderManager().destroyLoader(LOADER_ID_CONFIG);
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
    private LoaderManager.LoaderCallbacks<AsyncTaskResult<ArrayList<TmdbData.Movie>>> movieListLoaderListener =
            new LoaderManager.LoaderCallbacks<AsyncTaskResult<ArrayList<TmdbData.Movie>>>() {

                @NonNull
                @Override
                public Loader<AsyncTaskResult<ArrayList<TmdbData.Movie>>> onCreateLoader(int id, @Nullable Bundle args) {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    return new TmdbMovieListLoader(mContext, args);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<AsyncTaskResult<ArrayList<TmdbData.Movie>>> loader,
                                           AsyncTaskResult<ArrayList<TmdbData.Movie>> data) {

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
                        // Patching poster path to include poster base URL
                        String posterBaseUrl = mTmdbConfig.getSecureBaseUrl() + TmdbData.Config.getPosterSize();

                        mTmdbMovieList = data.getResult();

                        for (TmdbData.Movie movie : mTmdbMovieList) {
                            String posterPath = posterBaseUrl + movie.getPosterPath();
                            movie.setPosterPath(posterPath);
                        }

                        mMovieGridAdapter.setMovieData(mTmdbMovieList);
                        showMovieDataView();

                        // Destroy this loader (otherwise is gets called twice for some reason)
                        getSupportLoaderManager().destroyLoader(LOADER_ID_MOVIE_LIST);
                    }
                }

                @Override
                public void onLoaderReset(@NonNull Loader<AsyncTaskResult<ArrayList<TmdbData.Movie>>> loader) {
                    // Not used
                }
            };

    /**
     * Loader callbacks for favorite list loader
     */
    private LoaderManager.LoaderCallbacks<Cursor> favoriteLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @NonNull
                @Override
                public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                    return new CursorLoader(mContext,
                            MovieContract.MovieEntry.CONTENT_URI,
                            null,
                            null,
                            null,
                            null
                            );
                }

                @Override
                public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {

                    // Copy data from cursor to array list
                    mTmdbMovieList = new ArrayList<>();

                    while(data.moveToNext()) {
                        TmdbData.Movie movie = new TmdbData.Movie(
                                data.getInt(data.getColumnIndex(MovieEntry.COL_MOVIE_ID)),
                                data.getString(data.getColumnIndex(MovieEntry.COL_TITLE)),
                                data.getString(data.getColumnIndex(MovieEntry.COL_POSTER_PATH)),
                                data.getString(data.getColumnIndex(MovieEntry.COL_OVERVIEW)),
                                data.getString(data.getColumnIndex(MovieEntry.COL_RELEASE_DATE)),
                                data.getDouble(data.getColumnIndex(MovieEntry.COL_VOTE_AVERAGE))
                        );
                        mTmdbMovieList.add(movie);
                    }
                    mMovieGridAdapter.setMovieData(mTmdbMovieList);
                    showMovieDataView();

                    // Destroy this loader (otherwise is gets called twice for some reason)
                    getSupportLoaderManager().destroyLoader(LOADER_ID_CURSOR);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                    // Not used
                }
            };


    /**
     * TMDb API movie list async task loader implementation
     */
    public static class TmdbMovieListLoader
            extends AsyncTaskLoader<AsyncTaskResult<ArrayList<TmdbData.Movie>>> {

        final PackageManager mPackageManager;
        AsyncTaskResult<ArrayList<TmdbData.Movie>> mResult;
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
        public AsyncTaskResult<ArrayList<TmdbData.Movie>> loadInBackground() {

            // Get API results page to load from bundle
            int movieResultPage = mArgs.getInt(LOADER_BUNDLE_KEY_PAGE, 1);
            // Get sort order from bundle
            String sortOrder = mArgs.getString(LOADER_BUNDLE_KEY_SORT_ORDER);

            if (sortOrder == null) {
                throw new IllegalArgumentException("Sort order cannot be null");
            }

            try {
                // Example mock request used for debugging to avoid sending network queries
                // String jsonMovies = MockDataUtils.getMockJson(getContext(), "mock_popular");

                // Load movie result page
                URL movieUrl = NetworkUtils.buildMovieUrl(getContext(), sortOrder, movieResultPage);
                String jsonMovies = NetworkUtils.getResponseFromHttpUrl(movieUrl);

                TmdbJsonUtils.TmdbJsonResult<ArrayList<TmdbData.Movie>> movieResult =
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
     * TMDb API configuration async task loader
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
                // Example mock request used for debugging to avoid sending network queries
                // String jsonConfig = MockDataUtils.getMockJson(getContext(), "mock_configuration");

                // Load current API configuration
                URL configUrl = NetworkUtils.buildConfigurationUrl();
                String jsonConfig = NetworkUtils.getResponseFromHttpUrl(configUrl);

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
