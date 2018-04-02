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

import java.util.List;

import cz.jtek.popularmovies.TmdbData;

/**
 * TMDb API JSON parsing utilities
 */
public class TmdbJsonUtils {

    private static final String TAG = TmdbJsonUtils.class.getSimpleName();

    /**
     * Parses TMDb API /configuration reply and updates tmdbData Config values
     *
     * @param tmdbData                  tmdbData instance
     * @param tmdbConfigJsonString  API JSON response string
     */
    public static void getConfigFromJson(TmdbData tmdbData, String tmdbConfigJsonString) {

        boolean isJsonValidApiConfig = false;

        tmdbData.getStatus().setDataValid(true);

        try {
            JSONObject tmdbConfigJson = new JSONObject(tmdbConfigJsonString);

            // Check whether TMDb API reports an error
            if (tmdbConfigJson.has(TmdbData.Status.CODE)) {
                tmdbData.getStatus().setDataValid(false);
                int statusCode = tmdbConfigJson.getInt(TmdbData.Status.CODE);

                String statusMsg = "";
                if (tmdbConfigJson.has(TmdbData.Status.MSG)) {
                    statusMsg = tmdbConfigJson.getString(TmdbData.Status.MSG);
                    tmdbData.getStatus().setStatusMessage(statusMsg);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return;
            }

            // Currently we are only interested in "secure base URL" string from "images" object
            if (tmdbConfigJson.has(TmdbData.Config.IMAGES)) {
                JSONObject images = tmdbConfigJson.getJSONObject(TmdbData.Config.IMAGES);

                if (images.has(TmdbData.Config.SECURE_BASE_URL)) {

                    // Update Config secureBaseUrl value
                    tmdbData.getConfig().setSecureBaseUrl(images.getString(TmdbData.Config.SECURE_BASE_URL));
                    isJsonValidApiConfig = true;
                }
            }

            if (!isJsonValidApiConfig) {
                // We didn't find all required JSON objects
                Log.e(TAG, "Invalid TMDb API configuration data.");
            }

        } catch (JSONException ex) {
            Log.e(TAG, "JSONException parsing API configuration reply.");
            ex.printStackTrace();
        }
    }

    public static List<TmdbData.Movie> getMovieListFromJson(TmdbData tmdbData, String tmdbMovieJsonString) {

        List<TmdbData.Movie> moviesList = tmdbData.getMovieList();

        try {
            JSONObject movieJson = new JSONObject(tmdbMovieJsonString);

            // Check whether TMDb API reports an error
            if (movieJson.has(TmdbData.Status.CODE)) {
                int statusCode = movieJson.getInt(TmdbData.Status.CODE);
                String statusMsg = "";
                if (movieJson.has(TmdbData.Status.MSG)) {
                    statusMsg = movieJson.getString(TmdbData.Status.MSG);
                }
                Log.e(TAG, "TMDb status: " + statusCode + " - " + statusMsg);
                return null;
            }

            // Parsing returned data
            if (movieJson.has(TmdbData.Movie.RESULTS)) {
                JSONArray results = movieJson.getJSONArray(TmdbData.Movie.RESULTS);
                int resultCount = results.length();

                for (int i=0; i<resultCount; i++) {
                    JSONObject movieObj = results.getJSONObject(i);
                    TmdbData.Movie movie = new TmdbData.Movie();

                    if (movieObj.has(TmdbData.Movie.ID)) {
                        movie.setId(movieObj.getInt(TmdbData.Movie.ID));
                    }

                    if (movieObj.has(TmdbData.Movie.TITLE)) {
                        movie.setTitle(movieObj.getString(TmdbData.Movie.TITLE));
                    }

                    if (movieObj.has(TmdbData.Movie.RELEASE_DATE)) {
                        movie.setReleaseDate(movieObj.getString(TmdbData.Movie.RELEASE_DATE));
                    }

                    if (movieObj.has(TmdbData.Movie.POSTER_PATH)) {
                        movie.setPosterPath(movieObj.getString(TmdbData.Movie.POSTER_PATH));
                    }

                    if (movieObj.has(TmdbData.Movie.VOTE_AVERAGE)) {
                        movie.setVoteAverage(movieObj.getDouble(TmdbData.Movie.VOTE_AVERAGE));
                    }

                    if (movieObj.has(TmdbData.Movie.OVERVIEW)) {
                        movie.setOverview(movieObj.getString(TmdbData.Movie.OVERVIEW));
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
