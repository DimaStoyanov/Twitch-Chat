package ru.ifmo.android_2016.irc.client;

/**
 * Created by ghost3432 on 14.11.16.
 */

@SuppressWarnings("unused")
public enum Notice {
    SUBS_ON,
    ALREADY_SUBS_ON,
    SUBS_OFF,
    ALREADY_SUBS_OFF,
    SLOW_ON,
    SLOW_OFF,
    R9K_ON,
    ALREADY_R9K_ON,
    R9K_OFF,
    ALREADY_R9K_OFF,
    HOST_ON,
    BAD_HOST_HOSTING,
    HOST_OFF,
    HOSTS_REMAINING,
    EMOTE_ONLY_ON,
    ALREADY_EMOTE_ONLY_ON,
    EMOTE_ONLY_OFF,
    ALREADY_EMOTE_ONLY_OFF,
    MSG_CHANNEL_SUSPENDED,
    TIMEOUT_SUCCESS,
    BAN_SUCCESS,
    UNBAN_SUCCESS,
    BAD_UNBAN_NO_BAD,
    ALREADY_BANNED,
    UNRECOGNIZED_CMD,
    RESUB,
    MSG_DUPLICATE,
    WHISPER_INVALID_SELF,

    UNKNOWN;

    public String unknown;

//    static Map<String, Notice> map = Stream.of(Notice.values())
//            .map(n -> Pair.create(n.name(), n))
//            .collect(Collectors.toMap(p -> p.first, p -> p.second));

    static Notice parse(String notice) {
        if (notice == null) return null;
        try {
            return Enum.valueOf(Notice.class, notice.toUpperCase());
        } catch (IllegalArgumentException x) {
            Notice n = UNKNOWN;
            n.unknown = notice;
            return n;
        }
    }
}
