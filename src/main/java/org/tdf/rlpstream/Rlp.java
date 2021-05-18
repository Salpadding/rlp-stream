package org.tdf.rlpstream;

import lombok.SneakyThrows;

import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.Collection;
import java.util.function.Function;

import static org.tdf.rlpstream.Constants.*;

public class Rlp {
    // rlp list encode
    public static byte[] encodeBytes(byte[] srcData) {
        // [0x80]
        if (srcData == null || srcData.length == 0) {
            return new byte[]{(byte) OFFSET_SHORT_ITEM};
            // [0x00]
        }
        if (srcData.length == 1 && (srcData[0] & 0xFF) < OFFSET_SHORT_ITEM) {
            return srcData;
            // [0x80, 0xb7], 0 - 55 bytes
        }
        if (srcData.length < SIZE_THRESHOLD) {
            // length = 8X
            byte length = (byte) (OFFSET_SHORT_ITEM + srcData.length);
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
        data[0] = (byte) (OFFSET_LONG_ITEM + lengthOfLength);

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

    public static byte[] encodeElements(byte[]... elements) {
        int totalLength = 0;

        for (int i = 0; i < elements.length; i++)
            totalLength += elements[i].length;

        byte[] data;
        int copyPos;
        if (totalLength < SIZE_THRESHOLD) {

            data = new byte[1 + totalLength];
            data[0] = (byte) (OFFSET_SHORT_LIST + totalLength);
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
            data[0] = (byte) (OFFSET_LONG_LIST + byteNum);
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
        int leadingZeroBytes = Long.numberOfLeadingZeros(l) / Byte.SIZE;
        byte[] data = new byte[8 - leadingZeroBytes];
        for (int i = data.length - 1; i >= 0; i--) {
            data[i] = (byte) (l & 0xffL);
            l = l >>> 8;
        }
        return encodeBytes(data);
    }

    public static byte[] encodeString(String str) {
        return encodeBytes(str.getBytes(StandardCharsets.UTF_8));
    }

    public static String decodeString(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return new String(RlpStream.asBytes(bin, streamId), StandardCharsets.UTF_8);
    }

    public static long decodeLong(byte[] raw) {
        long id = RlpStream.decodeElement(raw, 0, raw.length, true);
        return RlpStream.asLong(raw, id);
    }

    public static int decodeInt(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFFFFFFFL) > 0)
            throw new RuntimeException("decode as int failed, numeric overflow");
        return (int) l;
    }

    public static short decodeShort(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFFFL) > 0)
            throw new RuntimeException("decode as short failed, numeric overflow");
        return (short) l;
    }

    public static byte decodeByte(byte[] raw) {
        long l = decodeLong(raw);
        if (Long.compareUnsigned(l, 0xFFL) > 0)
            throw new RuntimeException("decode as byte failed, numeric overflow");
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
        return encodeBytes(Util.asUnsignedByteArray(i));
    }

    public static BigInteger decodeBigInteger(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return RlpStream.asBigInteger(bin, streamId);
    }

    public static <T> T decode(byte[] bin, Class<T> clazz) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return RlpStream.decode(bin, streamId, clazz);
    }

    public static byte[] decodeBytes(byte[] bin) {
        long streamId = RlpStream.decodeElement(bin, 0, bin.length, true);
        return RlpStream.asBytes(bin, streamId);
    }

    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static byte[] encode(Object o) {
        if (o == null)
            return NULL;
        if (o instanceof RlpEncodable)
            return ((RlpEncodable) o).getEncoded();

        Function<Object, byte[]> encoder =
            (Function<Object, byte[]>) RlpStream.ENCODER.get(o.getClass());
        if (encoder != null)
            return encoder.apply(o);
        if (o instanceof String) {
            return encodeBytes(((String) o).getBytes(StandardCharsets.UTF_8));
        }
        if (o instanceof Boolean) {
            return ((Boolean) o) ? ONE : NULL;
        }
        if (o instanceof BigInteger)
            return encodeBytes(Util.asUnsignedByteArray((BigInteger) o));
        if (o instanceof byte[])
            return encodeBytes((byte[]) o);
        if (o instanceof Short)
            return encodeLong(Short.toUnsignedLong((Short) o));
        if (o instanceof Byte)
            return encodeLong(Byte.toUnsignedLong((Byte) o));
        if (o instanceof Integer)
            return encodeLong(Integer.toUnsignedLong((Integer) o));
        if (o instanceof Long)
            return encodeLong((Long) o);

        if (o instanceof Collection) {
            byte[][] elements = new byte[((Collection<?>) o).size()][];
            int i = 0;
            for (Object obj : (Collection) o) {
                elements[i++] = encode(obj);
            }
            return encodeElements(elements);
        }

        if (o.getClass().isArray()) {
            byte[][] elements = new byte[Array.getLength(o)][];
            for (int i = 0; i < Array.getLength(o); i++) {
                elements[i] = encode(Array.get(o, i));
            }
            return encodeElements(elements);
        }
        if (o.getClass().isAnnotationPresent(RlpProps.class)) {
            String[] fieldNames = o.getClass().getAnnotation(RlpProps.class).value();
            Method[] getters = new Method[fieldNames.length];
            Field[] fields = new Field[fieldNames.length];

            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                String setterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Field field = o.getClass().getDeclaredField(fieldName);

                // try to set field by setter
                try {
                    Method getter = o.getClass().getMethod(setterName, field.getType());
                    getters[i] = getter;
                } catch (Exception ignored) {

                }

                // try to set by assign
                field.setAccessible(true);
                fields[i] = field;
            }

            FieldsEncoder en = new FieldsEncoder(getters, fields);
            RlpStream.addEncoder(o.getClass(), en);
            return en.apply(o);
        }
        throw new RuntimeException("encode rlp failed, class " + o.getClass() + " is not annotated with RlpProps and not implements RlpEncodable");
    }
}
