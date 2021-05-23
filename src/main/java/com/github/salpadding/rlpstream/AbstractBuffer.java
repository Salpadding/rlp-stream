package com.github.salpadding.rlpstream;

import java.io.Closeable;
import java.io.DataOutput;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

abstract class AbstractBuffer implements RlpBuffer, Closeable {
    abstract void write(byte b);

    // shifts > 0 -> right shift
    // shifts < 0 -> left shift
    abstract void shift(int offset, int size, int shifts);

    abstract int getSize();

    abstract void setSize(int size);

    abstract byte[] toByteArray();

    abstract void intoStream(DataOutput output);

    @Override
    public int writeRaw(byte[] bytes) {
        return RlpWriter.writeRaw(this, bytes);
    }

    @Override
    public int writeElements(byte[]... elements) {
        return RlpWriter.writeElements(this, elements);
    }

    @Override
    public int writeList(Object... objects) {
        Object[] objs = objects;
        return RlpWriter.writeObject(this, objs);
    }

    @Override
    public int writeObject(Object o) {
        return RlpWriter.writeObject(this, o);
    }

    @Override
    public int writeBytes(byte[] bytes) {
        return RlpWriter.writeBytes(this, bytes);
    }

    @Override
    public int writeLong(long l) {
        return RlpWriter.writeLong(this, l);
    }

    @Override
    public int writeString(String s) {
        return RlpWriter.writeBytes(this, s.getBytes(StandardCharsets.UTF_8));
    }

    @Override
    public int writeInt(int i) {
        return RlpWriter.writeLong(this, Integer.toUnsignedLong(i));
    }

    @Override
    public int writeShort(short i) {
        return RlpWriter.writeLong(this, Short.toUnsignedLong(i));
    }

    @Override
    public int writeByte(byte b) {
        return RlpWriter.writeLong(this, Byte.toUnsignedLong(b));
    }

    @Override
    public int writeBigInt(BigInteger b) {
        return RlpWriter.writeBigInteger(this, b);
    }

    @Override
    public void close() {

    }
}
