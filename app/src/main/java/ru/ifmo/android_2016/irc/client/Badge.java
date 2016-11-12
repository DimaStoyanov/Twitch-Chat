package ru.ifmo.android_2016.irc.client;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ghost on 11/12/2016.
 */

public class Badge {
    private final int value;
    private final String name;

    Badge(String badge) {
        String[] p = badge.split("/");
        if (p.length >= 2) {
            name = p[0];
            value = TwitchMessage.parseNumber(p[1]);
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

    public static List<Badge> parseBadges(String badges) {
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
}