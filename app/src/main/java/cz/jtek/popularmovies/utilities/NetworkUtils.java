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

import android.content.Context;
import android.net.Uri;
import android.util.Log;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import cz.jtek.popularmovies.BuildConfig;
import cz.jtek.popularmovies.R;

public final class NetworkUtils {

    private static final String TAG = NetworkUtils.class.getSimpleName();

    /*
     Relevant TMDb API docs:
     Configuration - https://developers.themoviedb.org/3/configuration/get-api-configuration
     Popular movies - https://developers.themoviedb.org/3/movies/get-popular-movies
     Top rated movies - https://developers.themoviedb.org/3/movies/get-top-rated-movies
     */

    private static final String TMDB_API_AUTHORITY = "api.themoviedb.org";
    private static final String API_SCHEME = "https";

    private static final String API_PATH_VERSION = "3";
    private static final String API_PATH_CONFIGURATION = "configuration";
    private static final String API_PATH_MOVIE = "movie";
    private static final String API_PATH_POPULAR = "popular";
    private static final String API_PATH_TOP_RATED = "top_rated";

    private static final String API_PARAM_API_KEY = "api_key";
    private static final String API_PARAM_PAGE = "page";


    public static URL buildConfigurationUrl() {

        // Build TMDb configuration Uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(API_SCHEME)
                .authority(TMDB_API_AUTHORITY)
                .appendPath(API_PATH_VERSION)
                .appendPath(API_PATH_CONFIGURATION)
                // API token comes from grade.properties file, see README
                .appendQueryParameter(API_PARAM_API_KEY, BuildConfig.TMDB_API_TOKEN);

        Uri tmdbConfigUri = uriBuilder.build();

        try {
            URL tmdbConfigUrl = new URL(tmdbConfigUri.toString());
            Log.d(TAG, "URL config: " + tmdbConfigUrl);
            return tmdbConfigUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static URL buildMovieUrl(Context context, String sortOrder, Integer page) {

        // Check input params sanity
        if (context == null) {
            Log.e(TAG, "buildMovieUrl: context parameter cannot be null.");
            return null;
        }

        if (sortOrder == null || sortOrder.length() == 0) {
            Log.e(TAG, "buildMovieUrl: sort order parameter cannot be null or empty.");
            return null;
        }

        if (page == null || page <= 0) {
            page = 1;
        }

        // Build TMDb movie Uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(API_SCHEME)
                .authority(TMDB_API_AUTHORITY)
                .appendPath(API_PATH_VERSION)
                .appendPath(API_PATH_MOVIE);

        // Possible sortOrder parameter values are listed in array.xml array 'sort-order-values'
        if (sortOrder.equals(context.getResources().getString(R.string.pref_sort_order_most_popular))) {
            uriBuilder.appendPath(API_PATH_POPULAR);
        } else if (sortOrder.equals(context.getResources().getString(R.string.pref_sort_order_top_rated))) {
            uriBuilder.appendPath(API_PATH_TOP_RATED);
        } else {
            Log.e(TAG, "buildMovieUrl: unknown sort order parameter.");
            return null;
        }

        // API token comes from grade.properties file, see README
        uriBuilder.appendQueryParameter(API_PARAM_API_KEY, BuildConfig.TMDB_API_TOKEN)
                .appendQueryParameter(API_PARAM_PAGE, page.toString());

        Uri tmdbMovieUri = uriBuilder.build();

        try {
            URL tmdbMovieUrl = new URL(tmdbMovieUri.toString());
            Log.d(TAG, "URL movie: " + tmdbMovieUrl);
            return tmdbMovieUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    public static URL buildImageUrl(Uri baseUri, String size, String filePath) {

        // Check input params sanity
        if (baseUri == null) {
            Log.e(TAG, "buildImageUrl: baseUri parameter cannot be null.");
            return null;
        }

        if (size == null || size.length() == 0) {
            Log.e(TAG, "buildImageUrl: size parameter cannot be null or empty.");
            return null;
        }

        if (filePath == null || filePath.length() == 0) {
            Log.e(TAG, "buildImageUrl: filePath parameter cannot be null or empty.");
            return null;
        }

        // Strip leading forward slashes from filePath
        while (filePath.substring(0,1).equals("/")) {
            filePath = filePath.substring(1);
        }

        // Build TMDb image Uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(baseUri.getScheme())
                .authority(baseUri.getAuthority());

        List<String> pathSegments = baseUri.getPathSegments();
        for (String pathSegment : pathSegments ) {
            uriBuilder.appendPath(pathSegment);
        }

        uriBuilder.appendPath(size)
                .appendPath(filePath);

        Uri tmdbImageUri = uriBuilder.build();

        try {
            URL tmdbImageUrl = new URL(tmdbImageUri.toString());
            Log.d(TAG, "URL image: " + tmdbImageUrl);
            return tmdbImageUrl;
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
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