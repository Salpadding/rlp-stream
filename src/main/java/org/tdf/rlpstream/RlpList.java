package org.tdf.rlpstream;

import java.util.ArrayList;
import java.util.List;

public class RlpList {
    private final List<Long> streamIds;
    private final byte[] bin;


    RlpList(byte[] bin, long streamId) {
        if (streamId >= 0)
            throw new RuntimeException("not a rlp list");
        this.streamIds = new ArrayList<>();
        this.bin = bin;
        long i = streamId & 0xffffffffL;
        while (true) {
            i = RlpStream.iterateList(bin, streamId, i);
            if (i == Constants.EOF)
                break;
            streamIds.add(i);
        }
    }

    public int size() {
        return streamIds.size();
    }

    public RlpList listAt(int idx) {
        return new RlpList(this.bin, streamIds.get(idx));
    }

    public byte[] bytesAt(int idx) {
        return RlpStream.asBytes(bin, streamIds.get(idx));
    }

    public boolean isNullAt(int idx) {
        long id = streamIds.get(idx);
        int size = (int) ((id >>> 32) & 0x7fffffffL);
        return (size == 1) && (bin[(int) id] == (byte) 0x80);
    }
}
