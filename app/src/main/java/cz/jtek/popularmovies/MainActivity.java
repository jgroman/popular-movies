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

import android.content.Intent;
import android.content.SharedPreferences;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
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
import java.util.List;

import cz.jtek.popularmovies.utilities.NetworkUtils;

public class MainActivity
        extends AppCompatActivity
        implements MovieGridAdapter.MovieGridOnClickHandler,
        LoaderManager.LoaderCallbacks<List>,
        SharedPreferences.OnSharedPreferenceChangeListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    private RecyclerView mRecyclerView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    private MovieGridAdapter mMovieGridAdapter;

    private static final int MOVIELIST_LOADER_ID = 0;

    private static boolean WERE_PREFERENCES_UPDATED = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mRecyclerView = findViewById(R.id.recyclerview_movies);
        mErrorMessage = findViewById(R.id.tv_error_message);
        mLoadingIndicator = findViewById(R.id.pb_loading);

        // Layout
        GridLayoutManager layoutManager = new GridLayoutManager(this, 2);
        mRecyclerView.setLayoutManager(layoutManager);

        mRecyclerView.setHasFixedSize(true);

        mMovieGridAdapter = new MovieGridAdapter(this);

        mRecyclerView.setAdapter(mMovieGridAdapter);

        // Loader
        LoaderManager.LoaderCallbacks<List> loaderCallbacks = MainActivity.this;

        Bundle loaderBundle = null;

        getSupportLoaderManager().initLoader(MOVIELIST_LOADER_ID, loaderBundle, loaderCallbacks);

        // Preference
        PreferenceManager.getDefaultSharedPreferences(this).registerOnSharedPreferenceChangeListener(this);


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

    @Override
    public void onClick(int itemId) {

    }

    @Override
    public Loader<List> onCreateLoader(int id, Bundle args) {

        return new AsyncTaskLoader<List>(this) {

            List<TmdbApi.Movie> mMovieList = null;

            @Override
            protected void onStartLoading() {
                if (mMovieList != null) {
                    deliverResult(mMovieList);
                } else {
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    forceLoad();
                }
            }

            @Override
            public List loadInBackground() {

                URL configUrl = NetworkUtils.buildConfigurationUrl();
                URL movieUrl = NetworkUtils.buildMovieUrl(this, null, 1);

                try {
                    String jsonConfig = NetworkUtils.getResponseFromHttpUrl(configUrl);
                    String jsonMovies = NetworkUtils.getResponseFromHttpUrl(movieUrl);

                    // TODO


                } catch (IOException iex) {
                    Log.e(TAG, "IOException ");
                    iex.printStackTrace();
                    return null;
                }

                return null;
            }
        };
    }

    @Override
    public void onLoadFinished(Loader<List> loader, List data) {

    }

    @Override
    public void onLoaderReset(Loader<List> loader) {

    }

    @Override
    public void onSharedPreferenceChanged(SharedPreferences sharedPreferences, String s) {

    }

    @Override
    protected void onStart() {
        super.onStart();
    }
}
