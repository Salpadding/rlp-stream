package examples;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import lombok.Getter;
import lombok.ToString;
import org.spongycastle.util.encoders.Hex;

import java.util.Arrays;
import java.util.Collections;
import java.util.List;

// immutable class
@Getter
@RlpProps({"field1", "field2", "field3"})
@ToString
public class ValueClass {
    private final String field1;
    private final String field2;
    private final List<HexBytes> field3;

    // construct List<HexBytes> from HexBytes[], since generic is not supported
    @RlpCreator
    public ValueClass(String field1, String field2, HexBytes[] field3) {
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = Arrays.asList(field3);
    }

    public ValueClass(String field1, String field2, List<HexBytes> field3) {
        this.field1 = field1;
        this.field2 = field2;
        this.field3 = Collections.unmodifiableList(field3);
    }

    public static void main(String[] args) {
        ValueClass v = new ValueClass("field1", "field2", new HexBytes[]{new HexBytes(new byte[]{1, 1 })});
        byte[] encoded = Rlp.encode(v);
        System.out.println(
                Hex.toHexString(encoded)
        );

        ValueClass decoded = Rlp.decode(encoded, ValueClass.class);
        System.out.println(decoded);
    }
}
