package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import lombok.Data;
import org.spongycastle.util.encoders.Hex;

public class Main2 {
    @Data
    @RlpProps({"item0", "item1", "item2"})
    public static class List {
        public List() {}

        @RlpCreator
        public static List fromRlpStream(byte[] bin, long streamId) {
            RlpList li = StreamId.asList(bin, streamId);
            List l = new List();
            l.item0 = li.listAt(0);
            l.item1 = li.listAt(1);
            l.item2 = li.listAt(2);
            return l;
        }

        private RlpList item0;
        private RlpList item1;
        private RlpList item2;

    }

    public static void main(String[] args) throws Exception {
        boolean performanceEnabled = true;

        if (performanceEnabled) {
            String blockRaw = "f8cbf8c7a00000000000000000000000000000000000000000000000000000000000000000a01dcc4de8dec75d7aab85b567b6ccd41ad312451b948a7413f0a142fd40d49347940000000000000000000000000000000000000000a02f4399b08efe68945c1cf90ffe85bbe3ce978959da753f9e649f034015b8817da00000000000000000000000000000000000000000000000000000000000000000834000008080830f4240808080a004994f67dc55b09e814ab7ffc8df3686b4afb2bb53e60eae97ef043fe03fb829c0c0";
            byte[] payload = Hex.decode(blockRaw);

            final int ITERATIONS = 10000000;
            List list = null;
            System.out.println("Starting " + ITERATIONS + " decoding iterations...");

            long start1 = System.currentTimeMillis();
            for (int i = 0; i < ITERATIONS; i++) {
                list = Rlp.decode(payload, List.class);
            }
            long end1 = System.currentTimeMillis();

            System.out.println("Result RLPElement.fromEncoded\t: " + (end1 - start1) + "ms and\t " + (payload.length * ITERATIONS) + " bytes for each resulting object list");
        } else {
            System.out.println("Performance test for RLP.decode() disabled");
        }
    }
}

