package org.jfrog.bamboo.utils;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.databind.DeserializationFeature;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;

import java.util.ArrayList;
import java.util.List;


public class Utils {
    /**
     * Creates and configures an ObjectMapper instance.
     *
     * @return The configured ObjectMapper instance.
     */
    public static ObjectMapper createMapper() {
        return new ObjectMapper().configure(DeserializationFeature.FAIL_ON_UNKNOWN_PROPERTIES, false)
                .disable(SerializationFeature.FAIL_ON_EMPTY_BEANS)
                .setSerializationInclusion(JsonInclude.Include.NON_NULL);
    }

    /**
     * Removes the surrounding quotes from the given string, if present.
     *
     * @param str The string to unquote.
     * @return The unquoted string.
     */
    public static String unQuote(String str) {
        if ((str.startsWith("\"") && str.endsWith("\"")) || (str.startsWith("'") && str.endsWith("'"))) {
            return str.substring(1, str.length() - 1);
        }
        return str;
    }

    /**
     * Splits the input string into tokens while preserving quoted substrings.
     *
     * @param input The string to split.
     * @return A list of tokens.
     */
    public static List<String> splitStringPreservingQuotes(String input) {
        List<String> tokens = new ArrayList<>();
        StringBuilder token = new StringBuilder();
        boolean insideDoubleQuotes = false;
        boolean insideSingleQuotes = false;

        for (int i = 0; i < input.length(); i++) {
            char c = input.charAt(i);

            if (c == ' ' && !insideDoubleQuotes && !insideSingleQuotes) {
                if (token.length() > 0) {
                    tokens.add(token.toString());
                    token.setLength(0);
                }
            } else {
                if (c == '"') {
                    insideDoubleQuotes = !insideDoubleQuotes;
                } else if (c == '\'') {
                    insideSingleQuotes = !insideSingleQuotes;
                }
                token.append(c);
            }
        }

        if (token.length() > 0) {
            tokens.add(token.toString());
        }

        return tokens;
    }
}
