package com.github.salpadding.rlpstream;

import com.github.salpadding.rlpstream.annotation.RlpCreator;
import com.github.salpadding.rlpstream.annotation.RlpProps;
import lombok.Getter;
import lombok.SneakyThrows;
import org.junit.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.junit.runners.JUnit4;
import org.spongycastle.util.encoders.Hex;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.ObjectOutputStream;
import java.io.Serializable;
import java.math.BigInteger;
import java.util.*;

import static com.github.salpadding.rlpstream.Constants.EMPTY_LIST;
import static com.github.salpadding.rlpstream.Rlp.*;
import static org.junit.Assert.*;

@RunWith(JUnit4.class)
public class RlpTest {
    public static int byteArrayToInt(byte[] b) {
        if (b == null || b.length == 0)
            return 0;
        return new BigInteger(1, b).intValue();
    }

    @Test
    public void test2() throws Exception {

        String peersPacket = "F8 4E 11 F8 4B C5 36 81 " +
            "CC 0A 29 82 76 5F B8 40 D8 D6 0C 25 80 FA 79 5C " +
            "FC 03 13 EF DE BA 86 9D 21 94 E7 9E 7C B2 B5 22 " +
            "F7 82 FF A0 39 2C BB AB 8D 1B AC 30 12 08 B1 37 " +
            "E0 DE 49 98 33 4F 3B CF 73 FA 11 7E F2 13 F8 74 " +
            "17 08 9F EA F8 4C 21 B0";

        byte[] payload = Hex.decode(peersPacket);
        int oneInt = decodeInt(payload, 11);

        assertEquals(oneInt, 30303);
    }

    @Test
    public void testEncodeBoolean() {
        byte[] expected = {(byte) 0x80};
        byte[] data = Rlp.encode(false);
        assertArrayEquals(expected, data);
        assertFalse(Rlp.decode(expected, Boolean.class));
        assertArrayEquals(new byte[]{0x01}, Rlp.encode(true));
        assertTrue(Rlp.decode(new byte[]{0x01}, Boolean.class));
    }

    @Test(expected = RuntimeException.class)
    public void testDecodeBoolOverflow() {
        Rlp.decode(new byte[]{0x02}, Boolean.class);
    }

    @Test
    /** encode byte */
    public void testEncodeByte() {

        byte[] expected = {(byte) 0x80};
        byte[] data = Rlp.encodeByte((byte) 0);
        assertArrayEquals(expected, data);
        assertArrayEquals(expected, Rlp.encode(Byte.valueOf((byte) 0)));
        assertEquals((byte) 0, Rlp.decodeByte(expected));
        assertEquals((byte) 0, (byte) Rlp.decode(expected, Byte.class));

        byte[] expected2 = {(byte) 0x78};
        data = Rlp.encodeByte((byte) 120);
        assertArrayEquals(expected2, data);
        assertArrayEquals(expected2, Rlp.encode(Byte.valueOf((byte) 120)));


        byte[] expected3 = {(byte) 0x7F};
        data = Rlp.encodeByte((byte) 127);
        assertArrayEquals(expected3, data);
        assertArrayEquals(expected3, Rlp.encode(Byte.valueOf((byte) 127)));
        assertEquals((byte)127, Rlp.decodeByte(expected3));
        assertEquals((byte)127, (byte) Rlp.decode(expected3, Byte.class));
    }

