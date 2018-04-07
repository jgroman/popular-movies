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
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;

public class TmdbData {

    private static final String TAG = TmdbData.class.getSimpleName();

    /**
     *
     */
    public static class TmdbStatusException extends Exception {
        public TmdbStatusException(String message) {
            super(message);
        }

        public TmdbStatusException(int statusCode, String statusMessage) {
            super("TMDb status: " + statusCode + " (" + statusMessage + ")");
        }

    }

    /**
     * TMDb Status object
     */
    public static class Status {
        // API Status
        // https://www.themoviedb.org/documentation/api/status-codes
        static final String CODE = "status_code";
        static final String MSG = "status_message";

        int mCode;
        String mMessage;

        Status() {}

        // Code
        public void setCode(int code) { mCode = code; }
        public int getCode() { return mCode; }

        // Message
        public void setMessage(String msg) { mMessage = msg; }
        public String getMessage() { return mMessage; }

        // Constructor converting JSON object to object instance
        public static Status fromJson(JSONObject jsonObject)
                throws JSONException {
            Status s = new Status();

            if (jsonObject.has(CODE)) {
                s.mCode = jsonObject.getInt(CODE);
            }

            if (jsonObject.has(MSG)) {
                s.mMessage = jsonObject.getString(MSG);
            }

            return s;
        }

        /**
         * Check whether status code is present in JSON object
         * It would mean that API request failed
         *
         * @param jsonObject JSON object to check
         *
         * @return true if status code is present
         */
        public static boolean isPresent(JSONObject jsonObject) {
            return jsonObject.has(CODE);
        }
    }

    /**
     * TMDb Configuration object
     */
    public static class Config implements Parcelable {
        // API Configuration
        // https://developers.themoviedb.org/3/configuration/get-api-configuration
        static final String IMAGES = "images";
        static final String SECURE_BASE_URL = "secure_base_url";

        // These values are currently hardcoded
        static final String DEFAULT_IMAGE_SIZE = "w185";
        static final int DEFAULT_MOVIE_POSTER_WIDTH = 185;
        static final int DEFAULT_MOVIE_POSTER_HEIGHT = 278;

        String mSecureBaseUrl;

        static String getPosterSize() { return DEFAULT_IMAGE_SIZE; }

        static int getPosterWidth() { return DEFAULT_MOVIE_POSTER_WIDTH; }
        static int getPosterHeight() { return DEFAULT_MOVIE_POSTER_HEIGHT; }

        String getSecureBaseUrl() { return mSecureBaseUrl; }
        public void setSecureBaseUrl(String url) { mSecureBaseUrl = url; }

        Config() {}

        // Constructor converting JSON object to object instance
        public static Config fromJson(JSONObject jsonObject)
                throws JSONException {
            Config c = new Config();

            // Currently we are only interested in "secure base URL" string from "images" object
            if (jsonObject.has(IMAGES)) {
                JSONObject imagesObject = jsonObject.getJSONObject(IMAGES);
                if (imagesObject.has(SECURE_BASE_URL)) {
                    c.mSecureBaseUrl = imagesObject.getString(SECURE_BASE_URL);
                }
            }
            return c;
        }

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

    /**
     *
     */
    public static class Movie implements Parcelable {
        // API Movie
        // https://developers.themoviedb.org/3/movies/get-popular-movies
        // https://developers.themoviedb.org/3/movies/get-top-rated-movies
        public static final String RESULTS = "results";
        static final String MOVIE_ID = "id";
        static final String TITLE = "title";
        static final String RELEASE_DATE = "release_date";
        static final String POSTER_PATH = "poster_path";
        static final String VOTE_AVERAGE = "vote_average";
        static final String OVERVIEW = "overview";

        // Movie API properties
        // int mVoteCount;
        int mId;
        // boolean mVideo;
        double mVoteAverage;
        String mTitle;
        // double mPopularity;
        String mPosterPath;
        // String mOriginalLanguage;
        // String mOriginalTitle;
        // int[] mGenreIds;
        // String mBackdropPath;
        // boolean mAdult;
        String mOverview;
        String mReleaseDate;

        // Id
        public int getId() { return mId; }
        public void setId(int id) { mId = id; }

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

        public Movie() { }

        // Constructor converting JSON object to object instance
        static Movie fromJson(JSONObject jsonObject)
                throws JSONException {
            Movie m = new Movie();

            if (jsonObject.has(MOVIE_ID)) {
                m.mId = jsonObject.getInt(TmdbData.Movie.MOVIE_ID);
            }

            if (jsonObject.has(TITLE)) {
                m.mTitle = jsonObject.getString(TmdbData.Movie.TITLE);
            }

            if (jsonObject.has(RELEASE_DATE)) {
                m.mReleaseDate = jsonObject.getString(RELEASE_DATE);
            }

            if (jsonObject.has(POSTER_PATH)) {
                m.mPosterPath = jsonObject.getString(POSTER_PATH);
            }

            if (jsonObject.has(VOTE_AVERAGE)) {
                m.mVoteAverage = jsonObject.getDouble(VOTE_AVERAGE);
            }

            if (jsonObject.has(OVERVIEW)) {
                m.mOverview = jsonObject.getString(OVERVIEW);
            }

            return m;
        }

