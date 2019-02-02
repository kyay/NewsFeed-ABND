package com.example.android.newsfeed;

import android.annotation.SuppressLint;
import android.text.TextUtils;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.MalformedURLException;
import java.net.ProtocolException;
import java.net.URL;
import java.nio.charset.Charset;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.List;
import java.util.TimeZone;

import timber.log.Timber;

/**
 * A Utility class containing methods to help easily receive news data from NewsApi.org.
 */
@SuppressWarnings({"HardCodedStringLiteral", "UtilityClass"})
final class QueryUtils {
    /**
     * The read timeout of the connection to the API.
     */
    private static final int CONNECTION_READ_TIMEOUT = 1000000;
    /**
     * The connect timeout of the connection to the API.
     */
    private static final int CONNECTION_CONNECT_TIMEOUT = 1500000;

    private QueryUtils() {
    }

    /**
     * Returns a list of {@link News} objects that we got from the response
     *
     * @param requestUrl The Url to call for the {@link JSONObject} that contains info
     *                   about the News.
     * @return A list of News.
     */
    static List<News> getNewsFromStringUrl(String requestUrl, boolean forceRequest) {
        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url, forceRequest);
        } catch (IOException e) {
            Timber.e(e, "Error closing input stream");
        }

        return extractNewsFromJSON(jsonResponse);
    }

    /**
     * Returns a list of {@link News} objects that we got from the response
     *
     * @param requestUrl The Url to call for the {@link JSONObject} that contains info
     *                   about the News.
     * @return A list of News.
     */
    static String[][] getItemsFromStringUrl(String requestUrl) {
        URL url = createUrl(requestUrl);

        String jsonResponse = null;
        try {
            jsonResponse = makeHttpRequest(url, false);
        } catch (IOException e) {
            Timber.e(e, "Error closing input stream");
        }

        return extractItemsFromJSON(jsonResponse);
    }

    /**
     * Returns new URL object from the given string.
     */
    private static URL createUrl(String stringUrl) {
        URL url = null;
        try {
            url = new URL(stringUrl);
        } catch (MalformedURLException e) {
            Timber.e(e, "Error while creating URL ");
        }
        return url;
    }

    /**
     * Make an HTTP request to the given URL and return the string response
     *
     * @param url The {@link URL} object to query from
     * @return A string that contains the response
     */
    private static String makeHttpRequest(URL url, boolean forceRequest) throws IOException {
        String jsonResponse = "";

        // If the URL is null, then return early.
        if (null == url) {
            return jsonResponse;
        }

        HttpURLConnection urlConnection = null;
        InputStream inputStream = null;
        try {
            urlConnection = (HttpURLConnection) url.openConnection();
            urlConnection.setReadTimeout(CONNECTION_READ_TIMEOUT);
            urlConnection.setConnectTimeout(CONNECTION_CONNECT_TIMEOUT);
            if (forceRequest)
                urlConnection.addRequestProperty("Cache-Control", "no-cache");
            else
                urlConnection.setUseCaches(true);
            urlConnection.setRequestMethod("GET");
            urlConnection.connect();

            // If the request was successful, read from the stream and return the response
            if (HttpURLConnection.HTTP_OK == urlConnection.getResponseCode()) {
                inputStream = urlConnection.getInputStream();
                jsonResponse = readFromStream(inputStream);
            } else {
                Timber.e("Error while connecting to stream with response code: %d and url %s",
                        urlConnection.getResponseCode(), url.toString());
            }
        } catch (ProtocolException e) {
            Timber.e(e, "Problem retrieving the news from the url %s", url.toString());
        } finally {
            if (null != urlConnection) {
                urlConnection.disconnect();
            }
            if (null != inputStream) {
                inputStream.close();
            }
        }
        return jsonResponse;
    }

    /**
     * Convert the {@link InputStream} into a String that contains the response from the server.
     *
     * @param inputStream The input stream that contains the response.
     * @return The String that contains the response.
     */
    private static String readFromStream(InputStream inputStream) throws IOException {
        StringBuilder output = new StringBuilder();
        if (null != inputStream) {
            InputStreamReader inputStreamReader =
                    new InputStreamReader(inputStream, Charset.forName("UTF-8"));
            BufferedReader reader = new BufferedReader(inputStreamReader);
            String line = reader.readLine();
            while (null != line) {
                output.append(line);
                line = reader.readLine();
            }
        }
        return output.toString();
    }

    /**
     * Parses the JSON string and return a List of {@link News} objects.
     *
     * @param newsJSON The JSON string to parse
     * @return The list of News found in the string.
     */
    private static List<News> extractNewsFromJSON(String newsJSON) {
        List<News> news = new ArrayList<>();
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        try {
            JSONArray articleArray = new JSONObject(newsJSON)
                    .getJSONObject("response")
                    .getJSONArray("results");

            // If there are results in the articles array
            if (0 < articleArray.length()) {
                for (int i = 0; i < articleArray.length(); i++) {
                    JSONObject curJSONNews = articleArray.optJSONObject(i);
                    JSONObject curJSONNewsFields = curJSONNews.optJSONObject("fields");
                    String HTMLMainString = curJSONNewsFields.optString("main");
                    news.add(new News(
                            curJSONNewsFields.optString("publication"),
                            curJSONNewsFields.optString("productionOffice"),
                            curJSONNews.optString("sectionName"),
                            curJSONNews.optString("webTitle"),
                            curJSONNewsFields.optString("trailText"),
                            getDate(curJSONNews.optString("webPublicationDate")),
                            curJSONNewsFields.optString("byline"),
                            curJSONNews.optString("webUrl"),
                            HTMLMainString.contains("img") ?
                                    HTMLMainString.substring(HTMLMainString.indexOf("src") + 5,
                                            HTMLMainString.indexOf("alt") - 2) :
                                    ""));
                }
            }
        } catch (JSONException e) {
            Timber.e(e, "Problem parsing the news");
        }
        return news;
    }

    /**
     * Parses the JSON string and return a List of {@link News} objects.
     *
     * @param newsJSON The JSON string to parse
     * @return The list of News found in the string.
     */
    private static String[][] extractItemsFromJSON(String newsJSON) {
        ArrayList<String> ids = new ArrayList<>();
        ArrayList<String> titles = new ArrayList<>();
        // If the JSON string is empty or null, then return early.
        if (TextUtils.isEmpty(newsJSON)) {
            return null;
        }

        int curPage = 0, pageSize, pageCount = 1;
        try {
            JSONObject responseObject = new JSONObject(newsJSON).getJSONObject("response");
            JSONArray articleArray = responseObject.getJSONArray("results");
            pageSize = responseObject.optInt("pageSize", responseObject.optInt("total"));
            curPage = responseObject.optInt("currentPage", 1);
            pageCount = responseObject.optInt("pages", 1);
            ids = new ArrayList<>(pageSize);
            titles = new ArrayList<>(pageSize);
            // If there are results in the articles array
            if (0 < articleArray.length()) {
                for (int i = 0; i < articleArray.length(); i++) {
                    JSONObject curJSONItem = articleArray.optJSONObject(i);
                    ids.add(curJSONItem.optString("id"));
                    titles.add(curJSONItem.optString("webTitle"));
                    if (curJSONItem.optString("webTitle") == null)
                        Timber.e(i + "" + curJSONItem.optString("id"));
                }
            }
        } catch (JSONException e) {
            Timber.e(e, "Problem parsing the items");
        }
        String[][] items = new String[3][];
        items[0] = ids.toArray(new String[0]);
        items[1] = titles.toArray(new String[0]);
        items[2] = new String[]{curPage > 0 ? String.valueOf(curPage) : String.valueOf(pageCount), String.valueOf(pageCount)};
        return items;
    }

    /**
     * Takes in a date of format 2018-03-23T05:34:37.45634Z and parses it into a {@link Date} object
     *
     * @param orgDate The date to format
     * @return The Date Object.
     */
    @SuppressWarnings("UseOfObsoleteDateTimeApi")
    @SuppressLint("SimpleDateFormat")
    private static Date getDate(String orgDate) {
        String[] formats = {
                "yyyy-MM-dd'T'HH:mm:ssX",
                "yyyy-MM-dd'T'HH:mm:ss.SX",
                "yyyy-MM-dd'T'HH:mm:ss.SSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSX",
                "yyyy-MM-dd'T'HH:mm:ss.SSSSSSSX"};
        // A fail-safe that uses the current time as the date of the news
        Date date = Calendar.getInstance().getTime();
        SimpleDateFormat formatter;
        for (String format : formats) {
            try {
                formatter = new SimpleDateFormat(format);

                // The Api uses The UTC timezone, so we should also use it
                formatter.setTimeZone(TimeZone.getTimeZone("UTC"));
                // Parse the passed in date by using the formatter.
                try {
                    date = formatter.parse(orgDate);
                    break;
                } catch (ParseException e) {
                    Timber.e(e);
                }
            } catch (IllegalArgumentException e) {
                Timber.e(e);
            }
        }
        // Return the date
        return date;
    }
}