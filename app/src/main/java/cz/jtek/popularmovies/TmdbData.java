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

import android.os.Parcel;
import android.os.Parcelable;

import java.util.ArrayList;
import java.util.List;

public class TmdbData {

    // API Status
    // https://www.themoviedb.org/documentation/api/status-codes
    public static final String STATUS_CODE = "status_code";
    public static final String STATUS_MSG = "status_message";

    // API Configuration
    public static final String CONFIG_IMAGES = "images";
    public static final String CONFIG_SECURE_BASE_URL = "secure_base_url";

    private static final String DEFAULT_IMAGE_SIZE = "w185";
    private static final int DEFAULT_MOVIE_POSTER_WIDTH = 185;
    private static final int DEFAULT_MOVIE_POSTER_HEIGHT = 278;

    // API Movie
    public static final String MOVIE_RESULTS = "results";
    public static final String MOVIE_TITLE = "title";
    public static final String MOVIE_RELEASE_DATE = "release_date";
    public static final String MOVIE_POSTER_PATH = "poster_path";
    public static final String MOVIE_VOTE_AVERAGE = "vote_average";
    public static final String MOVIE_OVERVIEW = "overview";

    final private TmdbData.Config mConfig;
    final private List<Movie> mMovieList;
    final private Status mStatus;

    TmdbData() {
        mStatus = new Status();
        mConfig = new Config();
        mMovieList = new ArrayList<>();
    }

    public TmdbData.Status getStatus() {
        return mStatus;
    }

    public TmdbData.Config getConfig() {
        return mConfig;
    }

    public List<Movie> getMovieList() {
        return mMovieList;
    }

    public static class Status {
        private boolean mDataValid;
        // private int mStatusCode;
        private String mStatusMessage;

        Status() {
            mDataValid = true;
        }

        public void setDataValid(boolean valid) {
            mDataValid = valid;
        }

        boolean getDataValid() {
            return mDataValid;
        }

        /*
        public void setStatusCode(int code) {
            mStatusCode = code;
        }

        public int getStatusCode() { return mStatusCode; }
        */

        public void setStatusMessage(String message) {
            mStatusMessage = message;
        }

        String getStatusMessage() {
            return mStatusMessage;
        }
    }

    public static class Config implements Parcelable {
        private String mSecureBaseUrl;

        String getSecureBaseUrl() {
            return mSecureBaseUrl;
        }
        public void setSecureBaseUrl(String url) {
            mSecureBaseUrl = url;
        }

        static String getPosterSize() { return DEFAULT_IMAGE_SIZE; }

        static int getPosterWidth() { return DEFAULT_MOVIE_POSTER_WIDTH; }
        static int getPosterHeight() { return DEFAULT_MOVIE_POSTER_HEIGHT; }

        public Config() {}

        private Config(Parcel in) {
            mSecureBaseUrl = in.readString();
        }

        @Override
        public int describeContents() {
            // No file descriptors in class members
            return 0;
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(mSecureBaseUrl);
        }

        static final Parcelable.Creator<Config> CREATOR
                = new Parcelable.Creator<Config>() {

            public Config createFromParcel(Parcel in) {
                return new Config(in);
            }

            public Config[] newArray(int size) {
                return new Config[size];
            }
        };
    }

    public static class Movie implements Parcelable {

        /* Movie API properties */
        // private int mVoteCount;
        // private int mId;
        // private boolean mVideo;
        private double mVoteAverage;
        private String mTitle;
        // private double mPopularity;
        private String mPosterPath;
        // private String mOriginalLanguage;
        // private String mOriginalTitle;
        // private int[] mGenreIds;
        // private String mBackdropPath;
        // private boolean mAdult;
        private String mOverview;
        private String mReleaseDate;

        // Title
        public String getTitle() { return mTitle; }
        public void setTitle(String title) { mTitle = title; }

        // Release date
        String getReleaseDate() { return mReleaseDate; }
        public void setReleaseDate(String releaseDate) { mReleaseDate = releaseDate; }

        // Poster path
        String getPosterPath() { return mPosterPath; }
        public void setPosterPath(String posterPath) { mPosterPath = posterPath; }

        // Vote average
        double getVoteAverage() { return mVoteAverage; }
        public void setVoteAverage(double voteAverage) { mVoteAverage = voteAverage; }

        // Overview
        String getOverview() { return mOverview; }
        public void setOverview(String overview) { mOverview = overview; }

        @Override
        public int describeContents() {
            // No file descriptors in class members
            return 0;
        }

        public Movie() { }

        private Movie(Parcel in) {
            mTitle = in.readString();
            mOverview = in.readString();
            mPosterPath = in.readString();
            mReleaseDate = in.readString();
            mVoteAverage = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeString(mTitle);
            parcel.writeString(mOverview);
            parcel.writeString(mPosterPath);
            parcel.writeString(mReleaseDate);
            parcel.writeDouble(mVoteAverage);
        }

        static final Parcelable.Creator<Movie> CREATOR
                = new Parcelable.Creator<Movie>() {

            public Movie createFromParcel(Parcel in) {
                return new Movie(in);
            }

            public Movie[] newArray(int size) {
                return new Movie[size];
            }
        };

    }
}
