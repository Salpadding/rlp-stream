package com.github.salpadding.rlpstream;

import java.math.BigInteger;
import java.util.Collection;

public class RlpList {
    private final byte[] bin;
    private final long streamId;
    private long[] children; // stream id of children
    private int childrenCnt;
    private byte[] encoded;

    private void pushChildren(long streamId) {
        if (children.length == childrenCnt) {
            long[] tmp = children;
            this.children = new long[tmp.length * 2];
            System.arraycopy(tmp, 0, this.children, 0, childrenCnt);
        }
        children[childrenCnt++] = streamId;
    }

    private long getChildren(int index) {
        if (index >= childrenCnt)
            throw new RuntimeException("array index overflow");
        return children[index];
    }


    public RlpList(byte[] bin, long streamId, int bufSize) {
        this.streamId = streamId;
        this.bin = bin;
        if (!StreamId.isList(streamId))
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
        RlpList li = new RlpList(encoded, 0, encoded.length, 0);
        li.encoded = encoded;
        return li;
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
        return StreamId.isNull(streamId);
    }

    public boolean isListAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.isList(streamId);
    }

    public RlpList listAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asList(bin, streamId);
    }

    public byte[] rawAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.rawOf(bin, streamId);
    }

    public byte[] bytesAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asBytes(bin, streamId);
    }

    public <T> T valueAt(int idx, Class<T> clazz) {
        long streamId = getChildren(idx);
        return RlpStream.decode(bin, streamId, clazz);
    }

    public byte byteAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asByte(bin, streamId);
    }

    public int intAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asInt(bin, streamId);
    }

    public short shortAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asShort(bin, streamId);
    }

    public long longAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asLong(bin, streamId);
    }

    public String stringAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asString(bin, streamId);
    }

    public BigInteger bigIntAt(int idx) {
        long streamId = getChildren(idx);
        return StreamId.asBigInteger(bin, streamId);
    }

    public <T> T as(Class<T> clazz) {
        return StreamId.as(bin, streamId, clazz);
    }

    public byte[] getEncoded() {
        if (encoded != null)
            return encoded;
        int prefixSize = StreamId.prefixSizeOf(streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        int rawSize = StreamId.sizeOf(streamId) + prefixSize;
        if (rawSize == bin.length) {
            encoded = bin;
            return encoded;
        }
        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        encoded = r;
        return encoded;
    }
}
