package com.urlshortener.unit;

import com.urlshortener.service.Base62Encoder;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

import static org.assertj.core.api.Assertions.*;

class Base62EncoderTest {

    private final Base62Encoder encoder = new Base62Encoder();

    @Test
    void encode_zero_returnsSingleChar() {
        assertThat(encoder.encode(0)).isEqualTo("0");
    }

    @Test
    void encode_one_returnsSingleChar() {
        assertThat(encoder.encode(1)).isEqualTo("1");
    }

    @Test
    void encode_decode_roundTrip() {
        long[] ids = {1L, 62L, 3843L, 1_000_000L, Long.MAX_VALUE / 1000};
        for (long id : ids) {
            String encoded = encoder.encode(id);
            assertThat(encoder.decode(encoded))
                .as("round-trip for id=%d, encoded=%s", id, encoded)
                .isEqualTo(id);
        }
    }

    @Test
    void encode_negativId_throwsIllegalArgument() {
        assertThatThrownBy(() -> encoder.encode(-1))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("non-negative");
    }

    @Test
    void decode_invalidChar_throwsIllegalArgument() {
        assertThatThrownBy(() -> encoder.decode("abc!"))
            .isInstanceOf(IllegalArgumentException.class)
            .hasMessageContaining("Invalid Base62 character");
    }

    @ParameterizedTest
    @ValueSource(longs = {1, 61, 62, 63, 3843, 238327, 999_999})
    void encode_producesOnlyBase62Characters(long id) {
        String encoded = encoder.encode(id);
        assertThat(encoded).matches("[0-9a-zA-Z]+");
    }

    @Test
    void encode_differentIds_produceDifferentCodes() {
        assertThat(encoder.encode(1)).isNotEqualTo(encoder.encode(2));
        assertThat(encoder.encode(61)).isNotEqualTo(encoder.encode(62));
    }
}
