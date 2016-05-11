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
    private final static String TAG = "ChagMessage";

    public final static int KIND_UNKNOWN = 0;
    public final static int KIND_MESSAGE = 1;
    public final static int KIND_ACK = 2;
    public final static int KIND_CLOSE = 3;

    private final static String FIELD_KIND = "kind";
    private final static String FIELD_SEQ = "seq";
    private final static String FIELD_TIME = "time";
    private final static String FIELD_CONTENT = "content";

    public int kind;
    public int seq;
    public long time;
    public String content;

    public ChatMessage(int kind, int seq, long time, String content) {
        this.kind = kind;
        this.seq = seq;
        this.time = time;
        this.content = content;
    }

    @Override
    public String toString() {
        return content;
    }

    private ChatMessage(Parcel in) {
        kind = in.readInt();
        seq = in.readInt();
        time = in.readLong();
        content = in.readString();
    }

    @Override
    public int describeContents() {
        return 0;
    }

    @Override
    public void writeToParcel(Parcel dest, int flags) {
        dest.writeInt(kind);
        dest.writeInt(seq);
        dest.writeLong(time);
        dest.writeString(content);
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
            int kind = KIND_UNKNOWN;
            int seq = -1;
            long time = -1;
            String content = null;
            reader.beginObject();
            while (reader.hasNext()) {
                switch (reader.nextName()) {
                case FIELD_KIND:
                    kind = reader.nextInt();
                    break;
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
                default:
                    reader.skipValue();
                    break;
                }
            }
            reader.endObject();
            return new ChatMessage(kind, seq, time, content);
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
            writer.name(FIELD_KIND).value(message.kind);
            writer.name(FIELD_SEQ).value(message.seq);
            writer.name(FIELD_TIME).value(message.time);
            writer.name(FIELD_CONTENT);
            if (message.content == null)
                writer.nullValue();
            else
                writer.value(message.content);
            writer.endObject();
        }
    }
}
