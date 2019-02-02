package com.example.android.newsfeed;

import android.content.Context;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import androidx.loader.content.AsyncTaskLoader;
import android.view.View;
import android.widget.LinearLayout;
import android.widget.TextView;

import java.lang.ref.SoftReference;
import java.util.List;

/**
 * An {@link AsyncTaskLoader} that loads a {@link List} of {@link News} objects
 */
class NewsLoader extends AsyncTaskLoader<List<News>> {
    /**
     * The url to use to fetch the {@link News} objects
     */
    private final Uri mUrl;
    /**
     * The Empty view used by the list view
     */
    private final SoftReference<LinearLayout> mEmptyView;

    private final boolean mForceRequest;

    /**
     * @param context   The {@link Context} of the app
     * @param url       The url to query from
     * @param emptyView The empty view to change based on internet connectivity
     */
    NewsLoader(Context context, Uri url, LinearLayout emptyView, boolean forceRequest) {
        super(context);
        mUrl = url;
        mEmptyView = new SoftReference<>(emptyView);
        mForceRequest = forceRequest;
    }

    @Override
    protected void onStartLoading() {
        ConnectivityManager cm =
                (ConnectivityManager) getContext().getSystemService(Context.CONNECTIVITY_SERVICE);

        assert null != cm;
        NetworkInfo activeNetwork = cm.getActiveNetworkInfo();
        boolean isNotConnected = !((null != activeNetwork) &&
                activeNetwork.isConnectedOrConnecting());
        LinearLayout emptyView = mEmptyView.get();
        if (null != emptyView) {
            // If there is no internet connection, then tell the user
            // to check their connection and return
            if (isNotConnected) {
                ((TextView) emptyView.getChildAt(0)).setText(R.string.no_internet);
                emptyView.getChildAt(1).setVisibility(View.VISIBLE);
                emptyView.getChildAt(2).setVisibility(View.GONE);
                return;
            }
            // Tell the users to wait until we get the news
            ((TextView) emptyView.getChildAt(0)).setText(R.string.please_wait);
            emptyView.getChildAt(1).setVisibility(View.GONE);
            emptyView.getChildAt(2).setVisibility(View.VISIBLE);
            mEmptyView.clear();
        }
        forceLoad();
    }

    @Override
    public List<News> loadInBackground() {
        // Return the list of News.
        return QueryUtils.getNewsFromStringUrl(mUrl.toString(), mForceRequest);
    }
}
