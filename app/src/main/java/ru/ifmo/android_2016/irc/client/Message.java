package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost on 10/23/2016.
 */

public class Message {
    private static final String TAG = Message.class.getSimpleName();

    public final long time;

    @Nullable
    String optPrefix;
    @NonNull
    private String command = "@NOT_SET";
    @NonNull
    private List<String> params = Collections.emptyList();
    @Nullable
    private String trailing;

    @Nullable
    private String serverName;
    @Nullable
    private String nickname;
    @Nullable
    private String username;
    @Nullable
    private String hostname;

    private static final Pattern actionPattern = Pattern.compile("\1ACTION ([^\1]+)\1");
    private boolean action = false;
    private List<Splitter.Result> splitText = null;

    public Message() {
        time = System.currentTimeMillis();
    }

    public static Message fromString(String rawMessage) {
        return new Message().parse(rawMessage);
    }

    protected Message parse(String rawMessage) {
        Matcher matcher = MessagePattern.matcher(rawMessage);
        if (matcher.matches()) {
            optPrefix = MessagePattern.group(matcher, "opt-prefix");
            Prefix.parse(this, MessagePattern.group(matcher, "prefix"));
            command = MessagePattern.group(matcher, "command");
            String param = MessagePattern.group(matcher, "params");

            if (param != null) {
                params = Arrays.asList(param.split(" "));
            }

            trailing = MessagePattern.group(matcher, "trailing");
        }
        trailing = parseTrailing(trailing);
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

    public String getTrailing() {
        return trailing;
    }

    public List<String> getParams() {
        return params;
    }

    @NonNull
    public String getCommand() {
        return command;
    }

    public Message setPrivmsg(String name, String message) {
        return Message.this
                .setCommand("PRIVMSG")
                .setParams(Collections.singletonList(name))
                .setTrailing(message);
    }

    public String getJoinChannel() {
        if (params.size() > 0) return params.get(0);
        return null;
    }

    public void setPrivmsgText(String privmsgText) {
        this.trailing = privmsgText;
    }

    public long getTime() {
        return time;
    }

    private static class Prefix {
        private static final Pattern pattern =
                Pattern.compile("([\\w.-]+)|(?:([\\w_]+)(?:(?:!([\\w]+))?@([\\w.-]+))?)");

        private static boolean parse(Message message, String s) {
            if (s != null) {
                Matcher matcher = pattern.matcher(s);
                if (matcher.matches()) {
                    message.serverName = matcher.group(1);
                    message.nickname = matcher.group(2);
                    message.username = matcher.group(3);
                    message.hostname = matcher.group(4);
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

    @Nullable
    public String getNickname() {
        return nickname;
    }

    public boolean getAction() {
        return action;
    }

    public Message setOptPrefix(String optPrefix) {
        this.optPrefix = optPrefix;
        return this;
    }

    public Message setCommand(String command) {
        this.command = command;
        return this;
    }

    public Message setParams(List<String> params) {
        this.params = params;
        return this;
    }

    public Message setTrailing(String trailing) {
        this.trailing = trailing;
        return this;
    }

    public Message setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public Message setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public Message setUsername(String username) {
        this.username = username;
        return this;
    }

    public Message setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public Message setAction(boolean action) {
        this.action = action;
        return this;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (optPrefix != null) {
            sb.append(optPrefix).append(' ');
        }
        sb.append(command).append(' ');
        for (String param : params) {
            sb.append(param).append(' ');
        }
        if (trailing != null) {
            sb.append(':').append(trailing);
        }
        return sb.toString();
    }

    public String getPrivmsgTarget() {
        if (params.size() > 0) return params.get(0);
        return null;
    }

    public String getPrivmsgText() {
        return trailing;
    }

    public List<Splitter.Result> getSplitText() {
        return splitText != null ? splitText : (splitText = Splitter.splitWithSpace(trailing));
    }
}
