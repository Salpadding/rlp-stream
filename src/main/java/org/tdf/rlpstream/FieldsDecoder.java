package org.tdf.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.function.BiFunction;

import static org.tdf.rlpstream.Constants.EOF;

@RequiredArgsConstructor
public class FieldsDecoder<T> implements BiFunction<byte[], Long, T> {
    private final Constructor<T> constructor;
    private final Method[] setters;
    private final Field[] fields;

    @Override
    @SneakyThrows
    public T apply(byte[] bin, Long streamId) {
        long[] children = new long[setters.length];
        long j = streamId & 0xffffffffL;
        int c = 0;
        while (true) {
            j = RlpStream.iterateList(bin, streamId, j);
            if (j == EOF)
                break;
            if (c >= children.length) {
                throw new RuntimeException("class arguments length not match to rlp list size");
            }
            children[c++] = j;
        }
        if (c != setters.length)
            throw new RuntimeException("class arguments length not match to rlp list size");

        T dst = constructor.newInstance();
        for (int i = 0; i < setters.length; i++) {
            Method setter = setters[i];
            if (setter != null) {
                setter.invoke(dst, RlpStream.decode(bin, children[i], fields[i].getType()));
            } else {
                fields[i].set(dst, RlpStream.decode(bin, children[i], fields[i].getType()));
            }
        }
        return dst;
    }
}
