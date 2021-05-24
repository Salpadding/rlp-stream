package com.github.salpadding.rlpstream;

import java.math.BigInteger;

import static com.github.salpadding.rlpstream.Constants.NULL;

public interface RlpBuffer {
    int writeObject(Object o);

    int writeBytes(byte[] bytes);

    int writeLong(long l);

    int writeString(String s);

    int writeInt(int i);

    int writeShort(short i);

    int writeByte(byte b);

    int writeBigInt(BigInteger b);

    int writeList(Object... objects);

    int writeRaw(byte[] bytes);

    default int writeNull() {
        return writeRaw(NULL);
    }

    int writeElements(byte[]... elements);
}
