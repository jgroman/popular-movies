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

    public static class Config {

        private String mSecureBaseUrl;

        public String getSecureBaseUrl() {
            return mSecureBaseUrl;
        }

        public void setSecureBaseUrl(String url) {
            mSecureBaseUrl = url;
        }
    }

    public static class Movie {

        private int mVoteCount;
        private int mId;
        private boolean mVideo;
        private float mVoteAverage;
        private String mTitle;
        private float mPopularity;
        private String mPosterPath;
        private String mOriginalLanguage;
        private String mOriginalTitle;
        private int[] mGenreIds;
        private String mBackdropPath;
        private boolean mAdult;
        private String mOverview;
        private String mReleaseDate;
    }
}
