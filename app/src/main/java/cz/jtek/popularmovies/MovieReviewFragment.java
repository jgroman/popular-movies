package cz.jtek.popularmovies;

import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.Uri;
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
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

import cz.jtek.popularmovies.utilities.NetworkUtils;
import cz.jtek.popularmovies.utilities.NetworkUtils.AsyncTaskResult;
import cz.jtek.popularmovies.utilities.TmdbJsonUtils;
import cz.jtek.popularmovies.utilities.UIUtils;

public class MovieReviewFragment extends Fragment
        implements AdapterView.OnItemClickListener {

    @SuppressWarnings("unused")
    private static final String TAG = MovieReviewFragment.class.getSimpleName();

    private Context mContext;

    ArrayList<TmdbData.Review> mReviewList;
    ReviewListItemAdapter mReviewListItemAdapter;

    private ListView mReviewListView;
    private TextView mErrorMessage;
    private ProgressBar mLoadingIndicator;

    private int mViewWidth;

    // AsyncLoader
    private static final int LOADER_ID_REVIEW_LIST = 33;
    private static final String LOADER_BUNDLE_MOVIE_ID = "movie-id";

    // Saved instance state bundle keys
    private static final String KEY_REVIEW_LIST = "review-list";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = getActivity();

        if (null == activity) {
            return null;
        }

        // Store current context
        mContext = activity.getApplicationContext();

        View view = inflater.inflate(R.layout.fragment_movie_review, container, false);

        // Obtain view width for list height calculation
        mViewWidth = UIUtils.getDisplayWidth(mContext);

        mErrorMessage = view.findViewById(R.id.tv_review_error_message);
        mLoadingIndicator = view.findViewById(R.id.pb_review_loading);

        if (savedInstanceState != null) {
            // Restoring review list from saved instance state
            mReviewList = savedInstanceState.getParcelableArrayList(KEY_REVIEW_LIST);
        }
        else {
            mReviewList = new ArrayList<>();
        }

        // Review ListView
        mReviewListItemAdapter = new ReviewListItemAdapter(mContext, R.layout.item_movie_review, mReviewList);
        mReviewListView = view.findViewById(R.id.lv_detail_reviews);
        mReviewListView.setAdapter(mReviewListItemAdapter);
        mReviewListView.setOnItemClickListener(this);
        UIUtils.showListViewFullHeight(mReviewListView, mViewWidth);

        if (savedInstanceState == null) {
            // If not restoring from saved instance state, start review list loader

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
                getLoaderManager().initLoader(LOADER_ID_REVIEW_LIST, loaderArgsBundle, reviewLoaderListener);
            } else {
                // Network is not available
                showErrorMessage(getResources().getString(R.string.error_msg_no_network));
            }
        }

        return(view);
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        // Store review list
        outState.putParcelableArrayList(KEY_REVIEW_LIST, mReviewList);

        super.onSaveInstanceState(outState);
    }
    
    /**
     * Review list item click listener
     *
     * @param adapterView   AdapterView where the click happened
     * @param view          Clicked view
     * @param position      Position of the view in the adapter
     * @param id            Row id of the clicked item
     */
    @Override
    public void onItemClick(AdapterView<?> adapterView, View view, int position, long id) {
        TmdbData.Review review =  mReviewListItemAdapter.getItem(position);

        if (review != null) {
            Uri webpage = Uri.parse(review.getUrl());
            Intent intent = new Intent(Intent.ACTION_VIEW, webpage);

            // Verify there's an app to receive the intent
            PackageManager packageManager = mContext.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(intent,0);
            boolean isIntentSafe = activities.size() > 0;

            if (isIntentSafe) { startActivity(intent); }
        }
    }

    /**
     * Loader callbacks for review loader
     */
    LoaderManager.LoaderCallbacks<AsyncTaskResult<ArrayList<TmdbData.Review>>> reviewLoaderListener =
            new LoaderManager.LoaderCallbacks<AsyncTaskResult<ArrayList<TmdbData.Review>>>() {

                @NonNull
                @Override
                public Loader<AsyncTaskResult<ArrayList<TmdbData.Review>>> onCreateLoader(int id, @Nullable Bundle args) {
                    // Show loading indicator
                    mLoadingIndicator.setVisibility(View.VISIBLE);
                    return new MovieReviewFragment.TmdbMovieReviewLoader(mContext, args);
                }

                @Override
                public void onLoadFinished(@NonNull Loader<AsyncTaskResult<ArrayList<TmdbData.Review>>> loader,
                                           AsyncTaskResult<ArrayList<TmdbData.Review>> data) {
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
                        mReviewListItemAdapter.addAll(data.getResult());
                        mReviewListItemAdapter.notifyDataSetChanged();
                        UIUtils.showListViewFullHeight(mReviewListView, mViewWidth);
                        showMovieReviewsView();
                    }

                    // Destroy this loader (otherwise is gets called twice for some reason)
                    getLoaderManager().destroyLoader(LOADER_ID_REVIEW_LIST);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<NetworkUtils.AsyncTaskResult<ArrayList<TmdbData.Review>>> loader) {
                    // Not used
                }
            };

    /**
     * This method will make the View for the review list visible and
     * hide the error message.
     */
    private void showMovieReviewsView() {
        // Hide error message
        mErrorMessage.setVisibility(View.INVISIBLE);
        // Display review list
        mReviewListView.setVisibility(View.VISIBLE);
    }

    /**
     * This method will make the error message visible and hide the review list
     */
    private void showErrorMessage(String errorMessage) {
        // Hide video list
        mReviewListView.setVisibility(View.INVISIBLE);
        // Display error message
        mErrorMessage.setVisibility(View.VISIBLE);

        if (errorMessage != null && errorMessage.length() > 0) {
            mErrorMessage.setText(errorMessage);
        }
    }


    /**
     * Custom ArrayAdapter for review list
     */
    class ReviewListItemAdapter extends ArrayAdapter<TmdbData.Review> {

        private final int mResource;

        ReviewListItemAdapter(Context context, int resource, List<TmdbData.Review> reviewList) {
            super(context, resource, reviewList);
            mResource = resource;
        }

        @NonNull
        @Override
        public View getView(int position, @Nullable View convertView, @NonNull ViewGroup parent) {

            View rowView = convertView;

            if (rowView == null) {
                rowView = LayoutInflater.from(getContext()).inflate(mResource, parent, false);
            }

            TmdbData.Review review = getItem(position);

            if (review != null) {
                // Author
                TextView reviewAuthorTextView = rowView.findViewById(R.id.tv_review_item_author);
                reviewAuthorTextView.setText(review.getAuthor());
                // Content
                TextView reviewContentTextView = rowView.findViewById(R.id.tv_review_item_content);
                reviewContentTextView.setText(review.getContent());
            }

            return rowView;
        }
    }

    /**
     * Review list AsyncTaskLoader implementation
     */
    public static class TmdbMovieReviewLoader
            extends AsyncTaskLoader<NetworkUtils.AsyncTaskResult<ArrayList<TmdbData.Review>>> {

        final PackageManager mPackageManager;
        AsyncTaskResult<ArrayList<TmdbData.Review>> mResult;
        final Bundle mArgs;

        private TmdbMovieReviewLoader(Context context, Bundle args) {
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
        public AsyncTaskResult<ArrayList<TmdbData.Review>> loadInBackground() {
            // Get movie id from argument bundle
            int movieId = mArgs.getInt(LOADER_BUNDLE_MOVIE_ID, 1);

            try {
                // Load movie review list
                URL movieReviewsUrl = NetworkUtils.buildMovieReviewsUrl(movieId);
                String jsonMovieReviews = NetworkUtils.getResponseFromHttpUrl(movieReviewsUrl);

                // Example mock request used for debugging to avoid sending network queries
                // String jsonMovieReviews = MockDataUtils.getMockJson(getContext(), "mock_reviews");

                // Use only videos of type "Trailer"
                TmdbJsonUtils.TmdbJsonResult<ArrayList<TmdbData.Review>> reviewResult =
                        TmdbJsonUtils.getReviewListFromJson(jsonMovieReviews);

                mResult = new AsyncTaskResult<>(reviewResult.getResult(), reviewResult.getException());
            } catch (IOException iex) {
                Log.e(TAG, "IOException when fetching API data.");
                iex.printStackTrace();
                mResult = new AsyncTaskResult<>(null, iex);
            }
            return mResult;
        }
    }

}
