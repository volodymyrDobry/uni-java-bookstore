package com.cengage.contenttaggingservice.unibookstore.service;

import com.cengage.contenttaggingservice.unibookstore.dto.OrderSnapshot;

public interface ReceiptPipeline {

    void generateAndStore(OrderSnapshot snapshot);

    String generateAndStoreSync(OrderSnapshot snapshot);
}
