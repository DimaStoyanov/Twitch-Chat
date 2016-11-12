package ru.ifmo.android_2016.irc.utils;

/**
 * Created by ghost on 11/12/2016.
 */

public interface Function<P, R> {
    R apply(P param);
}
