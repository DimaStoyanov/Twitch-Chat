package ru.ifmo.android_2016.irc.client;

import android.support.annotation.Nullable;

import com.annimon.stream.Collectors;
import com.annimon.stream.Stream;

import java.util.Collection;
import java.util.Collections;
import java.util.HashSet;
import java.util.Set;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/29/2016.
 */

public final class MessagePatterns {
    private final static MessagePatterns instance = new MessagePatterns();
    private final Set<String> highlightSet = new HashSet<>();
    private final Set<String> banWordsSet = new HashSet<>();
    private final Set<String> ignoredUsersSet = new HashSet<>();
    private Pattern highlightPattern = null;
    private Pattern banWordsPattern = null;
    private Pattern ignoredUsersPattern = null;

    private MessagePatterns() {
        addHighlight("raffle", "waffle", "waffie");
    }

    public static MessagePatterns getInstance() {
        return instance;
    }

    public void addHighlight(String... words) {
        Collections.addAll(highlightSet, words);
        highlightPattern = null;
    }

    public void addBanWords(String... words) {
        Collections.addAll(banWordsSet, words);
        banWordsPattern = null;
    }

    public void addIgnoredUsers(String... users) {
        Collections.addAll(ignoredUsersSet, users);
        ignoredUsersPattern = null;
    }

    public Set<String> getHighlightSet() {
        return Collections.unmodifiableSet(highlightSet);
    }

    public Set<String> getBanWordsSet() {
        return Collections.unmodifiableSet(banWordsSet);
    }

    public Set<String> getIgnoredUsersSet() {
        return Collections.unmodifiableSet(ignoredUsersSet);
    }

    private Pattern compilePattern(Collection<String> words) {
        String pattern = Stream.of(words)
                .map(w -> "(?:" + w + ")")
                .collect(Collectors.joining("|"));
        return Pattern.compile(pattern, Pattern.CASE_INSENSITIVE);
    }

    @Nullable
    Pattern getHighlightRegex() {
        if (highlightPattern == null && highlightSet.size() > 0) {
            highlightPattern = compilePattern(highlightSet);
        }
        return highlightPattern;
    }

    @Nullable
    Pattern getBanWordsPattern() {
        if (banWordsPattern == null && banWordsSet.size() > 0) {
            banWordsPattern = compilePattern(banWordsSet);
        }
        return banWordsPattern;
    }

    @Nullable
    Pattern getIgnoredUsersPattern() {
        if (ignoredUsersPattern == null && ignoredUsersSet.size() > 0) {
            ignoredUsersPattern = compilePattern(ignoredUsersSet);
        }
        return ignoredUsersPattern;
    }
}
