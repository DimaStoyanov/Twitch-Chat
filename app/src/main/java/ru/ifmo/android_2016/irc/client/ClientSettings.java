package ru.ifmo.android_2016.irc.client;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by ghost on 10/23/2016.
 */

public class ClientSettings implements Parcelable, Serializable {
    final String address, username, password, channels;
    final String[] nicks;
    final int port;
    final boolean ssl;

    private ClientSettings(String address, String username, String password, String[] nicks,
                           String channels, int port, boolean ssl) {
        this.address = address;
        this.username = username;
        this.password = password;
        this.nicks = nicks;
        this.channels = channels;
        this.port = port;
        this.ssl = ssl;
    }

    public static class Builder {
        String address;
        int port = 6667;
        boolean ssl = false;
        String username;
        String password;
        String joinList;
        String[] nicks;

        public ClientSettings.Builder setAddress(String address) {
            this.address = address;
            return this;
        }

        public ClientSettings.Builder setPort(int port) {
            this.port = port;
            return this;
        }

        public ClientSettings.Builder setSsl(boolean ssl) {
            this.ssl = ssl;
            return this;
        }

        public ClientSettings.Builder setUsername(String username) {
            this.username = username;
            return this;
        }

        public ClientSettings.Builder setPassword(String password) {
            this.password = password;
            return this;
        }

        public ClientSettings.Builder addNicks(String... nicks) {
            this.nicks = nicks;
            return this;
        }

        public ClientSettings build() {
            return new ClientSettings(address, username, password, nicks, joinList, port, ssl);
        }

        public ClientSettings.Builder setChannels(String string) {
            joinList = string;
            return this;
        }
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.address);
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeString(this.channels);
        dest.writeStringArray(this.nicks);
        dest.writeInt(this.port);
        dest.writeByte(this.ssl ? (byte) 1 : (byte) 0);
    }

    protected ClientSettings(Parcel in) {
        this.address = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.channels = in.readString();
        this.nicks = in.createStringArray();
        this.port = in.readInt();
        this.ssl = in.readByte() != 0;
    }

    public static final Creator<ClientSettings> CREATOR = new Creator<ClientSettings>() {
        @Override
        public ClientSettings createFromParcel(Parcel source) {
            return new ClientSettings(source);
        }

        @Override
        public ClientSettings[] newArray(int size) {
            return new ClientSettings[size];
        }
    };
}
