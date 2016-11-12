package ru.ifmo.android_2016.irc.client;

import ru.ifmo.android_2016.irc.utils.Function;
import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/12/2016.
 */

public class TwitchPostExecute implements Function<Message, CharSequence> {
    @Override
    public CharSequence apply(Message message) {
        return TextUtils.buildTextDraweeView((TwitchMessage) message);
    }
}