    @Test
    /** encode short */
    public void testEncodeShort() {

        byte[] expected = {(byte) 0x80};
        byte[] data = encodeShort((byte) 0);
        assertArrayEquals(expected, data);
        assertArrayEquals(expected, Rlp.encode(Short.valueOf((short) 0)));
        assertEquals((short) 0, Rlp.decodeShort(expected));
        assertEquals((short) 0, (short) Rlp.decode(expected, Short.class));


        byte[] expected2 = {(byte) 0x78};
        data = encodeShort((short) 120);
        assertArrayEquals(expected2, data);
        assertArrayEquals(expected2, Rlp.encode(Short.valueOf((short) 120)));
        assertEquals((short) 120, Rlp.decodeShort(expected2));
        assertEquals((short) 120, (short) Rlp.decode(expected2, Short.class));



        byte[] expected3 = {(byte) 0x7F};
        data = encodeShort((byte) 127);
        assertArrayEquals(expected3, data);
        assertArrayEquals(expected3, Rlp.encode(Short.valueOf((short) 127)));
        assertEquals((short) 127, Rlp.decodeShort(expected3));
        assertEquals((short) 127, (short) Rlp.decode(expected3, Short.class));


        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = encodeShort((short) 30303);
        assertArrayEquals(expected4, data);
        assertArrayEquals(expected4, Rlp.encode(Short.valueOf((short) 30303)));
        assertEquals((short) 30303, Rlp.decodeShort(expected4));
        assertEquals((short) 30303, (short) Rlp.decode(expected4, Short.class));


        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = encodeShort((short) 20202);
        assertArrayEquals(expected5, data);
        assertArrayEquals(expected5, Rlp.encode(Short.valueOf((short) 20202)));
        assertEquals((short) 20202, Rlp.decodeShort(expected5));
        assertEquals((short) 20202, (short) Rlp.decode(expected5, Short.class));


        byte[] expected6 = {(byte) 0x82, (byte) 0xff, (byte) 0xff};
        data = encodeShort((short) 0xffff);
        assertArrayEquals(expected6, data);
        assertArrayEquals(expected6, Rlp.encode(Short.valueOf((short) 0xffff)));
        assertEquals((short) 0xffff, Rlp.decodeShort(expected6));
        assertEquals((short) 0xffff, (short) Rlp.decode(expected6, Short.class));
    }

    @Test(expected = RuntimeException.class)
    public void testDecodeShortOverflow() {
        Rlp.decodeShort(new byte[] {(byte) 0x83 ,0x01, 0x00, 0x00});
    }

    @Test
    /** encode int */
    public void testEncodeInt() {

        byte[] expected = {(byte) 0x80};
        byte[] data = encodeInt(0);
        assertArrayEquals(expected, data);
        assertArrayEquals(expected, Rlp.encode(Integer.valueOf(0)));
        Assert.assertEquals(0, decodeInt(data));
        Assert.assertEquals(0, (int) Rlp.decode(data, Integer.class));

        byte[] expected2 = {(byte) 0x78};
        data = encodeInt(120);
        assertArrayEquals(expected2, data);
        assertArrayEquals(expected2, Rlp.encode(Integer.valueOf(120)));
        Assert.assertEquals(120, decodeInt(data));
        Assert.assertEquals(120, (int) Rlp.decode(data, Integer.class));

        byte[] expected3 = {(byte) 0x7F};
        data = encodeInt(127);
        assertArrayEquals(expected3, data);
        assertArrayEquals(expected3, Rlp.encode(Integer.valueOf(127)));
        Assert.assertEquals(127, decodeInt(data));
        Assert.assertEquals(127, (int) Rlp.decode(data, Integer.class));

        Assert.assertEquals(256, decodeInt(encodeInt(256)));
        Assert.assertEquals(255, decodeInt(encodeInt(255)));
        Assert.assertEquals(127, decodeInt(encodeInt(127)));
        Assert.assertEquals(128, decodeInt(encodeInt(128)));
        Assert.assertEquals(0xFFFFFFFF, decodeInt(encodeInt(0xFFFFFFFF)));


        Assert.assertEquals(256, decodeInt(Rlp.encode(256)));
        Assert.assertEquals(255, decodeInt(Rlp.encode(255)));
        Assert.assertEquals(127, decodeInt(Rlp.encode(127)));
        Assert.assertEquals(128, decodeInt(Rlp.encode(128)));
        Assert.assertEquals(0xFFFFFFFF, decodeInt(Rlp.encode(0xFFFFFFFF)));

        Assert.assertEquals(256, (int) Rlp.decode(Rlp.encode(Integer.valueOf(256)), Integer.class));
        Assert.assertEquals(255, (int) Rlp.decode(Rlp.encode(Integer.valueOf(255)), Integer.class));
        Assert.assertEquals(127, (int) Rlp.decode(Rlp.encode(Integer.valueOf(127)), Integer.class));
        Assert.assertEquals(128, (int) Rlp.decode(Rlp.encode(Integer.valueOf(128)), Integer.class));
        Assert.assertEquals(0xFFFFFFFF, (int) Rlp.decode(Rlp.encode(Integer.valueOf(0xFFFFFFFF)), Integer.class));


        data = encodeInt(1024);
        Assert.assertEquals(1024, decodeInt(data));
        Assert.assertEquals(1024, (int) Rlp.decode(data, Integer.class));

        byte[] expected4 = {(byte) 0x82, (byte) 0x76, (byte) 0x5F};
        data = encodeInt(30303);
        assertArrayEquals(expected4, data);
        assertArrayEquals(expected4, Rlp.encode(30303));
        Assert.assertEquals(30303, decodeInt(data));
        Assert.assertEquals(30303, (int) Rlp.decode(data, Integer.class));


        byte[] expected5 = {(byte) 0x82, (byte) 0x4E, (byte) 0xEA};
        data = encodeInt(20202);
        assertArrayEquals(expected5, data);
        assertArrayEquals(expected5, Rlp.encode(20202));
        Assert.assertEquals(20202, decodeInt(data));
        Assert.assertEquals(20202, (int) Rlp.decode(data, Integer.class));


        byte[] expected6 = {(byte) 0x83, 1, 0, 0};
        data = encodeInt(65536);
        assertArrayEquals(expected6, data);
        assertArrayEquals(expected6, Rlp.encode(65536));
        Assert.assertEquals(65536, decodeInt(data));
        Assert.assertEquals(65536, (int) Rlp.decode(data, Integer.class));

        byte[] expected8 = {(byte) 0x84, (byte) 0x7F, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF};
        data = encodeInt(Integer.MAX_VALUE);
        assertArrayEquals(expected8, data);
        assertArrayEquals(expected8, Rlp.encode(Integer.MAX_VALUE));
        Assert.assertEquals(Integer.MAX_VALUE, decodeInt(data));
        Assert.assertEquals(Integer.MAX_VALUE, (int) Rlp.decode(data, Integer.class));
    }


