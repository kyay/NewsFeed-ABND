package com.example.android.newsfeed;

import androidx.annotation.NonNull;

import timber.log.Timber;

/**
 * Implementation of {@link timber.log.Timber.Tree} for the release build
 */
public class Tree extends Timber.DebugTree {
    @Override
    protected void log(final int priority, final String tag, @NonNull final String message, final Throwable throwable) {
    }
}
