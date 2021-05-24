package examples;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpBuffer;
import com.github.salpadding.rlpstream.RlpList;
import com.github.salpadding.rlpstream.StreamId;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpWriter;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spongycastle.util.encoders.Hex;

// customize encoding/decoding
@Data
@AllArgsConstructor
@NoArgsConstructor
public class Custom {
    private String field1;
    private String field2;
    private HexBytes field3;
    private HexBytes encoded;

    @RlpCreator
    public static Custom fromRlpStream(byte[] bin, long streamId) {
        RlpList li = StreamId.asList(bin, streamId);
        return new Custom(li.stringAt(0), li.stringAt(1), li.valueAt(2, HexBytes.class), new HexBytes(li.getEncoded()));
    }

    @RlpWriter
    public static int writeToBuf(Custom obj, RlpBuffer buf) {
        if(obj.encoded != null)
            return buf.writeRaw(obj.encoded.getData());
        return buf.writeList(obj.field1, obj.field2, obj.field3);
    }

    public static void main(String[] args) {
        Custom c = new Custom("foo", "bar", new HexBytes(new byte[]{1, 1}), null);
        byte[] encoded = Rlp.encode(c);
        System.out.println(
                Hex.toHexString(encoded)
        );

        Custom decoded = Rlp.decode(encoded, Custom.class);
        System.out.println(decoded);
    }
}
