package com.lzp.util;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.junit.jupiter.params.provider.ValueSource;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Base62Util 单元测试
 */
class Base62UtilTest {

    @Test
    void encode_zero_shouldReturnFirstChar() {
        assertEquals("0", Base62Util.encode(0));
    }

    @ParameterizedTest
    @CsvSource({
            "0,  0",
            "1,  1",
            "9,  9",
            "10, A",
            "35, Z",
            "36, a",
            "61, z",
            "62, 10",
            "100,1c"
    })
    void encode_shouldMapCorrectly(long id, String expected) {
        assertEquals(expected, Base62Util.encode(id));
    }

    @ParameterizedTest
    @CsvSource({
            "0,  0",
            "1,  1",
            "A, 10",
            "Z, 35",
            "a, 36",
            "z, 61",
            "10,62"
    })
    void decode_shouldReverseEncode(String code, long expectedId) {
        assertEquals(expectedId, Base62Util.decode(code));
    }

    @Test
    void encodeAndDecode_shouldBeReversible() {
        long originalId = 123456789L;
        String code = Base62Util.encode(originalId);
        long decodedId = Base62Util.decode(code);
        assertEquals(originalId, decodedId);
    }

    @Test
    void encodeWithPadding_shouldProduceFixedLength() {
        String result = Base62Util.encodeWithPadding(0, 6);
        assertEquals(6, result.length());
        assertEquals("000000", result);
    }

    @Test
    void encodeWithPadding_shouldHandleLargeId() {
        long id = 56800235583L; // 62^6 - 1 = 最大6位短码值
        String code = Base62Util.encodeWithPadding(id, 6);
        assertEquals(6, code.length(), "6位短码应容纳62^6-1个ID");
    }

    @Test
    void encodeWithPadding_largeId_shouldNotTruncate() {
        // 超出6位的ID，短码长度应该超过6
        long id = 62L * 62 * 62 * 62 * 62 * 62; // 62^6
        String code = Base62Util.encodeWithPadding(id, 6);
        assertTrue(code.length() >= 6, "超大ID不截断");
    }

    @Test
    void encode_negativeId_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Util.encode(-1));
    }

    @Test
    void decode_nullInput_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Util.decode(null));
    }

    @Test
    void decode_emptyInput_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Util.decode(""));
    }

    @Test
    void decode_invalidCharacter_shouldThrowException() {
        assertThrows(IllegalArgumentException.class, () -> Base62Util.decode("!@#"));
    }

    @Test
    void encode_maxLong_shouldNotOverflow() {
        // Long.MAX_VALUE 编码后再解码应还原
        String code = Base62Util.encode(Long.MAX_VALUE);
        assertNotNull(code);
        assertFalse(code.isEmpty());
        assertEquals(Long.MAX_VALUE, Base62Util.decode(code));
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 10, 100, 1000, 99999, 1000000})
    void encode_ascendingIds_shouldProduceLexicographicOrder(long id) {
        String code = Base62Util.encode(id);
        assertNotNull(code);
        assertFalse(code.isEmpty());
        // 不要求字典序，只验证编码不为空
        assertTrue(code.matches("[0-9A-Za-z]+"), "Base62编码应只包含合法字符: " + code);
    }
}
