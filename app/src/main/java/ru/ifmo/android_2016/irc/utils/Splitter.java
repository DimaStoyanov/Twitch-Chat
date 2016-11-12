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

    public static Splitter splitWithSpace(String trailing) {
        StringBuilder sb = new StringBuilder();
        Splitter result = new Splitter();
        int b = 0;
        for (int i = 0; i < trailing.length(); i++) {
            char c = trailing.charAt(i);
            if (c != ' ') {
                sb.append(c);
            } else {
                //Log.d(TAG, sb.toString() + " " + b + " " + (i - 1));
                result.words.add(sb.toString());
                result.begin.add(b);
                result.end.add(i - 1);
                b = i + 1;
                sb = new StringBuilder();
            }
        }
        result.words.add(sb.toString());
        result.begin.add(b);
        result.end.add(trailing.length() - 1);
        //Log.d(TAG, sb.toString() + " " + b + " " + (trailing.length() - 1));
        return result;
    }
}