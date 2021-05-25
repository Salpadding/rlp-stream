package com.github.salpadding.rlpstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.github.salpadding.rlpstream.exceptions.RlpDecodeException;

import lombok.SneakyThrows;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;

import java.util.Iterator;

import static org.junit.Assert.assertArrayEquals;
import static org.junit.Assert.assertEquals;

@RunWith(JUnit4.class)
public class SpecTest {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();
    private static JsonNode INVALID = null;

    @Test
    public void test0() throws Exception{
        JsonNode n = OBJECT_MAPPER.readValue(
            TestUtil.readClassPathFile("rlptest.json"),
            JsonNode.class
        );

        Iterator<String> it = n.fieldNames();

        while (it.hasNext()) {
            String field = it.next();
            System.out.println("testing " + field);
            JsonNode n0 = n.get(field);

            RlpValue v = new RlpValue(n0.get("in"));

            String out = n0.get("out").asText();
            if(out.startsWith("0x"))
                out = out.substring(2);

            byte[] expected = Hex.decode(out);
            // test encode
            assertArrayEquals(Rlp.encode(v), expected);

            // test decode
            RlpValue decoded = Rlp.decode(expected, RlpValue.class);
            assertEquals(decoded, v);
        }
    }

    @SneakyThrows
    private byte[] testInvalid(String test) {
        JsonNode n;

        if(INVALID != null) {
            n = INVALID;
        } else {
            n = OBJECT_MAPPER.readValue(
                TestUtil.readClassPathFile("invalidRLPTest.json"),
                JsonNode.class
            );
            INVALID = n;
        }


        String out = n.get(test).get("out").asText();
        if(out.startsWith("0x"))
            out = out.substring(2);

        return Hex.decode(out);
    }

    @Test(expected = RlpDecodeException.class)
    public void testInt32Overflow() {
        byte[] encoded = testInvalid("int32Overflow");
        Rlp.decodeInt(encoded);
    }

    @Test(expected = RlpDecodeException.class)
    public void testInt32Overflow1() {
        byte[] encoded = testInvalid("int32Overflow");
        Rlp.decode(encoded, Integer.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testInt32Overflow2() {
        byte[] encoded = testInvalid("int32Overflow2");
        Rlp.decodeInt(encoded);
    }

    @Test(expected = RlpDecodeException.class)
    public void testInt32Overflow3() {
        byte[] encoded = testInvalid("int32Overflow2");
        Rlp.decode(encoded, Integer.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testWrongSizeList() {
        byte[] encoded = testInvalid("wrongSizeList");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testWrongSizeList1() {
        byte[] encoded = testInvalid("wrongSizeList");
        Rlp.decodeList(encoded, 0);
    }

    @Test(expected = RlpDecodeException.class)
    public void testWrongSizeList2() {
        byte[] encoded = testInvalid("wrongSizeList2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testWrongSizeList3() {
        byte[] encoded = testInvalid("wrongSizeList");
        Rlp.decodeList(encoded, 0);
    }

    @Test(expected = RlpDecodeException.class)
    public void testIncorrectLengthInArray() {
        byte[] encoded = testInvalid("incorrectLengthInArray");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = Exception.class)
    public void testRandomRLP() {
        byte[] encoded = testInvalid("randomRLP");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte00() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte00");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte001() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte00");
        Rlp.decodeBytes(encoded);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte01() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte01");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte011() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte01");
        Rlp.decodeBytes(encoded);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte7F() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte7F");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testBytesShouldBeSingleByte7F1() {
        byte[] encoded = testInvalid("bytesShouldBeSingleByte7F");
        Rlp.decodeBytes(encoded);
    }

    @Test(expected = RlpDecodeException.class)
    public void testLeadingZerosInLongLengthArray1() {
        byte[] encoded = testInvalid("leadingZerosInLongLengthArray1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testLeadingZerosInLongLengthArray2() {
        byte[] encoded = testInvalid("leadingZerosInLongLengthArray2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testLeadingZerosInLongLengthList1() {
        byte[] encoded = testInvalid("leadingZerosInLongLengthList1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testLeadingZerosInLongLengthList2() {
        byte[] encoded = testInvalid("leadingZerosInLongLengthList2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testNonOptimalLongLengthArray1() {
        byte[] encoded = testInvalid("nonOptimalLongLengthArray1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testNonOptimalLongLengthArray2() {
        byte[] encoded = testInvalid("nonOptimalLongLengthArray2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testNonOptimalLongLengthList1() {
        byte[] encoded = testInvalid("nonOptimalLongLengthList1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testNonOptimalLongLengthList2() {
        byte[] encoded = testInvalid("nonOptimalLongLengthList2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testEmptyEncoding() {
        byte[] encoded = testInvalid("emptyEncoding");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testlessThanShortLengthArray1() {
        byte[] encoded = testInvalid("lessThanShortLengthArray1");
        Rlp.decode(encoded, RlpValue.class);
    }


    @Test(expected = RlpDecodeException.class)
    public void testlessThanShortLengthArray2() {
        byte[] encoded = testInvalid("lessThanShortLengthArray2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testlessThanShortLengthList1() {
        byte[] encoded = testInvalid("lessThanShortLengthList1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testlessThanShortLengthList2() {
        byte[] encoded = testInvalid("lessThanShortLengthList2");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testlessThanLongLengthArray1() {
        byte[] encoded = testInvalid("lessThanLongLengthArray1");
        Rlp.decode(encoded, RlpValue.class);
    }

    @Test(expected = RlpDecodeException.class)
    public void testlessThanLongLengthArray2() {
        byte[] encoded = testInvalid("lessThanLongLengthArray2");
        Rlp.decode(encoded, RlpValue.class);
    }
}
