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

    public static TmdbApi.Config getConfigFromJson(Context context, String tmdbConfigJsonString) {

        boolean isJsonValidApiConfig = false;

        // DEBUG ONLY: Load mock data for testing
        tmdbConfigJsonString = MockDataUtils.getMockJson(context, "mock_configuration");

        TmdbApi.Config tmdbConfig = new TmdbApi.Config();

        try {
            JSONObject tmdbConfigJson = new JSONObject(tmdbConfigJsonString);

            if (tmdbConfigJson.has(TmdbApi.Status.STATUS_CODE)) {
                // TMDb API reports an error
                int statusCode = tmdbConfigJson.getInt(TmdbApi.Status.STATUS_CODE);
                String statusMsg = "";
                if (tmdbConfigJson.has(TmdbApi.Status.STATUS_MSG)) {
                    statusMsg = tmdbConfigJson.getString(TmdbApi.Status.STATUS_MSG);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return null;
            }

            // Currently we are only interested in "secure base URL" string from "images" object
            if (tmdbConfigJson.has(TmdbApi.Config.IMAGES)) {
                JSONObject images = tmdbConfigJson.getJSONObject(TmdbApi.Config.IMAGES);

                if (images.has(TmdbApi.Config.SECURE_BASE_URL)) {
                    tmdbConfig.setSecureBaseUrl(images.getString(TmdbApi.Config.SECURE_BASE_URL));
                    isJsonValidApiConfig = true;
                }
            }

            if (!isJsonValidApiConfig) {
                // We didn't find all required JSON objects
                Log.e(TAG, "Invalid TMDb API configuration data.");
                return null;
            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing configuration.");
            return null;
        }

        return tmdbConfig;
    }

    public static List<TmdbApi.Movie> getMovieListFromJson(Context context, String tmdbMovieJsonString) {

        boolean isJsonValidApiMovie = false;

        // DEBUG ONLY: Load mock data for testing
        tmdbMovieJsonString = MockDataUtils.getMockJson(context, "mock_popular");

        List<TmdbApi.Movie> moviesList = new ArrayList<>();

        try {
            JSONObject movieJson = new JSONObject(tmdbMovieJsonString);

            if (movieJson.has(TmdbApi.Status.STATUS_CODE)) {
                // TMDb API reports an error
                int statusCode = movieJson.getInt(TmdbApi.Status.STATUS_CODE);
                String statusMsg = "";
                if (movieJson.has(TmdbApi.Status.STATUS_MSG)) {
                    statusMsg = movieJson.getString(TmdbApi.Status.STATUS_MSG);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return null;
            }

            if (movieJson.has(TmdbApi.Movie.RESULTS)) {
                JSONArray results = movieJson.getJSONArray(TmdbApi.Movie.RESULTS);
                int resultCount = results.length();

                for (int i=0; i<resultCount; i++) {
                    JSONObject movieObj = results.getJSONObject(i);
                    TmdbApi.Movie movie = new TmdbApi.Movie();

                    if (movieObj.has(TmdbApi.Movie.TITLE)) {
                        movie.setTitle(movieObj.getString(TmdbApi.Movie.TITLE));
                    }

                    if (movieObj.has(TmdbApi.Movie.RELEASE_DATE)) {
                        movie.setReleaseDate(movieObj.getString(TmdbApi.Movie.RELEASE_DATE));
                    }

                    if (movieObj.has(TmdbApi.Movie.POSTER_PATH)) {
                        movie.setPosterPath(movieObj.getString(TmdbApi.Movie.POSTER_PATH));
                    }

                    if (movieObj.has(TmdbApi.Movie.VOTE_AVERAGE)) {
                        movie.setVoteAverage(movieObj.getDouble(TmdbApi.Movie.VOTE_AVERAGE));
                    }

                    if (movieObj.has(TmdbApi.Movie.OVERVIEW)) {
                        movie.setOverview(movieObj.getString(TmdbApi.Movie.OVERVIEW));
                    }

                    moviesList.add(movie);
                }

            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing configuration.");
            return null;
        }

        return moviesList;
    }
}
