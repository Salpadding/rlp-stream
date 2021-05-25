package com.github.salpadding.rlpstream;

import lombok.RequiredArgsConstructor;
import lombok.SneakyThrows;

import java.lang.reflect.Method;

@RequiredArgsConstructor
class StaticMethodWriter implements ObjectWriter{
    private final Method method;

    @Override
    @SneakyThrows
    public int writeToBuf(AbstractBuffer buf, Object o) {
        return (Integer) method.invoke(null, o, buf);
    }
}
