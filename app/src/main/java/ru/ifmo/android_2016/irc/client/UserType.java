package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost3432 on 14.11.16.
 */

@SuppressWarnings("unused")
public enum UserType {
    UNKNOWN,
    EMPTY,

    MOD,
    GLOBAL_MOD,
    ADMIN,
    STAFF;

    public static UserType parse(String userType) {
        if (userType == null) return EMPTY;
        try {
            return Enum.valueOf(UserType.class, userType.toUpperCase());
        } catch (IllegalArgumentException x) {
            return UNKNOWN;
        }
    }
}
