package ru.ifmo.android_2016.irc.client;

import android.graphics.Color;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import ru.ifmo.android_2016.irc.utils.Splitter;

/**
 * Created by ghost3432 on 14.11.16.
 */

public class Bits {
    private static final Pattern pattern = Pattern.compile("\\bcheer\\d+\\b");
    private static final String TAG = Bits.class.getSimpleName();

    private final int bits;
    private final String BITS_URL_TEMPLATE =
            "https://static-cdn.jtvnw.net/bits/{{theme}}/{{type}}/{{color}}/{{size}}";
    private final int beginImage;
    private final int endImage;
    private final int beginText;
    private final int endText;

    private Bits(String bits, int begin, int end) {
        try {
            bits = bits.replace("cheer", "");
            this.bits = Integer.parseInt(bits);
            this.beginImage = begin;
            this.endImage = end - bits.length();
            this.beginText = this.endImage;
            this.endText = end;
        } catch (NumberFormatException x) {
            throw new ParserException(x);
        }
    }

    public static List<Bits> parse(String bitsCount, String message) {
        if (bitsCount == null) return null;
        if (message == null) return null;

        List<Bits> result = new ArrayList<>();
        Matcher matcher = pattern.matcher(message);
        while (matcher.find()) {
            Bits bits = new Bits(
                    matcher.group(),
                    matcher.start(),
                    matcher.end());
            result.add(bits);
        }
        return result;
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
                .replace("{{theme}}", "dark")
                .replace("{{type}}", "animated")
                .replace("{{color}}", getColorString())
                .replace("{{size}}", "2");
    }

    private String getColorString() {
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

    public int getColor() {
        if (bits >= 10000) {
            return Color.RED;
        } else if (bits >= 5000) {
            return Color.BLUE;
        } else if (bits >= 1000) {
            return Color.GREEN;
        } else if (bits >= 100) {
            return 0xFF800080;
        } else {
            return Color.GRAY;
        }
    }

    public int getImageBegin() {
        return beginImage;
    }

    public int getImageEnd() {
        return endImage;
    }

    public int getNumberBegin() {
        return beginText;
    }

    public int getNumberEnd() {
        return endText;
    }

    @Override
    public String toString() {
        return bits + " " + beginImage + ":" + endImage + "/" + beginText + ":" + endText;
    }

    public int getWidth() {
        return 25;
    }

    public int getHeight() {
        return 25;
    }
}
