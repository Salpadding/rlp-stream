package com.github.salpadding.rlpstream;


import lombok.Getter;
import lombok.Setter;
import lombok.SneakyThrows;

import java.io.DataOutput;

final class BytesBuf extends AbstractBuffer {
    static AbstractBuffer alloc(int size) {
        return new BytesBuf(new byte[size]);
    }

    private BytesBuf(byte[] bin) {
        this.bin = bin;
    }

    private byte[] bin;

    @Getter
    @Setter
    private int size;

    private void tryExtend() {
        if (size >= bin.length) {
            byte[] tmp = new byte[size * 2];
            System.arraycopy(bin, 0, tmp, 0, bin.length);
            this.bin = tmp;
        }
    }

    @Override
    public void write(byte b) {
        tryExtend();
        bin[size++] = b;
    }


    public void primitiveLeftShift(int offset, int size, int shifts) {
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
