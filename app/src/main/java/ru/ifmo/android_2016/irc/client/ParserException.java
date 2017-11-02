package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost on 11/19/2016.
 */

final class ParserException extends RuntimeException {
    ParserException(Throwable cause) {
        super(cause);
    }

    ParserException(String message) {
        super(message);
    }
}
