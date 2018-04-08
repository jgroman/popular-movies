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
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v4.app.LoaderManager;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.CompoundButton;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.ToggleButton;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

import cz.jtek.popularmovies.data.MovieContract;

public class MovieDetailFragment extends Fragment {

    private static final String TAG = MovieDetailFragment.class.getSimpleName();

    private Context mContext;
    ToggleButton mFavoriteToggle;
    private TmdbData.Movie mMovie = new TmdbData.Movie();

    private static final int LOADER_ID_FAVORITE_ITEM = 12;
    private static final String LOADER_BUNDLE_MOVIE_ID = "movie-id";

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = getActivity();
        if (null == activity) { return null; }

        mContext = activity.getApplicationContext();

        View view = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        mFavoriteToggle  = view.findViewById(R.id.tb_favorite);
        mFavoriteToggle.setOnCheckedChangeListener(onFavoriteToggleClick);

        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(MainActivity.EXTRA_MOVIE)) {

                mMovie = args.getParcelable(MainActivity.EXTRA_MOVIE);

                if (mMovie != null) {

                    // Movie title
                    TextView titleTextView = view.findViewById(R.id.tv_detail_title);
                    titleTextView.setText(mMovie.getTitle());

                    // Poster
                    ImageView posterImageView = view.findViewById(R.id.iv_detail_poster);
                    Picasso.with(mContext)
                            .load(mMovie.getPosterPath())
                            .into(posterImageView);

                    // Vote average
                    TextView voteAverageTextView = view.findViewById(R.id.tv_detail_vote_average);
                    voteAverageTextView.setText(String.format(Locale.getDefault(),"%.1f", mMovie.getVoteAverage()));

                    // Release date
                    TextView releaseTextView = view.findViewById(R.id.tv_detail_release_date);

                    String releaseDateString = mMovie.getReleaseDate();

                    DateFormat dateFormatAPI = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                    DateFormat dateFormatOutput = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());

                    try {
                        Date releaseDate = dateFormatAPI.parse(releaseDateString);
                        releaseTextView.setText(dateFormatOutput.format(releaseDate));
                    } catch (ParseException pe) {
                        Log.e(TAG, "Release date parse exception.");
                    }

                    // Overview
                    TextView overviewTextView = view.findViewById(R.id.tv_detail_overview);

                    String overview = mMovie.getOverview();
                    SpannableString overviewSpannable = new SpannableString(overview);
                    overviewSpannable.setSpan(new LeadingMarginSpan.Standard(24, 0),0, overview.length(),0);

                    overviewTextView.setText(overviewSpannable);

                    // Start favorite status loader
                    Bundle loaderArgsBundle = new Bundle();
                    loaderArgsBundle.putInt(LOADER_BUNDLE_MOVIE_ID, mMovie.getId());
                    // Loader initialization
                    getLoaderManager().initLoader(LOADER_ID_FAVORITE_ITEM, loaderArgsBundle, favoriteItemLoaderListener);

                }
            }
        }
        return(view);
    }

    /**
     * Favorite toggle button change listener
     */
    CompoundButton.OnCheckedChangeListener onFavoriteToggleClick =
            new CompoundButton.OnCheckedChangeListener() {
                @Override
                public void onCheckedChanged(CompoundButton compoundButton, boolean isChecked) {
                    processFavoriteMovie(mMovie, isChecked);
                }
            };


    /**
     * Store favorite status changes
     *
     * @param movie        Movie to be processed
     * @param isFavorite  Favorite flag status
     */
    private void processFavoriteMovie(@NonNull TmdbData.Movie movie, boolean isFavorite) {

        if (isFavorite) {
            // Store favorite movie and set favorite flag to 1
            ContentValues values = new ContentValues();
            values.put(MovieContract.MovieEntry.COL_MOVIE_ID, movie.getId());
            values.put(MovieContract.MovieEntry.COL_TITLE, movie.getTitle());
            values.put(MovieContract.MovieEntry.COL_OVERVIEW, movie.getOverview());
            values.put(MovieContract.MovieEntry.COL_POSTER_PATH, movie.getPosterPath());
            values.put(MovieContract.MovieEntry.COL_RELEASE_DATE, movie.getReleaseDate());
            values.put(MovieContract.MovieEntry.COL_VOTE_AVERAGE, movie.getVoteAverage());
            values.put(MovieContract.MovieEntry.COL_FAVORITE, 1);

            mContext.getContentResolver().insert(MovieContract.MovieEntry.CONTENT_URI, values);
        }
        else {
            // Currently there's no need for non favorite movies to be stored
            Uri currentMovieUri = ContentUris.withAppendedId(MovieContract.MovieEntry.CONTENT_URI, movie.getId());
            int rowsDeleted = mContext.getContentResolver().delete(currentMovieUri, null, null);
            if (rowsDeleted == 0) {
                Log.e(TAG, "processFavoriteMovie: Error deleting movie id " + movie.getId() );
            }
        }
    }

    /**
     * Loader callbacks for favorite item loader
     */
    private LoaderManager.LoaderCallbacks<Cursor> favoriteItemLoaderListener =
            new LoaderManager.LoaderCallbacks<Cursor>() {

                @NonNull
                @Override
                public Loader<Cursor> onCreateLoader(int id, @Nullable Bundle args) {
                    if (args == null) {
                        throw new NullPointerException("Argument bundle cannot be null");
                    }

                    int movieId = args.getInt(LOADER_BUNDLE_MOVIE_ID, 0);

                    String[] projection = {
                            MovieContract.MovieEntry._ID,
                            MovieContract.MovieEntry.COL_MOVIE_ID
                    };
                    String selection = MovieContract.MovieEntry.COL_MOVIE_ID + " = ?";
                    selection += " AND " + MovieContract.MovieEntry.COL_FAVORITE + " = ?";
                    String[] selectionArgs = new String[]{ String.valueOf(movieId), "1"};

                    return new CursorLoader(mContext,
                            MovieContract.MovieEntry.CONTENT_URI,
                            projection,
                            selection,
                            selectionArgs,
                            null
                    );
                }

                @Override
                public void onLoadFinished(@NonNull Loader<Cursor> loader, Cursor data) {
                    if (data.getCount() > 0) {
                        // Non zero rows on favorite query cursor = this movie is favorite
                        mFavoriteToggle.setChecked(true);
                    }
                    else {
                        mFavoriteToggle.setChecked(false);
                    }

                    // Destroy this loader (otherwise is gets called twice for some reason)
                    getLoaderManager().destroyLoader(LOADER_ID_FAVORITE_ITEM);
                }

                @Override
                public void onLoaderReset(@NonNull Loader<Cursor> loader) {
                    // Not implemented
                }
            };

}
