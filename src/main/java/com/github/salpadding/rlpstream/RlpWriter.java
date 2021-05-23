package com.github.salpadding.rlpstream;

import lombok.SneakyThrows;

import java.io.DataOutput;
import java.lang.reflect.Array;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

class RlpWriter {
    // write result = LIST_SIGN | prefix size | content size
    static Map<Class<?>, FieldsWriter> OBJECT_WRITERS = new HashMap<>();

    static final int MONO_MASK = 1;
    static final int LIST_MASK = 2;

    // max prefix size = 1(length of length) + 4(length) = 5
    static final int MAX_PREFIX_SIZE = 5;

    public static byte[] encode(Object o) {
        try (AbstractBuffer buf = new UnsafeBuf(256)) {
            writeObject(buf, o);
            return buf.toByteArray();
        }
    }

    public static void encode(Object o, DataOutput out) {
        try (AbstractBuffer buf = new UnsafeBuf(256)) {
            writeObject(buf, o);
            buf.intoStream(out);
        }
    }

    public static int writeLong(AbstractBuffer buf, long l) {
        int leadingZeroBytes = Long.numberOfLeadingZeros(l) / Byte.SIZE;
        int size = 8 - leadingZeroBytes;
        int prefixSize = writePrefix(buf, size, Long.compareUnsigned(l, Integer.toUnsignedLong(Constants.OFFSET_SHORT_ITEM)) < 0, false);
        for (int i = 0; i < size; i++) {
            buf.write((byte) (l >>> (8 * (size - i - 1))));
        }
        return prefixSize + size;
    }

    public static int writeBytes(AbstractBuffer buf, byte[] bytes) {
        return writeBytes(buf, bytes, 0, bytes == null ? 0 : bytes.length);
    }

    public static int writeBytes(AbstractBuffer buf, byte[] bytes, int offset, int size) {
        if (bytes == null || size == 0) {
            buf.write((byte) 0x80);
            return 1;
        }
        int prefixSize = writePrefix(buf, size, size == 1 && Integer.compareUnsigned(bytes[offset] & 0xff, Constants.OFFSET_SHORT_ITEM) < 0, false);
        for (int i = 0; i < size; i++) {
            buf.write(bytes[i + offset]);
        }
        return prefixSize + size;
    }

    @SneakyThrows
    public static int writeBigInteger(AbstractBuffer buf, BigInteger bn) {
        if (bn.signum() < 0)
            throw new RuntimeException("unexpected negative big integer");

        byte[] bytes = bn.toByteArray();

        if (bytes[0] == 0) {
            return writeBytes(buf, bytes, 1, bytes.length - 1);
        }
        return writeBytes(buf, bytes);
    }

    public static int writeRaw(AbstractBuffer buf, byte[] bytes) {
        for (int i = 0; i < bytes.length; i++)
            buf.write(bytes[i]);
        return bytes.length;
    }

    public static int writeElements(AbstractBuffer buf, byte[]... elements) {
        int cur = buf.getSize();
        buf.setSize(cur + MAX_PREFIX_SIZE);
        int size = 0;
        for (int i = 0; i < elements.length; i++) {
            size += writeRaw(buf, elements[i]);
        }
        buf.setSize(cur);
        int prefix = writePrefix(buf, size, false, true);
        buf.leftShift(cur + MAX_PREFIX_SIZE, size, MAX_PREFIX_SIZE - prefix);
        buf.setSize(cur + prefix + size);
        return size + prefix;
    }

