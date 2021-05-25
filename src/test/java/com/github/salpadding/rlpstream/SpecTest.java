package com.github.salpadding.rlpstream;

import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

@RunWith(JUnit4.class)
public class SpecTest {
    public static final ObjectMapper OBJECT_MAPPER = new ObjectMapper();

    @Test
    public void test0() throws Exception{
        JsonNode n = OBJECT_MAPPER.readValue(
            TestUtil.readClassPathFile("rlptest.json"),
            JsonNode.class
        );

    }
}
