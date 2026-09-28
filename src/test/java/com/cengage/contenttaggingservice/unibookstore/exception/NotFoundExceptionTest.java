package com.cengage.contenttaggingservice.unibookstore.exception;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class NotFoundExceptionTest {

    @Test
    void carriesMessageAndIsRuntimeException() {
        NotFoundException ex = new NotFoundException("Book 42 not found");

        assertThat(ex).isInstanceOf(RuntimeException.class);
        assertThat(ex.getMessage()).isEqualTo("Book 42 not found");
    }
}
