package com.github.salpadding.rlpstream;

import com.fasterxml.jackson.core.JsonParser;
import com.fasterxml.jackson.databind.JsonNode;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import lombok.SneakyThrows;

import java.io.InputStream;
public class TestUtil {
    public static ObjectMapper MAPPER = new ObjectMapper()
        .enable(SerializationFeature.INDENT_OUTPUT)
        .enable(JsonParser.Feature.ALLOW_COMMENTS);



    @SneakyThrows
    public static byte[] readClassPathFile(String name){
        InputStream in = Util.class.getClassLoader().getResource(name).openStream();
        byte[] all = new byte[in.available()];
        if(in.read(all) != all.length)
            throw new RuntimeException("read failed");
        return all;
    }
    
    public static void readRlpTestJson(JsonNode node) {
        for (JsonNode jsonNode : node) {
            
        }
    }
}
