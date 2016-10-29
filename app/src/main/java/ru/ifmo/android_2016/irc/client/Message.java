package ru.ifmo.android_2016.irc.client;

import android.util.Log;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/23/2016.
 */

public class Message implements Serializable {
    private static final String TAG = Message.class.getSimpleName();

    public static final Pattern pattern =
            Pattern.compile(":([\\w]+)![\\w@.]+ [\\w]+ (#?[\\w]+) :(.*)");
    public String from, to, text;
    public Date date;
    String opt_prefix;
    String command;
    String params;
    String param;
    String serverName;
    String nickName;
    String userName;
    String hostName;

    public Message(String string) {
        Matcher matcher = Message.pattern.matcher(string);
        if (matcher.find()) {
            from = matcher.group(1);
            to = matcher.group(2);
            text = matcher.group(3);
        }
    }

    protected Message() {
    }

    public static Message fromString(String rawMessage) {
        return new Message().parse(rawMessage);
    }

    protected Message parse(String rawMessage) {
        Matcher matcher = MessagePattern.matcher(rawMessage);
        if (matcher.matches()) {
            opt_prefix = MessagePattern.group(matcher, "opt-prefix");
            Prefix.parse(this, MessagePattern.group(matcher, "prefix"));
            command = MessagePattern.group(matcher, "command");
            params = MessagePattern.group(matcher, "params");
            param = MessagePattern.group(matcher, "param");
        }
        from = nickName;
        to = params;
        text = param;
        //Log.i(TAG, serverName + " " + nickName + " " + userName + " " + hostName);
        return this;
    }

    public Message(String from, String to, String text) {
        this.from = from;
        this.to = to;
        this.text = text;
    }

    private static class Prefix {
        private static final Pattern pattern =
                Pattern.compile("([\\w.-]+)|(?:([\\w_]+)(?:(?:!([\\w]+))?@([\\w.-]+))?)");

        private static boolean parse(Message message, String s) {
            Matcher matcher = pattern.matcher(s);
            if (matcher.matches()) {
                message.serverName = matcher.group(1);
                message.nickName = matcher.group(2);
                message.userName = matcher.group(3);
                message.hostName = matcher.group(4);
                return true;
            }
            return false;
        }
    }

    private static class MessagePattern {
        private final static HashMap<String, Integer> map = new HashMap<>();
        private final static Pattern message = Pattern.compile(
                "(?:@([^ ]+) )?" +  //opt-prefix
                        "(?::([^ ]+) )?" +  //prefix
                        "([\\w]+)" + //command
                        "(?:(?: ([^: ][^ ]*))*(?: :(.*))?)?"); //params / param

        static {
            map.put("opt-prefix", 1);
            map.put("prefix", 2);
            map.put("command", 3);
            map.put("params", 4);
            map.put("param", 5);
        }

        public static String group(Matcher matcher, String group) {
            return matcher.group(map.get(group));
        }

        public static Matcher matcher(String s) {
            return message.matcher(s);
        }
    }
}
