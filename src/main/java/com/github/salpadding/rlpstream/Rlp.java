package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.exceptions.RlpDecodeException;

import java.io.DataOutput;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;

import static com.github.salpadding.rlpstream.Constants.*;


public final class Rlp {
    private Rlp() {
    }

    // rlp list encode
    public static byte[] encodeBytes(byte[] srcData) {
        // [0x80]
        if (srcData == null || srcData.length == 0) {
            return NULL;
            // [0x00]
        }
        if (srcData.length == 1 && (srcData[0] & 0xFF) < Constants.OFFSET_SHORT_ITEM) {
            return srcData;
            // [0x80, 0xb7], 0 - 55 bytes
        }
        if (srcData.length < Constants.SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (Constants.OFFSET_SHORT_ITEM + srcData.length);
            byte[] data = Arrays.copyOf(srcData, srcData.length + 1);
            System.arraycopy(data, 0, data, 1, srcData.length);
            data[0] = length;

            return data;
            // [0xb8, 0xbf], 56+ bytes
        }
        // length of length = BX
        // prefix = [BX, [length]]
        int tmpLength = srcData.length;
        byte lengthOfLength = 0;
        while (tmpLength != 0) {
            ++lengthOfLength;
            tmpLength = tmpLength >> 8;
        }

        // set length Of length at first byte
        byte[] data = new byte[1 + lengthOfLength + srcData.length];
        data[0] = (byte) (Constants.OFFSET_LONG_ITEM + lengthOfLength);

        // copy length after first byte
        tmpLength = srcData.length;
        for (int i = lengthOfLength; i > 0; --i) {
            data[i] = (byte) (tmpLength & 0xFF);
            tmpLength = tmpLength >> 8;
        }

        // at last copy the number bytes after its length
        System.arraycopy(srcData, 0, data, 1 + lengthOfLength, srcData.length);

        return data;
    }

    public static byte[] encodeElements(Collection<? extends byte[]> elements) {
        if (elements.size() == 0)
            return EMPTY_LIST;
        byte[][] array = new byte[elements.size()][];
        int i = 0;
        for (byte[] bytes : elements) {
            array[i++] = bytes;
        }
        return encodeElements(array);
    }

    public static RlpList decodeList(byte[] bin) {
        return new RlpList(bin, 0, bin.length, 0);
    }

    public static RlpList decodeList(byte[] bin, int offset) {
        long streamId = RlpStream.decodeElement(bin, offset, bin.length, false);
        return new RlpList(bin, streamId, 0);
    }

    public static byte[] encodeElements(byte[]... elements) {
        if (elements.length == 0)
            return EMPTY_LIST;

        int totalLength = 0;

        for (int i = 0; i < elements.length; i++)
            totalLength += elements[i].length;

        byte[] data;
        int copyPos;
        if (totalLength < Constants.SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (Constants.OFFSET_SHORT_LIST + totalLength);
            copyPos = 1;
        } else {
            // length of length = BX
            // prefix = [BX, [length]]
            int tmpLength = totalLength;
            byte byteNum = 0;
            while (tmpLength != 0) {
                ++byteNum;
                tmpLength = tmpLength >> 8;
            }
            tmpLength = totalLength;
            byte[] lenBytes = new byte[byteNum];
            for (int i = 0; i < byteNum; ++i) {
                lenBytes[byteNum - 1 - i] = (byte) ((tmpLength >> (8 * i)) & 0xFF);
            }
            // first byte = F7 + bytes.length
            data = new byte[1 + lenBytes.length + totalLength];
            data[0] = (byte) (Constants.OFFSET_LONG_LIST + byteNum);
            System.arraycopy(lenBytes, 0, data, 1, lenBytes.length);

            copyPos = lenBytes.length + 1;
        }
        for (int i = 0; i < elements.length; i++) {
            byte[] element = elements[i];
            System.arraycopy(element, 0, data, copyPos, element.length);
            copyPos += element.length;
        }
        return data;
    }