    @Test(expected = RuntimeException.class)
    public void incorrectZero() {
        decodeInt(new byte[]{0x00});
    }

    @Test(expected = RuntimeException.class)
    public void incorrectZero1() {
        decodeInt(new byte[]{0x00, 0x01, 0x00});
    }

    @Test
    /** encode BigInteger */
    public void test6() {

        byte[] expected = new byte[]{(byte) 0x80};

        byte[] data = Rlp.encodeBigInteger(BigInteger.ZERO);
        assertArrayEquals(expected, data);
        assertArrayEquals(expected, Rlp.encode(BigInteger.ZERO));
        assertArrayEquals(Constants.ONE, Rlp.encode(BigInteger.ONE));
        assertArrayEquals(Constants.ONE, Rlp.encodeBigInteger(BigInteger.ONE));
    }

    @Test
    public void testMaxNumerics() {
        int expected1 = Integer.MAX_VALUE;
        assertEquals(expected1, decodeInt(encodeInt(expected1), 0));
        assertEquals(expected1, (int) Rlp.decode(Rlp.encode(expected1), 0, Integer.class));

        short expected2 = Short.MAX_VALUE;
        assertEquals(expected2, decodeShort(encodeShort(expected2), 0));
        assertEquals(expected2, (short) Rlp.decode(Rlp.encode(expected2), 0, Short.class));

        long expected3 = Long.MAX_VALUE;
        assertEquals(expected3, decodeLong(encodeBigInteger(BigInteger.valueOf(expected3)), 0));
        assertEquals(expected3, (long) Rlp.decode(Rlp.encode(BigInteger.valueOf(expected3)), 0, Long.class));

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
        assertArrayEquals(expected, Rlp.encode(test));


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
        assertArrayEquals(expected2, Rlp.encode(test2));

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
        assertArrayEquals(expected3, Rlp.encode(test3));
    }

    @Test
    /** encode byte array */
    public void test8() {

        String byteArr = "ce73660a06626c1b3fda7b18ef7ba3ce17b6bf604f9541d3c6c654b7ae88b239"
                + "407f659c78f419025d785727ed017b6add21952d7e12007373e321dbc31824ba";

        byte[] byteArray = Hex.decode(byteArr);

        String expected = "b840" + byteArr;

        assertEquals(expected, Hex.toHexString(Rlp.encodeBytes(byteArray)));
        assertEquals(expected, Hex.toHexString(Rlp.encode(byteArray)));
    }