        // Factory method for converting JSON object array to list of object instances
        public static ArrayList<Movie> fromJson(JSONArray jsonArray)
                throws JSONException {
            JSONObject movieJson;

            int objectCount = jsonArray.length();
            ArrayList<Movie> movies = new ArrayList<>(objectCount);
            for (int i = 0; i < objectCount; i++) {
                movieJson = jsonArray.getJSONObject(i);
                Movie m = Movie.fromJson(movieJson);
                if (m != null) { movies.add(m); }
            }
            return movies;
        }

        private Movie(Parcel in) {
            mId = in.readInt();
            mTitle = in.readString();
            mOverview = in.readString();
            mPosterPath = in.readString();
            mReleaseDate = in.readString();
            mVoteAverage = in.readDouble();
        }

        @Override
        public void writeToParcel(Parcel parcel, int flags) {
            parcel.writeInt(mId);
            parcel.writeString(mTitle);
            parcel.writeString(mOverview);
            parcel.writeString(mPosterPath);
            parcel.writeString(mReleaseDate);
            parcel.writeDouble(mVoteAverage);
        }

        @Override
        public int describeContents() {
            // No file descriptors in class members
            return 0;
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

    /**
     *
     */
    public static class Video {
        // API Movie Videos
        // https://developers.themoviedb.org/3/movies/get-movie-videos
        public static final String RESULTS = "results";
        static final String VIDEO_ID = "id";
        static final String KEY = "key";
        static final String NAME = "name";
        static final String SITE = "site";
        static final String TYPE = "type";

        // Available Video types
        static final String TYPE_TRAILER = "Trailer";
        static final String TYPE_TEASER = "Teaser";
        static final String TYPE_CLIP = "Clip";
        static final String TYPE_FEATURETTE = "Featurette";

        String mId;
        String mName;
        String mKey;
        String mSite;
        String mType;

        // Id
        public String getId() { return mId; }
        public void setId(String id) { mId = id; }

        // Name
        public String getName() { return mName; }
        public void setName(String name) { mName = name; }

        // Key
        public String getKey() { return mKey; }
        public void setKey(String key) { mKey = key; }

        // Site
        public String getSite() { return mSite; }
        public void setSite(String site) { mSite = site; }

        // Type
        public String getType() { return mType; }
        public void setType(String type) { mType = type; }

        // Constructor converting JSON object to object instance
        static Video fromJson(JSONObject jsonObject) throws JSONException {
            Video v = new Video();

            if (jsonObject.has(VIDEO_ID)) {
                v.mId = jsonObject.getString(VIDEO_ID);
            }

            if (jsonObject.has(NAME)) {
                v.mName = jsonObject.getString(NAME);
            }

            if (jsonObject.has(KEY)) {
                v.mKey = jsonObject.getString(KEY);
            }

            if (jsonObject.has(SITE)) {
                v.mSite = jsonObject.getString(SITE);
            }

            if (jsonObject.has(TYPE)) {
                v.mType = jsonObject.getString(TYPE);
            }

            return v;
        }

        // Factory method for converting JSON object array to list of object instances
        public static ArrayList<Video> fromJson(JSONArray jsonArray, String filterType)
                throws JSONException {
            JSONObject videoJson;

            int objectCount = jsonArray.length();
            ArrayList<Video> videos = new ArrayList<>(objectCount);
            for (int i = 0; i < objectCount; i++) {
                videoJson = jsonArray.getJSONObject(i);
                Video video = Video.fromJson(videoJson);
                if (video != null) {
                    if (filterType == null || video.mType.equals(filterType)) {
                        videos.add(video);
                    }
                }
            }
            return videos;
        }

    }

    /**
     *
     */
    public static class Review {
        // API Movie Reviews
        // https://developers.themoviedb.org/3/movies/get-movie-reviews
        public static final String RESULTS = "results";

        static final String REVIEW_ID = "id";
        static final String AUTHOR = "author";
        static final String CONTENT = "content";
        static final String URL = "url";

        String mId;
        String mAuthor;
        String mContent;
        String mUrl;

        // Id
        public String getId() { return mId; }
        public void setId(String id) { mId = id; }

        // Author
        public String getAuthor() { return mAuthor; }
        public void setAuthor(String author) { mAuthor = author; }

        // Content
        public String getContent() { return mContent; }
        public void setContent(String content) { mContent = content; }

        // URL
        public String getUrl() { return mUrl; }
        public void setUrl(String url) { mUrl = url; }

        // Constructor converting JSON object to object instance
        static Review fromJson(JSONObject jsonObject)
                throws JSONException {
            Review r = new Review();

            if (jsonObject.has(REVIEW_ID)) {
                r.mId = jsonObject.getString(REVIEW_ID);
            }

            if (jsonObject.has(AUTHOR)) {
                r.mAuthor = jsonObject.getString(AUTHOR);
            }

            if (jsonObject.has(CONTENT)) {
                r.mContent = jsonObject.getString(CONTENT);
            }

            if (jsonObject.has(URL)) {
                r.mUrl = jsonObject.getString(URL);
            }

            return r;
        }

        // Factory method for converting JSON object array to list of object instances
        public static ArrayList<Review> fromJson(JSONArray jsonArray)
                throws JSONException {
            JSONObject reviewJson;

            int objectCount = jsonArray.length();
            ArrayList<Review> reviews = new ArrayList<>(objectCount);
            for (int i = 0; i < objectCount; i++) {
                reviewJson = jsonArray.getJSONObject(i);
                Review review = Review.fromJson(reviewJson);
                if (review != null) { reviews.add(review); }
            }
            return reviews;
        }

    }
}
