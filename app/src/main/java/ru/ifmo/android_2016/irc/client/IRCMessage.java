package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost on 10/23/2016.
 */

public class IRCMessage {
    private static final String TAG = IRCMessage.class.getSimpleName();

    public final long time;

    @Nullable
    protected String tags;
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

    public IRCMessage() {
        time = System.currentTimeMillis();
    }

    public static IRCMessage fromString(String rawMessage) {
        return new IRCMessage().parse(rawMessage);
    }

    protected IRCMessage parse(String rawMessage) {
        Matcher matcher = MessagePattern.matcher(rawMessage);
        if (matcher.matches()) {
            tags = MessagePattern.group(matcher, "tags");
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
        }
        return null;
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

    public IRCMessage setPrivmsg(String name, String message) {
        return IRCMessage.this
                .setCommand("PRIVMSG")
                .setParams(Collections.singletonList(name))
                .setTrailing(message);
    }

    @Nullable
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

        private static boolean parse(IRCMessage message, String s) {
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
                "(?:@([^ ]+) )?" +  //tags
                        "(?::([^ ]+) )?" +  //prefix
                        "([\\w]+)" + //command
                        "(?: ((?:[^: ][^ ]*)(?: (?:[^: ][^ ]*))*))?" + //params
                        "(?:(?: :(.*))?)?"); //trailing

        static {
            map.put("tags", 1);
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

    public boolean isAction() {
        return action;
    }

    public IRCMessage setTags(String tags) {
        this.tags = tags;
        return this;
    }

    public IRCMessage setCommand(String command) {
        this.command = command;
        return this;
    }

    public IRCMessage setParams(List<String> params) {
        this.params = params;
        return this;
    }

    public IRCMessage setTrailing(String trailing) {
        this.trailing = trailing;
        return this;
    }

    public IRCMessage setServerName(String serverName) {
        this.serverName = serverName;
        return this;
    }

    public IRCMessage setNickname(String nickname) {
        this.nickname = nickname;
        return this;
    }

    public IRCMessage setUsername(String username) {
        this.username = username;
        return this;
    }

    public IRCMessage setHostname(String hostname) {
        this.hostname = hostname;
        return this;
    }

    public IRCMessage setAction(boolean action) {
        this.action = action;
        return this;
    }

    @Nullable
    public String getServerName() {
        return serverName;
    }

    @Nullable
    public String getUsername() {
        return username;
    }

    @Nullable
    public String getHostname() {
        return hostname;
    }

    public final boolean isPrivmsg() {
        return getCommand().equals("PRIVMSG");
    }

    public final boolean isJoin() {
        return getCommand().equals("JOIN");
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (tags != null) {
            sb.append('@').append(tags).append(' ');
        }
        if (serverName != null) {
            sb.append(':').append(serverName).append(' ');
        } else if (nickname != null) {
            sb.append(':').append(nickname);
            if (hostname != null) {
                if (username != null) {
                    sb.append('!').append(username);
                }
                sb.append('@').append(hostname);
            }
            sb.append(' ');
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

    public Iterable<Splitter.Result> getSplitText() {
        if (trailing != null) {
            return Splitter.iteratorSplit(trailing);
        }
        return Splitter::getEmptyIterator;
    }

    public IRCMessage applyExtension(MessageExtension extension) {
        //nothing
        return this;
    }
}
