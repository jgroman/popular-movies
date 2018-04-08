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

import android.content.ActivityNotFoundException;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.URL;
import java.util.List;
import java.util.Scanner;

import cz.jtek.popularmovies.BuildConfig;
import cz.jtek.popularmovies.R;

/**
 * Utilities for TMDb API network communication
 */
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
    private static final String API_PATH_VIDEOS = "videos";
    private static final String API_PATH_REVIEWS = "reviews";

    private static final String API_PARAM_API_KEY = "api_key";
    private static final String API_PARAM_PAGE = "page";


    /**
     * Creates valid TMDb API /configuration URL for network requests
     *
     * @return TMDb API configuration URL
     */
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

    /**
     * Creates valid TMDb API /movies URL for network requests
     *
     * @param context     Current context
     * @param sortOrder  Movie sort order ("Most Popular" or "Top Rated")
     * @param page        Results page
     * @return TMDb API /movies URL
     */
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
            return new URL(tmdbMovieUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates valid TMDb API /movie/[movieId]/videos URL for network requests
     *
     * @param movieId Movie id to use in URL
     *
     * @return  TMDb movie videos URL
     */
    public static URL buildMovieVideosUrl(int movieId) {

        // Build TMDb movie videos Uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(API_SCHEME)
                .authority(TMDB_API_AUTHORITY)
                .appendPath(API_PATH_VERSION)
                .appendPath(API_PATH_MOVIE)
                .appendPath(String.valueOf(movieId))
                .appendPath(API_PATH_VIDEOS);

        // API token comes from grade.properties file, see README
        uriBuilder.appendQueryParameter(API_PARAM_API_KEY, BuildConfig.TMDB_API_TOKEN);

        Uri videosUri = uriBuilder.build();

        try {
            return new URL(videosUri.toString());
        } catch (MalformedURLException e) {
            e.printStackTrace();
            return null;
        }
    }

    /**
     * Creates valid TMDb API /movie/[movieId]/reviews URL for network requests
     *
     * @param movieId Movie id to use in URL
     *
     * @return  TMDb movie reviews URL
     */
    public static URL buildMovieReviewsUrl(int movieId) {
        // Build TMDb movie reviews Uri
        Uri.Builder uriBuilder = new Uri.Builder();
        uriBuilder.scheme(API_SCHEME)
                .authority(TMDB_API_AUTHORITY)
                .appendPath(API_PATH_VERSION)
                .appendPath(API_PATH_MOVIE)
                .appendPath(String.valueOf(movieId))
                .appendPath(API_PATH_REVIEWS);

        // API token comes from grade.properties file, see README
        uriBuilder.appendQueryParameter(API_PARAM_API_KEY, BuildConfig.TMDB_API_TOKEN);

        Uri reviewsUri = uriBuilder.build();

        try {
            return new URL(reviewsUri.toString());
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
        } catch (FileNotFoundException fnfe) {
          // We read error response from API to be processed later with JSON parsing utilities
            InputStream errorStream = urlConnection.getErrorStream();

            Scanner scanner = new Scanner(errorStream);
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

    /**
     * This method tests for network availability
     *
     * @return true if network connection available
     */
    public static boolean isNetworkAvailable(Context context) {
        ConnectivityManager cm = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        if (cm != null) {
            NetworkInfo netInfo = cm.getActiveNetworkInfo();
            return netInfo != null && netInfo.isConnected();
        }

        return false;
    }

    /**
     * Class for returning results from AsyncTaskLoader
     * Result contains either result of desired type or an exception
     *
     * @param <T> Result type
     */
    public static class AsyncTaskResult<T> {
        private final T result;
        private final Exception exception;

        public AsyncTaskResult(T result, Exception exception) {
            this.result = result;
            this.exception = exception;
        }

        public Exception getException() { return exception; }
        public T getResult() { return result; }

        public boolean hasException() { return exception != null; }
        public boolean hasResult() { return result != null; }
    }

    /**
     * Intent to open YouTube video
     * Tries to use YoutTube app, if it fails, it uses web browser
     *
     * @param context       Starting context
     * @param videoKey      YouTube video key string
     */
    public static void openYoutubeIntent(Context context, String videoKey) {
        Intent appIntent = new Intent(Intent.ACTION_VIEW, Uri.parse("vnd.youtube:" + videoKey));
        Intent webIntent = new Intent(Intent.ACTION_VIEW,
                Uri.parse("http://www.youtube.com/watch?v=" + videoKey));

        try {
            context.startActivity(appIntent);
        } catch (ActivityNotFoundException ex) {
            // Verify there's at least a web browser to receive the intent
            PackageManager packageManager = context.getPackageManager();
            List<ResolveInfo> activities = packageManager.queryIntentActivities(webIntent,0);
            boolean isIntentSafe = activities.size() > 0;
            if (isIntentSafe) {
                context.startActivity(webIntent);
            }
        }
    }
}