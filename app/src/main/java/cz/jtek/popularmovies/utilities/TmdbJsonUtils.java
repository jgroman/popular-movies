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

package cz.jtek.popularmovies.utilities;

import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.jtek.popularmovies.TmdbData;

/**
 * TMDb API JSON parsing utilities
 */
public class TmdbJsonUtils {

    private static final String TAG = TmdbJsonUtils.class.getSimpleName();

    /**
     * JSON result wrapper
     * Allows returning either result or exception
     *
     * @param <T> Result type
     */
    public static class TmdbJsonResult<T> {
        private final T result;
        private final Exception exception;

        TmdbJsonResult(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public T getResult() { return result; }

        public Exception getException() { return exception; }

        // Checks whether instance contains an exception
        public boolean hasException() { return exception != null; }
    }

    /**
     * Parses TMDb API /configuration reply
     *
     * @param tmdbConfigJsonString  API JSON response string
     */
    public static  TmdbJsonResult<TmdbData.Config> getConfigFromJson(String tmdbConfigJsonString) {

        TmdbData.Config config = null;

        try {
            JSONObject configJson = new JSONObject(tmdbConfigJsonString);

            // Check whether TMDb API reports an error
            if (TmdbData.Status.isPresent(configJson)) {
                TmdbData.Status status = TmdbData.Status.fromJson(configJson);

                Log.e(TAG, "TMDb status: " + status.getCode() + " (" + status.getMessage() + ")");
                return new TmdbJsonResult<>(null,
                        new TmdbData.TmdbStatusException(status.getCode(), status.getMessage()));
            }

            config = TmdbData.Config.fromJson(configJson);

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing API configuration reply");
            return new TmdbJsonResult<>(null, ex);
        }

        return new TmdbJsonResult<>(config, null);
    }

    /**
     *
     * @param tmdbMovieJsonString
     * @return
     */
    public static TmdbJsonResult<List<TmdbData.Movie>> getMovieListFromJson(String tmdbMovieJsonString) {

        List<TmdbData.Movie> moviesList = new ArrayList<>();

        try {
            JSONObject movieJson = new JSONObject(tmdbMovieJsonString);

            // Check whether TMDb API reports an error
            if (TmdbData.Status.isPresent(movieJson)) {
                TmdbData.Status status = TmdbData.Status.fromJson(movieJson);

                Log.e(TAG, "TMDb status: " + status.getCode() + " (" + status.getMessage() + ")");
                return new TmdbJsonResult<>(null,
                        new TmdbData.TmdbStatusException(status.getCode(), status.getMessage()));
            }

            // Parsing returned data
            if (movieJson.has(TmdbData.Movie.RESULTS)) {
                JSONArray results = movieJson.getJSONArray(TmdbData.Movie.RESULTS);
                moviesList = TmdbData.Movie.fromJson(results);
            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing movies.");
            return new TmdbJsonResult<>(null, ex);
        }

        return new TmdbJsonResult<>(moviesList, null);
    }

    /**
     * Parses TMDb API /movie/{movie_id}/videos reply
     *
     * @param tmdbJson              API JSON response string
     * @param filterType            Return only videos of this type
     *
     * @return TmdbJsonResult object with either list of Tmdb.Video objects or exception
     */
    public static TmdbJsonResult<List<TmdbData.Video>> getVideoListFromJson(String tmdbJson, String filterType) {

        List<TmdbData.Video> videoList = new ArrayList<>();

        try {
            JSONObject videoJson = new JSONObject(tmdbJson);

            // Check whether TMDb API reports an error
            if (TmdbData.Status.isPresent(videoJson)) {
                TmdbData.Status status = TmdbData.Status.fromJson(videoJson);

                Log.e(TAG, "TMDb status: " + status.getCode() + " (" + status.getMessage() + ")");
                return new TmdbJsonResult<>(null,
                        new TmdbData.TmdbStatusException(status.getCode(), status.getMessage()));
            }

            // Parsing returned data
            if (videoJson.has(TmdbData.Video.RESULTS)) {
                JSONArray results = videoJson.getJSONArray(TmdbData.Video.RESULTS);
                videoList = TmdbData.Video.fromJson(results, filterType);
            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing movie videos.");
            return new TmdbJsonResult<>(null, ex);
        }

        return new TmdbJsonResult<>(videoList, null);
    }

    /**
     * Parses TMDb API /movie/{movie_id}/reviews reply
     *
     * @param tmdbJson              API JSON response string
     *
     * @return TmdbJsonResult object with either list of Tmdb.Review objects or exception
     */
    public static TmdbJsonResult<List<TmdbData.Review>> getReviewListFromJson(String tmdbJson) {

        List<TmdbData.Review> reviewList = new ArrayList<>();

        try {
            JSONObject reviewJson = new JSONObject(tmdbJson);

            // Check whether TMDb API reports an error
            if (TmdbData.Status.isPresent(reviewJson)) {
                TmdbData.Status status = TmdbData.Status.fromJson(reviewJson);

                Log.e(TAG, "TMDb status: " + status.getCode() + " (" + status.getMessage() + ")");
                return new TmdbJsonResult<>(null,
                        new TmdbData.TmdbStatusException(status.getCode(), status.getMessage()));
            }

            // Parsing returned data
            if (reviewJson.has(TmdbData.Review.RESULTS)) {
                JSONArray results = reviewJson.getJSONArray(TmdbData.Review.RESULTS);
                reviewList = TmdbData.Review.fromJson(results);
            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing configuration.");
            return new TmdbJsonResult<>(null, ex);
        }

        return new TmdbJsonResult<>(reviewList, null);
    }

}
