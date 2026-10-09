package com.urlshortener.service;

import org.springframework.stereotype.Component;

/**
 * Encodes a positive long ID to a Base62 string (a-z, A-Z, 0-9).
 * Minimum output length is padded to the configured length with leading '0' chars.
 */
@Component
public class Base62Encoder {

    private static final String ALPHABET = "0123456789abcdefghijklmnopqrstuvwxyzABCDEFGHIJKLMNOPQRSTUVWXYZ";
    private static final int BASE = ALPHABET.length(); // 62

    /**
     * Encode a positive long to a Base62 string.
     */
    public String encode(long id) {
        if (id < 0) {
            throw new IllegalArgumentException("ID must be non-negative: " + id);
        }
        if (id == 0) {
            return String.valueOf(ALPHABET.charAt(0));
        }
        StringBuilder sb = new StringBuilder();
        long value = id;
        while (value > 0) {
            sb.append(ALPHABET.charAt((int) (value % BASE)));
            value /= BASE;
        }
        return sb.reverse().toString();
    }

    /**
     * Decode a Base62 string back to a long.
     */
    public long decode(String code) {
        long result = 0;
        for (char c : code.toCharArray()) {
            int index = ALPHABET.indexOf(c);
            if (index < 0) {
                throw new IllegalArgumentException("Invalid Base62 character: " + c);
            }
            result = result * BASE + index;
        }
        return result;
    }
}
