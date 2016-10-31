package ru.ifmo.android_2016.irc.utils;

import android.util.Log;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectInput;
import java.io.ObjectInputStream;
import java.io.ObjectOutput;
import java.io.ObjectOutputStream;

/**
 * Created by Dima Stoyanov on 31.10.2016.
 * Project Android-IRC
 * Start time : 16:22
 */

public final class ObjectUtils {
    public static boolean checkNonNull(Object... args) {
        for (Object o : args) {
            if (o == null)
                return false;
        }
        return true;
    }


    public static byte[] serialize(Object object) throws IOException {
        ByteArrayOutputStream bos = new ByteArrayOutputStream();
        ObjectOutput out;
        byte[] result;
        try {
            out = new ObjectOutputStream(bos);
            out.writeObject(object);
            out.flush();
            result = bos.toByteArray();
        } finally {
            try {
                bos.close();
            } catch (IOException ex) {
                Log.d("IRC serialize", "Can't close baos " + ex.getMessage());
            }
        }
        return result;
    }

    public static Object deserialize(byte[] bytes) throws IOException, ClassNotFoundException {
        ByteArrayInputStream bis = new ByteArrayInputStream(bytes);
        ObjectInput in = null;
        Object result;
        try {
            in = new ObjectInputStream(bis);
            result = in.readObject();
        } finally {
            try {
                if (in != null) {
                    in.close();
                }
            } catch (IOException ex) {
                Log.d("IRC deserialize", "Can't close input stream" + ex.getMessage());
            }
        }
        return result;
    }


    public static String getSeparatedString(Object... objects) {
        StringBuilder sb = new StringBuilder();
        for (Object o : objects) {
            sb.append(o).append(" ");
        }
        sb.deleteCharAt(sb.length() - 1);
        return sb.toString();
    }

}