    @Test
    /** encode list */
    public void test9() {
        byte[] actuals = Rlp.encodeElements(Collections.emptyList());
        assertArrayEquals(new byte[]{(byte) 0xc0}, actuals);
        assertArrayEquals(new byte[]{(byte) 0xc0}, Rlp.encode(Collections.emptyList()));
        assertArrayEquals(new byte[]{(byte) 0xc0}, Rlp.encode(new byte[][]{}));
        assertArrayEquals(new byte[]{(byte) 0xc0}, Rlp.encodeElements());
        assertArrayEquals(new byte[]{(byte) 0xc0}, Rlp.encode(new Object[]{}));
    }

    @Test
    /** encode null value */
    public void testEncodeElementNull() {

        byte[] actuals = Rlp.encodeBytes(new byte[0]);
        assertArrayEquals(new byte[]{(byte) 0x80}, actuals);
        assertArrayEquals(new byte[]{(byte) 0x80}, Rlp.encode(null));
        assertArrayEquals(new byte[]{(byte) 0x80}, Rlp.encode(Constants.EMPTY));
        assertArrayEquals(new byte[]{(byte) 0x80}, Rlp.encodeBytes(null));
        assertArrayEquals(new byte[]{(byte) 0x80}, Rlp.encodeBytes(Constants.EMPTY));
        assertArrayEquals(new byte[]{(byte) 0x80}, Rlp.encodeBigInteger(null));
    }

    @Test
    /** encode single byte 0x00 */
    public void testEncodeElementZero() {

        byte[] actuals = Rlp.encodeBytes(new byte[]{0x00});
        assertArrayEquals(new byte[]{0x00}, actuals);
        assertArrayEquals(new byte[]{0x00}, Rlp.encode(new byte[]{0x00}));
    }

    @Test
    /** encode single byte 0x01 */
    public void testEncodeElementOne() {
        byte[] actuals = Rlp.encodeBytes(new byte[]{0x01});
        assertArrayEquals(new byte[]{(byte) 0x01}, actuals);
        assertArrayEquals(new byte[]{(byte) 0x01}, Rlp.encode(Constants.ONE));
    }

    @Test
    public void encodeDecodeBigInteger() {
        BigInteger expected = new BigInteger("9650128800487972697726795438087510101805200020100629942070155319087371611597658887860952245483247188023303607186148645071838189546969115967896446355306572");
        byte[] encoded = Rlp.encodeBigInteger(expected);
        BigInteger decoded = Rlp.decodeBigInteger(encoded);
        assertNotNull(decoded);
        assertEquals(expected, decoded);
        assertEquals(expected, Rlp.decode(encoded, BigInteger.class));
    }

    @Test
    public void testEncodeEmptyString() {
        String test = "";
        String expected = "80";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        String decodeResult = decode(encoderesult, 0, String.class);
        assertEquals(test, decodeResult);
        assertEquals(test, Rlp.decodeString(encoderesult));

    }

    @Test
    public void testEncodeShortString() {
        String test = "dog";
        String expected = "83646f67";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        assertEquals(test, bytesToAscii(decodeResult));
    }

    @Test
    public void testEncodeSingleCharacter() {
        String test = "d";
        String expected = "64";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        assertEquals(test, bytesToAscii(decodeResult));
    }

    @Test
    public void testEncodeLongString() {
        String test = "Lorem ipsum dolor sit amet, consectetur adipisicing elit"; // length = 56
        String expected = "b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        assertEquals(test, bytesToAscii(decodeResult));
    }

    @Test
    public void testEncodeZero() {
        Integer test = 0;
        String expected = "80";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        String decodeResult = decode(encoderesult, 0, String.class);
        assertEquals("", decodeResult);
    }

    @Test
    public void testEncodeSmallInteger() {
        Integer test = 15;
        String expected = "0f";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        int result = byteArrayToInt(decodeResult);
        assertEquals(test, Integer.valueOf(result));
    }

    @Test
    public void testEncodeMediumInteger() {
        Integer test = 1000;
        String expected = "8203e8";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        int result = byteArrayToInt(decodeResult);
        assertEquals(test, Integer.valueOf(result));

        test = 1024;
        expected = "820400";
        encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        decodeResult = decode(encoderesult, 0, byte[].class);
        result = byteArrayToInt(decodeResult);
        assertEquals(test, Integer.valueOf(result));
    }

