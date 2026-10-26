package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderResponse;
import com.cengage.contenttaggingservice.unibookstore.dto.ReceiptDownload;

import java.util.List;

public interface OrderingService {

    OrderResponse placeOrder();

    List<OrderResponse> getMyOrders();

    ReceiptDownload downloadReceipt(Long orderId);
}
