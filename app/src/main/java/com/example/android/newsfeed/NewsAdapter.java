package com.example.android.newsfeed;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.ViewGroup;

import java.util.Collection;
import java.util.List;

/**
 * A {@link RecyclerView.Adapter}
 * that shows a {@link List} of {@link News} objects using a {@link NewsHolder}
 */
@SuppressWarnings("WeakerAccess")
class NewsAdapter extends RecyclerView.Adapter<NewsHolder> {
    /**
     * A List of News that will get shown
     */
    private final List<News> mNews;

    /**
     * @param news The {@link List} of {@link News} objects to be shown by the adapter
     */
    NewsAdapter(@NonNull List<News> news) {
        mNews = news;
    }

    @SuppressWarnings("MethodReturnOfConcreteClass")
    @NonNull
    @Override
    public NewsHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        return new NewsHolder(parent.getContext(),
                LayoutInflater.from(parent.getContext()).inflate(R.layout.list_item, parent, false));
    }

    @SuppressWarnings("MethodParameterOfConcreteClass")
    @Override
    public void onBindViewHolder(@NonNull NewsHolder holder, int position) {
        holder.bindNews(mNews.get(position));
    }

    @Override
    public int getItemCount() {
        return mNews.size();
    }

    /**
     * Clears The List of News
     */
    public void clear() {
        mNews.clear();
        notifyDataSetChanged();
    }

    /**
     * Adds the {@param newsToAdd} to the List of News
     *
     * @param newsToAdd The Collection of News to add
     */
    public void addAll(Collection<? extends News> newsToAdd) {
        mNews.addAll(newsToAdd);
        notifyDataSetChanged();
    }
}
