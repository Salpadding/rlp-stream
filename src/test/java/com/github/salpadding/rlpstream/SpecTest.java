package com.github.salpadding.rlpstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
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
}
