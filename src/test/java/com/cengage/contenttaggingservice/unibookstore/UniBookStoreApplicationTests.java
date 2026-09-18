package com.cengage.contenttaggingservice.unibookstore;

import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.service.BookService;
import org.junit.jupiter.api.Assertions;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;

@SpringBootTest
class UniBookStoreApplicationTests {

    @Autowired
    private BookService bookService;

    @Test
    void contextLoads() {
    }

    @Test
    void availableBooksSupportsAnOmittedSearchParameter() {
        Page<BookResponse> page = bookService.available(null, null, null, null, PageRequest.of(0, 10));
        Assertions.assertEquals(50, page.getTotalElements());
    }
}
