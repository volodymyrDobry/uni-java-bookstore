package com.cengage.contenttaggingservice.unibookstore.properties;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

class CorsPropertiesTest {

    @Test
    void holdsConfiguredValues() {
        CorsProperties props = new CorsProperties();
        props.setAllowedOrigins(List.of("http://localhost"));
        props.setAllowedMethods(List.of("GET", "POST"));
        props.setAllowedHeaders(List.of("Authorization"));

        assertThat(props.getAllowedOrigins()).containsExactly("http://localhost");
        assertThat(props.getAllowedMethods()).containsExactly("GET", "POST");
        assertThat(props.getAllowedHeaders()).containsExactly("Authorization");
    }
}
