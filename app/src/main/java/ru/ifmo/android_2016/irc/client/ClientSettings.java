package ru.ifmo.android_2016.irc.client;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;

/**
 * Created by ghost on 10/23/2016.
 */

public class ClientSettings implements Parcelable, Serializable {
    String address, username, password, channels;
    String[] nicks;
    int port = 6667;
    boolean ssl, twitch;
    long id = 0;
    String name;

    public ClientSettings() {
    }

    public ClientSettings setChannels(String channels) {
        this.channels = channels;
        return this;
    }

    public ClientSettings setAddress(String address) {
        this.address = address;
        return this;
    }

    public ClientSettings setUsername(String username) {
        this.username = username;
        return this;
    }

    public ClientSettings setPassword(String password) {
        this.password = password;
        return this;
    }

    public ClientSettings setNicks(String... nicks) {
        this.nicks = nicks;
        return this;
    }

    public ClientSettings setPort(int port) {
        this.port = port;
        return this;
    }

    public ClientSettings setSsl(boolean ssl) {
        this.ssl = ssl;
        return this;
    }

    public ClientSettings setTwitch(boolean twitch) {
        this.twitch = twitch;
        return this;
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
        dest.writeByte(this.twitch ? (byte) 1 : (byte) 0);
    }

    protected ClientSettings(Parcel in) {
        this.address = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.channels = in.readString();
        this.nicks = in.createStringArray();
        this.port = in.readInt();
        this.ssl = in.readByte() != 0;
        this.twitch = in.readByte() != 0;
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

    public String getAddress() {
        return address;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    public String getChannels() {
        return channels;
    }

    public String[] getNicks() {
        return nicks;
    }

    public int getPort() {
        return port;
    }

    public boolean isSsl() {
        return ssl;
    }

    public boolean isTwitch() {
        return twitch;
    }

    ClientSettings setId(long id) {
        this.id = id;
        return this;
    }

    public long getId() {
        return id;
    }

    public static ClientSettings getTwitchSettings(String token, boolean ssl) {
        return new ClientSettings()
                .setAddress("irc.chat.twitch.tv")
                .setPort(ssl ? 443 : 6667)
                .setSsl(ssl)
                .setPassword("oauth:" + token)
                .setTwitch(true);
    }

    public ClientSettings setName(String name) {
        this.name = name;
        return this;
    }

    public String getName() {
        return name;
    }
}
