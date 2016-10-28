package ru.ifmo.android_2016.irc.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;
import java.util.List;

import ru.ifmo.android_2016.irc.model.LoginData;
import ru.ifmo.android_2016.irc.utils.FileUtils;

/**
 * Created by Dima Stoyanov on 26.10.2016.
 * Project Android-IRC
 * Start time : 1:23
 */

public class LoginReadTask extends AsyncTaskLoader<LoadResult<List<LoginData>>> {

    private final String TAG = "Read data task";

    private final String path;

    public LoginReadTask(Context context, String path) {
        super(context);
        this.path = path;
    }

    @Override
    protected void onStartLoading() {
        super.onStartLoading();
        Log.d(TAG, "Start reading file");
        forceLoad();
    }

    @Override
    public LoadResult<List<LoginData>> loadInBackground() {
        ResultType resultType = ResultType.ERROR;
        List<LoginData> data = null;
        try {
            data = FileUtils.getData(path);
            resultType = ResultType.OK;
        } catch (IOException e) {
            e.printStackTrace();
        }

        return new LoadResult<>(resultType, data);

    }
}
