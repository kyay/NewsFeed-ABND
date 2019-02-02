package com.example.android.newsfeed;

import android.content.Context;
import android.content.Intent;
import android.graphics.Paint;
import android.net.Uri;
import androidx.recyclerview.widget.RecyclerView;
import android.text.Html;
import android.text.Layout;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.Spanned;
import android.text.TextUtils;
import android.text.style.AlignmentSpan;
import android.text.style.LeadingMarginSpan;
import android.text.style.LineHeightSpan;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;

import com.bumptech.glide.Glide;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * A {@link RecyclerView.ViewHolder} that Holds a {@link News} list item
 */
@SuppressWarnings("WeakerAccess")
class NewsHolder extends RecyclerView.ViewHolder implements View.OnClickListener {
    private final Context mContext;
    /**
     * The text view that shows the source and category of the news
     */
    @BindView(R.id.source_category_text_view)
    TextView sourceCategoryTextView;
    /**
     * The text view that shows the title of the news
     */
    @BindView(R.id.title_text_view)
    TextView titleTextView;
    /**
     * The text view that shows the description of the news
     */
    @BindView(R.id.description_text_view)
    TextView descriptionTextView;
    /**
     * The text view that shows the date and author of the news
     */
    @BindView(R.id.date_author_text_view)
    TextView dateAuthorTextView;
    /**
     * The image view that shows the image of the news
     */
    @BindView(R.id.background_image_view)
    ImageView backgroundImageView;
    private String mNewsUrl = "";

    /**
     * @param context  The context of the app
     * @param newsItem The view that contains the appropriate views that corresponds to the fields
     *                 in the {@link News} class
     */
    NewsHolder(Context context, View newsItem) {
        super(newsItem);
        mContext = context.getApplicationContext();
        ButterKnife.bind(this, newsItem);
        newsItem.setOnClickListener(this);
    }

    /**
     * Returns whether the passed string has an important value or not.
     * More technically, it returns false if the string is equal to "null" or empty, and true otherwise
     *
     * @param string The string to check for significance
     * @return whether the string is significant
     */
    private static boolean hasASignificantValue(String string) {
        String lowerCaseString = string.toLowerCase();
        return !"null".equals(lowerCaseString) && !TextUtils.isEmpty(lowerCaseString);
    }

    /**
     * Returns a {@link SpannableString} that contains the leftText and rightText,
     * so that the left text will be aligned left and the right text will be aligned right,
     * and that any overflow text will start from the start of the text view. For example:
     * ---------------------------
     * |left text  super long text|
     * |   that will be aligned to|
     * |                 the right|
     * ---------------------------
     *
     * @param textContainer The {@link TextView} that will contains the text(used for some calculations).
     * @param leftText      The text that will be left-aligned
     * @param rightText     The text that will be right-aligned
     * @return a String that has the formatted left and right strings
     */
    private static CharSequence createSameLineAlignedText(TextView textContainer, String leftText, String rightText) {
        String fullText = leftText + "\n " + rightText;
        Spannable sameLineSpannableString = new SpannableString(fullText);
        AlignmentSpan alignmentSpan = new AlignmentSpan.Standard(Layout.Alignment.ALIGN_OPPOSITE);
        sameLineSpannableString.setSpan(alignmentSpan, leftText.length(), fullText.length(), Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sameLineSpannableString.setSpan(new LineOverlapSpan(textContainer.getLineHeight()), leftText.length(), leftText.length() + 1, Spanned.SPAN_EXCLUSIVE_EXCLUSIVE);
        sameLineSpannableString.setSpan(new LeadingMarginSpan.Standard(
                        (int) textContainer.getPaint().measureText(leftText), 0),
                leftText.length() + 1, fullText.length(), 0);
        return sameLineSpannableString;
    }

    /**
     * Binds the {@param news} object to the list item associated with this item
     *
     * @param news The news object to show on the list item
     */
    @SuppressWarnings({"FeatureEnvy", "MethodParameterOfConcreteClass"})
    public void bindNews(News news) {
        mNewsUrl = news.getUrl();
        // Setting the source text view
        String sourceText = news.getSource();
        if (hasASignificantValue(sourceText))
            sourceCategoryTextView.setVisibility(View.VISIBLE);
        else
            sourceCategoryTextView.setVisibility(View.GONE);
        sourceCategoryTextView.setText(createSameLineAlignedText(sourceCategoryTextView,
                mContext.getString(R.string.news_from_source, sourceText, news.getCountry()).toUpperCase(),
                news.getCategory().toUpperCase()));

        // Setting the title text view
        titleTextView.setText(news.getTitle());

        // Setting the description text view
        String descriptionText = news.getDescription();
        if (hasASignificantValue(descriptionText))
            descriptionTextView.setVisibility(View.VISIBLE);
        else
            descriptionTextView.setVisibility(View.GONE);
        descriptionTextView.setText(Html.fromHtml(descriptionText));

        // Setting the date and author text view
        String authorText = news.getAuthor();
        String date = news.getFormattedDate(mContext);
        if (hasASignificantValue(authorText)) {
            dateAuthorTextView.setText(createSameLineAlignedText(dateAuthorTextView,
                    date,
                    mContext.getString(R.string.news_by_author, authorText)));
        } else {
            dateAuthorTextView.setText(date);
        }

        // Setting the image view using the url associated with the news
        // using Glide for easier image fetching and memory/disk caching
        String urlToImage = news.getUrlToImage();
        if (hasASignificantValue(urlToImage))
            Glide.with(mContext).load(urlToImage).into(backgroundImageView);
        else {
            Glide.with(mContext).clear(backgroundImageView);
        }
    }

    @Override
    public void onClick(View v) {
        mContext.startActivity(
                new Intent(Intent.ACTION_VIEW, Uri.parse(mNewsUrl))
                        .addFlags(Intent.FLAG_ACTIVITY_NEW_TASK));
    }

    /**
     * A {@link LineHeightSpan} that overlaps the line by removing the line space.
     */
    private static class LineOverlapSpan implements LineHeightSpan {
        private final int mLineHeight;

        LineOverlapSpan(int lineHeight) {
            super();
            mLineHeight = lineHeight;
        }

        @Override
        public void chooseHeight(CharSequence text, int start, int end,
                                 int spanstartv, int v,
                                 Paint.FontMetricsInt fm) {
            fm.bottom -= mLineHeight;
            fm.descent -= mLineHeight;
        }
    }
}
