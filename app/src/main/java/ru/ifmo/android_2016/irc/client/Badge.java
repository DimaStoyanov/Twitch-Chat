package ru.ifmo.android_2016.irc.client;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;

/**
 * Created by ghost on 11/12/2016.
 */

@SuppressWarnings("WeakerAccess")
public class Badge {
    private final int value;
    private final String name;

    Badge(String badge) {
        String[] p = badge.split("/");
        if (p.length >= 2) {
            name = p[0];
            value = TwitchMessage.parseNumber(p[1]);
        } else {
            throw null; //TODO: Something happened
        }
    }

    @SuppressWarnings("unused")
    public int getValue() {
        return value;
    }

    public String getName() {
        return name;
    }

    public static List<Badge> parseBadges(String badges) {
        return badges == null ? null : Stream.of(badges.split(","))
                .map(Badge::new)
                .collect(Collectors.toList());
    }
}