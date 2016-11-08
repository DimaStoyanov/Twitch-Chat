package ru.ifmo.android_2016.irc.client;

import android.support.v4.util.LongSparseArray;

import java.util.List;
import java.util.concurrent.atomic.AtomicLong;

/**
 * Created by ghost on 11/7/2016.
 */
public class MessageStorage {
    private static MessageStorage ourInstance = new MessageStorage();
    private final LongSparseArray<List<?>> messages = new LongSparseArray<>();
    private final AtomicLong lastId = new AtomicLong(1);

    public static MessageStorage getInstance() {
        return ourInstance;
    }

    private MessageStorage() {
    }

    public synchronized long getNewStorage(List<?> instance) {
        long id = lastId.getAndIncrement();
        messages.put(id, instance);
        return id;
    }

    public synchronized void removeStorage(long id) {
        messages.delete(id);
    }

    @SuppressWarnings("unchecked")
    public <T> List<T> get(long id) {
        return (List<T>) messages.get(id);
    }
}
