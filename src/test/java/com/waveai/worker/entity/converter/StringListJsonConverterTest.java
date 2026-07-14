package com.waveai.worker.entity.converter;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * Unit tests for {@link StringListJsonConverter}.
 *
 * <p>This converter was migrated from Jackson 2 ({@code com.fasterxml.jackson.databind})
 * to Jackson 3 ({@code tools.jackson.databind}) during the Spring Boot 3.5 -> 4.1 upgrade.
 * In Jackson 3 the (de)serialization methods throw the unchecked {@code JacksonException}
 * instead of the checked {@code JsonProcessingException}. These tests pin down the
 * round-trip and error-handling behaviour so the migration stays correct.
 */
class StringListJsonConverterTest {

    private final StringListJsonConverter converter = new StringListJsonConverter();

    @Test
    void convertToDatabaseColumn_serializesListAsJsonArray() {
        String json = converter.convertToDatabaseColumn(List.of("Alice", "Bob"));

        assertThat(json).isEqualTo("[\"Alice\",\"Bob\"]");
    }

    @Test
    void convertToDatabaseColumn_nullAttribute_returnsEmptyJsonArray() {
        assertThat(converter.convertToDatabaseColumn(null)).isEqualTo("[]");
    }

    @Test
    void convertToDatabaseColumn_emptyList_returnsEmptyJsonArray() {
        assertThat(converter.convertToDatabaseColumn(List.of())).isEqualTo("[]");
    }

    @Test
    void convertToEntityAttribute_parsesJsonArrayIntoList() {
        List<String> result = converter.convertToEntityAttribute("[\"Alice\",\"Bob\"]");

        assertThat(result).containsExactly("Alice", "Bob");
    }

    @Test
    void convertToEntityAttribute_null_returnsEmptyList() {
        assertThat(converter.convertToEntityAttribute(null)).isEmpty();
    }

    @Test
    void convertToEntityAttribute_blank_returnsEmptyList() {
        assertThat(converter.convertToEntityAttribute("   ")).isEmpty();
    }

    @Test
    void convertToEntityAttribute_malformedJson_returnsEmptyList() {
        // Malformed input must be swallowed (Jackson 3 throws the unchecked
        // JacksonException here) and fall back to an empty list.
        assertThat(converter.convertToEntityAttribute("{not valid json")).isEmpty();
    }

    @Test
    void roundTrip_preservesElementsAndOrder() {
        List<String> original = List.of("Carol", "Dave", "Erin");

        String json = converter.convertToDatabaseColumn(original);
        List<String> restored = converter.convertToEntityAttribute(json);

        assertThat(restored).isEqualTo(original);
    }
}