    public static byte[] encodeLong(long l) {
        if (l == 0)
            return NULL;
        if (l == 1)
            return ONE;
        int leadingZeroBytes = Long.numberOfLeadingZeros(l) / Byte.SIZE;
        byte[] data = new byte[8 - leadingZeroBytes];
        for (int i = data.length - 1; i >= 0; i--) {
            data[i] = (byte) (l & 0xffL);
            l = l >>> 8;
        }
        return encodeBytes(data);
    }

    public static byte[] encodeString(String str) {
        if (str == null)
            return NULL;
        return encodeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeString(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return new String(StreamId.asBytes(bin, streamId), StandardCharsets.UTF_8);
    }

    public static String decodeString(byte[] bin, int offset) {
        long streamId = RlpStream.decodeElement(bin, offset, bin.length, true);
        return new String(StreamId.asBytes(bin, streamId), StandardCharsets.UTF_8);
    }

    public static long decodeLong(byte[] raw) {
        long id = RlpStream.decodeElement(raw, 0, raw.length, true);
        return StreamId.asLong(raw, id);
    }

    public static long decodeLong(byte[] raw, int offset) {
        long id = RlpStream.decodeElement(raw, offset, raw.length, false);
        return StreamId.asLong(raw, id);
    }

    public static int decodeInt(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFFFFFFFL) > 0)
            throw new RlpDecodeException("decode as int failed, numeric overflow");
        return (int) l;
    }

    public static int decodeInt(byte[] raw, int offset) {
        long l = decodeLong(raw, offset);
        if (Long.compareUnsigned(l, 0xFFFFFFFFL) > 0)
            throw new RlpDecodeException("decode as int failed, numeric overflow");
        return (int) l;
    }

    public static short decodeShort(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFFFL) > 0)
            throw new RlpDecodeException("decode as short failed, numeric overflow");
        return (short) l;
    }

    public static short decodeShort(byte[] raw, int offset) {
        long l = decodeLong(raw, offset);
        if (Long.compareUnsigned(l, 0xFFFFL) > 0)
            throw new RlpDecodeException("decode as short failed, numeric overflow");
        return (short) l;
    }

    public static byte decodeByte(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFL) > 0)
            throw new RlpDecodeException("decode as byte failed, numeric overflow");
        return (byte) l;
    }

    public static byte decodeByte(byte[] raw, int offset) {
        long l = decodeLong(raw, offset);
        if (Long.compareUnsigned(l, 0xFFL) > 0)
            throw new RlpDecodeException("decode as byte failed, numeric overflow");
        return (byte) l;
    }

    public static byte[] encodeInt(int i) {
        return encodeLong(Integer.toUnsignedLong(i));
    }

    public static byte[] encodeShort(short i) {
        return encodeLong(Short.toUnsignedLong(i));
    }

    public static byte[] encodeByte(byte i) {
        return encodeLong(Byte.toUnsignedLong(i));
    }

    public static byte[] encodeBigInteger(BigInteger i) {
        if (i == null || i.equals(BigInteger.ZERO))
            return NULL;
        if (i.equals(BigInteger.ONE))
            return ONE;
        return encodeBytes(Util.asUnsignedByteArray(i));
    }

    public static BigInteger decodeBigInteger(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return StreamId.asBigInteger(bin, streamId);
    }

    public static BigInteger decodeBigInteger(byte[] bin, int offset) {
        long streamId = RlpStream.decodeElement(bin, offset, bin.length, false);
        return StreamId.asBigInteger(bin, streamId);
    }

    public static <T> T decode(byte[] bin, int offset, Class<T> clazz) {
        long streamId = RlpStream.decodeElement(bin, offset, bin.length, false);
        return RlpStream.decode(bin, streamId, clazz);
    }

    public static <T> T decode(byte[] bin, Class<T> clazz) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return RlpStream.decode(bin, streamId, clazz);
    }

    public static byte[] decodeBytes(byte[] bin, int offset) {
        long streamId = RlpStream.decodeElement(bin, offset, bin.length, false);
        return StreamId.asBytes(bin, streamId);
    }

    public static byte[] decodeBytes(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return StreamId.asBytes(bin, streamId);
    }

    public static byte[] encode(Object o) {
        return RlpWriter.encode(o);
    }

    public static void encode(Object o, DataOutput out) {
        RlpWriter.encode(o, out);
    }
}
