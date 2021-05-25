package com.github.salpadding.rlpstream;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

public class RlpValue {
    // bytes / array, non null
    private Object val;

    public boolean isList() {
        return val.getClass().isArray();
    }

    private byte[] asByteArray() {
        return (byte[]) val;
    }

    private BigInteger asBigInteger() {
        return new BigInteger(1, asByteArray());
    }

    private String asString() {
        return new String(asByteArray(), StandardCharsets.UTF_8);
    }

    private Long asLong() {
        return new BigInteger(1, asByteArray()).longValue();
    }

    private RlpValue[] asList() {
        return (RlpValue[]) val;
    }

    @Override
    public boolean equals(Object another) {
        if(another == null)
            return !isList() && asByteArray().length == 0;
        if(another instanceof Long) {
            return !isList() && asLong().equals(another);
        }
        if(another instanceof Integer)
            return !isList() && asLong().equals(Integer.toUnsignedLong((Integer) another));
        if(another instanceof String)
            return !isList() && asString().equals(another);
        if(another instanceof BigInteger)
            return !isList() && asBigInteger().equals(another);
        if(another instanceof RlpValue)
            return equals(((RlpValue) another).val);
        if(another)
    }
}
