package irc.android_2016.ifmo.ru.irc.client;

import java.util.Date;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Created by ghost on 10/23/2016.
 */

public class Message {
    public static Pattern pattern = Pattern.compile(":([\\w]+)!?[\\w@.]+ [\\w]+ (#?[\\w]+) :(.*)");
    public String from, to, message;
    public Date date;

    public Message(String string) {
        Matcher matcher = Message.pattern.matcher(string);
        if (matcher.find()) {
            from = matcher.group(1);
            to = matcher.group(2);
            message = matcher.group(3);
        }
    }
}
