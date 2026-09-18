package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Book;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookStock;
import com.cengage.contenttaggingservice.unibookstore.dto.BookRequest;
import com.cengage.contenttaggingservice.unibookstore.dto.BookResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;
import org.mapstruct.MappingTarget;

@Mapper(componentModel = "spring")
public interface BookMapper {
    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    Book toEntity(BookRequest request);

    @Mapping(target = "id", ignore = true)
    @Mapping(target = "enabled", ignore = true)
    void update(BookRequest request, @MappingTarget Book book);

    @Mapping(target = "id", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    @Mapping(target = "description", source = "book.description")
    @Mapping(target = "author", source = "book.author")
    @Mapping(target = "genre", source = "book.genre")
    @Mapping(target = "imageUrl", source = "book.imageUrl")
    @Mapping(target = "enabled", source = "book.enabled")
    @Mapping(target = "quantity", source = "stock.quantity")
    @Mapping(target = "price", source = "stock.price")
    BookResponse toResponse(Book book, BookStock stock);
}
