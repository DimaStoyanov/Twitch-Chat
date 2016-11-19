package ru.ifmo.android_2016.irc.client;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;

/**
 * Created by ghost on 11/12/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Badge {
    private final String version;
    private final Type name;

    Badge(String badge) {
        String[] p = badge.split("/");
        if (p.length == 2) {
            name = Type.parse(p[0]);
            version = p[1];
        } else {
            throw new ParserException("Badge can't be parsed: " + badge);
        }
    }

    @SuppressWarnings("unused")
    public String getVersion() {
        return version;
    }

    public Type getName() {
        return name;
    }

    public static List<Badge> parseBadges(String badges) {
        return badges == null ? null : Stream.of(badges.split(","))
                .map(Badge::new)
                .collect(Collectors.toList());
    }

    @SuppressWarnings("unused")
    public enum Type {
        UNKNOWN,

        ADMIN,
        BITS,
        BROADCASTER,
        GLOBAL_MOD,
        MODERATOR,
        PREMIUM,
        STAFF,
        SUBSCRIBER,
        TURBO,
        WARCRAFT,

        BTTV_BOT,;

        static Type parse(String type) {
            try {
                return Enum.valueOf(Type.class, type.toUpperCase());
            } catch (IllegalArgumentException x) {
                return UNKNOWN;
            }
        }
    }
}