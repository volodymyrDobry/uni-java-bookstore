package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.UpdateBookDetailsRequest;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {

    @Mapping(target = "quantity", source = "book.stock.quantity")
    @Mapping(target = "price", source = "book.stock.price")
    BookResponse toResponse(Book book);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "stock", ignore = true)
    void updateBook(UpdateBookDetailsRequest request, @MappingTarget Book book);
}
