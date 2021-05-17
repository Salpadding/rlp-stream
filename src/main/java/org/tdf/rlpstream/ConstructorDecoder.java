package org.tdf.rlpstream;

import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;

import static org.tdf.rlpstream.Constants.EOF;

public class ConstructorDecoder<T> implements BiFunction<byte[], Long, T> {
    private final Constructor<T> constructor;
    private final Class<?>[] parameterTypes;

    public ConstructorDecoder(Constructor<T> constructor) {
        this.constructor = constructor;
        this.parameterTypes = this.constructor.getParameterTypes();
    }

    @Override
    @SneakyThrows
    public T apply(byte[] bin, Long streamId) {
        Object[] args = new Object[constructor.getParameterCount()];
        long[] children = new long[args.length];

        long j = streamId & 0xffffffffL;
        int c = 0;
        while (true) {
            j = RlpStream.iterateList(bin, streamId, j);
            if (j == EOF)
                break;
            if (c >= children.length)
                throw new RuntimeException("constructor arguments length not match to rlp list size");
            children[c++] = j;
        }
        if (c != args.length)
            throw new RuntimeException("constructor arguments length not match to rlp list size");

        for (int i = 0; i < args.length; i++) {
            args[i] = RlpStream.decode(bin, children[i], parameterTypes[i]);
        }
        return constructor.newInstance(args);
    }
}
