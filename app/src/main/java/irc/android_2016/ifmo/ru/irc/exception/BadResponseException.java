package irc.android_2016.ifmo.ru.irc.exception;

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