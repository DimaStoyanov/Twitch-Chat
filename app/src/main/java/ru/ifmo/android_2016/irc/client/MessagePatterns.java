package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost on 10/29/2016.
 */

final class MessagePatterns {
    final String OR = "|";
    final String LETTER = "[A-Za-z]";
    final String DIGIT = "[0-9]";
    final String SPECIAL = "[\\[\\]\\\\\\`\\_\\^\\{\\|\\}]";
    final String IPV4ADDR = DIGIT + "{1,3}." + DIGIT + "{1,3}." + DIGIT + "{1,3}." + DIGIT + "{1,3}";
    final String HOSTNAME = IPV4ADDR;
    final String NICKNAME = "(?:" + LETTER + OR + SPECIAL + ")(" + LETTER + OR + DIGIT + OR +
            SPECIAL + OR + "-)*";
    final String SERVERNAME = HOSTNAME;
//    final String MESSAGE = "(:" + PREFIX + " )?" + COMMAND + "(" + PARAMS + ")?";
//    final String PREFIX = SERVERNAME + "|(?:" + NICKNAME
}
