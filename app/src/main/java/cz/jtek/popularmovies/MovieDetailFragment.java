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
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
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

public class MovieDetailFragment extends Fragment {

    private static final String TAG = MovieDetailFragment.class.getSimpleName();

    private Context mContext;
    ToggleButton mFavoriteToggle;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {

        super.onCreateView(inflater, container, savedInstanceState);

        Activity activity = getActivity();
        if (null == activity) { return null; }

        // Store current context
        mContext = getActivity().getApplicationContext();

        View view = inflater.inflate(R.layout.fragment_movie_detail, container, false);

        mFavoriteToggle  = view.findViewById(R.id.tb_favorite);
        mFavoriteToggle.setOnCheckedChangeListener(onFavoriteToggleClick);

        Bundle args = getArguments();

        if (args != null) {
            if (args.containsKey(MainActivity.EXTRA_MOVIE) && args.containsKey(MainActivity.EXTRA_CONFIG)) {

                // TMDb configuration contains base URL for posters
                TmdbData.Config config = args.getParcelable(MainActivity.EXTRA_CONFIG);
                TmdbData.Movie movie = args.getParcelable(MainActivity.EXTRA_MOVIE);

                if (movie != null) {
                    // Movie title
                    TextView titleTextView = view.findViewById(R.id.tv_detail_title);
                    titleTextView.setText(movie.getTitle());

                    // Poster
                    if (config != null) {
                        ImageView posterImageView = view.findViewById(R.id.iv_detail_poster);
                        String posterBaseUrl = config.getSecureBaseUrl() + TmdbData.Config.getPosterSize();

                        Picasso.with(mContext)
                                .load(posterBaseUrl + movie.getPosterPath())
                                .into(posterImageView);
                    }

                    // Vote average
                    TextView voteAverageTextView = view.findViewById(R.id.tv_detail_vote_average);
                    voteAverageTextView.setText(String.format(Locale.getDefault(),"%.1f", movie.getVoteAverage()));

                    // Release date
                    TextView releaseTextView = view.findViewById(R.id.tv_detail_release_date);

                    String releaseDateString = movie.getReleaseDate();

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

                    String overview = movie.getOverview();
                    SpannableString overviewSpannable = new SpannableString(overview);
                    overviewSpannable.setSpan(new LeadingMarginSpan.Standard(24, 0),0, overview.length(),0);

                    overviewTextView.setText(overviewSpannable);

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
                    if (isChecked) {
                        // The toggle is enabled
                        Log.d(TAG, "onCheckedChanged: enabled");
                    } else {
                        // The toggle is disabled
                        Log.d(TAG, "onCheckedChanged: disabled");
                    }
                }
            };
}
