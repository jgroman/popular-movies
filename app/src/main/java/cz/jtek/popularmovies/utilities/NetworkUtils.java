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

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.util.Scanner;

public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    /*
     Relevant TMDb API:
     Configuration - https://developers.themoviedb.org/3/configuration/get-api-configuration
     Discover - https://developers.themoviedb.org/3/discover/movie-discover

     */

    private static final String TMDB_API_V3_URL = "https://api.themoviedb.org/3";

    private static final String API_PATH_CONFIGURATION = "/configuration";

    private static final String API_PATH_DISCOVER_MOVIE = "/discover/movie";

    private static final String API_PARAM_API_KEY = "api_key";
    private static final String API_PARAM_SORT_BY = "sort_by";
    private static final String API_PARAM_PAGE = "page";
    private static final String API_PARAM_INCLUDE_ADULT = "include_adult";


    private static URL buildConfigurationUrl() {
        return null;
    }

    private static URL buildDiscoverUrl() {
        return null;
    }

    /**
     * This method returns the entire result from the HTTP response.
     *
     * @param url The URL to fetch the HTTP response from.
     * @return The contents of the HTTP response, null if no response
     * @throws IOException Related to network and stream reading
     */
    public static String getResponseFromHttpUrl(URL url) throws IOException {
        HttpURLConnection urlConnection = (HttpURLConnection) url.openConnection();
        try {
            InputStream in = urlConnection.getInputStream();

            Scanner scanner = new Scanner(in);
            scanner.useDelimiter("\\A");

            boolean hasInput = scanner.hasNext();
            String response = null;
            if (hasInput) {
                response = scanner.next();
            }
            scanner.close();
            return response;
        } finally {
            urlConnection.disconnect();
        }
    }

}