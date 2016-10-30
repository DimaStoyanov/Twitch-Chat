package ru.ifmo.android_2016.irc.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.facebook.stetho.urlconnection.StethoURLConnectionManager;

import org.json.JSONException;
import org.json.JSONObject;

import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;

import ru.ifmo.android_2016.irc.api.TwitchApi;
import ru.ifmo.android_2016.irc.exception.BadResponseException;
import ru.ifmo.android_2016.irc.utils.IOUtils;

/**
 * Created by Dima Stoyanov on 30.10.2016.
 * Project Android-IRC
 * Start time : 17:21
 */

public class TwitchUserNickLoader extends AsyncTaskLoader<LoadResult<String>> {
    private final String TAG = TwitchUserNickLoader.class.getSimpleName();
    private final String token;

    public TwitchUserNickLoader(Context context, final String token) {
        super(context);
        this.token = token;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public LoadResult<String> loadInBackground() {
        final StethoURLConnectionManager stethoManager = new StethoURLConnectionManager("API");

        ResultType resultType = ResultType.ERROR;
        String nick = null;
        HttpURLConnection connection = null;

        try {
            connection = TwitchApi.getUserTwitchRequest(token);
            Log.d(TAG, "Performing request: " + connection.getURL());

            connection.setConnectTimeout(15000); // 15 sec
            connection.setReadTimeout(15000); // 15 sec

            stethoManager.preConnect(connection, null);
            connection.connect();
            stethoManager.postConnect();

            if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                nick = parse(stethoManager.interpretResponseStream(connection.getInputStream()));
                resultType = ResultType.OK;
                Log.d(TAG, "Data downloaded and parsed");
            } else {
                throw new BadResponseException("HTTP: " + connection.getResponseCode()
                        + ", " + connection.getResponseMessage());
            }
        } catch (IOException e) {
            stethoManager.httpExchangeFailed(e);
            if (!IOUtils.isConnectionAvailable(getContext(), false)) {
                resultType = ResultType.NO_INTERNET;
                Log.e(TAG, "Failed to get popular movies: internet connection is not available", e);
            }
        } catch (Exception e) {
            Log.e(TAG, "Failed to get twitch emotions: unexpected error", e);
        } finally {
            if (connection != null) {
                connection.disconnect();
            }
        }
        return new LoadResult<>(resultType, nick);
    }


    private String parse(InputStream in) throws IOException, JSONException {
        JSONObject resultJSON = new JSONObject(IOUtils.readToString(in, "UTF-8"));
        Log.d(TAG, resultJSON.toString());
        return resultJSON.optJSONObject("token").optString("user_name");
    }
}
