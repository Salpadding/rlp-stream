package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import com.github.salpadding.rlpstream.exceptions.RlpDecodeException;
import lombok.SneakyThrows;

import java.lang.reflect.*;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.*;
import java.util.function.BiFunction;

import static com.github.salpadding.rlpstream.Constants.MONO_MASK;

// reduce memory copy when parse rlp
// represent rlp element by
// streamid = LIST SIGN | raw size | raw offset
// list sign bit | prefix size(0x00 ~ 0x7f) << 56 | size << 32 | offset, MSG of rlp list will be 1
final class RlpStream {
    private RlpStream() {
    }

    static Map<Class<?>, BiFunction<byte[], Long, ?>> DECODER = new HashMap<>();

    static <T> void addDecoder(Class<T> clazz, BiFunction<byte[], Long, T> decoder) {
        Map<Class<?>, BiFunction<byte[], Long, ?>> m = new HashMap<>(DECODER);
        m.put(clazz, decoder);
        DECODER = m;
    }


    @SuppressWarnings("unchecked")
    @SneakyThrows
    static <T> T decode(byte[] bin, long streamId, Class<T> clazz) {
        BiFunction<byte[], Long, T> decoder = (BiFunction<byte[], Long, T>) DECODER.get(clazz);
        if (decoder != null)
            return decoder.apply(bin, streamId);

        int size = StreamId.sizeOf(streamId);
        boolean isList = StreamId.isList(streamId);

        if (clazz == boolean.class || clazz == Boolean.class) {
            return (T) Boolean.valueOf(StreamId.asBoolean(bin, streamId));
        }
        if (clazz == Byte.class || clazz == byte.class) {
            return (T) Byte.valueOf(StreamId.asByte(bin, streamId));
        }
        if (clazz == Short.class || clazz == short.class) {
            return (T) Short.valueOf(StreamId.asShort(bin, streamId));
        }
        if (clazz == Integer.class || clazz == int.class) {
            return (T) Integer.valueOf(StreamId.asInt(bin, streamId));
        }
        if (clazz == Long.class || clazz == long.class) {
            long l = StreamId.asLong(bin, streamId);
            return (T) Long.valueOf(l);
        }
        if (clazz == byte[].class) {
            return (T) StreamId.asBytes(bin, streamId);
        }
        // String is non-null, since we cannot differ between null empty string and null
        if (clazz == String.class) {
            return (T) new String(StreamId.asBytes(bin, streamId), StandardCharsets.UTF_8);
        }
        // big integer is non-null, since we cannot differ between zero and null
        if (clazz == BigInteger.class) {
            return (T) StreamId.asBigInteger(bin, streamId);
        }


        if (clazz.isArray()) {
            if (!isList && size == 0)
                return null;

            if (!isList)
                throw new RlpDecodeException("rlp list expected when decode as " + clazz.getComponentType() + "[]");
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
                    throw new RlpDecodeException("set array entry failed type " + elementType + " expected while " + o + " received");
                }
            }
            return (T) res;
        }

        // priority
        // 1  decoder in cache

        // 2. constructor with RlpCreator annotated
        // 3. static streamable decoder

        // try to create object by constructor


        Constructor<T> noArg = null;

        Method[] methods = clazz.getMethods();
        for (int i = 0; i < methods.length; i++) {
            Method m = methods[i];
            if (m.isAnnotationPresent(RlpCreator.class)) {
                if (!Modifier.isStatic(m.getModifiers())) {
                    throw new RlpDecodeException("RlpCreator of class " + clazz + " method " + m + " should be static");
                }
                if (
                        m.getParameterCount() != 2 ||
                                !(m.getParameterTypes()[0]).isAssignableFrom(byte[].class) ||
                                (!(m.getParameterTypes()[1]).isAssignableFrom(long.class) && !(m.getParameterTypes()[1]).isAssignableFrom(Long.class)) ||
                                !clazz.isAssignableFrom(m.getReturnType())
                )
                    throw new RlpDecodeException(
                            String.format("static method RlpCreator of class %s method should be %s %s(byte[] bin, long streamId)", clazz.getName(), clazz.getName(), m.getName())
                    );
                StaticMethodDecoder<T> de = new StaticMethodDecoder<>(m);
                addDecoder(clazz, de);
                return de.apply(bin, streamId);
            }
        }

        Constructor<T> creator = null;

        for (int i = 0; i < clazz.getConstructors().length; i++) {
            Constructor<T> con = (Constructor<T>) clazz.getConstructors()[i];
            if (con.getParameterCount() == 0) {
                noArg = con;
            }
            if (con.isAnnotationPresent(RlpCreator.class)) {
                if (!isList)
                    throw new RlpDecodeException("rlp list expected when decode as class " + clazz);
                if (creator == null || creator.getParameterCount() < con.getParameterCount()) {
                    creator = con;
                }
            }
        }

        if (creator != null) {
            ConstructorDecoder<T> de = new ConstructorDecoder<>(creator);
            addDecoder(clazz, de);
            return de.apply(bin, streamId);
        }

        if (clazz.isAnnotationPresent(RlpProps.class)) {
            if (StreamId.isNull(streamId))
                return null;
            if (!isList)
                throw new RlpDecodeException("rlp list expected when decode as class " + clazz);

            if (noArg == null)
                throw new RlpDecodeException("expect a no args constructor for class " + clazz);

            String[] fieldNames = clazz.getAnnotation(RlpProps.class).value();
            Method[] setters = new Method[fieldNames.length];
            Class<?>[] setterTypes = new Class[fieldNames.length];
            Field[] fields = new Field[fieldNames.length];
            Method[] allMethods = clazz.getMethods();

            for (int i = 0; i < fieldNames.length; i++) {
                String fieldName = fieldNames[i];
                String setterName = "set" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
                Field field = clazz.getDeclaredField(fieldName);

                // try to set field by setter
                try {
                    Optional<Method> setter =
                            Arrays.stream(allMethods)
                                    .filter(m -> m.getName().equals(setterName) && m.getParameterCount() == 1)
                                    .findFirst();

                    if (setter.isPresent()) {
                        setters[i] = setter.get();
                        setterTypes[i] = setter.get().getParameterTypes()[0];
                    }
                } catch (Exception ignored) {

                }

                // try to set by assign
                field.setAccessible(true);
                fields[i] = field;
            }

            FieldsDecoder<T> de = new FieldsDecoder<>(noArg, setters, setterTypes, fields);
            addDecoder(clazz, de);
            return de.apply(bin, streamId);
        }
        throw new RlpDecodeException("decode failed");
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
    static long iterateList(byte[] bin, long listStreamId, long prev) {
        int listSize = StreamId.sizeOf(listStreamId);
        int listOffset = StreamId.offsetOf(listStreamId);
        int listLimit = listSize + listOffset;

        int prevSize = prev == listStreamId ? 0 : StreamId.sizeOf(prev);
        int prevOffset = StreamId.offsetOf(prev);

        if (listSize + listOffset == prevSize + prevOffset) {
            return Constants.EOF;
        }
        return decodeElement(bin, (prevSize + prevOffset), listLimit, false);
    }

    static long decodeElement(byte[] bin, int rawOffset, int rawLimit, boolean full) {
        if (rawLimit <= rawOffset)
            throw new RlpDecodeException("empty encoding");
        int prefix = bin[rawOffset] & 0xff;

        if (prefix < Constants.OFFSET_SHORT_ITEM) {
            // prefix size = 0, length = 1
            // assert size
            if (rawOffset + 1 > rawLimit)
                throw new RlpDecodeException("invalid rlp");
            if (full && rawOffset + 1 != rawLimit)
                throw new RlpDecodeException("invalid rlp, unexpected tails");
            // prefix size = 0, actual size = 1, offset = rawOffset
            return (1L << 32) | (Integer.toUnsignedLong(rawOffset)) | MONO_MASK;
        }

        if (prefix <= Constants.OFFSET_LONG_ITEM) {
            // prefix size = 1, length
            int length = prefix - Constants.OFFSET_SHORT_ITEM;
            if (rawOffset + 1 + length > rawLimit)
                throw new RlpDecodeException("invalid rlp");
            if (full && rawOffset + 1 + length != rawLimit)
                throw new RlpDecodeException("invalid rlp, unexpected tails");
            // prefix size = 1, actual size = length, LIST_SIGN = false
            if (length == 1 && (bin[rawOffset + 1] & 0xff) < Constants.OFFSET_SHORT_ITEM)
                throw new RlpDecodeException("invalid rlp, not a canonical short item");
            return (Integer.toUnsignedLong(length) << 32) | (Integer.toUnsignedLong(rawOffset + 1));
        }

        if (prefix < Constants.OFFSET_SHORT_LIST) {
            int lengthBits = prefix - Constants.OFFSET_LONG_ITEM; // length of length the encoded bytes
            int len = (int) StreamId.asLong(bin, rawOffset + 1, lengthBits);
            if (len < 0)
                throw new RlpDecodeException("rlp size overflow");

            if (len < Constants.SIZE_THRESHOLD)
                throw new RlpDecodeException("not a canonical long rlp item, length <= 55");
            if (rawOffset + 1 + lengthBits + len > rawLimit)
                throw new RlpDecodeException("invalid rlp");
            return
                    (Integer.toUnsignedLong(len) << 32) | Integer.toUnsignedLong(1 + lengthBits + rawOffset);
        }

        if (prefix <= Constants.OFFSET_LONG_LIST) {
            int len = prefix - Constants.OFFSET_SHORT_LIST; // length of length the encoded bytes
            // skip preifx
            if (rawOffset + 1 + len > rawLimit)
                throw new RlpDecodeException("invalid rlp");
            if (full && rawOffset + 1 + len != rawLimit)
                throw new RlpDecodeException("invalid rlp, unexpected tails");
            // prefix size = 1, acutal size = len, LIST_SIGN = true
            return (Integer.toUnsignedLong(len) << 32) | Constants.LIST_SIGN_MASK | Integer.toUnsignedLong(rawOffset + 1);
        }
        int lengthBits = prefix - Constants.OFFSET_LONG_LIST; // length of length the encoded list
        int len = (int) StreamId.asLong(bin, rawOffset + 1, lengthBits);
        if (len < 0)
            throw new RlpDecodeException("rlp size overflow");
        if (len < Constants.SIZE_THRESHOLD)
            throw new RlpDecodeException("not a canonical long rlp list, length <= 55");

        if (rawOffset + 1 + lengthBits + len > rawLimit)
            throw new RlpDecodeException("invalid rlp");
        if (full && rawOffset + 1 + lengthBits + len != rawLimit)
            throw new RlpDecodeException("invalid rlp, unexpected tails");
        // prefix size = 1 + lengthBits, acutal size = len, LIST_SIGN = true
        return (Integer.toUnsignedLong(len) << 32) | Constants.LIST_SIGN_MASK | Integer.toUnsignedLong(rawOffset + 1 + lengthBits);
    }

    // get raw without prefix
    static byte[] copyFrom(byte[] bin, long streamId) {
        int size = (int) ((streamId & Constants.SIZE_MASK) >>> 32);
        int offset = (int) (streamId & Constants.OFFSET_MASK);
        if (size == 0)
            return Constants.EMPTY;
        byte[] r = new byte[size];
        System.arraycopy(bin, offset, r, 0, size);
        return r;
    }

}
