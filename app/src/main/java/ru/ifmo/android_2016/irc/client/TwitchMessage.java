package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.util.Log;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.Map;

import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost on 10/29/2016.
 */

public final class TwitchMessage extends Message {
    private final static String TAG = TwitchMessage.class.getSimpleName();

    @Nullable private List<Badge> badges;
    private int color;
    @Nullable private String displayName;
    @Nullable private String id;

    private boolean mod;
    private boolean subscriber;
    private boolean turbo;
    private String roomId;
    private String userId;
    private boolean r9k;
    private boolean subsOnly;
    private int slow;
    private Notice msgId;
    private int msgParamMonths;
    private String systemMsg;
    private String login;
    private List<Emote> emotes;
    private UserType userType;
    private List<Bits> bits;
    private String broadcasterLang;
    private Integer[] emoteSets;
    private Ban ban;
    private List<Splitter.Result> splitResult;

    TwitchMessage() {
    }

    public static TwitchMessage fromString(String rawMessage) {
        return new TwitchMessage().parse(rawMessage);
    }

    @Override
    protected TwitchMessage parse(String rawMessage) {
        super.parse(rawMessage);
        if (getTrailing() != null) {
            splitResult = Splitter.splitWithSpace(getTrailing());
        }
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

        emotes = Emote.parse(map.get("emotes"), splitResult, getPrivmsgTarget());

        id = map.get("id");
        mod = parseBool(map.get("mod"));
        subscriber = parseBool(map.get("subscriber"));
        turbo = parseBool(map.get("turbo"));
        roomId = map.get("room-id");
        userId = map.get("user-id");
        userType = UserType.parse(map.get("user-type"));
        bits = Bits.parse(map.get("bits"), splitResult);

        emoteSets = parseEmoteSets(map.get("emote-sets"));

        broadcasterLang = map.get("broadcaster-lang");
        r9k = parseBool(map.get("r9k"));
        subsOnly = parseBool(map.get("subs-only"));
        slow = parseNumber(map.get("slow"));

        msgId = Notice.parse(map.get("msg-id"));
        msgParamMonths = parseNumber(map.get("msg-param-months"));
        systemMsg = parseMessage(map.get("system-msg"));
        login = map.get("login");

        ban = Ban.parse(parseMessage(map.get("ban-reason")), map.get("ban-duration"));
    }

    private static Integer[] parseEmoteSets(String emoteSets) {
        if (emoteSets == null) return null;
        try {
            return Stream.of(emoteSets.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
        } catch (NumberFormatException x) {
            throw new ParserException(x);
        }
    }

    private static int parseColor(String color) {
        if (color == null) {
            return 0;
        }
        try {
            return Color.parseColor(color);
        } catch (IllegalArgumentException x) {
            throw new ParserException(x);
        }
    }

    public static int parseNumber(String number) {
        if (number == null) {
            return 0;
        }
        try {
            return Integer.parseInt(number);
        } catch (NumberFormatException x) {
            throw new ParserException(x);
        }
    }

    private static String parseMessage(String message) {
        if (message == null) return null;
        String result = message.replaceAll("\\\\s", " ");
        Log.d(TAG, "SystemMessage/BanReason: " + result);
        return result;
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
                throw new ParserException("can't parse bool " + value);
        }
    }

    public String getDisplayName() {
        return displayName;
    }

    @Override
    public String getNickname() {
        return getDisplayName() == null ? super.getNickname() : getDisplayName();
    }

    public List<Emote> getEmotes() {
        return emotes;
    }

    public int getColor() {
        return color;
    }

    public TwitchMessage setDisplayName(String displayName) {
        this.displayName = displayName;
        return this;
    }

    public TwitchMessage setColor(int color) {
        this.color = color;
        return this;
    }

    public List<Badge> getBadges() {
        return badges;
    }

    public Ban getBan() {
        return ban;
    }

    public TwitchMessage setEmotes(List<Emote> emotes) {
        this.emotes = emotes;
        return this;
    }

    public Integer[] getEmoteSets() {
        return emoteSets;
    }
}


