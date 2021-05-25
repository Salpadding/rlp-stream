package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.annotation.RlpProps;
import org.apache.commons.codec.binary.Hex;

import java.util.Arrays;

@RlpProps({"first", "second"})
public class PojoWritable implements RlpWritable{
    private String first = "hello";
    private String[] second = {"empty0", "empty1", "empty2"};
    private Pojo child = null;

    @Override
    public String toString() {
        return "Pojo{" +
                "first='" + first + '\'' +
                ", second=" + Arrays.toString(second) +
                ", child=" + child +
                '}';
    }

    // a no argument constructor is required
    public PojoWritable() {
    }


    public void setFirst(String first) {
        this.first = first;
    }

    public void setSecond(String[] second) {
        this.second = second;
    }

    public void setChild(Pojo child) {
        this.child = child;
    }

    public static void main(String[] args) {
        byte[] encoded = Rlp.encode(new PojoWritable());
        System.out.println(Hex.encodeHexString(encoded));

        PojoWritable p = Rlp.decode(encoded, PojoWritable.class);

        System.out.println(p);
    }

    @Override
    public int writeToBuf(RlpBuffer buffer) {
        return buffer.writeList(first, second);
    }
}
