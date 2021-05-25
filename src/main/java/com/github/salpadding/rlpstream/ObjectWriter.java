package com.github.salpadding.rlpstream;

interface ObjectWriter {
    int writeToBuf(AbstractBuffer buf, Object o);
}
