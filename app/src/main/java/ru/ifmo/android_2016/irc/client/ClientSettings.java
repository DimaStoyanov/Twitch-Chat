package ru.ifmo.android_2016.irc.client;

import android.os.Parcel;
import android.os.Parcelable;

import java.io.Serializable;
import java.util.Arrays;
import java.util.Collections;
import java.util.List;

/**
 * Created by ghost on 10/23/2016.
 */

public class ClientSettings implements Parcelable, Serializable {
    String address;
    String username;
    String password;
    List<String> channels;
    String[] nicks;
    int port = 6667;
    boolean ssl, twitch;
    long id = 0;
    String name;

    public ClientSettings() {
    }

    @Deprecated
    public ClientSettings setChannel(String channel) {
        this.channels = Arrays.asList(channel.split(","));
        return this;
    }

    public ClientSettings setChannels(String... channels) {
        this.channels = Arrays.asList(channels);
        return this;
    }

    @SuppressWarnings("ArraysAsListWithZeroOrOneArgument")
    public ClientSettings addChannel(String channel) {
        if (channels == null) {
            channels = Arrays.asList(channel);
        } else {
            channels.add(channel);
        }
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

    public String getAddress() {
        return address;
    }

    public String getUsername() {
        return username;
    }

    public String getPassword() {
        return password;
    }

    @Deprecated
    public String getChannel() {
        return channels.get(0);
    }

    public String getChannel(int index) {
        return channels.get(index);
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

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(this.address);
        dest.writeString(this.username);
        dest.writeString(this.password);
        dest.writeStringList(this.channels);
        dest.writeStringArray(this.nicks);
        dest.writeInt(this.port);
        dest.writeByte(this.ssl ? (byte) 1 : (byte) 0);
        dest.writeByte(this.twitch ? (byte) 1 : (byte) 0);
        dest.writeLong(this.id);
        dest.writeString(this.name);
    }

    protected ClientSettings(Parcel in) {
        this.address = in.readString();
        this.username = in.readString();
        this.password = in.readString();
        this.channels = in.createStringArrayList();
        this.nicks = in.createStringArray();
        this.port = in.readInt();
        this.ssl = in.readByte() != 0;
        this.twitch = in.readByte() != 0;
        this.id = in.readLong();
        this.name = in.readString();
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
