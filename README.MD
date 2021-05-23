# rlp stream encoding/decoding

## Encoding

1. encode item(long, bytes, string ...)


```java
import com.github.salpadding.rlpstream.Rlp;


public class Main{
    public static void main(String[] args) {
        
        // encode number, string...
        byte[] encoded = Rlp.encodeLong(1L);
        byte[] encoded = Rlp.encodeString("hello world");
        
        // Rlp.encodeBigInteger, encodeByte, encodeShort, encodeInt
    }
}

```

2. use @RlpProps to encode/decode pojo

```java
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

    public static void main(String[] args) {
        byte[] encoded = Rlp.encode(new Pojo());
        System.out.println(Hex.encodeHexString(encoded));

        Pojo p = Rlp.decode(encoded, Pojo.class);

        System.out.println(p);
    }
}
```

3. override Pojo encoding

customize your encoding scheme, i.e. ignore "child" field of Pojo when encoding/decoding

```java
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
```

or encoding/decoding wrapper class

use @RlpCreator to create your Wrapper from streamId
write long value to RlpBuffer

```java
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

```

4. write big object to outputstream


```java
class Main {
    public static void main(String[]args){
        DataOutput o = new DataOutputStream(new FileOutputStream("data"));
        Rlp.encode(p, o);
    }  
}
```


