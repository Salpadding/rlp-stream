package org.tdf.rlpstream;

import java.util.Collection;

public class RlpList {
    private final byte[] bin;
    private final long streamId; // rawId = (rawSize << 32)| rawOffset
    private long[] children; // stream id of children
    private int childrenCnt;

    private void pushChildren(long streamId) {
        if(children.length == childrenCnt) {
            long[] tmp = children;
            this.children = new long[tmp.length * 2];
            System.arraycopy(tmp, 0, this.children, 0, childrenCnt);
        }
        children[childrenCnt++] = streamId;
    }

    private long getChildren(int index) {
        if(index >= childrenCnt)
            throw new RuntimeException("array index overflow");
        return children[index];
    }


    public RlpList(byte[] bin, long streamId, int bufSize) {
        this.streamId = streamId;
        this.bin = bin;
        if(!StreamId.isList(streamId))
            throw new RuntimeException("not a rlp list");
        this.children = new long[Math.max(bufSize, 1)];
        
        long now = this.streamId;
        while (true) {
            now = RlpStream.iterateList(bin, streamId, now);
            if (StreamId.isEOF(now)) {
                break;
            }
            pushChildren(now);
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

    public int size() {
        return childrenCnt;
    }

    public boolean isNullAt(int idx) {
        long streamId = getChildren(idx);
        int size = StreamId.sizeOf(streamId);
        return size == 0 && !StreamId.isList(streamId);
    }

    public boolean isListAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.isList(streamId);
    }

    public RlpList listAt(int idx) {
        long streamId = getChildren(idx);
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;
        return new RlpList(bin, rawOffset, rawOffset + rawSize, 0);
    }

    public byte[] rawAt(int idx) {
        long streamId = getChildren(idx);
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;

        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        return r;
    }

    public byte[] bytesAt(int idx) {
        long streamId = getChildren(idx);
        return RlpStream.asBytes(bin, streamId);
    }

    public <T> T valueAt(int idx, Class<T> clazz) {
        long streamId = getChildren(idx);
        return RlpStream.decode(bin, streamId, clazz);
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
