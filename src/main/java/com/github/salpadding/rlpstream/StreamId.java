package com.github.salpadding.rlpstream;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static com.github.salpadding.rlpstream.Constants.*;

// stream is a inlined object combined with raw binary
public class StreamId {
    public static boolean isEOF(long streamId) {
        return (streamId & EOF_MASK) != 0;
    }

    public static boolean isNull(long streamId) {
        return !isList(streamId) && sizeOf(streamId) == 0;
    }

    public static boolean isList(long streamId) {
        return (streamId & LIST_SIGN_MASK) != 0;
    }

    public static int sizeOf(long streamId) {
        return (int) ((streamId & SIZE_MASK) >>> 32);
    }

    public static int offsetOf(long streamId) {
        return (int) (streamId & OFFSET_MASK);
    }


    public static int prefixSizeOf(byte[] bin, long streamId) {
        if (isList(streamId)) {
            int size = sizeOf(streamId);
            if (size < SIZE_THRESHOLD)
                return 1;
            int noZero = 4 - Integer.numberOfLeadingZeros(size) / 8;
            return 1 + noZero;
        }
        int size = sizeOf(streamId);
        if (size == 0)
            return 1;
        int offset = offsetOf(streamId);
        if (size == 1 && (bin[offset] & 0xff) < OFFSET_SHORT_ITEM) {
            return 0;
        }
        if (size < SIZE_THRESHOLD) {
            return 1;
        }
        int noZero = 4 - Integer.numberOfLeadingZeros(size) / 8;
        return 1 + noZero;
    }

    public static BigInteger asBigInteger(byte[] bin, long streamId) {
        if (streamId < 0)
            throw new RuntimeException("not a rlp item");
        byte[] bytes = asBytes(bin, streamId);
        if (bytes.length == 0)
            return BigInteger.ZERO;

        int firstNoZero = -1;
        // no leading zero
        for (int i = 0; i < bytes.length; i++) {
            if (bytes[i] != 0) {
                firstNoZero = i;
                break;
            }
        }
        if (firstNoZero != 0)
            throw new RuntimeException("leading zero found");
        return new BigInteger(1, bytes);
    }

    public static long asLong(byte[] bin, long streamId) {
        if (streamId < 0)
            throw new RuntimeException("not a rlp item");
        // rlp number cannot starts with zero
        long r = 0;
        int offset = offsetOf(streamId);
        int size = sizeOf(streamId);

        if (size == 0)
            return 0;

        if (size > 8)
            throw new RuntimeException("number too big, cannot convert to long");

        int firstNoZero = -1;

        // no leading zero
        for (int i = 0; i < size; i++) {
            if (bin[offset + i] != 0) {
                firstNoZero = i;
                break;
            }
        }

        if (firstNoZero != 0)
            throw new RuntimeException("leading zero found");

        for (int i = 0; i < size; i++) {
            long b = bin[offset + size - 1 - i] & 0xffL;
            r |= b << (i * 8);
        }
        return r;
    }


    public static byte[] rawOf(byte[] bin, long streamId) {
        int prefixSize = prefixSizeOf(bin, streamId);
        int rawSize = prefixSize + sizeOf(streamId);
        int rawOffset = offsetOf(streamId) - prefixSize;
        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        return r;
    }

    public static byte[] asBytes(byte[] bin, long streamId) {
        if (streamId < 0)
            throw new RuntimeException("not a rlp item");
        return RlpStream.copyFrom(bin, streamId);
    }

    public static int asInt(byte[] bin, long streamId) {
        long l = StreamId.asLong(bin, streamId);
        if (Long.compareUnsigned(l, 0xffffffffL) > 0)
            throw new RuntimeException("number too big, not a integer");
        return (int) l;
    }

    public static short asShort(byte[] bin, long streamId) {
        long l = StreamId.asLong(bin, streamId);
        if (Long.compareUnsigned(l, 0xffffL) > 0)
            throw new RuntimeException("number too big, not a short");
        return (short) l;
    }

    public static byte asByte(byte[] bin, long streamId) {
        long l = StreamId.asLong(bin, streamId);
        if (Long.compareUnsigned(l, 0xffL) > 0)
            throw new RuntimeException("number too big, not a byte");
        return (byte) l;
    }

    public static boolean asBoolean(byte[] bin, long streamId) {
        long l = StreamId.asLong(bin, streamId);
        if (Long.compareUnsigned(l, 1L) > 0)
            throw new RuntimeException("number too big, not a boolean");
        return l != 0;
    }

    public static String asString(byte[] bin, long streamId) {
        return new String(asBytes(bin, streamId), StandardCharsets.UTF_8);
    }

    public static long decodeElement(byte[] bin, int rawOffset, int rawLimit, boolean full) {
        return RlpStream.decodeElement(bin, rawOffset, rawLimit, full);
    }

    public static long iterateList(byte[] bin, long streamId, long prev) {
        return RlpStream.iterateList(bin, streamId, prev);
    }

    public static <T> T as(byte[] bin, long streamId, Class<T> clazz) {
        return RlpStream.decode(bin, streamId, clazz);
    }
}
