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

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import cz.jtek.popularmovies.data.MovieContract.MovieEntry;

import static cz.jtek.popularmovies.data.MovieContract.MovieEntry.TABLE_NAME;

public class MovieDbHelper extends SQLiteOpenHelper {

    // Database file name
    private static final String DB_NAME = "movie.db";

    // This db version should be updated on every db schema change to trigger
    // onUpgrade method to run
    private static final int DB_VERSION = 1;

    MovieDbHelper(Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    /**
     * Called when the database is created for the first time.
     *
     * @param sqLiteDatabase The database.
     */
    @Override
    public void onCreate(SQLiteDatabase sqLiteDatabase) {

        final String SQL_CREATE_MOVIE_TABLE =
                "CREATE TABLE " + TABLE_NAME + " (" +
                MovieEntry._ID + " INTEGER PRIMARY KEY AUTOINCREMENT, " +
                MovieEntry.COL_MOVIE_ID + " INTEGER NOT NULL, " +
                MovieEntry.COL_TITLE + " TEXT NOT NULL, " +
                MovieEntry.COL_OVERVIEW + " TEXT NOT NULL, " +
                MovieEntry.COL_POSTER_PATH + " TEXT NOT NULL, " +
                MovieEntry.COL_RELEASE_DATE + " TEXT NOT NULL, " +
                MovieEntry.COL_VOTE_AVERAGE + " REAL NOT NULL, " +
                MovieEntry.COL_FAVORITE + " INTEGER NOT NULL, " +
                " UNIQUE (" + MovieEntry.COL_MOVIE_ID + ") ON CONFLICT REPLACE " +
                " );";

        sqLiteDatabase.execSQL(SQL_CREATE_MOVIE_TABLE);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        // Warning: this drops old table on upgrade
        db.execSQL("DROP TABLE IF EXISTS " + TABLE_NAME);
        onCreate(db);
    }


}
