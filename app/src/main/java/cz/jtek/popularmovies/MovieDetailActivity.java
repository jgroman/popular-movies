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

public class MovieDetailActivity extends AppCompatActivity
    implements MovieVideoFragment.OnVideoSelectedListener {

    private static final String TAG = MovieDetailActivity.class.getSimpleName();

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        setContentView(R.layout.activity_detail);

        if (findViewById(R.id.detail_container) != null) {

            if (null == savedInstanceState) {

                MovieDetailFragment detail = new MovieDetailFragment();
                Intent startingIntent = getIntent();
                if (startingIntent != null) {
                    detail.setArguments(startingIntent.getExtras());
                }

                MovieVideoFragment video = new MovieVideoFragment();

                MovieReviewFragment review = new MovieReviewFragment();

                // Add fragments to detail fragment container
                getSupportFragmentManager().beginTransaction()
                        .add(R.id.detail_container, detail)
                        .add(R.id.detail_container, video)
                        .add(R.id.detail_container, review)
                        .commit();
            }
        }


    }

    @Override
    public void onVideoSelected(int position) {

    }
}
