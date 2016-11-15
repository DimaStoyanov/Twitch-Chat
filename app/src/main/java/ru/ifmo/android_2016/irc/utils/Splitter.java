package ru.ifmo.android_2016.irc.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by ghost on 11/12/2016.
 */

public class Splitter {
    public List<String> words = new ArrayList<>();
    public List<Integer> begin = new ArrayList<>();
    public List<Integer> end = new ArrayList<>();

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