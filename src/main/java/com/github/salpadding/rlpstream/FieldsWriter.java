package com.github.salpadding.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;

import static com.github.salpadding.rlpstream.RlpWriter.*;

@RequiredArgsConstructor
class FieldsWriter implements ObjectWriter{
    private final Method[] getters;
    private final Field[] fields;

    @SneakyThrows
    public int writeToBuf(AbstractBuffer buf, Object o) {
        int size = 0;
        buf.allocateListPrefix();

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
        return size + buf.writeListPrefix(size);
    }
}
