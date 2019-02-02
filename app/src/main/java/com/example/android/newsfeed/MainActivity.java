package com.example.android.newsfeed;

import android.annotation.SuppressLint;
import android.app.AlertDialog;
import android.app.SearchManager;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.net.Uri;
import android.net.http.HttpResponseCache;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.provider.SearchRecentSuggestions;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.loader.app.LoaderManager;
import androidx.loader.content.Loader;
import androidx.swiperefreshlayout.widget.SwipeRefreshLayout;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import androidx.appcompat.widget.SearchView;
import androidx.recyclerview.widget.StaggeredGridLayoutManager;

import android.text.TextUtils;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import org.jetbrains.annotations.NonNls;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

import butterknife.BindView;
import butterknife.ButterKnife;
import timber.log.Timber;

@SuppressWarnings("WeakerAccess")
public class MainActivity extends AppCompatActivity implements LoaderManager.LoaderCallbacks<List<News>> {
    /**
     * The String Url to query from.
     */
    @NonNls
    private static final String QUERY_URL = "https://content.guardianapis.com/search";
    /**
     * The Delay for the check for the time elapsed since the last user interaction.
     */
    private static final int HANDLER_DELAY_TIME = 60000;
    /**
     * The number of milliseconds to wait since the last user interaction to refresh the list.
     */
    private static final int REFRESH_NO_INTERACTION_TIME = 36000;
    /**
     * The Cache size for the Http Cache.
     */
    private static final int HTTP_CACHE_SIZE = 20 * 1024 * 1024; // 20 MiB
    /**
     * A handler used to update the list every 2 minutes
     */
    private final Handler mHandler = new Handler();
    /**
     * The Empty View to show when there is no news data
     */
    @BindView(R.id.empty_view)
    LinearLayout mEmptyView;
    /**
     * The {@link RecyclerView} showing the News
     */
    @BindView(R.id.list)
    RecyclerView mNewsRecyclerView;
    /**
     * The {@link SwipeRefreshLayout} that is used to help the user refresh by swiping from top to bottom
     */
    @BindView(R.id.refresh_layout)
    SwipeRefreshLayout mRefreshLayout;
    /**
     * The search view menu item
     */
    MenuItem mSearchViewItem;
    /**
     * The current page that we had queried for
     */
    private int mPage = 1;
    /**
     * The user-provided query
     */
    private String mQuery = "";
    /**
     * A boolean indicating if we're still loading news data or not
     */
    private boolean mIsLoading = false;
    /**
     * A boolean indicating if should not use the cache or not
     */
    private boolean mForceRequest = false;
    /**
     * An adapter for the list of news
     */
    private NewsAdapter mAdapter;
    /**
     * The last time the list was interacted with
     */
    private long mLastInteractionTime;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        setQuery(getIntent());

        // If the device has a keyboard (i.e a bluetooth keyboard or an emulator)
        // then the search VIEW will appear.
        setDefaultKeyMode(DEFAULT_KEYS_SEARCH_LOCAL);

        setSupportActionBar(findViewById(R.id.toolbar));

        try {
            File httpCacheDir = new File(getCacheDir(), "http");
            HttpResponseCache.install(httpCacheDir, HTTP_CACHE_SIZE);
        } catch (IOException e) {
            Timber.i(e, "HTTP response cache installation failed:");
        }

        ButterKnife.bind(this);

        mLastInteractionTime = Calendar.getInstance().getTimeInMillis();
        mAdapter = new NewsAdapter(new ArrayList<>());
        mAdapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            @Override
            public void onChanged() {
                super.onChanged();
                checkEmpty();
            }

            @Override
            public void onItemRangeInserted(int positionStart, int itemCount) {
                super.onItemRangeInserted(positionStart, itemCount);
                checkEmpty();
            }

            @Override
            public void onItemRangeRemoved(int positionStart, int itemCount) {
                super.onItemRangeRemoved(positionStart, itemCount);
                checkEmpty();
            }

