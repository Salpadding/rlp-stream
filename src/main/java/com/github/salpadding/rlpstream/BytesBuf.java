package com.github.salpadding.rlpstream;


import lombok.SneakyThrows;

import java.io.DataOutput;

public class BytesBuf extends AbstractBuffer {
    public static AbstractBuffer alloc(int size) {
        return new BytesBuf(new byte[size]);
    }

    private BytesBuf(byte[] bin) {
        this.bin = bin;
    }

    private byte[] bin;

    @Override
    public int getSize() {
        return size;
    }

    @Override
    public void setSize(int size) {
        this.size = size;
    }

    private int size;

    @Override
    public void write(byte b) {
        if (size == bin.length) {
            byte[] tmp = new byte[size * 2];
            System.arraycopy(bin, 0, tmp, 0, bin.length);
            this.bin = tmp;
        }
        bin[size++] = b;
    }


    @Override
    public void shift(int offset, int size, int shifts) {
        System.arraycopy(bin, offset, bin, offset - shifts, size);
    }

    @Override
    public byte[] toByteArray() {
        byte[] r = new byte[size];
        System.arraycopy(bin, 0, r, 0, size);
        return r;
    }

    @Override
    @SneakyThrows
    void intoStream(DataOutput output) {
        output.write(this.bin, 0, this.size);
    }
}
