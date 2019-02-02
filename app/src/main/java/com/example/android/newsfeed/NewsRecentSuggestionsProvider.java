package com.example.android.newsfeed;

import android.app.SearchManager;
import android.content.SearchRecentSuggestionsProvider;

import java.lang.reflect.Field;

import timber.log.Timber;

public class NewsRecentSuggestionsProvider extends SearchRecentSuggestionsProvider {
    public final static String AUTHORITY = "com.example.android.newsfeed.NewsRecentSuggestionsProvider";
    public final static int MODE = DATABASE_MODE_QUERIES;

    public NewsRecentSuggestionsProvider() {
        setupSuggestions(AUTHORITY, MODE);
    }

    @Override
    protected void setupSuggestions(String authority, int mode) {
        super.setupSuggestions(authority, mode);
        try {
            //noinspection JavaReflectionMemberAccess
            Field suggestionProjectionField = SearchRecentSuggestionsProvider.class
                    .getDeclaredField("mSuggestionProjection");
            suggestionProjectionField.setAccessible(true);
            String[] suggestionProjection = (String[]) suggestionProjectionField.get(this);
            Timber.e(Integer.toString(R.drawable.ic_menu_recent_history));
            suggestionProjection[1] = "'"
                    + R.drawable.ic_menu_recent_history + "' AS "
                    + SearchManager.SUGGEST_COLUMN_ICON_1;
        } catch (NoSuchFieldException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        }
    }
}
