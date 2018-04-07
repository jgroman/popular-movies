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
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.AsyncTaskLoader;
import android.support.v4.content.Loader;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

import cz.jtek.popularmovies.utilities.MockDataUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils.AsyncTaskResult;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;
import cz.jtek.popularmovies.utilities.UIUtils;

public class MovieVideoFragment extends Fragment
    implements LoaderManager.LoaderCallbacks<AsyncTaskResult<List<TmdbData.Video>>>,
    AdapterView.OnItemClickListener
{
    private static final String TAG = MovieVideoFragment.class.getSimpleName();

    private Context mContext;

    List<TmdbData.Video> mVideoList;
    VideoListItemAdapter mVideoListItemAdapter;

    private ListView mVideoListView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    private int mViewWidth;

    // AsyncLoader
    private static final int LOADER_ID_VIDEO_LIST = 2;
    private static final String LOADER_BUNDLE_MOVIE_ID = "movie-id";

    @Nullable
    @Override
    public View onCreateView(@NonNull  LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = getActivity();

        if (null == activity) { return null; }

        // Store current context
        mContext = activity.getApplicationContext();

        View view = inflater.inflate(R.layout.fragment_movie_video, container, false);

        // Obtain view width for list height calculation
        mViewWidth = UIUtils.getDisplayWidth(mContext);

        // Video ListView
        mVideoList = new ArrayList<>();
        mVideoListItemAdapter = new VideoListItemAdapter(mContext, R.layout.item_movie_video, mVideoList);
        mVideoListView = view.findViewById(R.id.lv_detail_videos);
        mVideoListView.setAdapter(mVideoListItemAdapter);
        mVideoListView.setOnItemClickListener(this);
        UIUtils.showListViewFullHeight(mVideoListView, mViewWidth);

        mErrorMessage = view.findViewById(R.id.tv_video_error_message);
        mLoadingIndicator = view.findViewById(R.id.pb_video_loading);

        int movieId = 0;
        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(MovieDetailActivity.BUNDLE_MOVIE_ID)) {
                // Obtain movie ID from fragment arguments
                movieId = args.getInt(MovieDetailActivity.BUNDLE_MOVIE_ID);
            }
        }

        // Check for network availability
        if (NetworkUtils.isNetworkAvailable(mContext)) {
            // Store movie id into loader args bundle
            Bundle loaderArgsBundle = new Bundle();
            loaderArgsBundle.putInt(LOADER_BUNDLE_MOVIE_ID, movieId);
            // Loader initialization
            getLoaderManager().initLoader(LOADER_ID_VIDEO_LIST, loaderArgsBundle, MovieVideoFragment.this);
        } else {
            // Network is not available
            showErrorMessage(getResources().getString(R.string.error_msg_no_network));
        }

        return(view);
    }

    /**
     * Video list item click listener
     *
     * @param adapterView   AdapterView where the click happened
     * @param view             Clicked view
     * @param position        Position of the view in the adapter
     * @param id                Row id of the clicked item
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        TmdbData.Video video =  mVideoListItemAdapter.getItem(position);

        if (video != null && video.getSite().equals("YouTube")) {
            // NetworkUtils.openYoutubeIntent(mContext, video.getKey());
        }
    }

    @NonNull
    @Override
    public Loader<AsyncTaskResult<List<TmdbData.Video>>> onCreateLoader(int id, Bundle args) {
        // Show loading indicator
        mLoadingIndicator.setVisibility(View.VISIBLE);
        return new TmdbMovieVideoLoader(mContext, args);
    }

    @Override
    public void onLoadFinished(@NonNull  Loader<AsyncTaskResult<List<TmdbData.Video>>> loader,
                               AsyncTaskResult<List<TmdbData.Video>> data) {
        // Hide loading indicator
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
            mVideoList = data.getResult();
            mVideoListItemAdapter.addAll(mVideoList);
            mVideoListItemAdapter.notifyDataSetChanged();
            UIUtils.showListViewFullHeight(mVideoListView, mViewWidth);
            showMovieVideosView();
        }
    }

    @Override
    public void onLoaderReset(@NonNull  Loader<AsyncTaskResult<List<TmdbData.Video>>> loader) {
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
        mVideoListView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the video list
     */
    private void showErrorMessage(String errorMessage) {
        // Hide video list
        mVideoListView.setVisibility(View.INVISIBLE);
        // Display error message
        mErrorMessage.setVisibility(View.VISIBLE);

        if (errorMessage != null && errorMessage.length() > 0) {
            mErrorMessage.setText(errorMessage);
        }
    }


    /**
     * Custom ArrayAdapter for video list
     */
    class VideoListItemAdapter extends ArrayAdapter<TmdbData.Video> {

        private final int mResource;

        VideoListItemAdapter(Context context, int resource, List<TmdbData.Video> videoList) {
            super(context, resource, videoList);
            mResource = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View rowView = convertView;

            if (rowView == null) {
                rowView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
            }

            TmdbData.Video video = getItem(position);

            if (video != null) {
                TextView videoNameTextView = rowView.findViewById(R.id.tv_video_item_name);
                videoNameTextView.setText(video.getName());
            }

            return rowView;
        }
    }

    /**
     * Video list AsyncTaskLoader implementation
     */
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
            if (mResult != null && (mResult.hasResult() || mResult.hasException())) {
                // If there are already data available, deliver them
                deliverResult(mResult);
            } else {
                // Start background loader
                forceLoad();
            }
        }

        @Override
        protected void onStopLoading() { cancelLoad(); }

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

                // Use only videos of type "Trailer"
                TmdbJsonUtils.TmdbJsonResult<List<TmdbData.Video>> videoResult =
                        TmdbJsonUtils.getVideoListFromJson(jsonMovieVideos, TmdbData.Video.TYPE_TRAILER);

                mResult = new AsyncTaskResult<>(videoResult.getResult(), videoResult.getException());
            } catch (IOException iex) {
                Log.e(TAG, "IOException when fetching API data.");
                iex.printStackTrace();
                mResult = new AsyncTaskResult<>(null, iex);
            }
            return mResult;
        }
    }
}
