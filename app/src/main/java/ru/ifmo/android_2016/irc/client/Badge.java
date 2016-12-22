package ru.ifmo.android_2016.irc.client;

import android.support.annotation.NonNull;
import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.Map;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.api.twitch.badges.TwitchBadges;

/**
 * Created by ghost on 11/12/2016.
 */

@SuppressWarnings("WeakerAccess")
public final class Badge {
    private final static Pattern pattern = Pattern.compile("\\w+/\\w+");

    @NonNull
    private final String badge;
    @Nullable
    private String url;

    Badge(@NonNull String badge) {
        this(badge, null);
    }

    Badge(@NonNull String badge, @Nullable String url) {
        this.badge = badge;
        if (!pattern.matcher(badge).matches()) {
            throw new ParserException("Badge can't be parsed: " + badge);
        }

        this.url = url;
    }

    public static List<Badge> parse(String badges) {
        return badges == null ? null : Stream.of(badges.split(","))
                .map(Badge::new)
                .collect(Collectors.toList());
    }

    @Override
    public String toString() {
        return badge;
    }

    public String getUrl() {
        if (url != null) return url;
        return TwitchBadges.getBadgeUrl(toString());
    }

    public Badge setUrl(@Nullable String url) {
        this.url = url;
        return this;
    }

    public int getWidth() {
        return 18;
    }

    public int getHeight() {
        return 18;
    }
}