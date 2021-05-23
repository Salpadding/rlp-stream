package com.github.salpadding.rlpstream;

import java.math.BigInteger;

public final class Constants {
    /**
     * [0x80]
     * If a string is 0-55 bytes long, the RLP encoding consists of a single
     * byte with value 0x80 plus the length of the string followed by the
     * string. The range of the first byte is thus [0x80, 0xb7].
     */
    public static final int OFFSET_SHORT_ITEM = 0x80;

    /**
     * [0x80]
     * If a string is 0-55 bytes long, the RLP encoding consists of a single
     * byte with value 0x80 plus the length of the string followed by the
     * string. The range of the first byte is thus [0x80, 0xb7].
     */
    public static final BigInteger OFFSET_SHORT_ITEM_BN = BigInteger.valueOf(OFFSET_SHORT_ITEM);

    /**
     * Reason for threshold according to Vitalik Buterin:
     * - 56 bytes maximizes the benefit of both options
     * - if we went with 60 then we would have only had 4 slots for long strings
     * so RLP would not have been able to store objects above 4gb
     * - if we went with 48 then RLP would be fine for 2^128 space, but that's way too much
     * - so 56 and 2^64 space seems like the right place to put the cutoff
     * - also, that's where Bitcoin's varint does the cutof
     */
    public static final int SIZE_THRESHOLD = 56;

    /**
     * [0xb7]
     * If a string is more than 55 bytes long, the RLP encoding consists of a
     * single byte with value 0xb7 plus the length of the length of the string
     * in binary form, followed by the length of the string, followed by the
     * string. For example, a length-1024 string would be encoded as
     * \xb9\x04\x00 followed by the string. The range of the first byte is thus
     * [0xb8, 0xbf].
     */
    public static final int OFFSET_LONG_ITEM = 0xb7;

    /**
     * [0xc0]
     * If the total payload of a list (i.e. the combined length of all its
     * items) is 0-55 bytes long, the RLP encoding consists of a single byte
     * with value 0xc0 plus the length of the list followed by the concatenation
     * of the RLP encodings of the items. The range of the first byte is thus
     * [0xc0, 0xf7].
     */
    public static final int OFFSET_SHORT_LIST = 0xc0;

    /**
     * [0xf7]
     * If the total payload of a list is more than 55 bytes long, the RLP
     * encoding consists of a single byte with value 0xf7 plus the length of the
     * length of the list in binary form, followed by the length of the list,
     * followed by the concatenation of the RLP encodings of the items. The
     * range of the first byte is thus [0xf8, 0xff].
     */
    public static final int OFFSET_LONG_LIST = 0xf7;

    public static final byte[] NULL = new byte[]{(byte) 0x80};
    public static final byte[] EMPTY_LIST = new byte[]{(byte) 0xc0};
    public static final byte[] ONE = new byte[]{0x01};
    public static final byte[] EMPTY = new byte[0];


    // prefix size = 0
    public static final long MONO_MASK = 0x80000000L;
    public static final long OFFSET_MASK = 0x7fffffffL;
    public static final long SIZE_MASK = 0x7fffffffL << 32;
    // EOF, unreachable, when mono size should be one
    public static final long EOF = 0xFFFFFFFFFFFFFFFFL;
    public static final long LIST_SIGN_MASK = 1L << 63;
}
