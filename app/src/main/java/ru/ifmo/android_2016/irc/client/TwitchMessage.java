package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;
import android.os.Parcel;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.UnknownFormatConversionException;

/**
 * Created by ghost on 10/29/2016.
 */

public final class TwitchMessage extends Message {
    private final static String TAG = TwitchMessage.class.getSimpleName();

    private List<Badge> badges;
    private int color;
    private String displayName;
    private String id;
    private List<Badge> bagdes;

    @SuppressWarnings({"unused", "deprecation"})
    @Deprecated
    public TwitchMessage(String from, String to, String text) {
        super(from, to, text);
    }

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
        if (optPrefix != null) {
            addToMessage(Stream.of(optPrefix)
                    .flatMap(s -> Stream.of(s.split(";")))
                    .map(s -> s.split("="))
                    .filter(l -> l.length > 1)
                    .collect(Collectors.toMap(l -> l[0], l -> l[1])));
        }
        return this;
    }

    private void addToMessage(Map<String, String> map) {
        badges = Badge.parseBadges(map.get("badges"));
        color = parseColor(map.get("color"));
        displayName = map.get("display-name");

        emotes = Emote.parseEmotes(map.get("emotes"), trailing, params);

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

    private static int parseColor(String color) {
        if (color == null) {
            return 0;
        }
        return Color.parseColor(color);
    }

    public static int parseNumber(String number) {
        if (number == null) {
            return 0;
        }
        return Integer.parseInt(number);
    }

    private static String parseUserType(String type) {
        //TODO:
        return type;
    }

    private static String parseMsgId(String msgId) {
        //TODO:
        return msgId;
    }

    private static String parseMessage(String message) {
        if (message == null) {
            return null;
        }
        return message.replaceAll("\\s", " ");
    }

    private static boolean parseBool(String value) {
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

    public String getDisplayName() {
        return displayName;
    }

    public String getNickname() {
        return getDisplayName() == null ? super.getNickName() : getDisplayName();
    }

    public List<Emote> getEmotes() {
        return emotes;
    }

    public int getColor() {
        return color;
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
        this.badges = new ArrayList<>();
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
        this.emotes = new ArrayList<>();
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

    public TwitchMessage setDisplayName(String displayName) {
        this.displayName = displayName;
        this.nickName = displayName;
        return this;
    }

    public TwitchMessage setColor(int color) {
        this.color = color;
        return this;
    }

    public List<Badge> getBagdes() {
        return bagdes;
    }
}