    @Test
    public void testEncodeBigInteger() {
        BigInteger test = new BigInteger("100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f", 16);
        String expected = "a0100102030405060708090a0b0c0d0e0f101112131415161718191a1b1c1d1e1f";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        byte[] decodeResult = decode(encoderesult, 0, byte[].class);
        assertEquals(test, new BigInteger(1, decodeResult));
    }

    @Test
    public void TestEncodeEmptyList() {
        Object[] test = new Object[0];
        String expected = "c0";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        Object[] decodeResult = decode(encoderesult, 0, Object[].class);
        assertTrue(decodeResult.length == 0);
    }

    @Test
    public void testEncodeShortStringList() {
        String[] test = new String[]{"cat", "dog"};
        String expected = "c88363617483646f67";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        String[] decodeResult = decode(encoderesult, 0, String[].class);
        assertEquals("cat", decodeResult[0]);
        assertEquals("dog", decodeResult[1]);

        test = new String[]{"dog", "god", "cat"};
        expected = "cc83646f6783676f6483636174";
        encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        decodeResult = decode(encoderesult, 0, String[].class);
        assertEquals("dog", decodeResult[0]);
        assertEquals("god", decodeResult[1]);
        assertEquals("cat", decodeResult[2]);
    }

    @Test
    public void testEncodeLongStringList() {
        String element1 = "cat";
        String element2 = "Lorem ipsum dolor sit amet, consectetur adipisicing elit";
        String[] test = new String[]{element1, element2};
        String expected = "f83e83636174b8384c6f72656d20697073756d20646f6c6f722073697420616d65742c20636f6e7365637465747572206164697069736963696e6720656c6974";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        String[] decodeResult = decode(encoderesult, 0, String[].class);
        assertEquals(element1, decodeResult[0]);
        assertEquals(element2, decodeResult[1]);
    }

