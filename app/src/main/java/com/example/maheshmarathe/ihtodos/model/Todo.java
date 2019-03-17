package com.example.maheshmarathe.ihtodos.model;

import android.os.Parcel;
import android.os.Parcelable;

import com.google.firebase.firestore.IgnoreExtraProperties;
import com.google.firebase.firestore.ServerTimestamp;

import java.util.Date;

/**
 * Todo POJO.
 */
@IgnoreExtraProperties
public class Todo implements Parcelable {
    private String userId;
    private String title;
    private String progress;
    private @ServerTimestamp Date timestamp;
    private int alarmMinute;
    private int alarmHour;

    public Todo() {}

    public Todo(String userId, String title, String progress) {
        this.title = title;
        this.progress = progress;
        this.userId = userId;
    }

    public String getUserId() {
        return userId;
    }

    public void setUserId(String userId) {
        this.userId = userId;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String name) {
        this.title = name;
    }

    public String getProgress() {
        return progress;
    }

    public void setProgress(String progress) {
        this.progress = progress;
    }

    public Date getTimestamp() {
        return timestamp;
    }

    public void setTimestamp(Date timestamp) {
        this.timestamp = timestamp;
    }

    public int getAlarmMinute() {
        return alarmMinute;
    }

    public void setAlarmMinute(int alarmMinute) {
        this.alarmMinute = alarmMinute;
    }

    public int getAlarmHour() {
        return alarmHour;
    }

    public void setAlarmHour(int alarmHour) {
        this.alarmHour = alarmHour;
    }

    protected Todo(Parcel in) {
        userId = in.readString();
        title = in.readString();
        progress = in.readString();
        long tmpTimestamp = in.readLong();
        timestamp = tmpTimestamp != -1 ? new Date(tmpTimestamp) : null;
        alarmMinute = in.readInt();
        alarmHour = in.readInt();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeString(userId);
        dest.writeString(title);
        dest.writeString(progress);
        dest.writeLong(timestamp != null ? timestamp.getTime() : -1L);
        dest.writeInt(alarmMinute);
        dest.writeInt(alarmHour);
    }

    @SuppressWarnings("unused")
    public static final Parcelable.Creator<Todo> CREATOR = new Parcelable.Creator<Todo>() {
        @Override
        public Todo createFromParcel(Parcel in) {
            return new Todo(in);
        }

        @Override
        public Todo[] newArray(int size) {
            return new Todo[size];
        }
    };
}