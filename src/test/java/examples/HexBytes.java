package examples;

import com.github.salpadding.rlpstream.Rlp;
import com.github.salpadding.rlpstream.RlpBuffer;
import com.github.salpadding.rlpstream.RlpWritable;
import com.github.salpadding.rlpstream.StreamId;
import com.github.salpadding.rlpstream.annotation.RlpCreator;
import lombok.Getter;
import lombok.RequiredArgsConstructor;
import org.spongycastle.util.encoders.Hex;

// represent bytearray in hex encoding
@RequiredArgsConstructor
@Getter
public class HexBytes implements RlpWritable {
    private final byte[] data;

    @RlpCreator
    public static HexBytes fromRlpStream(byte[] bin, long streamId) {
        return new HexBytes(StreamId.asBytes(bin, streamId));
    }

    @Override
    public String toString() {
        return Hex.toHexString(data);
    }

    @Override
    public int writeToBuf(RlpBuffer buffer) {
        return buffer.writeBytes(data);
    }

    public static void main(String[] args) {
        System.out.println(
                Hex.toHexString(Rlp.encode(new HexBytes(new byte[]{0x01, 0x01})))
        );

        System.out.println(
                Rlp.decode(new byte[]{(byte) 0x82, 1, 1}, HexBytes.class)
        );
    }
}
