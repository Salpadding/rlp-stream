package org.tdf.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.Function;

@RequiredArgsConstructor
public class FieldsEncoder<T> implements Function<T, byte[]> {
    private final Method[] getters;
    private final Field[] fields;

    @Override
    @SneakyThrows
    public byte[] apply(T t) {
        byte[][] elements = new byte[fields.length][];
        for (int i = 0; i < fields.length; i++) {
            Method getter = getters[i];
            if (getter != null) {
                elements[i] = Rlp.encode(getter.invoke(t));
            } else {
                elements[i] = Rlp.encode(fields[i].get(t));
            }
        }
        return Rlp.encodeElements(elements);
    }
}
