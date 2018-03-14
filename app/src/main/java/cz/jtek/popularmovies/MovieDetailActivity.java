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
import android.text.SpannableString;
import android.text.style.LeadingMarginSpan;
import android.util.Log;
import android.widget.ImageView;
import android.widget.TextView;

import com.squareup.picasso.Picasso;

import java.text.DateFormat;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

public class MovieDetailActivity extends AppCompatActivity {

    private static final String TAG = MovieDetailActivity.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        Intent startingIntent = getIntent();

        if (startingIntent != null) {
            if (startingIntent.hasExtra(MainActivity.EXTRA_DETAIL_TITLE)) {
                TextView titleTextView = findViewById(R.id.tv_detail_title);

                String title = startingIntent.getStringExtra(MainActivity.EXTRA_DETAIL_TITLE);
                titleTextView.setText(title);
            }

            if (startingIntent.hasExtra(MainActivity.EXTRA_DETAIL_POSTER_URL)) {
                ImageView posterImageView = findViewById(R.id.iv_detail_poster);

                String posterUrl = startingIntent.getStringExtra(MainActivity.EXTRA_DETAIL_POSTER_URL);
                Picasso.with(this)
                        .load(posterUrl)
                        .into(posterImageView);
            }

            if (startingIntent.hasExtra(MainActivity.EXTRA_DETAIL_VOTE_AVERAGE)) {
                TextView voteAverageTextView = findViewById(R.id.tv_detail_vote_average);

                double voteAverage = startingIntent.getDoubleExtra(MainActivity.EXTRA_DETAIL_VOTE_AVERAGE, 0.0f);
                voteAverageTextView.setText(String.format(Locale.getDefault(),"%.1f", voteAverage));
            }

            if (startingIntent.hasExtra(MainActivity.EXTRA_DETAIL_RELEASE_DATE)) {
                TextView releaseTextView = findViewById(R.id.tv_detail_release_date);

                String releaseDateString = startingIntent.getStringExtra(MainActivity.EXTRA_DETAIL_RELEASE_DATE);

                DateFormat dateFormatAPI = new SimpleDateFormat("yyyy-MM-dd", Locale.US);
                DateFormat dateFormatOutput = DateFormat.getDateInstance(DateFormat.SHORT, Locale.getDefault());

                try {
                    Date releaseDate = dateFormatAPI.parse(releaseDateString);
                    releaseTextView.setText(dateFormatOutput.format(releaseDate));
                } catch (ParseException pe) {
                    Log.e(TAG, "Release date parse exception.");
                }

            }

            if (startingIntent.hasExtra(MainActivity.EXTRA_DETAIL_OVERVIEW)) {
                TextView overviewTextView = findViewById(R.id.tv_detail_overview);

                String overview = startingIntent.getStringExtra(MainActivity.EXTRA_DETAIL_OVERVIEW);
                SpannableString overviewSpannable = new SpannableString(overview);
                overviewSpannable.setSpan(new LeadingMarginSpan.Standard(24, 0),0, overview.length(),0);

                overviewTextView.setText(overviewSpannable);
            }

        }
    }
}
