package examples;

import com.github.salpadding.rlpstream.*;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spongycastle.util.encoders.Hex;

// customize encoding by overriding RlpWritable
@RlpProps({"field1", "field2", "field3"})
@AllArgsConstructor
@NoArgsConstructor
@Data
public class Pojo implements RlpWritable {
    private String field1;
    private String field2;
    private HexBytes field3;
    private HexBytes encoded;

    @Override
    public int writeToBuf(RlpBuffer buffer) {
        if(encoded != null)
            return buffer.writeRaw(encoded.getData());

        return buffer.writeList(field1, field2, field3);
    }

    @RlpCreator
    public static Pojo fromRlpStream(byte[] bin, long streamId) {
        RlpList li = StreamId.asList(bin, streamId);
        return new Pojo(li.stringAt(0), li.stringAt(1), li.valueAt(2, HexBytes.class), new HexBytes(li.getEncoded()));
    }

    public static void main(String[] args) {
        Pojo p = new Pojo("foo", "bar", new HexBytes(new byte[]{1, 1}), null);
        byte[] encoded = Rlp.encode(p);
        System.out.println(
                Hex.toHexString(encoded)
        );

        Pojo decoded = Rlp.decode(encoded, Pojo.class);
        System.out.println(decoded);
    }
}
