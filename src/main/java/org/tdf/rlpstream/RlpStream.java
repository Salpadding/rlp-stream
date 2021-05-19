package org.tdf.rlpstream;

import lombok.SneakyThrows;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.util.*;
import java.util.function.BiFunction;
import java.util.function.Function;

import static org.tdf.rlpstream.Constants.*;

// reduce memory copy when parse rlp
// represent rlp element by
// streamid = LIST SIGN | raw size | raw offset
// list sign bit | prefix size(0x00 ~ 0x7f) << 56 | size << 32 | offset, MSG of rlp list will be 1
public class RlpStream {
    static Map<Class<?>, BiFunction<byte[], Long, ?>> DECODER = new HashMap<>();
    static Map<Class<?>, Function<?, byte[]>> ENCODER = new HashMap<>();

    public static <T> void addDecoder(Class<T> clazz, BiFunction<byte[], Long, T> decoder) {
        Map<Class<?>, BiFunction<byte[], Long, ?>> m = new HashMap<>(DECODER);
        m.put(clazz, decoder);
        DECODER = m;
    }

    public static <T> void addEncoder(Class<T> clazz, Function<T, byte[]> encoder) {
        Map<Class<?>, Function<?, byte[]>> m = new HashMap<>(ENCODER);
        m.put(clazz, encoder);
        ENCODER = m;
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
        int offset = StreamId.offsetOf(streamId);
        int size = StreamId.sizeOf(streamId);

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


    @SuppressWarnings("unchecked")
    @SneakyThrows
    public static <T> T decode(byte[] bin, long streamId, Class<T> clazz) {
        BiFunction<byte[], Long, T> decoder = (BiFunction<byte[], Long, T>) DECODER.get(clazz);
        if (decoder != null)
            return decoder.apply(bin, streamId);
        int size = StreamId.sizeOf(streamId);
        boolean isList = StreamId.isList(streamId);

        if (clazz == boolean.class || clazz == Boolean.class) {
            long l = asLong(bin, streamId);
            if (Long.compareUnsigned(l, 1L) > 0)
                throw new RuntimeException("number too big, not a rlp boolean");
            return (T) Boolean.valueOf(l != 0);
        }
        if (clazz == Byte.class || clazz == byte.class) {
            long l = asLong(bin, streamId);
            if (Long.compareUnsigned(l, 0xffL) > 0)
                throw new RuntimeException("number too big, not a byte");
            return (T) Byte.valueOf((byte) l);
        }
        if (clazz == Short.class || clazz == short.class) {
            long l = asLong(bin, streamId);
            if (Long.compareUnsigned(l, 0xffffL) > 0)
                throw new RuntimeException("number too big, not a short");
            return (T) Short.valueOf((short) l);
        }
        if (clazz == Integer.class || clazz == int.class) {
            long l = asLong(bin, streamId);
            if (Long.compareUnsigned(l, 0xffffffffL) > 0)
                throw new RuntimeException("number too big, not a integer");
            return (T) Integer.valueOf((int) l);
        }
        if (clazz == Long.class || clazz == long.class) {
            long l = asLong(bin, streamId);
            return (T) Long.valueOf(l);
        }
        if (clazz == byte[].class) {
            return (T) asBytes(bin, streamId);
        }
        // String is non-null, since we cannot differ between null empty string and null
        if (clazz == String.class) {
            return (T) new String(asBytes(bin, streamId));
        }
        // big integer is non-null, since we cannot differ between zero and null
        if (clazz == BigInteger.class) {
            return (T) asBigInteger(bin, streamId);
        }


        if (clazz.isArray()) {
            if (!isList && size == 0)
                return null;

            if (!isList)
                throw new RuntimeException("rlp list expected when decode as " + clazz.getComponentType() + "[]");
            List<Long> children = new ArrayList<>();

            long j = streamId;

            while (true) {
                j = iterateList(bin, streamId, j);
                if (StreamId.isEOF(j))
                    break;
                children.add(j);
            }
            Class<?> elementType = clazz.getComponentType();
            Object res = Array.newInstance(elementType, children.size());
            for (int i = 0; i < children.size(); i++) {
                Object o = decode(bin, children.get(i), elementType);
                try {
                    Array.set(res, i, o);
                } catch (Exception e) {
                    throw new RuntimeException("set array entry failed type " + elementType + " expected while " + o + " received");
                }
            }
            return (T) res;
        }

        // priority
        // 1  decoder in cache

        // 2. constructor with RlpCreator annotated
        // 3. clazz

        // try to create object by constructor


        Constructor<T> noArg = null;

        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.isAnnotationPresent(RlpCreator.class)) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    throw new RuntimeException("RlpCreator of class " + clazz + " method " + m + " should be static");
                }
                StaticMethodDecoder<T> de = new StaticMethodDecoder<>(m);
                addDecoder(clazz, de);
                return de.apply(bin, streamId);
            }
        }

        for (int i = 0; i < clazz.getConstructors().length; i++) {
            Constructor<T> con = (Constructor<T>) clazz.getConstructors()[i];
            if (con.getParameterCount() == 0) {
                noArg = con;
            }
            if (con.isAnnotationPresent(RlpCreator.class)) {
                if (!isList)
                    throw new RuntimeException("rlp list expected when decode as class " + clazz);
                ConstructorDecoder<T> de = new ConstructorDecoder<>(con);
                addDecoder(clazz, de);
                return de.apply(bin, streamId);
            }
        }

        if (clazz.isAnnotationPresent(RlpProps.class)) {
            if (StreamId.isNull(streamId))
                return null;
            if (!isList)
                throw new RuntimeException("rlp list expected when decode as class " + clazz);

            if (noArg == null)
                throw new RuntimeException("expect a no args constructor for class " + clazz);

            String[] fieldNames = clazz.getAnnotation(RlpProps.class).value();
            Method[] setters = new Method[fieldNames.length];
            Field[] fields = new Field[fieldNames.length];

            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Field field = clazz.getDeclaredField(fieldName);

                // try to set field by setter
                try {
                    Method setter = clazz.getMethod(setterName, field.getType());
                    setters[i] = setter;
                } catch (Exception ignored) {

                }

                // try to set by assign
                field.setAccessible(true);
                fields[i] = field;
            }

            FieldsDecoder<T> de = new FieldsDecoder<>(noArg, setters, fields);
            addDecoder(clazz, de);
            return de.apply(bin, streamId);
        }
        throw new RuntimeException("decode failed");
    }

    /**
     * iterate over rlp list without mem copy
     * long li = RLPReader.decodeElement(bytes, 0);
     * long i = li;
     * while (true) {
     * i = RLPReader.iterateList(bytes, li, i);
     * if(i == RLPReader.EOF)
     * break;
     * byte[] sub = RLPReader.copyFrom(bytes, i);
     * }
     *
     * @param bin
     * @param listStreamId
     * @param prev
     * @return
     */
    public static long iterateList(byte[] bin, long listStreamId, long prev) {
        int listSize = StreamId.sizeOf(listStreamId);
        int listOffset = StreamId.offsetOf(listStreamId);
        int listLimit = listSize + listOffset;

        int prevSize = prev == listStreamId ? 0 : StreamId.sizeOf(prev);
        int prevOffset = StreamId.offsetOf(prev);

        if (listSize + listOffset == prevSize + prevOffset) {
            return EOF_MASK;
        }
        return decodeElement(bin, (prevSize + prevOffset), listLimit, false);
    }

    public static long decodeElement(byte[] bin, int rawOffset, int rawLimit, boolean full) {
        int prefix = bin[rawOffset] & 0xff;

        if (prefix < OFFSET_SHORT_ITEM) {
            // prefix size = 0, length = 1
            // assert size
            if (rawOffset + 1 > rawLimit)
                throw new RuntimeException("invalid rlp");
            if (full && rawOffset + 1 != rawLimit)
                throw new RuntimeException("invalid rlp, unexpected tails");
            // prefix size = 0, actual size = 1, offset = rawOffset
            return (1L << 32) | (Integer.toUnsignedLong(rawOffset));
        }

        if (prefix <= OFFSET_LONG_ITEM) {
            // prefix size = 1, length
            int length = prefix - OFFSET_SHORT_ITEM;
            if (rawOffset + 1 + length > rawLimit)
                throw new RuntimeException("invalid rlp");
            if (full && rawOffset + 1 + length != rawLimit)
                throw new RuntimeException("invalid rlp, unexpected tails");
            // prefix size = 1, actual size = length, LIST_SIGN = false
            if(length == 1 && (bin[rawOffset + 1] & 0xff) < OFFSET_SHORT_ITEM)
                throw new RuntimeException("invalid rlp, not a canonical short item");
            return (Integer.toUnsignedLong(length) << 32) | (Integer.toUnsignedLong(rawOffset + 1));
        }

        if (prefix < OFFSET_SHORT_LIST) {
            int lengthBits = prefix - OFFSET_LONG_ITEM; // length of length the encoded bytes
            int len = 0;

            // MSB of length = bin[offset + 1],
            // LSB of length = bin[offset + 1 + lengthBits - 1] = bin[offset + lengthBits]
            // i = 0 is LSB, i = lengthBits - 1 is MSB
            for (int i = 0; i < lengthBits; i++) {
                int b = bin[rawOffset + lengthBits - i] & 0xff;
                len |= b << (i * 8);
            }

            if (rawOffset + 1 + lengthBits + len > rawLimit)
                throw new RuntimeException("invalid rlp");
            if (full && rawOffset + 1 + lengthBits + len != rawLimit)
                throw new RuntimeException("invalid rlp, unexpected tails");
            // prefix size = 1 + lengthBits, actual size = len, LIST_SIGN =  false

            if (len < SIZE_THRESHOLD)
                throw new RuntimeException("not a canonical long rlp item, length <= 55");
            return
                (Integer.toUnsignedLong(len) << 32) | Integer.toUnsignedLong(1 + lengthBits + rawOffset);
        }

        if (prefix <= OFFSET_LONG_LIST) {
            int len = prefix - OFFSET_SHORT_LIST; // length of length the encoded bytes
            // skip preifx
            if (rawOffset + 1 + len > rawLimit)
                throw new RuntimeException("invalid rlp");
            if (full && rawOffset + 1 + len != rawLimit)
                throw new RuntimeException("invalid rlp, unexpected tails");
            // prefix size = 1, acutal size = len, LIST_SIGN = true
            return (Integer.toUnsignedLong(len) << 32) | LIST_SIGN_MASK | Integer.toUnsignedLong(rawOffset + 1);
        }
        int lengthBits = prefix - OFFSET_LONG_LIST; // length of length the encoded list
        int len = 0;
        // MSB of length = bin[offset + 1],
        // LSB of length = bin[offset + 1 + lengthBits - 1] = bin[offset + lengthBits]
        // i = 0 is LSB, i = lengthBits - 1 is MSB
        for (int i = 0; i < lengthBits; i++) {
            int b = bin[rawOffset + lengthBits - i] & 0xff;
            len |= b << (i * 8);
        }

        if (len < SIZE_THRESHOLD)
            throw new RuntimeException("not a canonical long rlp list, length <= 55");

        if (rawOffset + 1 + lengthBits + len > rawLimit)
            throw new RuntimeException("invalid rlp");
        if (full && rawOffset + 1 + lengthBits + len != rawLimit)
            throw new RuntimeException("invalid rlp, unexpected tails");
        // prefix size = 1 + lengthBits, acutal size = len, LIST_SIGN = true
        return (Integer.toUnsignedLong(len) << 32) | LIST_SIGN_MASK | Integer.toUnsignedLong(rawOffset + 1 + lengthBits);
    }

    public static byte[] rawOf(byte[] bin, long streamId) {
        int prefixSize = StreamId.prefixSizeOf(bin, streamId);
        int rawSize = prefixSize + StreamId.sizeOf(streamId);
        int rawOffset = StreamId.offsetOf(streamId) - prefixSize;
        byte[] r = new byte[rawSize];
        System.arraycopy(bin, rawOffset, r, 0, rawSize);
        return r;
    }

    // get raw without prefix
    private static byte[] copyFrom(byte[] bin, long streamId) {
        int size = (int) ((streamId & SIZE_MASK) >>> 32);
        int offset = (int) (streamId & OFFSET_MASK);
        if (size == 0)
            return EMPTY;
        byte[] r = new byte[size];
        System.arraycopy(bin, offset, r, 0, size);
        return r;
    }

    public static byte[] asBytes(byte[] bin, long streamId) {
        if (streamId < 0)
            throw new RuntimeException("not a rlp item");
        return copyFrom(bin, streamId);
    }

}
