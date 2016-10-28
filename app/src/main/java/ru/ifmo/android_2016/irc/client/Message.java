package ru.ifmo.android_2016.irc.client;

import java.io.Serializable;
import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/23/2016.
 */

public class Message implements Serializable {
    public static Pattern pattern = Pattern.compile(":([\\w]+)![\\w@.]+ [\\w]+ (#?[\\w]+) :(.*)");
    public String from, to, text;
    public Date date;

    public Message(String string) {
        Matcher matcher = Message.pattern.matcher(string);
        if (matcher.find()) {
            from = matcher.group(1);
            to = matcher.group(2);
            text = matcher.group(3);
        }
    }

    public Message(String from, String to, String text) {
        this.from = from;
        this.to = to;
        this.text = text;
    }
}
