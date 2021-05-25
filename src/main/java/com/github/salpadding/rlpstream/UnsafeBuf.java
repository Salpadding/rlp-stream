package com.github.salpadding.rlpstream;

import lombok.Getter;
import lombok.Setter;
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

    @Getter@Setter
    private int size;

    public UnsafeBuf(int cap) {
        this.pointer = unsafe.allocateMemory(cap);
        unsafe.setMemory(pointer, cap, (byte) 0);
        this.cap = cap;
    }

    private void tryExtend() {
        if (size >= cap) {
            int newCap = cap * 2;
            if (newCap < 0) {
                close();
                throw new RuntimeException("memory overflow");
            }
            long newPointer = unsafe.reallocateMemory(pointer, newCap);
            unsafe.setMemory(newPointer + cap, newCap - cap, (byte) 0);
            this.pointer = newPointer;
            this.cap = newCap;
        }
    }

    @Override
    void write(byte b) {
        tryExtend();
        unsafe.putByte(pointer + size, b);
        this.size++;
    }

    void primitiveLeftShift(int offset, int size, int shifts) {
        long srcAddr = this.pointer + offset;
        long dstAddr = this.pointer + offset - shifts;
        unsafe.copyMemory(srcAddr, dstAddr, size);
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
