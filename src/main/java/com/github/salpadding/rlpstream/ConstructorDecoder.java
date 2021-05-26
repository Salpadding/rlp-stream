package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.exceptions.RlpDecodeException;
import lombok.SneakyThrows;

import java.lang.reflect.Constructor;
import java.util.function.BiFunction;


final class ConstructorDecoder<T> implements BiFunction<byte[], Long, T> {
    private final Constructor<T> constructor;
    private final Class<?>[] parameterTypes;

    public ConstructorDecoder(Constructor<T> constructor) {
        this.constructor = constructor;
        this.parameterTypes = this.constructor.getParameterTypes();
    }

    @Override
    @SneakyThrows
    public T apply(byte[] bin, Long streamId) {
        if (StreamId.isNull(streamId))
            return null;
        Object[] args = new Object[constructor.getParameterCount()];
        long[] children = new long[args.length];

        long j = streamId;
        int c = 0;
        while (true) {
            j = RlpStream.iterateList(bin, streamId, j);
            if (StreamId.isEOF(j))
                break;
            if (c >= children.length)
                throw new RlpDecodeException("constructor arguments length not match to rlp list size");
            children[c++] = j;
        }
        if (c != args.length)
            throw new RlpDecodeException("constructor arguments length not match to rlp list size");

        for (int i = 0; i < args.length; i++) {
            args[i] = RlpStream.decode(bin, children[i], parameterTypes[i]);
        }
        return constructor.newInstance(args);
    }
}
