package com.example.android.newsfeed;

import android.content.Context;
import android.os.Parcel;
import android.os.Parcelable;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;

/**
 * A class that holds information about a certain News.
 */
@SuppressWarnings("MethodReturnOfConcreteClass")
class News implements Parcelable {
    @SuppressWarnings({"AnonymousInnerClassWithTooManyMethods", "AnonymousInnerClass"})
    public static final Parcelable.Creator<News> CREATOR = new Parcelable.Creator<News>() {
        @Override
        public News createFromParcel(Parcel source) {
            return new News(source);
        }

        @Override
        public News[] newArray(int size) {
            return new News[size];
        }
    };
    /**
     * The Source that published the news
     */
    private final String mSource;
    /**
     * The Country that the news is talking about
     */
    private final String mCountry;
    /**
     * The Category that the news belongs to
     */
    private final String mCategory;
    /**
     * The title of the news
     */
    private final String mTitle;
    /**
     * The description of the news
     */
    private final String mDescription;
    /**
     * The date that the news was published at
     */
    @Nullable
    private final Date mDate;
    /**
     * The author that wrote the news
     */
    private final String mAuthor;
    /**
     * The Url that points to the original post that contains the news
     */
    private final String mUrl;
    /**
     * The Url to the image that describes the news
     */
    private final String mUrlToImage;

    News(@NonNull String source, @NonNull String country, @NonNull String category,
         @NonNull String title, @NonNull String description, @NonNull Date date,
         @NonNull String author, @NonNull String url, @NonNull String urlToImage) {
        mSource = source;
        mCountry = country;
        mCategory = category;
        mTitle = title;
        mDescription = description;
        mDate = (Date) date.clone();
        mAuthor = author;
        mUrl = url;
        mUrlToImage = urlToImage;
    }

    private News(Parcel in) {
        this.mSource = in.readString();
        mCountry = in.readString();
        mCategory = in.readString();
        mTitle = in.readString();
        mDescription = in.readString();
        long tmpMDate = in.readLong();
        mDate = (-1 == tmpMDate) ? null : new Date(tmpMDate);
        mAuthor = in.readString();
        mUrl = in.readString();
        mUrlToImage = in.readString();
    }

    /**
     * Returns the source of the news
     *
     * @return the news' source
     */
    public String getSource() {
        return mSource;
    }

    /**
     * Returns the country of the news
     *
     * @return the news' country
     */
    public String getCountry() {
        return mCountry;
    }

    /**
     * Returns the category of the news
     *
     * @return the news' category
     */
    public String getCategory() {
        return mCategory;
    }

    /**
     * Returns the title of the news
     *
     * @return the news' title
     */
    public String getTitle() {
        return mTitle;
    }

    /**
     * Returns the description of the news
     *
     * @return the news' description
     */
    public String getDescription() {
        return mDescription;
    }

    /**
     * Returns the publish date of the news in the form of Aug 4, 2018 at 05:34 pm.
     *
     * @return the news' formatted date
     */
    @SuppressWarnings("HardCodedStringLiteral")
    public String getFormattedDate(Context context) {
        // Format the date using the format.
        return new SimpleDateFormat("MMM dd, yyyy '" +
                context.getString(R.string.news_at_date) +
                "' hh:mm a", Locale.getDefault()).format(mDate);
    }

    /**
     * Returns the author(s) that wrote the news
     *
     * @return the news' author(s)
     */
    public String getAuthor() {
        return mAuthor;
    }

    /**
     * Returns the original Url of the post about the nes
     *
     * @return the news' Url
     */
    public String getUrl() {
        return mUrl;
    }

    /**
     * Returns the url of the image about the news
     *
     * @return the news' image url
     */
    public String getUrlToImage() {
        return mUrlToImage;
    }

    @SuppressWarnings("LocalVariableOfConcreteClass")
    @Override
    public boolean equals(Object obj) {
        if (!(obj instanceof News))
            return false;
        News news = (News) obj;
        return mTitle.equals(news.getTitle()) || mDescription.equals(news.getDescription());
    }

    @Override
    public int hashCode() {
        int hash = 3;
        hash = (53 * hash) + mTitle.hashCode();
        hash = (53 * hash) + mDescription.hashCode();
        return hash;
    }

    @NonNull
    @SuppressWarnings({"HardCodedStringLiteral", "MagicCharacter"})
    @Override
    public String toString() {
        return "News{" +
                "mSource='" + mSource + '\'' +
                ", mCountry='" + mCountry + '\'' +
                ", mCategory='" + mCategory + '\'' +
                ", mTitle='" + mTitle + '\'' +
                ", mDescription='" + mDescription + '\'' +
                ", mDate=" + mDate +
                ", mAuthor='" + mAuthor + '\'' +
                ", mUrl='" + mUrl + '\'' +
                ", mUrlToImage='" + mUrlToImage + '\'' +
                '}';
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(mSource);
        dest.writeString(mCountry);
        dest.writeString(mCategory);
        dest.writeString(mTitle);
        dest.writeString(mDescription);
        dest.writeLong((null != this.mDate) ? mDate.getTime() : -1);
        dest.writeString(mAuthor);
        dest.writeString(mUrl);
        dest.writeString(mUrlToImage);
    }
}
