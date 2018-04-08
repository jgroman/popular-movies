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

package cz.jtek.popularmovies.data;

import android.content.ContentProvider;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.UriMatcher;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.Uri;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

public class MovieProvider extends ContentProvider {

    private static final String TAG = MovieProvider.class.getSimpleName();

    // Constant to be used to match URIs using the UriMatcher class
    public static final int CODE_MOVIES = 100;
    public static final int CODE_MOVIE_ID = 101;

     // The URI Matcher used by this content provider.
    private static final UriMatcher sUriMatcher = buildUriMatcher();

    private MovieDbHelper mOpenHelper;

    public static UriMatcher buildUriMatcher() {

        final UriMatcher matcher = new UriMatcher(UriMatcher.NO_MATCH);
        final String authority = MovieContract.CONTENT_AUTHORITY;

        matcher.addURI(authority, MovieContract.PATH_MOVIES, CODE_MOVIES);
        matcher.addURI(authority, MovieContract.PATH_MOVIES + "/#", CODE_MOVIE_ID);

        return matcher;
    }

    @Override
    public boolean onCreate() {
        mOpenHelper = new MovieDbHelper(getContext());
        return true;
    }

    @Nullable
    @Override
    public Cursor query(@NonNull Uri uri, @Nullable String[] projection,
                        @Nullable String selection, @Nullable String[] selectionArgs,
                        @Nullable String sortOrder) {

        Context context  = getContext();
        if (context == null) {
            return null;
        }

        Cursor cursor;

        switch (sUriMatcher.match(uri)) {

            case CODE_MOVIES: {
                // Selecting all movies
                cursor = mOpenHelper.getReadableDatabase().query(
                        MovieContract.MovieEntry.TABLE_NAME,
                        projection,
                        selection,
                        selectionArgs,
                        null,
                        null,
                        sortOrder);
                break;
            }

            default:
                throw new UnsupportedOperationException("Unknown uri: " + uri);
        }

        cursor.setNotificationUri(context.getContentResolver(), uri);
        return cursor;
    }

    @Nullable
    @Override
    public String getType(@NonNull Uri uri) {
        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIES:
                return MovieContract.MIME_TYPE_DIR;

            case CODE_MOVIE_ID:
                return MovieContract.MIME_TYPE_ITEM;

            default:
                throw new IllegalArgumentException("Unknown URI " + uri);
        }
    }

    @Nullable
    @Override
    public Uri insert(@NonNull Uri uri, @Nullable ContentValues contentValues) {
        Context context = getContext();

        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        if (sUriMatcher.match(uri) != CODE_MOVIES) {
            throw new UnsupportedOperationException("Unknown URI " + uri);
        }

        if (contentValues == null) {
            throw new NullPointerException("ContentValues cannot be null");
        }

        if (!contentValues.containsKey(MovieContract.MovieEntry.COL_MOVIE_ID)) {
            throw new NullPointerException("ContentValues must contain key "  + MovieContract.MovieEntry.COL_MOVIE_ID);
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        long rowId = db.insert(MovieContract.MovieEntry.TABLE_NAME, null, contentValues);

        if (rowId > 0) {
            context.getContentResolver().notifyChange(uri, null);
            return ContentUris.withAppendedId(MovieContract.MovieEntry.CONTENT_URI, rowId);
        } else {
            return null;
        }
    }

    @Override
    public int delete(@NonNull Uri uri, @Nullable String selection, @Nullable String[] selectionArgs) {
        Context context = getContext();

        if (context == null) {
            throw new NullPointerException("Context cannot be null");
        }

        final SQLiteDatabase db = mOpenHelper.getWritableDatabase();

        int rowsDeleted;

        /*
         * If we pass null as the selection to SQLiteDatabase#delete, our entire table will be
         * deleted. However, if we do pass null and delete all of the rows in the table, we won't
         * know how many rows were deleted. According to the documentation for SQLiteDatabase,
         * passing "1" for the selection will delete all rows and return the number of rows
         * deleted, which is what the caller of this method expects.
         */
        if (null == selection) { selection = "1"; }

        switch (sUriMatcher.match(uri)) {
            case CODE_MOVIES:
                rowsDeleted = db.delete(MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            case CODE_MOVIE_ID:
                selection = MovieContract.MovieEntry.COL_MOVIE_ID + " = ?";
                selectionArgs = new String[] { String.valueOf(ContentUris.parseId(uri)) };

                rowsDeleted = db.delete(MovieContract.MovieEntry.TABLE_NAME, selection, selectionArgs);
                break;
            default:
                throw new UnsupportedOperationException("Unknown URI: " + uri);
        }

        // If some rows were deleted, notify all listeners
        if (rowsDeleted != 0) {
            context.getContentResolver().notifyChange(uri, null);
        }

        return rowsDeleted;
    }

    @Override
    public int update(@NonNull Uri uri, @Nullable ContentValues contentValues, @Nullable String s, @Nullable String[] strings) {
        // Not implemented
        return 0;
    }
}
