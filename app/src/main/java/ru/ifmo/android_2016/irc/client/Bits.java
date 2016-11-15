package ru.ifmo.android_2016.irc.client;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.List;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost3432 on 14.11.16.
 */

public class Bits {
    private static final Pattern pattern = Pattern.compile("cheer\\d+");
    private static final String TAG = Bits.class.getSimpleName();
    private final int bits;
    private final String BITS_URL_TEMPLATE =
            "https://static-cdn.jtvnw.net/bits/{{theme}}/{{type}}/{{color}}/{{size}}";
    private final int begin_text;
    private final int end_text;
    private final int begin_image;
    private final int end_image;

    public Bits(String bits, int begin, int end) {
        bits = bits.replace("cheer", "");
        this.bits = Integer.parseInt(bits);
        this.begin_image = begin;
        this.end_image = end - bits.length();
        this.begin_text = this.end_image + 1;
        this.end_text = this.end_image;
    }

    public static List<Bits> parse(String bits, List<Splitter.Result> message) {
        if (bits == null) return null;
        return Stream.of(message)
                .filter((m) -> pattern.matcher(m.word).matches())
                .map((m) -> new Bits(m.word, m.begin, m.end))
                .collect(Collectors.toList());
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
