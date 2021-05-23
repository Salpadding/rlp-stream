package com.github.salpadding.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Method;
import java.util.function.BiFunction;

@RequiredArgsConstructor
public class StaticMethodDecoder<T> implements BiFunction<byte[], Long, T> {
    private final Method method;

    @Override
    @SneakyThrows
    @SuppressWarnings("unchecked")
    public T apply(byte[] bin, Long streamId) {
        return (T) method.invoke(null, bin, streamId);
    }
}
