package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.OrderItem;
import com.cengage.contenttaggingservice.unibookstore.domain.model.Ordering;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderItemResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.OrderResponse;
import org.mapstruct.Mapper;
import org.mapstruct.Mapping;

@Mapper(componentModel = "spring")
public interface OrderMapper {

    OrderResponse toResponse(Ordering ordering);

    @Mapping(target = "bookId", source = "book.id")
    OrderItemResponse toItemResponse(OrderItem item);
}