            private void checkEmpty() {
                mEmptyView.setVisibility(mAdapter.getItemCount() == 0 ? View.VISIBLE : View.GONE);
            }
        });

        mNewsRecyclerView.setAdapter(mAdapter);
        int spanCount = getResources().getInteger(R.integer.recycler_view_span_count);
        mNewsRecyclerView.setLayoutManager(spanCount > 0 ?
                new StaggeredGridLayoutManager(spanCount, StaggeredGridLayoutManager.VERTICAL) :
                new LinearLayoutManager(this));
        mNewsRecyclerView.addItemDecoration(new InnerMarginItemDecorator((int) getResources().getDimension(R.dimen.news_item_inner_margin)));

        refreshList();

        // Set the on click listener for the try again button to refresh the list
        mEmptyView.getChildAt(1).setOnClickListener(v -> {
            refreshList();
            mForceRequest = true;
        });

        mRefreshLayout.setOnRefreshListener(
                () -> {
                    refreshList();
                    mForceRequest = true;
                }
        );

        mNewsRecyclerView.addOnScrollListener(new RecyclerView.OnScrollListener() {
            @Override
            public void onScrollStateChanged(@NonNull RecyclerView recyclerView, int newState) {
                if (newState == RecyclerView.SCROLL_STATE_IDLE)
                    // Fix the bug when the StaggeredGridLayoutManager moves items
                    // which messes up with item decoration by invalidating Item decorations
                    // Note: this method will get called AFTER the LayoutManager and The RecyclerView
                    // perform their onScrollStateChanged, so the LayoutManager surely had already
                    // called checkForGaps.
                    recyclerView.invalidateItemDecorations();
            }

            @Override
            public void onScrolled(@NonNull RecyclerView recyclerView, int dx, int dy) {
                RecyclerView.LayoutManager layoutManager = recyclerView.getLayoutManager();
                int visibleItemCount = Objects.requireNonNull(layoutManager).getChildCount();
                // Get the last item in screen depending on the layout manager the recyclerView is using
                int lastInScreen = (layoutManager instanceof LinearLayoutManager ?
                        ((LinearLayoutManager) layoutManager).findFirstVisibleItemPosition() :
                        (layoutManager instanceof StaggeredGridLayoutManager ?
                                ((StaggeredGridLayoutManager) layoutManager).findFirstVisibleItemPositions(null)[0]
                                : 0))
                        + visibleItemCount;
                // If the last item in screen should be the first item when we reach the end of the list
                // Then we should fetch more items
                if ((lastInScreen > layoutManager.getItemCount() - visibleItemCount) && !mIsLoading) {
                    mPage++;
                    mIsLoading = true;
                    LoaderManager.getInstance(MainActivity.this).restartLoader(0, null, MainActivity.this);
                }
                mLastInteractionTime = Calendar.getInstance().getTimeInMillis();
            }
        });
        this.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                // If we aren't currently loading data and 2 minutes had passed, then refresh the data
                if (!mIsLoading && Calendar.getInstance().getTimeInMillis() - mLastInteractionTime >= REFRESH_NO_INTERACTION_TIME) {
                    refreshList();
                }
                // Run this again after a minute.
                mHandler.postDelayed(this, HANDLER_DELAY_TIME);
            }
        });
    }

    /**
     * Refreshes the list view by creating a {@link Loader} that loads the news data
     */
    private void refreshList() {
        mRefreshLayout.setRefreshing(true);
        mPage = 1;
        mAdapter.clear();
        // Using the support Loader Manager because framework loaders got deprecated in android P
        // and also because support loader manager uses LiveData and ViewModel which is much better
        // than the regular loader manager.
        LoaderManager.getInstance(this).restartLoader(0, null, this);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (requestCode == 0 && resultCode == RESULT_OK) {
            if (data.getBooleanExtra(SettingsActivity.SETTINGS_CHANGED_KEY, true))
                refreshList();
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_refresh:
                refreshList();
                mForceRequest = true;
                return true;
            case R.id.action_settings:
                startActivityForResult(new Intent(this, SettingsActivity.class), 0);
                return true;
            case R.id.menu_search:
                item.setOnActionExpandListener(new MenuItem.OnActionExpandListener() {
                    @Override
                    public boolean onMenuItemActionExpand(MenuItem item) {
                        return true;
                    }

                    @Override
                    public boolean onMenuItemActionCollapse(MenuItem item) {
                        mQuery = "";
                        refreshList();
                        return true;
                    }
                });
                return true;
            case R.id.menu_clear_history:
                showHistoryClearConfirmationDialog();
                return true;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.main_menu, menu);

        mSearchViewItem = menu.findItem(R.id.menu_search);

        SearchView searchView = (SearchView) mSearchViewItem.getActionView();
        searchView.setSearchableInfo(((SearchManager) getSystemService(Context.SEARCH_SERVICE))
                .getSearchableInfo(getComponentName()));
        searchView.setIconifiedByDefault(false);
        searchView.setSubmitButtonEnabled(true);
        searchView.setQueryRefinementEnabled(true);

        return true;
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void triggerSearch(String query, @Nullable Bundle appSearchData) {
        mSearchViewItem.expandActionView();
        SearchView searchView = (SearchView) mSearchViewItem.getActionView();
        searchView.setAppSearchData(appSearchData);
        searchView.setQuery(query, true);
    }

    @SuppressLint("RestrictedApi")
    @Override
    public void startSearch(@Nullable String initialQuery, boolean selectInitialQuery, @Nullable Bundle appSearchData, boolean globalSearch) {
        if (globalSearch) {
            super.startSearch(initialQuery, selectInitialQuery, appSearchData, true);
            return;
        }
        mSearchViewItem.expandActionView();
        SearchView searchView = (SearchView) mSearchViewItem.getActionView();
        searchView.setAppSearchData(appSearchData);
        searchView.setQuery(initialQuery, false);
    }

    @NonNull
    @Override
    public Loader<List<News>> onCreateLoader(int id, Bundle args) {
        Uri baseUri = Uri.parse(QUERY_URL);
        Uri.Builder uriBuilder = baseUri.buildUpon();
        uriBuilder.appendQueryParameter(getString(R.string.news_api_key_name), BuildConfig.GUARDIAN_API_KEY);
        uriBuilder.appendQueryParameter(getString(R.string.news_page_name), String.valueOf(mPage));
        if (!TextUtils.isEmpty(mQuery))
            uriBuilder.appendQueryParameter("q", mQuery);
        uriBuilder.appendQueryParameter(getString(R.string.news_query_fields_name), getString(R.string.news_query_fields_value));
        uriBuilder.appendQueryParameter(getString(R.string.news_page_size_name), String.valueOf(getResources().getInteger(R.integer.news_page_size_value)));
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);

        Set<String> sections = sharedPreferences
                .getStringSet(getString(R.string.settings_sections_key), new HashSet<>());
        Set<String> tags = sharedPreferences
                .getStringSet(getString(R.string.settings_tags_key), new HashSet<>());
        if (Objects.requireNonNull(sections).iterator().hasNext())
            if (Objects.requireNonNull(tags).iterator().hasNext())
                uriBuilder.appendQueryParameter("tag", SettingsActivity.convertStringSetToString(tags));
            else
                uriBuilder.appendQueryParameter("section", SettingsActivity.convertStringSetToString(sections));

        return new NewsLoader(this, uriBuilder.build(), mEmptyView, mForceRequest);
    }

    @Override
    public void onLoadFinished(@NonNull Loader<List<News>> loader, List<News> data) {
        mIsLoading = false;
        mForceRequest = false;
        // Configure the empty view
        ((TextView) mEmptyView.getChildAt(0)).setText(R.string.no_data_found);
        mEmptyView.getChildAt(1).setVisibility(View.VISIBLE);
        mEmptyView.getChildAt(2).setVisibility(View.GONE);
        if (data == null || data.size() == 0)
            return;

        mAdapter.addAll(data);

        // Stop the refreshing circle from showing
        mRefreshLayout.setRefreshing(false);

        // Update the last update time
        mLastInteractionTime = Calendar.getInstance().getTimeInMillis();
    }

    @Override
    protected void onStop() {
        super.onStop();

        HttpResponseCache cache = HttpResponseCache.getInstalled();
        if (cache != null) {
            cache.flush();
        }
    }

    @Override
    public void onLoaderReset(@NonNull Loader<List<News>> loader) {
        mAdapter.clear();
    }

    private void setQuery(Intent searchIntent) {
        if (Intent.ACTION_SEARCH.equals(searchIntent.getAction())) {
            mQuery = searchIntent.getStringExtra(SearchManager.QUERY);
        }
        new SearchRecentSuggestions(this,
                NewsRecentSuggestionsProvider.AUTHORITY, NewsRecentSuggestionsProvider.MODE)
                .saveRecentQuery(mQuery, null);
    }

    @Override
    protected void onNewIntent(Intent intent) {
        setIntent(intent);
        setQuery(intent);
        refreshList();
    }

    private void showHistoryClearConfirmationDialog() {
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(R.string.clear_history_dialog_msg);
        builder.setPositiveButton(R.string.clear_history_yes, (dialog, id) -> new SearchRecentSuggestions(MainActivity.this,
                NewsRecentSuggestionsProvider.AUTHORITY, NewsRecentSuggestionsProvider.MODE)
                .clearHistory());
        builder.setNegativeButton(android.R.string.cancel, (dialog, id) -> {
            if (dialog != null) {
                dialog.dismiss();
            }
        });
        builder.show();
    }
}
