package com.cengage.contenttaggingservice.unibookstore.mapper;

import com.cengage.contenttaggingservice.unibookstore.domain.model.LowStockDigest;
import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import org.mapstruct.Mapper;

@Mapper(componentModel = "spring")
public interface LowStockDigestMapper {

    LowStockDigestResponse toResponse(LowStockDigest digest);
}
