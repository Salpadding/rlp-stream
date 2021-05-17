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
        int offset = (int) (streamId & OFFSET_MASK);
        int size = (int) ((streamId & SIZE_MASK) >>> 32);

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

        int offset = (int) (streamId & OFFSET_MASK);
        int size = (int) ((streamId & SIZE_MASK) >>> 32);
        boolean isList = streamId < 0;

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
            return (T) Arrays.copyOfRange(bin, offset, offset + size);
        }
        // String is non-null, since we cannot differ between null empty string and null
        if (clazz == String.class) {
            return (T) new String(Arrays.copyOfRange(bin, offset, offset + size));
        }
        // big integer is non-null, since we cannot differ between zero and null
        if (clazz == BigInteger.class) {
            return (T) asBigInteger(bin, streamId);
        }
        if (!isList && size == 0)
            return null;

        //
        if (!isList)
            throw new RuntimeException("rlp list expected");


        if (clazz.isArray()) {
            List<Long> children = new ArrayList<>();

            long j = streamId & OFFSET_MASK;

            while (true) {
                j = iterateList(bin, streamId, j);
                if (j == EOF)
                    break;
                children.add(j);
            }
            Class<?> elementType = clazz.getComponentType();
            Object res = Array.newInstance(clazz.getComponentType(), children.size());
            for (int i = 0; i < children.size(); i++) {
                Array.set(res, i, decode(bin, children.get(i), elementType));
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
                    throw new RuntimeException("RlpCreator method shoule be static");
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
                ConstructorDecoder<T> de = new ConstructorDecoder<>(con);
                addDecoder(clazz, de);
                return de.apply(bin, streamId);
            }
        }

        if (clazz.isAnnotationPresent(RlpProps.class)) {
            if (noArg == null)
                throw new RuntimeException("expect a no args constructor");

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
     * long i = li & 0xffffffffL;
     * while (true) {
     * i = RLPReader.iterateList(bytes, li, i);
     * if(i == RLPReader.EOF)
     * break;
     * byte[] sub = RLPReader.copyFrom(bytes, i);
     * }
     *
     * @param bin
     * @param listId
     * @param prev
     * @return
     */
    public static long iterateList(byte[] bin, long listId, long prev) {
        int listSize = (int) ((listId & SIZE_MASK) >>> 32);
        int listOffset = (int) (listId & OFFSET_MASK);
        int prevSize = (int) ((prev & SIZE_MASK) >>> 32);
        int prevOffset = (int) (prev & OFFSET_MASK);

        if (listSize + listOffset == prevSize + prevOffset) {
            return EOF;
        }

        long next = decodeElement(bin, (prevSize + prevOffset));
        int nextSize = (int) ((next & SIZE_MASK) >>> 32);
        int nextOffSet = (int) (next & OFFSET_MASK);
        if (nextSize + nextOffSet > listSize + listOffset)
            throw new RuntimeException("invalid list, size overflow");
        return next;
    }


    public static long decodeElement(byte[] bin, int offset) {
        return decodeElement(bin, offset, false);
    }



    /**
     * decode from rlp without memory copy
     * long item = RLPReader.decodeElement(bytes, 0);
     * byte[] it = RLPReader.copyFrom(bytes, item);
     *
     * @param bin
     * @param offset
     * @return
     */
    public static long decodeElement(byte[] bin, int offset, boolean full) {
        int prefix = bin[offset] & 0xff;

        // item = [0x00] ... [0x7f]
        if (prefix < OFFSET_SHORT_ITEM) {
            // assert size
            if (offset + 1 > bin.length)
                throw new RuntimeException("invalid rlp");
            if (full && offset + 1 != bin.length)
                throw new RuntimeException("invalid rlp, unexpected tails");
            return (1L << 32) | Integer.toUnsignedLong(offset);
        }


        // item = [0x80 + length, ...] ... [0xb7, ...]
        // size = 0 or size = 1,2..55
        if (prefix <= OFFSET_LONG_ITEM) {
            int length = prefix - OFFSET_SHORT_ITEM;
            if (offset + 1 + length > bin.length)
                throw new RuntimeException("invalid rlp");
            if (full && offset + 1 + length != bin.length)
                throw new RuntimeException("invalid rlp, unexpected tails");

            return (Integer.toUnsignedLong(length) << 32)
                | Integer.toUnsignedLong(offset + 1);
        }

        // item = [0xb8 + length of length + length, ...] ...[0xbf, ...]
        if (prefix < OFFSET_SHORT_LIST) {
            int lengthBits = prefix - OFFSET_LONG_ITEM; // length of length the encoded bytes
            int len = 0;

            // MSB of length = bin[offset + 1],
            // LSB of length = bin[offset + 1 + lengthBits - 1] = bin[offset + lengthBits]
            // i = 0 is LSB, i = lengthBits - 1 is MSB
            for (int i = 0; i < lengthBits; i++) {
                int b = bin[offset + lengthBits - i] & 0xff;
                len |= b << (i * 8);
            }

            if (offset + 1 + lengthBits + len > bin.length)
                throw new RuntimeException("invalid rlp");
            if (full && offset + 1 + lengthBits + len != bin.length)
                throw new RuntimeException("invalid rlp, unexpected tails");
            return (Integer.toUnsignedLong(len) << 32)
                | Integer.toUnsignedLong(offset + 1 + lengthBits);
        }

        if (prefix <= OFFSET_LONG_LIST) {
            int len = prefix - OFFSET_SHORT_LIST; // length of length the encoded bytes
            // skip preifx
            if (offset + 1 + len > bin.length)
                throw new RuntimeException("invalid rlp");
            if (full && offset + 1 + len != bin.length)
                throw new RuntimeException("invalid rlp, unexpected tails");
            return LIST_SIGN_MASK | (Integer.toUnsignedLong(len) << 32) | (offset + 1);
        }

        int lengthBits = prefix - OFFSET_LONG_LIST; // length of length the encoded list
        int len = 0;
        // MSB of length = bin[offset + 1],
        // LSB of length = bin[offset + 1 + lengthBits - 1] = bin[offset + lengthBits]
        // i = 0 is LSB, i = lengthBits - 1 is MSB
        for (int i = 0; i < lengthBits; i++) {
            int b = bin[offset + lengthBits - i] & 0xff;
            len |= b << (i * 8);
        }

        if (offset + 1 + lengthBits + len > bin.length)
            throw new RuntimeException("invalid rlp");
        if (full && offset + 1 + lengthBits + len != bin.length)
            throw new RuntimeException("invalid rlp, unexpected tails");
        return LIST_SIGN_MASK | (Integer.toUnsignedLong(len) << 32)
            | Integer.toUnsignedLong(offset + 1 + lengthBits);
    }

    public static byte[] copyFrom(byte[] bin, long streamId) {
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
