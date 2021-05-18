package org.tdf.rlpstream;

import lombok.RequiredArgsConstructor;

import java.lang.reflect.Constructor;
import java.lang.reflect.Field;
import java.lang.reflect.Method;
import java.util.Arrays;
import java.util.function.BiFunction;

import static org.tdf.rlpstream.StreamId.isEOF;


@RequiredArgsConstructor
public class FieldsDecoder<T> implements BiFunction<byte[], Long, T> {
    private final Constructor<T> constructor;
    private final Method[] setters;
    private final Field[] fields;

    @Override
    public T apply(byte[] bin, Long streamId) {
        long[] children = new long[setters.length];
        long j = streamId;
        int c = 0;
        while (true) {
            j = RlpStream.
                iterateList(bin, streamId, j);
            if (isEOF(j))
                break;
            if (c >= children.length) {
                throw new RuntimeException("arguments " + Arrays.toString(fields) + " length not match to rlp list size");
            }
            children[c++] = j;
        }
        if (c != setters.length)
            throw new RuntimeException("arguments " + Arrays.toString(fields) + " length not match to rlp list size");

        T dst;
        try {
             dst = constructor.newInstance();
        } catch (Exception e) {
            throw new RuntimeException("unexpected error when create instance by constructor " + constructor);
        }
        for (int i = 0; i < setters.length; i++) {
            Method setter = setters[i];
            if (setter != null) {
                try {
                    setter.invoke(dst, RlpStream.decode(bin, children[i], fields[i].getType()));
                } catch (Exception e){
                    throw new RuntimeException("unexpected error when invoke setter " + setter);
                }
            } else {
                try {
                    fields[i].set(dst, RlpStream.decode(bin, children[i], fields[i].getType()));
                } catch (Exception e) {
                    throw new RuntimeException("unexpected error when set field " + fields[i]);
                }
            }
        }
        return dst;
    }
}