    @Test
    public void testEncodeEmptyListOfList() {
        // list = [ [ [], [] ], [] ],
        Object[] test = new Object[]{new Object[]{new Object[]{}, new Object[]{}}, new Object[]{}};
        String expected = "c4c2c0c0c0";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));

        RlpList decodeResult = decodeList(encoderesult);
        assertTrue(decodeResult.size() == 2);
        assertTrue(decodeResult.listAt(0).size() == 2);
        assertTrue(decodeResult.listAt(1).size()== 0);
        assertTrue(decodeResult.listAt(0).listAt(0).size() == 0);
        assertTrue(decodeResult.listAt(0).listAt(1).size() == 0);
    }

    //The set theoretical representation of two
    @Test
    public void testEncodeRepOfTwoListOfList() {
        //list: [ [], [[]], [ [], [[]] ] ]
        Object[] test = new Object[]{new Object[]{}, new Object[]{new Object[]{}}, new Object[]{new Object[]{}, new Object[]{new Object[]{}}}};
        String expected = "c7c0c1c0c3c0c1c0";
        byte[] encoderesult = encode(test);
        assertEquals(expected, Hex.toHexString(encoderesult));
        assertEquals(expected, Hex.toHexString(encode(Arrays.asList(test))));

        RlpList decodeResult = decodeList(encoderesult);
        assertTrue(decodeResult.size() == 3);
        assertTrue(decodeResult.listAt(0).size() == 0);
        assertTrue(decodeResult.listAt(1).size() == 1);
        assertTrue(decodeResult.listAt(2).size() == 2);
        assertTrue(decodeResult.listAt(1).listAt(0).size() == 0);
        assertTrue(decodeResult.listAt(2).listAt(0).size() == 0);
        assertTrue(decodeResult.listAt(2).listAt(1).size() == 1);
        assertTrue(decodeResult.listAt(2).listAt(1).listAt(0).size() == 0);
    }

    public static int test01 = 0;
    public static String result01 = "80";

    public static String test02 = "";
    public static String result02 = "80";

    public static String test03 = "d";
    public static String result03 = "64";

    public static String test04 = "cat";
    public static String result04 = "83636174";

    public static String test05 = "dog";
    public static String result05 = "83646f67";

    public static String[] test06 = new String[]{"cat", "dog"};
    public static String result06 = "c88363617483646f67";

    public static String[] test07 = new String[]{"dog", "god", "cat"};
    public static String result07 = "cc83646f6783676f6483636174";

    public static int test08 = 1;
    public static String result08 = "01";

    public static int test09 = 10;
    public static String result09 = "0a";

    public static int test10 = 100;
    public static String result10 = "64";

    public static int test11 = 1000;
    public static String result11 = "8203e8";

    public static BigInteger test12 = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639935");
    public static String result12 = "a0ffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffffff";

    public static BigInteger test13 = new BigInteger("115792089237316195423570985008687907853269984665640564039457584007913129639936");
    public static String result13 = "a1010000000000000000000000000000000000000000000000000000000000000000";

    public static Object[] test14 = new Object[]{1, 2, new Object[]{}};
    public static String result14 = "c30102c0";
    public static Object[] expected14 = new Object[]{new byte[]{1}, new byte[]{2}, new Object[]{}};

    public static Object[] test15 = new Object[]{new Object[]{new Object[]{}, new Object[]{}}, new Object[]{}};
    public static String result15 = "c4c2c0c0c0";

    public static Object[] test16 = new Object[]{"zw", new Object[]{4}, "wz"};
    public static String result16 = "c8827a77c10482777a";
    public static Object[] expected16 = new Object[]{new byte[]{122, 119}, new Object[]{new byte[]{4}}, new byte[]{119, 122}};

    @Test
    public void testRlpEncode() {

        assertEquals(result01, Hex.toHexString(encode(test01)));
        assertEquals(result02, Hex.toHexString(encode(test02)));
        assertEquals(result03, Hex.toHexString(encode(test03)));
        assertEquals(result04, Hex.toHexString(encode(test04)));
        assertEquals(result05, Hex.toHexString(encode(test05)));
        assertEquals(result06, Hex.toHexString(encode(test06)));
        assertEquals(result07, Hex.toHexString(encode(test07)));
        assertEquals(result08, Hex.toHexString(encode(test08)));
        assertEquals(result09, Hex.toHexString(encode(test09)));
        assertEquals(result10, Hex.toHexString(encode(test10)));
        assertEquals(result11, Hex.toHexString(encode(test11)));
        assertEquals(result12, Hex.toHexString(encode(test12)));
        assertEquals(result13, Hex.toHexString(encode(test13)));
        assertEquals(result14, Hex.toHexString(encode(test14)));
        assertEquals(result15, Hex.toHexString(encode(test15)));
        assertEquals(result16, Hex.toHexString(encode(test16)));
    }


    // Code from: http://stackoverflow.com/a/4785776/459349
    private String bytesToAscii(byte[] b) {
        String hex = Hex.toHexString(b);
        StringBuilder output = new StringBuilder();
        for (int i = 0; i < hex.length(); i += 2) {
            String str = hex.substring(i, i + 2);
            output.append((char) Integer.parseInt(str, 16));
        }
        return output.toString();
    }

    private int determineSize(Serializable ser) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ObjectOutputStream oos = new ObjectOutputStream(baos);
        oos.writeObject(ser);
        oos.close();
        return baos.size();
    }


    @Test // found this with a bug - nice to keep
    public void encodeEdgeShortList() {

        String expectedOutput = "f7c0c0b4600160003556601359506301000000600035040f6018590060005660805460016080530160005760003560805760203560003557";

        byte[] rlpKeysList = Hex.decode("c0");
        byte[] rlpValuesList = Hex.decode("c0");
        byte[] rlpCode = Hex.decode("b4600160003556601359506301000000600035040f6018590060005660805460016080530160005760003560805760203560003557");
        byte[] output = encodeElements(rlpKeysList, rlpValuesList, rlpCode);

        assertEquals(expectedOutput, Hex.toHexString(output));
    }


    @Test
    public void encodeBigIntegerEdge_1() {

        BigInteger integer = new BigInteger("80", 10);
        byte[] encodedData = encodeBigInteger(integer);
        System.out.println(Hex.toHexString(encodedData));
    }


    @Test
    public void testEncodeSet_2(){

        byte[] setEncoded = encode(Collections.emptySet());
        assertEquals("c0", Hex.toHexString(setEncoded));
    }

    @Test
    public void testEncodeInt_7f(){
        String result =  Hex.toHexString(encodeInt(0x7f));
        String expected = "7f";
        assertEquals(expected, result);
    }

    @Test
    public void testEncodeInt_80(){
        String result =  Hex.toHexString(encodeInt(0x80));
        String expected = "8180";
        assertEquals(expected, result);
    }


    @Test
    public void testEncode_ED(){
        String result =  Hex.toHexString(encode(0xED));
        String expected = "81ed";
        assertEquals(expected, result);
    }


    @Test
    public void partialDataParseTest() {
        String hex = "000080c180000000000000000000000042699b1104e93abf0008be55f912c2ff";
        RlpList el =  Rlp.decodeList(Hex.decode(hex), 3);
        assertEquals(1, el.size());
        assertEquals(0, el.intAt(0));
    }

    @Test
    public void shortStringRightBoundTest(){
        String testString = "bbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbbb"; //String of length 55
        byte[] rlpEncoded = encode(testString);
        String res = decode(rlpEncoded, String.class);
        assertEquals(testString, res); //Fails
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
        byte[] bytes = Hex.decode(dt);

        PoolData[] datas = Rlp.decode(bytes, PoolData[].class);

        byte[] encoded = Rlp.encode(datas);

        assert Arrays.equals(bytes, encoded);

        String writed = Hex.toHexString(RlpWriter.encode(datas));

        System.out.println(writed);
        assert writed.equals(dt);
    }

    @Test
    public void testDecodeOneItem() throws Exception {
        String hex = "000080c180000000000000000000000042699b1104e93abf0008be55f912c2ff";
        byte[] bytes = Hex.decode(hex);
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

        assert Hex.toHexString(encoded).equals("d680850102030405ce8501020304058701020304050607");
    }

    @Test
    public void testWriteMono() {
        Object[] objects = new Object[] { 2L, 3L};
        assertArrayEquals(new byte[]{(byte) 0xc2, 0x02, 0x03}, Rlp.encode(objects));
    }

    @Test
    public void testEncodeLongString1() {
        byte[] str = new byte[56];
        byte[] expected = new byte[58];
        expected[0] = (byte) 0xb8;
        expected[1] = (byte) 56;
        assertArrayEquals(expected, Rlp.encode(str));
    }

    static class InvalidClass {
        @RlpCreator
        public static InvalidClass fromRlpStream(byte[] bin, int streamId) {
            return null;
        }

        @com.github.salpadding.rlpstream.annotation.RlpWriter
        public static long writeToBuf(InvalidClass i, RlpBuffer buf) {
            return buf.writeNull();
        }
    }

    static class ValidClass {
        @RlpCreator
        public static ValidClass fromRlpStream(byte[] bin, long streamId) {
            return null;
        }


        @com.github.salpadding.rlpstream.annotation.RlpWriter
        public static int writeToBuf(ValidClass v, RlpBuffer buf) {
            return buf.writeNull();
        }
    }

    interface ValidInterface {
        @RlpCreator
        static ValidInterfaceImpl fromRlpStream(byte[] bin, long streamId) {
            return new ValidInterfaceImpl();
        }
    }

    static class ValidInterfaceImpl implements ValidInterface {
        @com.github.salpadding.rlpstream.annotation.RlpWriter
        public static int writeToBuf(ValidInterface i, RlpBuffer buf) {
            return buf.writeNull();
        }
    }


    @Test(expected = RuntimeException.class)
    public void testInvalidRlpCreator() {
        Rlp.decode(EMPTY_LIST, InvalidClass.class);
    }

    @Test(expected = RuntimeException.class)
    public void testInvalidRlpWriter() {
        Rlp.encode(new InvalidClass());
    }

    @Test
    public void testValidRlpCreator() {
        Rlp.decode(EMPTY_LIST, ValidClass.class);
    }

    @Test
    public void testValidRlpWriter() {
        Rlp.encode(new ValidClass());
    }

    @Test
    public void testValidRlpCreator0() {
        Rlp.decode(EMPTY_LIST, ValidInterface.class);
    }

    @Test
    public void testValidRlpWriter1() {
        Rlp.encode(new ValidInterfaceImpl());
    }
}
