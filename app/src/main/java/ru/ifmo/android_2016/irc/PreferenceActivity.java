package ru.ifmo.android_2016.irc;

import android.os.Bundle;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.widget.Toolbar;
import android.view.MenuItem;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.facebook.drawee.backends.pipeline.Fresco;
import com.facebook.imagepipeline.core.ImagePipeline;

import ru.ifmo.android_2016.irc.client.ServerList;
import ru.ifmo.android_2016.irc.utils.Log;
import ru.ifmo.android_2016.irc.utils.WebUtils;

import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CLEAR_EMOTES_CACHE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.CLEAR_LOGIN_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.SIGN_OUT_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.TEXT_SIZE_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_DARK_KEY;
import static ru.ifmo.android_2016.irc.constant.PreferencesConstant.THEME_KEY;


/**
 * Created by Dima Stoyanov on 21.11.2016.
 * Project Android-IRC
 * Start time : 4:28
 */

public class PreferenceActivity extends BaseActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_preferences);

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        toolbar.setTitle(getResources().getString(R.string.settings));
        setSupportActionBar((Toolbar) findViewById(R.id.toolbar));
        if (getSupportActionBar() != null) {
            getSupportActionBar().setDisplayHomeAsUpEnabled(true);
            getSupportActionBar().setDisplayShowHomeEnabled(true);
        }
        getFragmentManager()
                .beginTransaction().replace(R.id.pref, new SettingsFragment()).commit();
    }


    @Override
    public void getStartPreferences() {
    }

    public static class SettingsFragment extends PreferenceFragment {


        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);
            addPreferencesFromResource(R.xml.pref_main);
            findPreference(CLEAR_LOGIN_KEY).setOnPreferenceClickListener(this::onPreferenceClick);
            findPreference(CLEAR_EMOTES_CACHE_KEY).setOnPreferenceClickListener(this::onPreferenceClick);
            findPreference(THEME_KEY).setOnPreferenceClickListener(this::onPreferenceClick);
            findPreference(TEXT_SIZE_KEY).setOnPreferenceClickListener(this::onPreferenceClick);

        }

        public boolean onPreferenceClick(Preference preference) {
            Log.d("settings", preference.getKey());
            switch (preference.getKey()) {
                case CLEAR_LOGIN_KEY:
                    try {
                        if (ServerList.getInstance() != null) ServerList.getInstance().clear();
                        Toast.makeText(getActivity(), "Cache cleared", Toast.LENGTH_SHORT).show();
                    } catch (NullPointerException e) {
                        e.printStackTrace();
                        Toast.makeText(getActivity(), "Can't clear cache now", Toast.LENGTH_SHORT).show();
                    }
                    break;
                case CLEAR_EMOTES_CACHE_KEY:
                    ImagePipeline imagePipeline = Fresco.getImagePipeline();
                    imagePipeline.clearMemoryCaches();
                    imagePipeline.clearDiskCaches();
                    imagePipeline.clearCaches();
                    break;
                case THEME_KEY:
                    ((PreferenceActivity) getActivity()).showDialog(THEME_KEY, R.string.theme, R.array.pref_theme, THEME_DARK_KEY);
                    break;
                case TEXT_SIZE_KEY:
                    ((PreferenceActivity) getActivity()).showDialog(TEXT_SIZE_KEY, R.string.text_size, R.array.text_size, "14");
                    break;
                case SIGN_OUT_KEY:
                    WebUtils.clearWebviewCookie(getActivity().getApplicationContext());
                    break;
            }
            return false;
        }


    }

    private void showDialog(String key, int titleR, int valuesR, String defaultValue) {
        new MaterialDialog.Builder(this)
                .title(titleR)
                .items(valuesR)
                .itemsCallbackSingleChoice(indexOf(getResources().getStringArray(valuesR), prefs.getString(key, defaultValue)),
                        (dialog, itemView, which, text) -> {
                            prefs.edit().putString(key, text.toString()).apply();
                            return false;
                        }).positiveText("OK")
                .negativeText("Cancel").show();
    }

    private int indexOf(String[] array, String s) {
        int i = 0;
        for (String o : array) {
            if (o.equals(s))
                return i;
            i++;
        }
        return -1;
    }



    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                onBackPressed();
                break;
        }
        return super.onOptionsItemSelected(item);
    }
}