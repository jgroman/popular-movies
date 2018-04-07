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
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v7.app.AppCompatActivity;

public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = MovieDetailActivity.class.getSimpleName();

    public static final String BUNDLE_MOVIE_ID = "movie-id";

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        if (findViewById(R.id.detail_container) != null) {

            if (null == savedInstanceState) {

                Intent startingIntent = getIntent();
                int movieId = 0;

                if (startingIntent == null) {
                    return;
                }

                if (startingIntent.hasExtra(MainActivity.EXTRA_MOVIE)) {
                    TmdbData.Movie movie = startingIntent.getParcelableExtra(MainActivity.EXTRA_MOVIE);
                    if (movie != null) {
                        movieId = movie.getId();
                    }
                }

                MovieDetailFragment detail = new MovieDetailFragment();
                detail.setArguments(startingIntent.getExtras());

                Bundle fragmentBundle = new Bundle();
                fragmentBundle.putInt(BUNDLE_MOVIE_ID, movieId);

                MovieVideoFragment video = new MovieVideoFragment();
                video.setArguments(fragmentBundle);

                MovieReviewFragment review = new MovieReviewFragment();
                video.setArguments(fragmentBundle);

                // Add fragments to detail fragment container
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.detail_container, detail)
                        .add(R.id.detail_container, video)
                        .add(R.id.detail_container, review)
                        .commit();
            }
        }


    }

}
