package com.github.salpadding.rlpstream;

import lombok.Getter;
import lombok.SneakyThrows;
import org.apache.commons.codec.binary.Hex;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;

import java.math.BigInteger;
import java.util.Arrays;
import java.util.Collections;

import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RlpTest {
    @Test
    /** encode byte */
    public void testEncodeByte() {

        byte[] expected = {(byte) 0x80};
        byte[] data = Rlp.encodeByte((byte) 0);
        assertArrayEquals(expected, data);

        byte[] expected2 = {(byte) 0x78};
        data = Rlp.encodeByte((byte) 120);
        assertArrayEquals(expected2, data);

        byte[] expected3 = {(byte) 0x7F};
        data = Rlp.encodeByte((byte) 127);
        assertArrayEquals(expected3, data);
    }

    @Test
    /** encode short */
    public void testEncodeShort() {

        byte[] expected = {(byte) 0x80};
        byte[] data = Rlp.encodeShort((byte) 0);
        assertArrayEquals(expected, data);

        byte[] expected2 = {(byte) 0x78};
        data = Rlp.encodeShort((byte) 120);
        assertArrayEquals(expected2, data);

        byte[] expected3 = {(byte) 0x7F};
        data = Rlp.encodeShort((byte) 127);
        assertArrayEquals(expected3, data);

        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = Rlp.encodeShort((short) 30303);
        assertArrayEquals(expected4, data);

        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = Rlp.encodeShort((short) 20202);
        assertArrayEquals(expected5, data);
    }

    @Test
    /** encode int */
    public void testEncodeInt() {

        byte[] expected = {(byte) 0x80};
        byte[] data = Rlp.encodeInt(0);
        assertArrayEquals(expected, data);
        Assert.assertEquals(0, Rlp.decodeInt(data));

        byte[] expected2 = {(byte) 0x78};
        data = Rlp.encodeInt(120);
        assertArrayEquals(expected2, data);
        Assert.assertEquals(120, Rlp.decodeInt(data));

        byte[] expected3 = {(byte) 0x7F};
        data = Rlp.encodeInt(127);
        assertArrayEquals(expected3, data);
        Assert.assertEquals(127, Rlp.decodeInt(data));

        Assert.assertEquals(256, Rlp.decodeInt(Rlp.encodeInt(256)));
        Assert.assertEquals(255, Rlp.decodeInt(Rlp.encodeInt(255)));
        Assert.assertEquals(127, Rlp.decodeInt(Rlp.encodeInt(127)));
        Assert.assertEquals(128, Rlp.decodeInt(Rlp.encodeInt(128)));

        data = Rlp.encodeInt(1024);
        Assert.assertEquals(1024, Rlp.decodeInt(data));

        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = Rlp.encodeInt(30303);
        assertArrayEquals(expected4, data);
        Assert.assertEquals(30303, Rlp.decodeInt(data));

        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = Rlp.encodeInt(20202);
        assertArrayEquals(expected5, data);
        Assert.assertEquals(20202, Rlp.decodeInt(data));

        byte[] expected6 = {(byte) 0x83, 1, 0, 0};
        data = Rlp.encodeInt(65536);
        assertArrayEquals(expected6, data);
        Assert.assertEquals(65536, Rlp.decodeInt(data));

        byte[] expected8 = {(byte) 0x84, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        data = Rlp.encodeInt(Integer.MAX_VALUE);
        assertArrayEquals(expected8, data);
        Assert.assertEquals(Integer.MAX_VALUE, Rlp.decodeInt(data));
    }


    @Test(expected = RuntimeException.class)
    public void incorrectZero() {
        Rlp.decodeInt(new byte[]{0x00});
    }

    @Test(expected = RuntimeException.class)
    public void incorrectZero1() {
        Rlp.decodeInt(new byte[]{0x00, 0x01, 0x00});
    }

    @Test
    /** encode BigInteger */
    public void test6() {

        byte[] expected = new byte[]{(byte) 0x80};
        byte[] data = Rlp.encodeBigInteger(BigInteger.ZERO);
        assertArrayEquals(expected, data);
    }

    @Test
    /** encode string */
    public void test7() {

        byte[] data = Rlp.encodeString("");
        assertArrayEquals(new byte[]{(byte) 0x80}, data);

        byte[] expected = {(byte) 0x90, (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x4A, (byte) 0x20, (byte) 0x43, (byte) 0x6C,
                (byte) 0x69, (byte) 0x65, (byte) 0x6E, (byte) 0x74};

        String test = "EthereumJ Client";
        data = Rlp.encodeString(test);

        assertArrayEquals(expected, data);

        String test2 = "Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++";

        byte[] expected2 = {(byte) 0xAD, (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B};

        data = Rlp.encodeString(test2);
        assertArrayEquals(expected2, data);

        String test3 = "Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++Ethereum(++)/ZeroGox/v0.5.0/ncurses/Linux/g++";

        byte[] expected3 = {(byte) 0xB8, (byte) 0x5A,
                (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B,

                (byte) 0x45, (byte) 0x74, (byte) 0x68, (byte) 0x65, (byte) 0x72, (byte) 0x65,
                (byte) 0x75, (byte) 0x6D, (byte) 0x28, (byte) 0x2B, (byte) 0x2B, (byte) 0x29, (byte) 0x2F,
                (byte) 0x5A, (byte) 0x65, (byte) 0x72, (byte) 0x6F, (byte) 0x47, (byte) 0x6F, (byte) 0x78,
                (byte) 0x2F, (byte) 0x76, (byte) 0x30, (byte) 0x2E, (byte) 0x35, (byte) 0x2E, (byte) 0x30,
                (byte) 0x2F, (byte) 0x6E, (byte) 0x63, (byte) 0x75, (byte) 0x72, (byte) 0x73, (byte) 0x65,
                (byte) 0x73, (byte) 0x2F, (byte) 0x4C, (byte) 0x69, (byte) 0x6E, (byte) 0x75, (byte) 0x78,
                (byte) 0x2F, (byte) 0x67, (byte) 0x2B, (byte) 0x2B};

        data = Rlp.encodeString(test3);
        assertArrayEquals(expected3, data);
    }

    @Test
    /** encode byte array */
    public void test8() {

        String byteArr = "ce73660a06626c1b3fda7b18ef7ba3ce17b6bf604f9541d3c6c654b7ae88b239"
                + "407f659c78f419025d785727ed017b6add21952d7e12007373e321dbc31824ba";

        byte[] byteArray = org.spongycastle.util.encoders.Hex.decode(byteArr);

        String expected = "b840" + byteArr;

        assertEquals(expected, org.spongycastle.util.encoders.Hex.toHexString(Rlp.encodeBytes(byteArray)));
    }

    @Test
    /** encode list */
    public void test9() {
        byte[] actuals = Rlp.encodeElements(Collections.emptyList());
        assertArrayEquals(new byte[]{(byte) 0xc0}, actuals);
    }

    @Test
    /** encode null value */
    public void testEncodeElementNull() {

        byte[] actuals = Rlp.encodeBytes(new byte[0]);
        assertArrayEquals(new byte[]{(byte) 0x80}, actuals);
    }

    @Test
    /** encode single byte 0x00 */
    public void testEncodeElementZero() {

        byte[] actuals = Rlp.encodeBytes(new byte[]{0x00});
        assertArrayEquals(new byte[]{0x00}, actuals);
    }

    @Test
    /** encode single byte 0x01 */
    public void testEncodeElementOne() {

        byte[] actuals = Rlp.encodeBytes(new byte[]{0x01});
        assertArrayEquals(new byte[]{(byte) 0x01}, actuals);
    }

    @Test
    public void encodeDecodeBigInteger() {
        BigInteger expected = new BigInteger("9650128800487972697726795438087510101805200020100629942070155319087371611597658887860952245483247188023303607186148645071838189546969115967896446355306572");
        byte[] encoded = Rlp.encodeBigInteger(expected);
        BigInteger decoded = Rlp.decodeBigInteger(encoded);
        assertNotNull(decoded);
        assertEquals(expected, decoded);
    }


    @Getter
    @RlpProps({"chainId", "address", "poolType", "f", "fm", "r", "rm", "debt", "price", "decimals"})
    public static class PoolData {
        long chainId;
        byte[] address;
        long poolType;
        BigInteger f;
        BigInteger fm;
        BigInteger r;
        BigInteger rm;
        BigInteger debt;
        BigInteger price;
        long decimals;

        public PoolData() {
        }

        public PoolData(long chainId, byte[] address, long poolType, BigInteger f, BigInteger fm, BigInteger r, BigInteger rm, BigInteger debt, BigInteger price, long decimals) {
            this.chainId = chainId;
            this.address = address;
            this.poolType = poolType;
            this.f = f;
            this.fm = fm;
            this.r = r;
            this.rm = rm;
            this.debt = debt;
            this.price = price;
            this.decimals = decimals;
        }
    }

    @SneakyThrows
    @Test
    public void testEncodeDecode() {
        String dt = "f90205f84e820539942c93e2f9f75382717af5de4c105ffb4c6503c5b4038a01605d9ee98627100000891b1ae4d6e2ef50000089f3f20b8dfa69d00000891b1ae4d6e2ef5000008089020281c283b028524012f182053994eb4d5af9f8cbb97f6eb95c21f2ff541b121c7fd1018814d1120d7b160000808080808923b97412d86c4ea13a12f84d8205399444915ecba748148cf6ad6a323af8be52d3befb8f01890ad78ebc5ac6200000890ad78ebc5ac6200000890ad78ebc5ac6200000890ad78ebc5ac62000008089d5c457fd13c65daff712f84e8205399434451604347d45ef4b5cbd790e88d09907b1706c0189055005f0c61448000089055005f0c6144800008915af1d78b58c4000008915af1d78b58c400000808a0df94d0efa177fd1a51812f8508205399438e4f0437edd9bda6f32caae007c985b97bbcff1808a01a46d2eef9995fe00008a010ec78cd35b142c00008a010f0cf064dd592000008a010f0cf064dd5920000080880de0b6b3a764000012f85082053994c7376932e8f7f03d33ffb3ed781d7f28c6c5bbb5808a01c37637845d6d2000008a010ec78cd35b142c00008a0202fefbf2d7c2f000008a010f0cf064dd5920000080880de0b6b3a764000012f83e820539945b536881e3c4fd7639ca0dcaeffcd73daff98523028a021e19e0c9bab24000008a021e19e0c9bab24000008a021e19e0c9bab240000080808012";
        byte[] bytes = Hex.decodeHex(dt);

        PoolData[] datas = Rlp.decode(bytes, PoolData[].class);

        byte[] encoded = Rlp.encode(datas);

        assert Arrays.equals(bytes, encoded);

        String writed = Hex.encodeHexString(RlpWriter.encode(datas));

        System.out.println(writed);
        assert writed.equals(dt);
    }

    @Test
    public void testDecodeOneItem() throws Exception {
        String hex = "000080c180000000000000000000000042699b1104e93abf0008be55f912c2ff";
        byte[] bytes = Hex.decodeHex(hex);
        long el = RlpStream.decodeElement(bytes, 3, bytes.length, false);
//        assertEquals(1, el.size());
//        assertEquals(0, Util.rlpDecodeInt(el.get(0)));
    }

    @Test
    public void testEncodeStream() throws Exception {
        // d680850102030405ce8501020304058701020304050607
        byte[] item0 = new byte[0];
        byte[] item1 = new byte[]{1, 2, 3, 4, 5};
        byte[] item2 = new byte[]{1, 2, 3, 4, 5, 6, 7};
        byte[] encoded = RlpWriter.encode(new Object[]{
                item0, item1,
                new byte[][]{
                        item1, item2
                }
        });

        assert Hex.encodeHexString(encoded).equals("d680850102030405ce8501020304058701020304050607");
    }
}
