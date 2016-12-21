package ru.ifmo.android_2016.irc.api.frankerfacez;

import android.support.annotation.NonNull;
import android.util.JsonReader;

import com.annimon.stream.Collectors;
import com.annimon.stream.Optional;
import com.annimon.stream.Stream;
import com.annimon.stream.function.Supplier;

import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;

/**
 * Created by ghost on 12/18/2016.
 */

public final class FrankerFaceZParser {
    private FrankerFaceZParser() {
    }

    private interface Parsable<T> {
        T parse(JsonReader reader) throws IOException;
    }

    private static class JsonInt implements Parsable<Integer> {
        JsonInt() {
        }

        @Override
        public Integer parse(JsonReader reader) throws IOException {
            return reader.nextInt();
        }
    }

    public static final class Response implements Parsable<Response> {
        @NonNull
        private Optional<Room> room = Optional.empty();
        @NonNull
        private Optional<List<Set>> set = Optional.empty();
        @NonNull
        private Optional<java.util.Set<Integer>> defaultSets = Optional.empty();

        public Response parse(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "room":
                        setRoom(new Room().parse(reader));
                        break;

                    case "set":
                        setSets(new Set().parse(reader));
                        break;

                    case "sets":
                        setSets(parseMap(reader, Set::new));
                        break;

                    case "default_sets":
                        defaultSets = Optional.of(new HashSet<>(parseArray(reader, JsonInt::new)));
                        break;

                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
            return this;
        }

        public void setRoom(@NonNull Room room) {
            this.room = Optional.of(room);
        }

        public void setSets(Map<String, Set> sets) {
            setSets(new ArrayList<>(sets.values()));
        }

        public void setSets(Set set) {
            setSets(Collections.singletonList(set));
        }

        public void setSets(List<Set> sets) {
            this.set = Optional.of(sets);
        }

        public List<Set> getSets() {
            return set.orElse(Collections.emptyList());
        }

        @NonNull
        public Optional<Room> getRoom() {
            return room;
        }

        @NonNull
        public Optional<java.util.Set<Integer>> getDefaultSets() {
            return defaultSets;
        }
    }

    public static final class Room implements Parsable<Room> {
        private int _id;
        private String id;
        private int twitchId;
        private int set;

        @Override
        public Room parse(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "_id":
                        _id = reader.nextInt();
                        break;

                    case "twitch_id":
                        twitchId = reader.nextInt();
                        break;

                    case "set":
                        set = reader.nextInt();
                        break;

                    case "id":
                        id = reader.nextString();
                        break;

                    default:
                        //TODO: еще добавить полей
                        reader.skipValue();
                }
            }
            reader.endObject();
            return this;
        }

        public String getName() {
            return id;
        }

        public int getId() {
            return _id;
        }
    }

    public static final class Set implements Parsable<Set> {
        private int id;
        private List<Emote> emoticons;

        @Override
        public Set parse(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "emoticons":
                        emoticons = parseArray(reader, Emote::new);
                        break;

                    case "id":
                        id = reader.nextInt();
                        break;

                    default:
                        reader.skipValue();
                }
            }
            reader.endObject();
            return this;
        }

        public int getId() {
            return id;
        }

        public Map<String, String> getEmotes() {
            return Stream.of(emoticons)
                    .collect(Collectors.toMap(e -> e.code, e -> String.valueOf(e.id)));
        }
    }

    public static final class Emote implements Parsable<Emote> {
        private int id;
        private String code;
        private int width;
        private int height;
        private boolean publicBool;

        @Override
        public Emote parse(JsonReader reader) throws IOException {
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                    case "id":
                        id = reader.nextInt();
                        break;

                    case "name":
                        code = reader.nextString();
                        break;

                    case "width":
                        width = reader.nextInt();
                        break;

                    case "height":
                        height = reader.nextInt();
                        break;

                    case "public":
                        publicBool = reader.nextBoolean();
                        break;

                    default:
                        //TODO: нужно больше полей...
                        reader.skipValue();
                }
            }
            reader.endObject();

            return this;
        }
    }

    private static <T> List<T> parseArray(JsonReader reader,
                                          Supplier<Parsable<T>> constructor)
            throws IOException {
        List<T> result = new ArrayList<>();

        reader.beginArray();
        while (reader.hasNext()) {
            result.add(constructor.get().parse(reader));
        }
        reader.endArray();

        return result;
    }

    private static <T extends Parsable<T>> Map<String, T> parseMap(JsonReader reader,
                                                                   Supplier<T> constructor)
            throws IOException {
        Map<String, T> result = new HashMap<>();

        reader.beginObject();
        while (reader.hasNext()) {
            result.put(reader.nextName(), constructor.get().parse(reader));
        }
        reader.endObject();

        return result;
    }

    public static Response parse(InputStream inputStream) throws IOException {
        JsonReader reader = new JsonReader(new InputStreamReader(inputStream));
        return new Response().parse(reader);
    }
}