    @SneakyThrows
    public static int writeObject(AbstractBuffer buf, Object o) {
        if (o instanceof RlpWritable || o == null) {
            return writeWritable(buf, o == null ? null : ((RlpWritable) o));
        }
        if (o instanceof byte[]) {
            byte[] bytes = (byte[]) o;
            return writeBytes(buf, bytes);
        }
        if (o instanceof Long) {
            return writeLong(buf, (Long) o);
        }
        if (o instanceof Boolean) {
            boolean b = (Boolean) o;
            return writeLong(buf, b ? 1L : 0L);
        }
        if (o instanceof Byte) {
            byte b = (Byte) o;
            return writeLong(buf, Byte.toUnsignedLong(b));
        }
        if (o instanceof Short) {
            short b = (short) o;
            return writeLong(buf, Short.toUnsignedLong(b));
        }
        if (o instanceof Integer) {
            int b = (Integer) o;
            return writeLong(buf, Integer.toUnsignedLong(b));
        }
        if (o instanceof BigInteger) {
            BigInteger b = (BigInteger) o;
            return writeBigInteger(buf, b);
        }
        if (o instanceof String) {
            String s = (String) o;
            return writeBytes(buf, s.getBytes(StandardCharsets.UTF_8));
        }
        if (o.getClass().isArray()) {
            // write empty prefix
            int cur = buf.getSize();
            buf.setSize(cur + MAX_PREFIX_SIZE);
            int size = 0;
            for (int i = 0; i < Array.getLength(o); i++) {
                Object oi = Array.get(o, i);
                size += writeObject(buf, oi);
            }
            buf.setSize(cur);
            int prefix = writePrefix(buf, size, false, true);
            buf.leftShift(cur + MAX_PREFIX_SIZE, size, MAX_PREFIX_SIZE - prefix);
            buf.setSize(cur + prefix + size);
            return size + prefix;
        }
        if (o instanceof Collection) {
            Collection col = (Collection) o;
            int cur = buf.getSize();
            buf.setSize(cur + MAX_PREFIX_SIZE);
            int size = 0;
            for (Object c : col) {
                size += writeObject(buf, c);
            }
            buf.setSize(cur);
            int prefix = writePrefix(buf, size, false, true);
            buf.leftShift(cur + MAX_PREFIX_SIZE, size, MAX_PREFIX_SIZE - prefix);
            buf.setSize(cur + prefix + size);
            return size + prefix;
        }
        FieldsWriter w = getWriter(o.getClass());
        return w.writeToBuf(buf, o);
    }

    @SneakyThrows
    static FieldsWriter getWriter(Class<?> clazz) {
        FieldsWriter w = OBJECT_WRITERS.get(clazz);
        if (w != null)
            return w;
        if (!clazz.isAnnotationPresent(RlpProps.class))
            throw new RuntimeException(clazz + " is not annotated with RlpProps");
        Map<Class<?>, FieldsWriter> ws = new HashMap<>(OBJECT_WRITERS);
        String[] fieldNames = clazz.getAnnotation(RlpProps.class).value();
        Method[] getters = new Method[fieldNames.length];
        Field[] fields = new Field[fieldNames.length];

        for (int i = 0; i < fieldNames.length; i++) {
            String fieldName = fieldNames[i];
            String setterName = "get" + fieldName.substring(0, 1).toUpperCase() + fieldName.substring(1);
            Field field = clazz.getDeclaredField(fieldName);

            // try to set field by setter
            try {
                Method getter = clazz.getMethod(setterName, field.getType());
                getters[i] = getter;
            } catch (Exception ignored) {

            }

            // try to set by assign
            field.setAccessible(true);
            fields[i] = field;
        }
        w = new FieldsWriter(getters, fields);
        ws.put(clazz, w);
        OBJECT_WRITERS = ws;
        return w;
    }


    public static int writePrefix(AbstractBuffer buf, int size, boolean mono, boolean isList) {
        if (size == 0) {
            buf.write(isList ? ((byte) 0xc0) : ((byte) 0x80));
            return 1;
        }

        if (mono) {
            return 0;
        }

        int base0 = isList ? Constants.OFFSET_SHORT_LIST : Constants.OFFSET_SHORT_ITEM;
        int base1 = isList ? Constants.OFFSET_LONG_LIST : Constants.OFFSET_LONG_ITEM;

        if (size < Constants.SIZE_THRESHOLD) {
            byte prefix = (byte) (base0 + size);
            buf.write(prefix);
            return 1;
        }


        int tmpLength = size;
        byte lengthOfLength = 0;
        while (tmpLength != 0) {
            ++lengthOfLength;
            tmpLength = tmpLength >> 8;
        }

        buf.write((byte) (base1 + lengthOfLength));

        for (int i = 0; i < lengthOfLength; i++) {
            buf.write((byte) (size >>> (8 * (lengthOfLength - i - 1))));
        }
        return 1 + lengthOfLength;
    }

    public static int writePrefix(AbstractBuffer buf, int size, int isMonoOrList) {
        return writePrefix(buf, size, (isMonoOrList & MONO_MASK) != 0, (isMonoOrList & LIST_MASK) != 0);
    }


    // write prefix to buf, return the number of bytes writed
    public static int writeWritable(AbstractBuffer buf, RlpWritable writable) {
        if (writable == null) {
            buf.write((byte) 0x80);
            return 1;
        }
        return writable.writeToBuf(buf);
    }
}
