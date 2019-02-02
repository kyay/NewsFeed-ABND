package com.example.android.newsfeed;

import android.app.Application;

import timber.log.Timber;

/**
 * A simple {@link Application} that provides support for Timber
 */
@SuppressWarnings("WeakerAccess")
public class App extends Application {
    @SuppressWarnings("PublicMethodNotExposedInInterface")
    @Override
    public void onCreate() {
        super.onCreate();
        // The tree is implemented in both the debug and release builds
        Timber.plant(new DynamicTree());
    }
}
