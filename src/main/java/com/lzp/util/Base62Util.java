package com.lzp.util;

/**
 * Base62 编码/解码工具类
 * 字符集：0-9 A-Z a-z（共62个字符）
 * 用于将自增ID转换为短码，或从短码还原ID
 */
public class Base62Util {

    private static final String BASE62_CHARS = "0123456789ABCDEFGHIJKLMNOPQRSTUVWXYZabcdefghijklmnopqrstuvwxyz";
    private static final int BASE = 62;

    /**
     * 将数字编码为 Base62 字符串
     * @param id 自增ID
     * @return Base62 编码后的短码
     */
    public static String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("id must be non-negative");
        }
        if (id == 0) {
            return String.valueOf(BASE62_CHARS.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        while (id > 0) {
            int remainder = (int) (id % BASE);
            sb.append(BASE62_CHARS.charAt(remainder));
            id /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * 将 Base62 字符串解码为数字
     * @param code Base62 编码的短码
     * @return 原始数字ID
     */
    public static long decode(String code) {
        if (code == null || code.isEmpty()) {
            throw new IllegalArgumentException("code must not be null or empty");
        }
        long result = 0;
        for (char c : code.toCharArray()) {
            int value = BASE62_CHARS.indexOf(c);
            if (value == -1) {
                throw new IllegalArgumentException("invalid Base62 character: " + c);
            }
            result = result * BASE + value;
        }
        return result;
    }

    /**
     * 将数字编码为固定长度的 Base62 字符串（左侧补0）
     * @param id     自增ID
     * @param length 目标长度
     * @return 固定长度的 Base62 短码
     */
    public static String encodeWithPadding(long id, int length) {
        String encoded = encode(id);
        if (encoded.length() >= length) {
            return encoded;
        }
        StringBuilder sb = new StringBuilder();
        for (int i = encoded.length(); i < length; i++) {
            sb.append(BASE62_CHARS.charAt(0));
        }
        sb.append(encoded);
        return sb.toString();
    }
}
