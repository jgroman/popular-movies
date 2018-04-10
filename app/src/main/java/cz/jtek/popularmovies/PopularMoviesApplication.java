package cz.jtek.popularmovies;

import android.app.Application;

import com.facebook.stetho.Stetho;
import com.facebook.stetho.okhttp3.StethoInterceptor;

import okhttp3.OkHttpClient;

public class PopularMoviesApplication extends Application {
    public void onCreate() {
        super.onCreate();

        if (BuildConfig.DEBUG) {
            // Initialize Stetho
            Stetho.initializeWithDefaults(this);
        }

    }

}
