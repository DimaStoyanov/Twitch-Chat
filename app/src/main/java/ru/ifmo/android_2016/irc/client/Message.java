package ru.ifmo.android_2016.irc.client;

import android.os.Parcel;
import android.os.Parcelable;

import java.util.Date;
import java.util.HashMap;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/23/2016.
 */

public class Message implements Parcelable {
    private static final String TAG = Message.class.getSimpleName();

    public String to, text;
    public Date date;
    String opt_prefix;
    String command;
    String params;
    String trailing;
    String serverName;
    String nickName;
    String userName;
    String hostName;
    private static final Pattern actionPattern = Pattern.compile("\1ACTION ([^\1]+)\1");
    boolean action = false;

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
            trailing = MessagePattern.group(matcher, "trailing");
        }
        to = params;
        text = parseTrailing(trailing);
        return this;
    }

    private String parseTrailing(String trailing) {
        if (trailing != null) {
            String text;
            Matcher matcher1 = actionPattern.matcher(trailing);
            if (matcher1.matches()) {
                text = matcher1.group(1);
                action = true;
            } else {
                text = trailing;
            }
            return text;
        } else {
            return null;
        }
    }

    public Message(String from, String to, String text) {
        this.nickName = from;
        this.to = to;
        this.text = text;
    }

    private static class Prefix {
        private static final Pattern pattern =
                Pattern.compile("([\\w.-]+)|(?:([\\w_]+)(?:(?:!([\\w]+))?@([\\w.-]+))?)");

        private static boolean parse(Message message, String s) {
            if (s != null) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    message.serverName = matcher.group(1);
                    message.nickName = matcher.group(2);
                    message.userName = matcher.group(3);
                    message.hostName = matcher.group(4);
                    return true;
                }
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
                        "(?:(?: ([^: ][^ ]*))*(?: :(.*))?)?"); //params / trailing

        static {
            map.put("opt-prefix", 1);
            map.put("prefix", 2);
            map.put("command", 3);
            map.put("params", 4);
            map.put("trailing", 5);
        }

        public static String group(Matcher matcher, String group) {
            return matcher.group(map.get(group));
        }

        public static Matcher matcher(String s) {
            return message.matcher(s);
        }
    }

    public String getNickName() {
        return nickName;
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.to);
        dest.writeString(this.text);
        dest.writeLong(this.date != null ? this.date.getTime() : -1);
        dest.writeString(this.opt_prefix);
        dest.writeString(this.command);
        dest.writeString(this.params);
        dest.writeString(this.trailing);
        dest.writeString(this.serverName);
        dest.writeString(this.nickName);
        dest.writeString(this.userName);
        dest.writeString(this.hostName);
        dest.writeByte(this.action ? (byte) 1 : (byte) 0);
    }

    protected Message(Parcel in) {
        this.to = in.readString();
        this.text = in.readString();
        long tmpDate = in.readLong();
        this.date = tmpDate == -1 ? null : new Date(tmpDate);
        this.opt_prefix = in.readString();
        this.command = in.readString();
        this.params = in.readString();
        this.trailing = in.readString();
        this.serverName = in.readString();
        this.nickName = in.readString();
        this.userName = in.readString();
        this.hostName = in.readString();
        this.action = in.readByte() != 0;
    }

    public static final Creator<Message> CREATOR = new Creator<Message>() {
        @Override
        public Message createFromParcel(Parcel source) {
            return new Message(source);
        }

        @Override
        public Message[] newArray(int size) {
            return new Message[size];
        }
    };

    public boolean getAction() {
        return action;
    }
}
