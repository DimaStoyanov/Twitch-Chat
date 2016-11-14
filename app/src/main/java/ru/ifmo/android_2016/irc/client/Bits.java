package ru.ifmo.android_2016.irc.client;

import android.util.Log;

import java.util.Collections;
import java.util.List;

/**
 * Created by ghost3432 on 14.11.16.
 */

public class Bits {
    private static final String TAG = Bits.class.getSimpleName();
    private final int bits;
    private final String BITS_URL_TEMPLATE =
            "https://static-cdn.jtvnw.net/bits/{{theme}}/{{type}}/{{color}}/{{size}}";

    public Bits(String bits) {
        try {
            this.bits = Integer.parseInt(bits);
        } catch (NumberFormatException x) {
            Log.d(TAG, bits);
            throw null; //TODO:
        }
    }

    public static List<Bits> parse(String bits, String message) {
        if (bits == null) return null;
        //TODO:
        return Collections.singletonList(new Bits(bits));
    }

    public String getBitsUrl() {
        return BITS_URL_TEMPLATE
                .replace("{{theme}}", "DARK")
                .replace("{{type}}", "static")
                .replace("{{color}}", getColor())
                .replace("{{size}}", "3");
    }

    private String getColor() {
        if (bits >= 10000) {
            return "red";
        } else if (bits >= 5000) {
            return "blue";
        } else if (bits >= 1000) {
            return "green";
        } else if (bits >= 100) {
            return "purple";
        } else {
            return "gray";
        }
    }
}
