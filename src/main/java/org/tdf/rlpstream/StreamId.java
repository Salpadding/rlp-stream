package org.tdf.rlpstream;

import static org.tdf.rlpstream.Constants.*;

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
        if(isList(streamId)) {
            int size = sizeOf(streamId);
            if(size < SIZE_THRESHOLD)
                return 1;
            int noZero = 4 - Integer.numberOfLeadingZeros(size) / 8;
            return 1 + noZero;
        }
        int size = sizeOf(streamId);
        if(size == 0)
            return 1;
        int offset = offsetOf(streamId);
        if(size == 1 && (bin[offset] & 0xff) < OFFSET_SHORT_ITEM) {
            return 0;
        }
        if(size < SIZE_THRESHOLD) {
            return 1;
        }
        int noZero = 4 - Integer.numberOfLeadingZeros(size) / 8;
        return 1 + noZero;
    }
}
