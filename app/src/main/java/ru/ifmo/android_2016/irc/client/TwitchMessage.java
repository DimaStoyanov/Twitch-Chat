package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;
import android.os.Parcel;
import android.support.annotation.NonNull;
import android.util.Log;

import java.text.ParseException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.UnknownFormatConversionException;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/29/2016.
 */

public final class TwitchMessage extends Message {
    private final static String TAG = TwitchMessage.class.getSimpleName();

    private List<Badge> badges;
    private int color;
    private String displayName;
    private String id;
    private boolean mod;
    private boolean subscriber;
    private boolean turbo;
    private String roomId;
    private String userId;
    private boolean r9k;
    private boolean subsOnly;
    private int slow;
    private String msgId;
    private int msg;
    private String systemMsg;
    private String login;
    private int banDuration;
    private String banReason;
    private List<Emote> emotes;
    private String userType;
    private int bits;
    private String broadcasterLang;
    private String emoteSets;

    private TwitchMessage() {
    }

    public static TwitchMessage fromString(String rawMessage) {
        return new TwitchMessage().parse(rawMessage);
    }

    @Override
    protected TwitchMessage parse(String rawMessage) {
        super.parse(rawMessage);
        HashMap<String, String> map = new HashMap<>();
        if (opt_prefix != null) {
            String[] params = opt_prefix.split(";");
            for (String param : params) {
                String[] p = param.split("=");
                String key = null, value = null;
                if (p.length > 0) {
                    key = p[0];
                }
                if (p.length > 1) {
                    value = p[1];
                }
                map.put(key, value);
            }
            addToMessage(map);
        }
        return this;
    }

    private void addToMessage(HashMap<String, String> map) {
        Log.i(TAG, map.values().toString());
        badges = parseBadges(map.get("badges"));
        color = parseColor(map.get("color"));
        displayName = map.get("display-name");
        emotes = parseEmotes(map.get("emotes"));
        id = map.get("id");
        mod = parseBool(map.get("mod"));
        subscriber = parseBool(map.get("subscriber"));
        turbo = parseBool(map.get("turbo"));
        roomId = map.get("room-id");
        userId = map.get("user-id");
        userType = parseUserType(map.get("user-type"));
        bits = parseNumber(map.get("bits"));

        emoteSets = map.get("emote-sets");

        broadcasterLang = map.get("broadcaster-lang");
        r9k = parseBool(map.get("r9k"));
        subsOnly = parseBool(map.get("subs-only"));
        slow = parseNumber(map.get("slow"));

        msgId = parseMsgId(map.get("msg-id"));
        msg = parseNumber(map.get("msg-param-months"));
        systemMsg = parseMessage(map.get("system-msg"));
        login = map.get("login");

        banDuration = parseNumber(map.get("ban-duration"));
        banReason = parseMessage(map.get("ban-reason"));
    }

    private int parseColor(String color) {
        if (color == null) {
            return 0;
        }
        return Color.parseColor(color);
    }

    private static int parseNumber(String number) {
        if (number == null) {
            return 0;
        }
        return Integer.parseInt(number);
    }

    private String parseUserType(String type) {
        //TODO:
        return type;
    }

    private List<Emote> parseEmotes(String emotes) {
        if (emotes == null) {
            return null;
        }
        String[] emote = emotes.split("/");
        List<Emote> result = new ArrayList<>(4);

        for (String e : emote) {
            Matcher matcher = Emote.pattern.matcher(e);
            if(matcher.matches()) {
                for (int i = 2; i < matcher.groupCount(); i += 2) {
                    result.add(new Emote(
                            matcher.group(1),
                            parseNumber(matcher.group(i)),
                            parseNumber(matcher.group(i + 1))));
                }
                Collections.sort(result);
            } else {
                throw null;
            }
        }
        return result;
    }

    private List<Badge> parseBadges(String badges) {
        if (badges == null) {
            return null;
        }
        String[] badge = badges.split(",");
        List<Badge> result = new ArrayList<>(badge.length);
        for (int i = 0; i < badge.length; i++) {
            result.add(i, new Badge(badge[i]));
        }
        return result;
    }

