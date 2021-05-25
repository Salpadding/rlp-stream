package com.github.salpadding.rlpstream;

import lombok.NonNull;

import java.io.Closeable;
import java.io.DataOutput;
import java.math.BigInteger;
import java.nio.charset.StandardCharsets;

import static com.github.salpadding.rlpstream.RlpWriter.MAX_PREFIX_SIZE;
import static com.github.salpadding.rlpstream.RlpWriter.writePrefix;

abstract class AbstractBuffer implements RlpBuffer, Closeable {
    abstract void write(byte b);

    // pre allocate list prefix
    void allocateListPrefix() {
        for(int i = 0; i < MAX_PREFIX_SIZE; i++)
            write((byte) 0);
    }

    abstract int getSize();

    abstract void setSize(int size);

    int writeListPrefix(int size) {
        int prevSize = this.getSize();
        setSize(prevSize - size - MAX_PREFIX_SIZE);
        int prefixSize = writePrefix(this, size, false, true);
        int shifts = MAX_PREFIX_SIZE - prefixSize;
        setSize(prevSize);
        leftShift(prevSize - size, size, shifts);
        setSize(prevSize - shifts);
        return prefixSize;
    }

    void leftShift(int offset, int size, int shifts) {
        if(shifts == 0)
            return;
        if(offset < 0)
            throw new RuntimeException("offset should be positive");
        if(offset >= getSize())
            throw new RuntimeException("memory access overflow");
        if(size < 0)
            throw new RuntimeException("size should be positive");
        if(size > getSize() - offset)
            throw new RuntimeException("size too large");
        if(shifts < 0)
            throw new RuntimeException("shift should be non-negative");
        if(shifts > offset)
            throw new RuntimeException("shifts too large");
        primitiveLeftShift(offset, size, shifts);
    }

    abstract void primitiveLeftShift(int offset, int size, int shifts);

    abstract byte[] toByteArray();

    abstract void intoStream(DataOutput output);

    @Override
    public int writeRaw(@NonNull byte[] bytes) {
        return RlpWriter.writeRaw(this, bytes);
    }

    @Override
    public int writeElements(byte[]... elements) {
        return RlpWriter.writeElements(this, elements);
    }

    @Override
    public int writeList(Object... objects) {
        return RlpWriter.writeObject(this, objects);
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
