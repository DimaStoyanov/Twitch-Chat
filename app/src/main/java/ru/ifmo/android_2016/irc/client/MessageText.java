package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.text.SpannableString;

import com.annimon.stream.function.Consumer;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.utils.TextUtils;

/**
 * Created by ghost on 11/24/2016.
 */

public final class MessageText {
    private final CharSequence spanned;

    private final String sender;
    private final String text;

    private final boolean colored;
    private final boolean mentioned;
    private final boolean twitchNotify;
    private final boolean whisper;

    MessageText(CharSequence text) {
        this(text, null, null, false, false, false, false);
    }

    private MessageText(CharSequence message,
                        String sender,
                        String text,
                        boolean colored,
                        boolean mentioned,
                        boolean twitchNotify, boolean whisper) {
        this.spanned = message;
        this.sender = sender;
        this.text = text;
        this.colored = colored;
        this.mentioned = mentioned;
        this.twitchNotify = twitchNotify;
        this.whisper = whisper;
    }

    public CharSequence getText() {
        return text;
    }

    public CharSequence getSpanned() {
        return spanned;
    }

    public String getSender() {
        return sender;
    }

    public boolean isColored() {
        return colored;
    }

    public boolean isMentioned() {
        return mentioned;
    }

    public boolean isTwitchNotify() {
        return twitchNotify;
    }

    public boolean isWhisper() {
        return whisper;
    }

    final static class Builder {
        @NonNull
        private IRCMessage msg;
        @Nullable
        private TextUtils.TextFunction function;
        @NonNull
        private List<MessageExtension> extensionList = new ArrayList<>();
        private Consumer<CharSequence> notificationListener;
        private boolean whisper;
        private String nick;

        Builder(@NonNull IRCMessage msg) {
            this.msg = msg;
        }

        Builder setFunction(@Nullable TextUtils.TextFunction function) {
            this.function = function;
            return this;
        }

        Builder addExtensions(MessageExtension... extensions) {
            Collections.addAll(this.extensionList, extensions);
            return this;
        }

        Builder setNotificationListener(Consumer<CharSequence> listener) {
            this.notificationListener = listener;
            return this;
        }

        MessageText build() {
            applyExtensions();

            String sender = msg.getNickname();
            String text = msg.getPrivmsgText();

            CharSequence spanned;
            if (function != null) {
                spanned = function.apply((TwitchMessage) msg);
            } else {
                spanned = new SpannableString(text);
            }
            boolean colored = msg.isAction();
            boolean twitchNotify = false;
            boolean whisper = this.whisper;

            if (sender != null) {
                twitchNotify = sender.toLowerCase().equals("twitchnotify");
            }

            boolean mentioned = false;
            if (text != null) {
                Pattern highlightPattern = MessagePatterns.getInstance().getHighlightRegex();
                mentioned = highlightPattern != null && highlightPattern
                        .matcher(text)
                        .find();

                if (nick != null) {
                    mentioned = text.toLowerCase().contains(nick.toLowerCase()) || mentioned;
                }
            }

            if (mentioned) {
                if (notificationListener != null) {
                    notificationListener.accept(TextUtils.buildNotificationText(msg));
                }
            }

            return new MessageText(
                    spanned,
                    sender,
                    text,
                    colored,
                    mentioned,
                    twitchNotify,
                    whisper);
        }

        private void applyExtensions() {
            for (MessageExtension extension : extensionList) {
                msg.applyExtension(extension);
            }
        }

        public Builder setWhisper(boolean whisper) {
            this.whisper = whisper;
            return this;
        }

        public Builder setNick(String nick) {
            this.nick = nick;
            return this;
        }
    }
}
