package com.github.salpadding.rlpstream;

import org.apache.commons.codec.binary.Hex;

public class LongWrapper implements RlpWritable{

    @RlpCreator
    public static LongWrapper fromRlpStream(byte[] bin, long streamId) {
        return new LongWrapper(StreamId.asLong(bin, streamId));
    }

    private final long data;

    public LongWrapper(long data) {
        this.data = data;
    }

    public long getData() {
        return data;
    }

    @Override
    public int writeToBuf(RlpBuffer buffer) {
        return buffer.writeLong(data);
    }

    public static void main(String[] args) throws Exception{
        LongWrapper wrapper = new LongWrapper(Long.MAX_VALUE);
        String hex = Hex.encodeHexString(Rlp.encodeLong(Long.MAX_VALUE));
        String hex1 = Hex.encodeHexString(Rlp.encode(wrapper));

        System.out.println(hex.equals(hex1));

        LongWrapper decoded = Rlp.decode(Hex.decodeHex(hex), LongWrapper.class);
        System.out.println(decoded.data == Long.MAX_VALUE);
    }
}
