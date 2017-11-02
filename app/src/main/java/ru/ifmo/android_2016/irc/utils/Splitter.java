package ru.ifmo.android_2016.irc.utils;

import java.util.Iterator;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 11/12/2016.
 */

public class Splitter {
    private final static Pattern wordPattern = Pattern.compile("[^\\s]+");
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