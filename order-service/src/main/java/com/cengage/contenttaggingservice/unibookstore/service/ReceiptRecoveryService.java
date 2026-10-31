package com.cengage.contenttaggingservice.unibookstore.service;

import java.util.List;

public interface ReceiptRecoveryService {

    void recoverPendingReceipts(int maxAgeMinutes, int batchSize, int maxAttempts);

    List<Long> findRecoverableOrderIds(int maxAgeMinutes, int batchSize, int maxAttempts);

    void recoverOne(long orderId, int maxAttempts);
}
