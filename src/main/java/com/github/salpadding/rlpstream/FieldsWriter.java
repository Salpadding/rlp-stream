package com.github.salpadding.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.github.salpadding.rlpstream.RlpWriter.*;

@RequiredArgsConstructor
public class FieldsWriter {
    private final Method[] getters;
    private final Field[] fields;

    @SneakyThrows
    public int writeToBuf(AbstractBuffer buf, Object o) {
        int size = 0;
        int cur = buf.getSize();
        buf.setSize(cur + MAX_PREFIX_SIZE);

        for (int i = 0; i < fields.length; i++) {
            Method getter = getters[i];
            Object oi;
            if (getter != null) {
                oi = getter.invoke(o);
            } else {
                oi = fields[i].get(o);
            }
            size += writeObject(buf, oi);
        }
        buf.setSize(cur);
        int prefix = writePrefix(buf, size, false, true);
        buf.leftShift(cur + MAX_PREFIX_SIZE, size, MAX_PREFIX_SIZE - prefix);
        buf.setSize(cur + prefix + size);
        return size + prefix;
    }
}
