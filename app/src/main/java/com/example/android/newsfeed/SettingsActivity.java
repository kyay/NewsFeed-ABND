package com.example.android.newsfeed;

import android.app.Activity;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.os.Bundle;
import android.preference.ListPreference;
import android.preference.MultiSelectListPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.preference.PreferenceManager;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.appcompat.app.AppCompatActivity;

import android.view.View;
import android.widget.LinearLayout;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Objects;
import java.util.Set;

public class SettingsActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<String[][]> {

    public static final String SETTINGS_CHANGED_KEY = "SETTINGS_CHANGED";
    private static final String SECTIONS_URL = "https://content.guardianapis.com/sections";
    private static final String TAGS_URL = "https://content.guardianapis.com/tags";
    private static final int TAGS_LOADER_ID = 1;

    private static final int SECTIONS_LOADER_ID = 2;

    private int tagsCurPage;

    private int tagsPageCount;

    private Set<String> mOriginalSections;

    private Set<String> mOriginalTags;


    private LinearLayout mEmptyView;

    @NonNull
    public static String convertStringSetToString(Set<String> set) {
        StringBuilder setBuilder = new StringBuilder();
        Iterator<String> setIterator = set.iterator();
        while (setIterator.hasNext()) {
            setBuilder.append(setIterator.next());
            if (setIterator.hasNext())
                setBuilder.append("| ");
        }
        return setBuilder.toString();
    }

    @Override
    protected void onDestroy() {
        Intent resultIntent = new Intent();
        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        resultIntent.putExtra(SETTINGS_CHANGED_KEY,
                !(mOriginalSections.equals(sharedPreferences.getStringSet(
                        getString(R.string.settings_sections_key), new HashSet<>())) &&
                        mOriginalTags.equals(sharedPreferences.getStringSet(
                                getString(R.string.settings_tags_key), new HashSet<>())))
        );
        setResult(Activity.RESULT_OK, resultIntent);
        super.onDestroy();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.settings_activity);

        setSupportActionBar(findViewById(R.id.toolbar));
        Objects.requireNonNull(getSupportActionBar()).setDisplayHomeAsUpEnabled(true);

