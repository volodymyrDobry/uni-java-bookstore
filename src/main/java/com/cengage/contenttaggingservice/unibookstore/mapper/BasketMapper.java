package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketItemResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface BasketMapper {
    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    BasketItemResponse toResponse(BookItem item);
}
