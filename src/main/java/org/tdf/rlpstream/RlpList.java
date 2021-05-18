package org.tdf.rlpstream;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

public class RlpList {
    private final byte[] bin;
    private final long streamId; // rawId = (rawSize << 32)| rawOffset
    private final List<Long> children; // stream id of children

    public RlpList(byte[] bin, long streamId, int bufSize) {
        this.streamId = streamId;
        this.bin = bin;
        if(!StreamId.isList(streamId))
            throw new RuntimeException("not a rlp list");
        this.children = new ArrayList<>(bufSize);
        
        long now = this.streamId;
        while (true) {
            now = RlpStream.iterateList(bin, streamId, now);
            if (StreamId.isEOF(now)) {
                break;
            }
            children.add(now);
        }        
    }

    public RlpList(byte[] encoded, int rawOffset, int rawLimit, int bufSize) {
        this(
            encoded,
            RlpStream.decodeElement(encoded, rawOffset, rawLimit, true),
            bufSize
        );
    }

    public static RlpList fromEncoded(byte[] encoded) {
        return new RlpList(encoded, 0, encoded.length, 0);
    }

    public static RlpList fromElements(Collection<byte[]> elements) {
        return fromEncoded(
            Rlp.encodeElements(elements)
        );
    }

    public static RlpList fromElements(byte[]... elements) {
        return fromEncoded(
            Rlp.encodeElements(elements)
        );
    }

    public int size() {
        return children.size();
    }

    public boolean isNullAt(int idx) {
        long streamId = children.get(idx);
        int size = StreamId.sizeOf(streamId);
        return size == 0 && !StreamId.isList(streamId);
    }

    public boolean isListAt(int idx) {
        long streamId = children.get(idx);
        return StreamId.isList(streamId);
    }

    public RlpList listAt(int idx) {
        long streamId = children.get(idx);
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;
        return new RlpList(bin, rawOffset, rawOffset + rawSize, 0);
    }

    public byte[] rawAt(int idx) {
        long streamId = children.get(idx);
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;

        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        return r;
    }

    public byte[] bytesAt(int idx) {
        long streamId = children.get(idx);
        return RlpStream.asBytes(bin, streamId);
    }

    public <T> T as(Class<T> clazz) {
        return Rlp.decode(getEncoded(), clazz);
    }

    public byte[] getEncoded() {
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;
        if (rawSize == bin.length)
            return bin;
        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        return r;
    }
}
