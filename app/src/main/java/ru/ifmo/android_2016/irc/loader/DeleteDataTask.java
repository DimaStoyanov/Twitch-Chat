package ru.ifmo.android_2016.irc.loader;

import android.content.Context;
import android.support.v4.content.AsyncTaskLoader;
import android.util.Log;

import java.io.IOException;

import ru.ifmo.android_2016.irc.utils.FileUtils;

/**
 * Created by Dima Stoyanov on 26.10.2016.
 * Project Android-IRC
 * Start time : 1:50
 */

public class DeleteDataTask extends AsyncTaskLoader<LoadResult<Void>> {

    private final String TAG = "Delete data task";
    private final Context context;
    private final String path;
    private final String package_name;
    private final String id;
    private final String extension = "bin";


    public DeleteDataTask(Context context, String path, String package_name, String id) {
        super(context);
        this.context = context;
        this.path = path;
        this.package_name = package_name;
        this.id = id;
    }

    @Override
    protected void onStartLoading() {
        forceLoad();
    }

    @Override
    public LoadResult<Void> loadInBackground() {
        ResultType resultType = ResultType.ERROR;
        try {
            FileUtils.deleteData(context, path, package_name, extension, id);
            resultType = ResultType.OK;
        } catch (IOException e) {
            e.printStackTrace();
            Log.e(TAG, "Error deleting login data");
        }
        return new LoadResult<>(resultType, null);
    }
}