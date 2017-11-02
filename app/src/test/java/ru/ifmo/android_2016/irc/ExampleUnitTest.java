package ru.ifmo.android_2016.irc;

import org.junit.Test;

import ru.ifmo.android_2016.irc.client.IRCMessage;

import static org.junit.Assert.*;

/**
 * Example local unit test, which will execute on the development machine (host).
 *
 * @see <a href="http://d.android.com/tools/testing">Testing documentation</a>
 */
public class ExampleUnitTest {
    public ExampleUnitTest() {
        IRCMessage.fromString("loli");
    }

    @Test
    public void addition_isCorrect() throws Exception {
        assertEquals(4, 2 + 2);
    }

    @Test
    public void parse_isCorrect() throws Exception {
        IRCMessage.fromString("JOIN #forsenlol");
        IRCMessage.fromString("PRIVMSG #forsenlol :gachiGASM Clap");
        IRCMessage.fromString("PING :tmi.twitch.tv");
        IRCMessage.fromString("PING :");
        IRCMessage.fromString("PONG");
        IRCMessage.fromString("@display-name PRIVMSG #test_channel :test message to test channel megalul");
        IRCMessage.fromString("USERNOTICE :you have been banned from this room FeelsBadMan");
    }


}