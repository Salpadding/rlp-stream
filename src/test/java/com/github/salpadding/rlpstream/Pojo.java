package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.annotation.RlpProps;
import org.apache.commons.codec.binary.Hex;

import java.io.DataOutput;
import java.io.DataOutputStream;
import java.io.FileOutputStream;
import java.util.Arrays;

@RlpProps({"first", "second", "child"})
class Pojo {
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
    public Pojo() {
    }

    public String getFirst() {
        return first;
    }

    public String[] getSecond() {
        return second;
    }

    public Pojo getChild() {
        return child;
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

    public static void main(String[] args) throws Exception{
        byte[] encoded = Rlp.encode(new Pojo());
        System.out.println(Hex.encodeHexString(encoded));

        Pojo p = Rlp.decode(encoded, Pojo.class);

        System.out.println(p);

        DataOutput o = new DataOutputStream(new FileOutputStream("data"));
        Rlp.encode(p, o);
    }
}
