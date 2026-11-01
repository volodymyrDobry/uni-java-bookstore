package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.Basket;
import com.cengage.contenttaggingservice.unibookstore.domain.model.BookItem;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketItemResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.BasketResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

import java.util.List;
import java.util.Set;

@Mapper(componentModel = "spring")
public interface BasketMapper {

    @Mapping(target = "items", source = "books")
    BasketResponse toBasketResponse(Basket basket);

    @Mapping(target = "bookId", source = "book.id")
    @Mapping(target = "title", source = "book.title")
    BasketItemResponse toItemResponse(BookItem bookItem);

    List<BasketItemResponse> toItemResponses(Set<BookItem> books);
}