        SharedPreferences sharedPreferences =
                PreferenceManager.getDefaultSharedPreferences(this);
        mOriginalSections = sharedPreferences
                .getStringSet(getString(R.string.settings_sections_key), new HashSet<>());
        mOriginalTags = sharedPreferences
                .getStringSet(getString(R.string.settings_tags_key), new HashSet<>());
        mEmptyView = findViewById(R.id.empty_view);
        LoaderManager.getInstance(this).initLoader(SECTIONS_LOADER_ID, null, this);
        mEmptyView.setVisibility(View.VISIBLE);
        Objects.requireNonNull(getFragmentManager().findFragmentById(R.id.settings_fragment)
                .getView()).setVisibility(View.GONE);
    }

    @SuppressWarnings("WeakerAccess")
    public void resetTagsLoader() {
        tagsCurPage = 0;
        tagsPageCount = 1;
        if (mEmptyView != null)
            mEmptyView.setVisibility(View.VISIBLE);
        View settingsFragmentRootView =
                getFragmentManager().findFragmentById(R.id.settings_fragment).getView();
        if (settingsFragmentRootView != null)
            settingsFragmentRootView.setVisibility(View.GONE);
        if (Objects.requireNonNull(PreferenceManager.getDefaultSharedPreferences(this)
                .getStringSet(getString(R.string.settings_sections_key), new HashSet<>())).iterator().hasNext())
            LoaderManager.getInstance(this).restartLoader(TAGS_LOADER_ID, null, this);
        ((SearchableMultiSelectListPreference)
                ((PreferenceFragment) getFragmentManager().findFragmentById(R.id.settings_fragment))
                        .findPreference(getString(R.string.settings_tags_key))).clear();
    }

    @NonNull
    @Override
    public Loader<String[][]> onCreateLoader(int id, @Nullable Bundle args) {
        Uri.Builder itemUriBuilder = Uri.parse(SECTIONS_URL).buildUpon();
        switch (id) {
            case TAGS_LOADER_ID:
                itemUriBuilder = Uri.parse(TAGS_URL).buildUpon();
                Set<String> sections = PreferenceManager.getDefaultSharedPreferences(this)
                        .getStringSet(getString(R.string.settings_sections_key), new HashSet<>());
                itemUriBuilder.appendQueryParameter("section", convertStringSetToString(Objects.requireNonNull(sections)));
                itemUriBuilder.appendQueryParameter("page", String.valueOf(tagsCurPage + 1));
                itemUriBuilder.appendQueryParameter("page-size", String.valueOf(getResources().getInteger(R.integer.news_page_size_value)));
        }
        itemUriBuilder.appendQueryParameter(getString(R.string.news_api_key_name), BuildConfig.GUARDIAN_API_KEY);
        return new ItemsLoader(this, itemUriBuilder.build(), mEmptyView);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<String[][]> loader, String[][] data) {
        PreferenceFragment preferenceFragment = (PreferenceFragment) getFragmentManager().findFragmentById(R.id.settings_fragment);
        SearchableMultiSelectListPreference multiSelectListPreference =
                (SearchableMultiSelectListPreference) preferenceFragment
                        .findPreference(getString(R.string.settings_sections_key));
        if (loader.getId() == TAGS_LOADER_ID) {
            multiSelectListPreference =
                    (SearchableMultiSelectListPreference) preferenceFragment.findPreference(getString(R.string.settings_tags_key));
            multiSelectListPreference.add(data);
            tagsCurPage = Integer.valueOf(data[2][0]);
            tagsPageCount = Integer.valueOf(data[2][1]);
            if (tagsCurPage < tagsPageCount)
                LoaderManager.getInstance(this).restartLoader(TAGS_LOADER_ID, null, this);
        } else {
            multiSelectListPreference.add(data);
        }
        ((Preference.OnPreferenceChangeListener) preferenceFragment)
                .onPreferenceChange(multiSelectListPreference, multiSelectListPreference.getValues());
        mEmptyView.setVisibility(View.GONE);
        Objects.requireNonNull(preferenceFragment.getView()).setVisibility(View.VISIBLE);
    }

    @Override
    public void onLoaderReset(@NonNull Loader<String[][]> loader) {

    }

    public static class NewsPreferenceFragment extends PreferenceFragment implements Preference.OnPreferenceChangeListener {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.settings_fragment);
            Preference sections = findPreference(getString(R.string.settings_sections_key));
            bindPreferenceSummaryToValue(sections);
            Preference tags = findPreference(getString(R.string.settings_tags_key));
            bindPreferenceSummaryToValue(tags);
        }

        @Override
        public boolean onPreferenceChange(Preference preference, Object value) {
            if (preference instanceof ListPreference) {
                ListPreference listPreference = (ListPreference) preference;
                int prefIndex = listPreference.findIndexOfValue(value.toString());
                if (prefIndex >= 0) {
                    CharSequence[] labels = listPreference.getEntries();
                    value = labels[prefIndex];
                }
            }
            if (preference instanceof MultiSelectListPreference) {
                MultiSelectListPreference multiSelectListPreference = (MultiSelectListPreference) preference;
                CharSequence[] entries = multiSelectListPreference.getEntries();
                //noinspection unchecked
                Set<String> values = (Set<String>) value;
                if (null == values)
                    values = new HashSet<>();
                Iterator<String> valuesIterator = values.iterator();
                StringBuilder valueBuilder = new StringBuilder();
                boolean isValuesEmpty = !valuesIterator.hasNext();
                if (multiSelectListPreference.getKey()
                        .equals(getString(R.string.settings_sections_key))) {
                    MultiSelectListPreference tagsPreference = (MultiSelectListPreference)
                            findPreference(getString(R.string.settings_tags_key));
                    tagsPreference.setEnabled(!isValuesEmpty);
                    tagsPreference.setValues(new HashSet<>());
                    if (!isValuesEmpty) {
                        ((SettingsActivity) getActivity()).resetTagsLoader();
                    }
                }
                if (isValuesEmpty) {
                    valueBuilder.append("All");
                }

                while (valuesIterator.hasNext()) {
                    int prefIndex = multiSelectListPreference.findIndexOfValue(valuesIterator.next());
                    if (prefIndex >= 0) {
                        valueBuilder.append(entries[prefIndex]);
                        if (valuesIterator.hasNext())
                            valueBuilder.append(", ");
                    }
                }
                value = valueBuilder;
            }
            preference.setSummary(value.toString());
            return true;
        }

        private void bindPreferenceSummaryToValue(Preference preference) {
            preference.setOnPreferenceChangeListener(this);
            SharedPreferences preferences = PreferenceManager.getDefaultSharedPreferences(preference.getContext());
            Object preferenceValue;
            if (preference instanceof MultiSelectListPreference)
                preferenceValue = preferences.getStringSet(preference.getKey(), null);
            else
                preferenceValue = preferences.getString(preference.getKey(), "");
            onPreferenceChange(preference, preferenceValue);
        }
    }
}