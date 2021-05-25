package com.github.salpadding.rlpstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import lombok.AllArgsConstructor;
import lombok.NoArgsConstructor;

import java.math.BigInteger;
import java.nio.charset.StandardCharsets;
import java.util.Arrays;

@NoArgsConstructor
@AllArgsConstructor
public class RlpValue implements RlpWritable {
    // bytes / RlpValue[]array, non null
    private Object val;

    @RlpCreator
    public static RlpValue fromRlpStream(byte[] bin, long streamId) {
        if (StreamId.isList(streamId)) {
            RlpList li = StreamId.asList(bin, streamId);
            RlpValue[] val = new RlpValue[li.size()];
            for (int i = 0; i < li.size(); i++) {
                val[i] = li.valueAt(i, RlpValue.class);
            }
            return new RlpValue(val);
        }
        return new RlpValue(StreamId.asBytes(bin, streamId));
    }

    public RlpValue(JsonNode node) {
        if (node.isArray()) {
            RlpValue[] arr = new RlpValue[node.size()];
            for (int i = 0; i < arr.length; i++)
                arr[i] = new RlpValue(node.get(i));
            this.val = arr;
            return;
        }

        if (node.isNumber()) {
            val = Util.asUnsignedByteArray(BigInteger.valueOf(node.longValue()));
            return;
        }

        if (node.isTextual()) {
            String txt = node.textValue();
            if (txt.startsWith("#")) {
                val = Util.asUnsignedByteArray(new BigInteger(txt.substring(1)));
                return;
            }

            val = txt.getBytes(StandardCharsets.UTF_8);
            return;
        }

        throw new RuntimeException("unexpected");
    }

    public boolean isList() {
        return val.getClass().equals(RlpValue[].class);
    }

    private byte[] asByteArray() {
        return (byte[]) val;
    }

    private RlpValue[] asList() {
        return (RlpValue[]) val;
    }

    @Override
    public boolean equals(Object another) {
        if (another == null)
            return !isList() && asByteArray().length == 0;
        if (another.getClass().equals(byte[].class))
            return !isList() && Arrays.equals(this.asByteArray(), (byte[]) another);
        if (another instanceof RlpValue)
            return equals(((RlpValue) another).val);
        if (another.getClass().equals(RlpValue[].class)) {
            if (!isList())
                return false;
            RlpValue[] self = asList();
            return Arrays.equals(self, (RlpValue[]) another);
        }
        throw new RuntimeException("unexpected");
    }

    @Override
    public int writeToBuf(RlpBuffer buffer) {
        if (!isList()) {
            return buffer.writeBytes(asByteArray());
        }
        Object[] args = new Object[asList().length];
        for (int i = 0; i < args.length; i++)
            args[i] = asList()[i];
        return buffer.writeList(args);
    }
}
