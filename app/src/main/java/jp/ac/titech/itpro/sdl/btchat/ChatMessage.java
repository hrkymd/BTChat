package jp.ac.titech.itpro.sdl.btchat;

import android.os.Parcel;
import android.os.Parcelable;
import android.util.JsonReader;
import android.util.JsonToken;
import android.util.JsonWriter;
import android.util.Log;

import java.io.Closeable;
import java.io.IOException;

public class ChatMessage implements Parcelable {
    private final static String FIELD_SEQ = "seq";
    private final static String FIELD_TIME = "time";
    private final static String FIELD_CONTENT = "content";
    private final static String FIELD_SENDER = "sender";

    public int seq;
    public long time;
    public String content;
    public String sender;

    public ChatMessage(int seq, long time, String content, String sender) {
        this.seq = seq;
        this.time = time;
        this.content = content;
        this.sender = sender;
    }

    @Override
    public String toString() {
        return content;
    }

    private ChatMessage(Parcel in) {
        seq = in.readInt();
        time = in.readLong();
        content = in.readString();
        sender = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(seq);
        dest.writeLong(time);
        dest.writeString(content);
        dest.writeString(sender);
    }

    public static final Parcelable.Creator<ChatMessage> CREATOR =
            new Parcelable.Creator<ChatMessage>() {

                @Override
                public ChatMessage createFromParcel(Parcel source) {
                    return new ChatMessage(source);
                }

                @Override
                public ChatMessage[] newArray(int size) {
                    return new ChatMessage[size];
                }
            };


    public static class Reader implements Closeable {
        private final static String TAG = "ChagMessage.Reader";
        private final JsonReader reader;

        public Reader(JsonReader reader) {
            if (reader == null)
                throw new NullPointerException("reader is null");
            this.reader = reader;
        }

        @Override
        public void close() throws IOException {
            Log.d(TAG, "close");
            reader.close();
        }

        public void beginArray() throws IOException {
            Log.d(TAG, "beginArray");
            reader.beginArray();
        }

        public void endArray() throws IOException {
            Log.d(TAG, "endArray");
            reader.endArray();
        }

        public ChatMessage read() throws IOException {
            Log.d(TAG, "read");
            int seq = -1;
            long time = -1;
            String content = null;
            String sender = null;
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                case FIELD_SEQ:
                    seq = reader.nextInt();
                    break;
                case FIELD_TIME:
                    time = reader.nextLong();
                    break;
                case FIELD_CONTENT:
                    if (reader.peek() == JsonToken.NULL) {
                        reader.skipValue();
                        content = null;
                    }
                    else
                        content = reader.nextString();
                    break;
                case FIELD_SENDER:
                    if (reader.peek() == JsonToken.NULL) {
                        reader.skipValue();
                        sender = null;
                    }
                    else
                        sender = reader.nextString();
                    break;
                default:
                    reader.skipValue();
                    break;
                }
            }
            reader.endObject();
            return new ChatMessage(seq, time, content, sender);
        }
    }

    public static class Writer implements Closeable {
        private final static String TAG = "ChatMessage.Writer";
        private final JsonWriter writer;

        public Writer(JsonWriter writer) {
            if (writer == null)
                throw new NullPointerException("writer is null");
            this.writer = writer;
        }

        @Override
        public void close() throws IOException {
            Log.d(TAG, "close");
            writer.close();
        }

        public void flush() throws IOException {
            Log.d(TAG, "flush");
            writer.flush();
        }

        public void beginArray() throws IOException {
            Log.d(TAG, "beginArray");
            writer.beginArray();
        }

        public void endArray() throws IOException {
            Log.d(TAG, "endArray");
            writer.endArray();
        }

        public void write(ChatMessage message) throws IOException {
            Log.d(TAG, "write");
            writer.beginObject();
            writer.name(FIELD_SEQ).value(message.seq);
            writer.name(FIELD_TIME).value(message.time);
            writer.name(FIELD_CONTENT);
            if (message.content == null)
                writer.nullValue();
            else
                writer.value(message.content);
            writer.name(FIELD_SENDER);
            if (message.sender == null)
                writer.nullValue();
            else
                writer.value(message.sender);
            writer.endObject();
        }
    }
}
