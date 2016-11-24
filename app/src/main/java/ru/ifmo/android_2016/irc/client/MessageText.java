package ru.ifmo.android_2016.irc.client;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Function;

import java.util.ArrayList;
import java.util.List;

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

    MessageText(CharSequence text) {
        this(text, null, null, false, false, false);
    }

    private MessageText(CharSequence message,
                        String sender,
                        String text,
                        boolean colored,
                        boolean mentioned,
                        boolean twitchNotify) {
        this.spanned = message;
        this.sender = sender;
        this.text = text;
        this.colored = colored;
        this.mentioned = mentioned;
        this.twitchNotify = twitchNotify;
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

    static class Builder {
        TwitchMessage msg;
        Function<Message, CharSequence> function;
        List<String> mentionList = new ArrayList<>();

        Builder setMessage(TwitchMessage msg) {
            this.msg = msg;
            return this;
        }

        Builder setFunction(Function<Message, CharSequence> function) {
            this.function = function;
            return this;
        }

        Builder setMentionList(String... mentionList) {
            this.mentionList.clear();
            return addMentionList(mentionList);
        }

        Builder addMentionList(String... mentionList) {
            this.mentionList.addAll(Stream.of(mentionList).map(String::toLowerCase)
                    .collect(Collectors.toList()));
            return this;
        }

        MessageText build() {
            CharSequence spanned = function.apply(msg);
            String sender = msg.getNickname();
            String text = msg.getPrivmsgText();
            boolean colored = msg.getAction();
            boolean twitchNotify = false;
            if (sender != null) {
                twitchNotify = sender.toLowerCase().equals("twitchnotify");
            }
            boolean mentioned = Stream.of(msg.getSplitText())
                    .map(r -> r.word.toLowerCase())
                    .filter(w -> mentionList.contains(w))
                    .count() > 0;

            return new MessageText(
                    spanned,
                    sender,
                    text,
                    colored,
                    mentioned,
                    twitchNotify
            );
        }
    }
}
