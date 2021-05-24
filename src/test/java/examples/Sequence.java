package examples;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.spongycastle.util.encoders.Hex;

// encode/decode object by properties
@RlpProps({"field1", "field2", "field3", "children"})
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Sequence {
    private String field1;
    private String field2;
    private HexBytes field3;
    private Sequence[] children;

    public static void main(String[] args) {
        Sequence seq = new Sequence("foo", "bar", new HexBytes(new byte[]{1, 1}), new Sequence[0]);
        byte[] encoded = Rlp.encode(seq);
        System.out.println(
                Hex.toHexString(encoded)
        );

        Sequence decoded = Rlp.decode(encoded, Sequence.class);
        System.out.println(decoded);
    }
}
