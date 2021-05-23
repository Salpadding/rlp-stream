package com.github.salpadding.rlpstream;

import lombok.SneakyThrows;
import sun.misc.Unsafe;

import java.io.Closeable;
import java.io.DataOutput;
import java.lang.reflect.Field;

class UnsafeBuf extends AbstractBuffer implements Closeable {
    public static Unsafe reflectGetUnsafe() {
        try {
            Field field = Unsafe.class.getDeclaredField("theUnsafe");
            field.setAccessible(true);
            return (Unsafe) field.get(null);
        } catch (Exception e) {
            throw new RuntimeException("access unsafe failed");
        }
    }

    private static final Unsafe unsafe = reflectGetUnsafe();
    private static final int ARRAY_OFFSET = unsafe.arrayBaseOffset(byte[].class);

    private long pointer;
    private int cap;
    private int size;

    public UnsafeBuf(int cap) {
        this.pointer = unsafe.allocateMemory(cap);
        unsafe.setMemory(pointer, cap, (byte) 0);
        this.cap = cap;
    }


    @Override
    void write(byte b) {
        if (size == cap) {
            int newCap = cap * 2;
            if (newCap < 0) {
                close();
                throw new RuntimeException("memory overflow");
            }
            long newPointer = unsafe.allocateMemory(newCap);
            unsafe.setMemory(newPointer, newCap, (byte) 0);
            unsafe.copyMemory(this.pointer, newPointer, this.cap);
            unsafe.freeMemory(this.pointer);
            this.pointer = newPointer;
            this.cap = newCap;
        }
        unsafe.putByte(pointer + size, b);
        this.size++;
    }

    @Override
    void leftShift(int offset, int size, int shifts) {
        unsafe.copyMemory(this.pointer + offset, this.pointer + offset - shifts, size);
    }

    @Override
    int getSize() {
        return size;
    }

    @Override
    void setSize(int size) {
        this.size = size;
    }

    @Override
    byte[] toByteArray() {
        byte[] r = new byte[size];
        unsafe.copyMemory(null, pointer, r, ARRAY_OFFSET, size);
        return r;
    }

    @Override
    @SneakyThrows
    void intoStream(DataOutput output) {
        for (int i = 0; i < size; i++)
            output.write(unsafe.getByte(pointer + i) & 0xff);
    }

    @Override
    public void close() {
        if (pointer == 0)
            return;
        unsafe.freeMemory(pointer);
        pointer = 0;
    }
}
