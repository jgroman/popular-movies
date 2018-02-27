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

import android.content.ContentValues;
import android.content.Context;
import android.util.Log;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

import cz.jtek.popularmovies.TmdbApi;

public class TmdbJsonUtils {

    private static final String TAG = TmdbJsonUtils.class.getSimpleName();

    // API Status Codes
    // https://www.themoviedb.org/documentation/api/status-codes
    private static final String TMDB_STATUS_CODE = "status_code";
    private static final String TMDB_STATUS_MSG = "status_message";

    // Configuration
    private static final String TMDB_CONFIG_IMAGES = "images";
    private static final String TMDB_CONFIG_SECURE_BASE_URL = "secure_base_url";

    // Movie
    private static final String TMDB_MOVIE_RESULTS = "results";
    private static final String TMDB_MOVIE_TITLE = "title";
    private static final String TMDB_MOVIE_RELEASE_DATE = "release_date";
    private static final String TMDB_MOVIE_POSTER_PATH = "poster_path";
    private static final String TMDB_MOVIE_VOTE_AVERAGE = "vote_average";
    private static final String TMDB_MOVIE_OVERVIEW = "overview";


    public static TmdbApi.Config getConfigFromJson(Context context, String tmdbConfigJsonString) {

        // Load mock data for testing
        tmdbConfigJsonString = MockDataUtils.getMockJson(context, "mock_configuration");

        TmdbApi.Config tmdbConfig = new TmdbApi.Config();

        try {
            JSONObject tmdbConfigJson = new JSONObject(tmdbConfigJsonString);

            if (tmdbConfigJson.has(TMDB_STATUS_CODE)) {
                // TMDb API reports an error
                int statusCode = tmdbConfigJson.getInt(TMDB_STATUS_CODE);
                String statusMsg = "";
                if (tmdbConfigJson.has(TMDB_STATUS_MSG)) {
                    statusMsg = tmdbConfigJson.getString(TMDB_STATUS_MSG);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return null;
            }

            // Currently we are only interested in "secure base URL" string from "images" object
            JSONObject images = tmdbConfigJson.getJSONObject(TMDB_CONFIG_IMAGES);

            tmdbConfig.setSecureBaseUrl(images.getString(TMDB_CONFIG_SECURE_BASE_URL));

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing configuration.");
            return null;
        }

        return tmdbConfig;
    }

    public static List<TmdbApi.Movie> getMovieListFromJson(Context context, String tmdbMovieJsonString) {

        // Load mock data for testing
        tmdbMovieJsonString = MockDataUtils.getMockJson(context, "mock_popular");

        List<TmdbApi.Movie> movies = new ArrayList<>();

        try {
            JSONObject movieJson = new JSONObject(tmdbMovieJsonString);

            if (movieJson.has(TMDB_STATUS_CODE)) {
                // TMDb API reports an error
                int statusCode = movieJson.getInt(TMDB_STATUS_CODE);
                String statusMsg = "";
                if (movieJson.has(TMDB_STATUS_MSG)) {
                    statusMsg = movieJson.getString(TMDB_STATUS_MSG);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return null;
            }

            JSONObject results = movieJson.getJSONObject(TMDB_MOVIE_RESULTS);

            // TODO

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing configuration.");
            return null;
        }

        return movies;
    }
}
