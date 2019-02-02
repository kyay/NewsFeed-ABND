package com.example.android.newsfeed;

import androidx.annotation.NonNull;

import timber.log.Timber;

/**
 * Implementation of {@link timber.log.Timber.Tree} for the debug build
 */
class DynamicTree extends Timber.DebugTree {
    @SuppressWarnings("HardCodedStringLiteral")
    @Override
    protected String createStackElementTag(@NonNull StackTraceElement element) {
        return String.format("C:%s:%s",
                super.createStackElementTag(element),
                element.getLineNumber());
    }
}
