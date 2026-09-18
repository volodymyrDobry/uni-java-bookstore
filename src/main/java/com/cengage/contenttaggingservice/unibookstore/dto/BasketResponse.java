package com.cengage.contenttaggingservice.unibookstore.dto;

import java.util.List;

public record BasketResponse(Long id, List<BasketItemResponse> items) {
}
