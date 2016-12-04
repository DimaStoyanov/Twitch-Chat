package ru.ifmo.android_2016.irc.utils;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 11/12/2016.
 */

public class Splitter {
    private final static Pattern wordPattern = Pattern.compile("\\w+");
    private final static Iterator emptyIterator = new Iterator() {
        @Override
        public boolean hasNext() {
            return false;
        }

        @Override
        public Object next() {
            return null;
        }
    };

    public static List<Result> splitWithSpace(String trailing) {
        StringBuilder sb = new StringBuilder();
        List<Result> result = new ArrayList<>();
        int b = 0;
        for (int i = 0; i < trailing.length(); i++) {
            char c = trailing.charAt(i);
            if (c != ' ') {
                sb.append(c);
            } else {
                //Log.d(TAG, sb.toString() + " " + b + " " + (i - 1));
                result.add(new Result(sb.toString(), b, i - 1));
                b = i + 1;
                sb = new StringBuilder();
            }
        }
        result.add(new Result(sb.toString(), b, trailing.length() - 1));
        //Log.d(TAG, sb.toString() + " " + b + " " + (trailing.length() - 1));
        return result;
    }

    @SuppressWarnings("unchecked")
    public static <T> Iterator<T> getEmptyIterator() {
        return (Iterator<T>) emptyIterator;
    }

    public static Iterable<Result> iteratorSplit(final String string) {
        return () -> new Iterator<Result>() {
            Matcher matcher = wordPattern.matcher(string);

            @Override
            public boolean hasNext() {
                return matcher.find();
            }

            @Override
            public Result next() {
                return new Result(matcher.group(), matcher.start(), matcher.end());
            }
        };
    }

    public static class Result {
        public String word;
        public int begin;
        public int end;

        private Result(String word, int begin, int end) {
            this.word = word;
            this.begin = begin;
            this.end = end;
        }
    }
}