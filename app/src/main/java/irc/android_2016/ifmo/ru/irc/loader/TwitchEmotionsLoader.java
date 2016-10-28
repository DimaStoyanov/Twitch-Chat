package irc.android_2016.ifmo.ru.irc.loader;

import android.content.Context;
import android.graphics.Bitmap;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import com.facebook.stetho.urlconnection.StethoURLConnectionManager;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.util.ArrayList;
import java.util.List;

import irc.android_2016.ifmo.ru.irc.api.TwitchApi;
import irc.android_2016.ifmo.ru.irc.constant.FilePathConstant;
import irc.android_2016.ifmo.ru.irc.exception.BadResponseException;
import irc.android_2016.ifmo.ru.irc.model.TwitchEmoticon;
import irc.android_2016.ifmo.ru.irc.utils.DownloadUtils;
import irc.android_2016.ifmo.ru.irc.utils.FileUtils;
import irc.android_2016.ifmo.ru.irc.utils.IOUtils;

/**
 * Created by Dima Stoyanov on 24.10.2016.
 * Project Android-IRC
 * Start time : 22:49
 */

public class TwitchEmotionsLoader extends AsyncTaskLoader<LoadResult<List<TwitchEmoticon>>> {
    private static final String TAG = "Twitch emoticons loader";
    private final String channel;
    private final Context context;
    private final FilePathConstant constant;

    public TwitchEmotionsLoader(Context context, String channel, FilePathConstant contant) {
        super(context);
        this.channel = channel;
        this.context = context;
        this.constant = contant;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public LoadResult<List<TwitchEmoticon>> loadInBackground() {
        final StethoURLConnectionManager stethoManager = new StethoURLConnectionManager("API");

        ResultType resultType = ResultType.ERROR;
        List<TwitchEmoticon> data = null;
        HttpURLConnection connection = null;

        try {
            connection = TwitchApi.getTwitchChannelEmotiсons(channel);
            Log.d(TAG, "Performing request: " + connection.getURL());

            connection.setConnectTimeout(15000); // 15 sec
            connection.setReadTimeout(15000); // 15 sec

            stethoManager.preConnect(connection, null);
            connection.connect();
            stethoManager.postConnect();

            if (HttpURLConnection.HTTP_OK == connection.getResponseCode()) {
                data = (new Parser(context, channel, constant)).parse(
                        stethoManager.interpretResponseStream(connection.getInputStream()));
                resultType = ResultType.OK;
                Log.d(TAG, "Data downloaded and parsed");
            } else {
                throw new BadResponseException("HTTP: " + connection.getResponseCode()
                        + ", " + connection.getResponseMessage());
            }
        } catch (IOException e)

        {
            stethoManager.httpExchangeFailed(e);

            if (!IOUtils.isConnectionAvailable(getContext(), false)) {
                resultType = ResultType.NO_INTERNET;
                Log.e(TAG, "Failed to get popular movies: internet connection is not available", e);
            }
        } catch (
                Exception e
                )

        {
            Log.e(TAG, "Failed to get twitch emotions: unexpected error", e);
        } finally

        {
            if (connection != null) {
                connection.disconnect();
            }
        }

        return new LoadResult<>(resultType, data);
    }


    private static class Parser {
        private final Context context;
        private final String channel;
        private final String SUB_ROOT = "emoticons";
        private final FilePathConstant constant;

        public Parser(Context context, String channel, FilePathConstant constant) {
            this.context = context;
            this.channel = channel;
            this.constant = constant;
        }

        private List<TwitchEmoticon> parse(InputStream in) throws IOException, JSONException {
            List<TwitchEmoticon> result = new ArrayList<>();
            JSONObject resultJSON = new JSONObject(IOUtils.readToString(in, "UTF-8"));
            Log.d(TAG, resultJSON.toString());
            JSONArray emoticons = resultJSON.getJSONArray("emoticons");
            String regex, url;
            for (int i = 0; i < emoticons.length(); i++) {
                regex = ((JSONObject) emoticons.get(i)).optString("regex");
                url = ((JSONObject) emoticons.get(i)).optString("url");
//                url = DownloadUtils.downloadFile(context, url, SUB_ROOT + File.separator + channel, null, "png");
                result.add(new TwitchEmoticon(regex, url));
            }

            FileUtils.addEmoticonData(FileUtils.createExternalFile(context, constant.EMOTION_PACKAGE_NAME + File.separator + channel,
                    constant.EMOTICON_INFO_NAME, constant.EMOTICON_INFO_EXTENSION).getPath(), result);
            return result;
        }


    }
}
