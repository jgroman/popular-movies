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

import android.app.Activity;
import android.content.Context;
import android.content.pm.PackageManager;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.ListFragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cz.jtek.popularmovies.utilities.MockDataUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;

public class MovieVideoFragment extends ListFragment
    implements LoaderManager.LoaderCallbacks<MovieVideoFragment.AsyncTaskResult<List<TmdbData.Video>>> {

    private static final String TAG = MovieVideoFragment.class.getSimpleName();

    private Context mContext;
    OnVideoSelectedListener mCallback;

    private ListView mVideoList;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    // AsyncLoader
    private static final int VIDEOLIST_LOADER_ID = 1;
    private static final String LOADER_BUNDLE_MOVIE_ID = "movie-id";

    public interface OnVideoSelectedListener {
        void onVideoSelected(int position);
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        // This makes sure that the container activity has implemented the callback interface
        try {
            mCallback = (OnVideoSelectedListener) context;
        } catch (ClassCastException cce) {
            throw new ClassCastException(context.toString()
                    + " must implement OnVideoSelectedListener");
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        mContext = getActivity().getApplicationContext();

        View view = inflater.inflate(R.layout.fragment_movie_video, container, false);

        mVideoList = view.findViewById(R.id.lv_detail_videos);
        mErrorMessage = view.findViewById(R.id.tv_video_error_message);
        mLoadingIndicator = view.findViewById(R.id.pb_video_loading);

        // Check for network availability
        if (!NetworkUtils.isNetworkAvailable(mContext)) {
            // Network is not available
            showErrorMessage(getResources().getString(R.string.error_msg_no_network));
        } else {
            /*
            // Loader
            LoaderManager.LoaderCallbacks<MovieVideoFragment.AsyncTaskResult<List<TmdbData.Video>>> loaderCallbacks = MainActivity.this;

            Bundle loaderArgsBundle = new Bundle();
            // Store movie id into loader args bundle
            loaderArgsBundle.putInt(LOADER_BUNDLE_MOVIE_ID, mApiResultsPageToLoad);
            // Prepare loader
            getSupportLoaderManager().initLoader(VIDEOLIST_LOADER_ID, loaderArgsBundle, loaderCallbacks);
            */
        }

        return(view);
    }

    @Override
    public void onListItemClick(ListView l, View v, int position, long id) {
        mCallback.onVideoSelected(position);
    }

    @Override
    public Loader<AsyncTaskResult<List<TmdbData.Video>>> onCreateLoader(int id, Bundle args) {
        mLoadingIndicator.setVisibility(View.VISIBLE);
        return new TmdbMovieVideoLoader(mContext, args);
    }

    @Override
    public void onLoadFinished(Loader<AsyncTaskResult<List<TmdbData.Video>>> loader,
                               AsyncTaskResult<List<TmdbData.Video>> result) {
        mLoadingIndicator.setVisibility(View.INVISIBLE);
        //mMovieGridAdapter.setMovieData(tmdbData);
        //mTmdbData = tmdbData;

        if (result.hasException()) {
            // There was an error during data loading
            Exception ex = result.getException();
            if (ex instanceof TmdbData.TmdbStatusException) {
                // TMDb API error
                showErrorMessage(ex.getMessage());
            } else {
                showErrorMessage(getResources().getString(R.string.error_msg_no_data));
            }
        } else {
            // Valid results received
            showMovieVideosView();
        }

    }

    @Override
    public void onLoaderReset(Loader<AsyncTaskResult<List<TmdbData.Video>>> loader) {
        // Not used
    }

    /**
     * This method will make the View for the video list visible and
     * hide the error message.
     */
    private void showMovieVideosView() {
        // Hide error message
        mErrorMessage.setVisibility(View.INVISIBLE);
        // Display video list
        mVideoList.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the video list
     */
    private void showErrorMessage(String errorMessage) {
        // Hide video list
        mVideoList.setVisibility(View.INVISIBLE);
        // Display error message
        mErrorMessage.setVisibility(View.VISIBLE);

        if (errorMessage != null && errorMessage.length() > 0) {
            mErrorMessage.setText(errorMessage);
        }
    }


    public static class AsyncTaskResult<T> {
        private final T result;
        private final Exception exception;

        public AsyncTaskResult(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public T getResult() {
            return result;
        }

        public boolean hasResult() {
            return result != null;
        }

        public Exception getException() {
            return exception;
        }

        public boolean hasException() {
            return exception != null;
        }

    }

    public static class TmdbMovieVideoLoader
            extends AsyncTaskLoader<AsyncTaskResult<List<TmdbData.Video>>> {

        final PackageManager mPackageManager;
        AsyncTaskResult<List<TmdbData.Video>> mResult;
        final Bundle mArgs;

        private TmdbMovieVideoLoader(Context context, Bundle args) {
            super(context);
            mPackageManager = getContext().getPackageManager();
            mArgs = args;
        }

        @Override
        protected void onStartLoading() {
            if (mResult.hasResult() || mResult.hasException()) {
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
        public AsyncTaskResult<List<TmdbData.Video>> loadInBackground() {
            // Get movie id
            int movieId = mArgs.getInt(LOADER_BUNDLE_MOVIE_ID, 1);

            try {
                // Load movie video list
                //URL movieVideosUrl = NetworkUtils.buildMovieVideosUrl(movieId);
                //String jsonMovieVideos = NetworkUtils.getResponseFromHttpUrl(movieVideosUrl);

                // Example mock request used for debugging to avoid sending network queries
                String jsonMovieVideos = MockDataUtils.getMockJson(getContext(), "mock_videos");

                TmdbJsonUtils.TmdbJsonResult<List<TmdbData.Video>> videoResult =
                        TmdbJsonUtils.getVideoListFromJson(jsonMovieVideos);

                mResult = new AsyncTaskResult<>(videoResult.getResult(), videoResult.getException());
                return mResult;

            } catch (IOException iex) {
                Log.e(TAG, "IOException when fetching API data.");
                iex.printStackTrace();
                mResult = new AsyncTaskResult<>(null, iex);
                return mResult;
            }
        }
    }
}