    private String parseMsgId(String msgId) {
        //TODO:
        return null;
    }

    private String parseMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("\\s", " ");
    }

    private boolean parseBool(String value) {
        if (value == null) {
            return false;
        }
        switch (value) {
            case "false":
            case "0":
                return false;
            case "true":
            case "1":
                return true;
            default:
                throw new UnknownFormatConversionException(value);
        }
    }

    private static class Badge {
        private final int value;
        private final String name;

        public Badge(String badge) {
            String[] p = badge.split("/");
            if (p.length >= 2) {
                name = p[0];
                value = parseNumber(p[1]);
            } else {
                name = null;
                value = 0;
            }
        }

        public int getValue() {
            return value;
        }

        public String getName() {
            return name;
        }
    }

    private static class Emote implements Comparable<Emote> {
        private static Pattern pattern = Pattern.compile("([\\w\\\\()-]+):(?:(\\d+)-(\\d+),)*(?:(\\d+)-(\\d+))");
        private final String emoteName;
        private final int begin;
        private final int end;

        public Emote(String emoteName, int begin, int end) {
            this.emoteName = emoteName;
            this.begin = begin;
            this.end = end;
        }

        @Override
        public int compareTo(@NonNull Emote o) {
            return this.begin - o.begin;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        super.writeToParcel(dest, flags);
        dest.writeList(this.badges);
        dest.writeInt(this.color);
        dest.writeString(this.displayName);
        dest.writeString(this.id);
        dest.writeByte(this.mod ? (byte) 1 : (byte) 0);
        dest.writeByte(this.subscriber ? (byte) 1 : (byte) 0);
        dest.writeByte(this.turbo ? (byte) 1 : (byte) 0);
        dest.writeString(this.roomId);
        dest.writeString(this.userId);
        dest.writeByte(this.r9k ? (byte) 1 : (byte) 0);
        dest.writeByte(this.subsOnly ? (byte) 1 : (byte) 0);
        dest.writeInt(this.slow);
        dest.writeString(this.msgId);
        dest.writeInt(this.msg);
        dest.writeString(this.systemMsg);
        dest.writeString(this.login);
        dest.writeInt(this.banDuration);
        dest.writeString(this.banReason);
        dest.writeList(this.emotes);
        dest.writeString(this.userType);
        dest.writeInt(this.bits);
        dest.writeString(this.broadcasterLang);
        dest.writeString(this.emoteSets);
    }

    protected TwitchMessage(Parcel in) {
        super(in);
        this.badges = new ArrayList<Badge>();
        in.readList(this.badges, Badge.class.getClassLoader());
        this.color = in.readInt();
        this.displayName = in.readString();
        this.id = in.readString();
        this.mod = in.readByte() != 0;
        this.subscriber = in.readByte() != 0;
        this.turbo = in.readByte() != 0;
        this.roomId = in.readString();
        this.userId = in.readString();
        this.r9k = in.readByte() != 0;
        this.subsOnly = in.readByte() != 0;
        this.slow = in.readInt();
        this.msgId = in.readString();
        this.msg = in.readInt();
        this.systemMsg = in.readString();
        this.login = in.readString();
        this.banDuration = in.readInt();
        this.banReason = in.readString();
        this.emotes = new ArrayList<Emote>();
        in.readList(this.emotes, Emote.class.getClassLoader());
        this.userType = in.readString();
        this.bits = in.readInt();
        this.broadcasterLang = in.readString();
        this.emoteSets = in.readString();
    }

    public static final Creator<TwitchMessage> CREATOR = new Creator<TwitchMessage>() {
        @Override
        public TwitchMessage createFromParcel(Parcel source) {
            return new TwitchMessage(source);
        }

        @Override
        public TwitchMessage[] newArray(int size) {
            return new TwitchMessage[size];
        }
    };
}
