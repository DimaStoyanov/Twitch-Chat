package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.Map;

import ru.ifmo.android_2016.irc.utils.Log;
import ru.ifmo.android_2016.irc.utils.Splitter;

import static ru.ifmo.android_2016.irc.utils.FunctionUtils.doIfNotNull;

/**
 * Created by ghost on 10/29/2016.
 */

public final class TwitchMessage extends Message {
    private final static String TAG = TwitchMessage.class.getSimpleName();

    @Nullable
    private List<Badge> badges;
    @Nullable
    private Integer color;
    @Nullable
    private String displayName;
    @Nullable
    private String id;

    private boolean mod;
    private boolean subscriber;
    private boolean turbo;
    private int roomId;
    private int userId;
    private boolean r9k;
    private boolean subsOnly;
    private int slow;
    private Notice msgId;
    private int msgParamMonths;
    @Nullable
    private String systemMsg;
    @Nullable
    private String login;
    @Nullable
    private List<Emote> emotes;
    @Nullable
    private UserType userType;
    private List<Bits> bits;
    private String broadcasterLang;
    private Integer[] emoteSets;
    private Ban ban;

    TwitchMessage() {
    }

    public static TwitchMessage fromString(String rawMessage) {
        return new TwitchMessage().parse(rawMessage);
    }

    @Override
    protected TwitchMessage parse(String rawMessage) {
        super.parse(rawMessage);
        if (optPrefix != null) {
            addToMessage(Stream.of(optPrefix.split(";"))
                    .map(s -> s.split("="))
                    .filter(l -> l.length > 1)
                    .collect(Collectors.toMap(l -> l[0], l -> l[1])));
        }
        return this;
    }

    private void addToMessage(Map<String, String> map) {
        badges = Badge.parse(map.get("badges"));
        color = parseColor(map.get("color"), null);
        displayName = map.get("display-name");

        emotes = Emote.parse(map.get("emotes"));

        id = map.get("id");
        mod = parseBool(map.get("mod"), false);
        subscriber = parseBool(map.get("subscriber"), false);
        turbo = parseBool(map.get("turbo"), false);
        roomId = parseNumber(map.get("room-id"), -1);
        userId = parseNumber(map.get("user-id"), -1);
        userType = UserType.parse(map.get("user-type"));
        bits = Bits.parse(map.get("bits"), getTrailing());

        emoteSets = parseEmoteSets(map.get("emote-sets"));

        broadcasterLang = map.get("broadcaster-lang");
        r9k = parseBool(map.get("r9k"), false);
        subsOnly = parseBool(map.get("subs-only"), false);
        slow = parseNumber(map.get("slow"), -1);

        msgId = Notice.parse(map.get("msg-id"));
        msgParamMonths = parseNumber(map.get("msg-param-months"), -1);
        systemMsg = parseMessage(map.get("system-msg"));
        login = map.get("login");

        ban = Ban.parse(parseMessage(map.get("ban-reason")), map.get("ban-duration"));
    }

    private static Integer[] parseEmoteSets(@Nullable String emoteSets) {
        if (emoteSets == null) return null;
        try {
            return Stream.of(emoteSets.split(",")).map(Integer::parseInt).toArray(Integer[]::new);
        } catch (NumberFormatException x) {
            throw new ParserException(x);
        }
    }

    private static Integer parseColor(@Nullable String color,
                                      @Nullable Integer defaultValue) {
        if (color == null) {
            return defaultValue;
        }
        try {
            return Color.parseColor(color);
        } catch (IllegalArgumentException x) {
            throw new ParserException(x);
        }
    }

    public static int parseNumber(String number, int defaultValue) {
        if (number == null) {
            return defaultValue;
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

    private static boolean parseBool(String value, boolean defaultValue) {
        if (value == null) {
            return defaultValue;
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

    @Nullable
    public String getDisplayName() {
        return displayName;
    }

    public TwitchMessage setDisplayName(@Nullable String displayName) {
        this.displayName = displayName;
        return this;
    }

    @Override
    public String getNickname() {
        return getDisplayName() == null ? super.getNickname() : getDisplayName();
    }

    public List<Emote> getEmotes() {
        return emotes;
    }

    @Nullable
    public Integer getColor() {
        return color;
    }

    public TwitchMessage setColor(int color) {
        this.color = color;
        return this;
    }

    @Nullable
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

    public int getRoomId() {
        return roomId;
    }

    @Override
    public void applyExtension(MessageExtension extension) {
        super.applyExtension(extension);
        doIfNotNull(extension.setBadges(this), badges -> this.badges = badges);
        doIfNotNull(extension.addBadges(this), badges -> {
            if (this.badges != null) {
                this.badges.addAll(badges);
            } else {
                this.badges = badges;
            }
        });
        doIfNotNull(extension.setEmotes(this), emotes -> this.emotes = emotes);
        doIfNotNull(extension.addEmotes(this), emotes -> {
            if (this.emotes != null) {
                this.emotes.addAll(emotes);
            } else {
                this.emotes = emotes;
            }
        });
    }

    public List<Bits> getBits() {
        return bits;
    }
}


