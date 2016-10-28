package ru.ifmo.android_2016.irc.exception;

/**
 * Kind of bad, incorrect or unexpected response from API.
 */
public class BadResponseException extends Exception {

    public BadResponseException(String message) {
        super(message);
    }

    public BadResponseException(String message, Throwable cause) {
        super(message, cause);
    }

    public BadResponseException(Throwable cause) {
        super(cause);
    }
}