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

public class TmdbApi {

    // API Status Codes
    // https://www.themoviedb.org/documentation/api/status-codes
    public static class Status {
        // JSON object names
        public static final String STATUS_CODE = "status_code";
        public static final String STATUS_MSG = "status_message";
    }

    public static class Config {
        // JSON object names
        public static final String IMAGES = "images";
        public static final String SECURE_BASE_URL = "secure_base_url";

        private String mSecureBaseUrl;

        public String getSecureBaseUrl() {
            return mSecureBaseUrl;
        }

        public void setSecureBaseUrl(String url) {
            mSecureBaseUrl = url;
        }
    }

    public static class Movie {
        // JSON object names
        public static final String RESULTS = "results";
        public static final String TITLE = "title";
        public static final String RELEASE_DATE = "release_date";
        public static final String POSTER_PATH = "poster_path";
        public static final String VOTE_AVERAGE = "vote_average";
        public static final String OVERVIEW = "overview";

        private int mVoteCount;
        private int mId;
        private boolean mVideo;
        private double mVoteAverage;
        private String mTitle;
        private double mPopularity;
        private String mPosterPath;
        private String mOriginalLanguage;
        private String mOriginalTitle;
        private int[] mGenreIds;
        private String mBackdropPath;
        private boolean mAdult;
        private String mOverview;
        private String mReleaseDate;

        // Title
        public String getTitle() { return mTitle; }
        public void setTitle(String title) { mTitle = title; }

        // Release date
        public String getReleaseDate() { return mReleaseDate; }
        public void setReleaseDate(String releaseDate) { mReleaseDate = releaseDate; }

        // Poster path
        public String getPosterPath() { return mPosterPath; }
        public void setPosterPath(String posterPath) { mPosterPath = posterPath; }

        // Vote average
        public double getVoteAverage() { return mVoteAverage; }
        public void setVoteAverage(double voteAverage) { mVoteAverage = voteAverage; }

        // Overview
        public String getOverview() { return mOverview; }
        public void setOverview(String overview) { mOverview = overview; }

    }
}
