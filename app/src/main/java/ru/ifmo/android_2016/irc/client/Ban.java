package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost3432 on 14.11.16.
 */

public class Ban {
    private String reason;
    private int duration;   //if duration == 0 -> permanent ban

    private Ban(String reason, int duration) {
        this.reason = reason;
        this.duration = duration;
    }

    public static Ban parse(String reason, String duration) {
        int time;
        try {
            time = duration == null ? 0 : Integer.parseInt(duration);
        } catch (NumberFormatException x) {
            throw new ParserException(x);
        }
        return new Ban(reason, time);
    }

    //TODO: локализацию запилить чтоль? но нужен Context
    public String toString(String nick) {
        String result;
        if (duration > 0) {
            result = nick + " has been timed out for " + duration + " seconds.";
        } else {
            result = nick + " has been banned from this room.";
        }
        return result + (reason != null ? (" Reason: " + reason) : "");
    }
}
