package ru.ifmo.android_2016.irc.utils;

/**
 * Created by ghost on 12/10/2016.
 */

public final class StringView implements CharSequence {
    private final String string;
    private final int begin;
    private final int end;

    public StringView(String string) {
        this(string, 0);
    }

    public StringView(String string, int begin) {
        this(string, begin, string.length() - begin);
    }

    public StringView(String string, int begin, int end) {
        this.string = string;
        this.begin = begin;
        this.end = end;
        if (end < begin) {
            throw new IllegalStateException();
        }
    }

    public StringView(StringView stringView, int begin) {
        this(stringView, begin, stringView.length() - begin);
    }

    public StringView(StringView stringView, int begin, int end) {
        this(stringView.string, stringView.begin + begin, stringView.begin + end);
    }

    @Override
    public String toString() {
        return string.substring(begin, end);
    }

    @Override
    public final int length() {
        return end - begin;
    }

    @Override
    public final char charAt(int i) {
        return string.charAt(begin + i);
    }

    @Override
    public final CharSequence subSequence(int begin, int end) {
        return substring(begin, end);
    }

    public StringView substring(int begin) {
        return substring(begin, length());
    }

    public final StringView substring(int begin, int end) {
        return new StringView(this, begin, end);
    }

    public int indexOf(char c) {
        int result = string.indexOf(c, begin);
        if (result >= end || result == -1) return -1;
        return result - begin;
    }

    public int indexOf(String c) {
        int result = string.indexOf(c, begin);
        if (result >= end || result == -1) return -1;
        return result - begin;
    }

    public boolean startsWith(String prefix) {
        return string.startsWith(prefix, begin);
    }

    public boolean startsWith(String prefix, int offset) {
        return string.startsWith(prefix, begin + offset);
    }

    public boolean endsWith(String suffix) {
        return startsWith(suffix, length() - suffix.length());
    }
}
