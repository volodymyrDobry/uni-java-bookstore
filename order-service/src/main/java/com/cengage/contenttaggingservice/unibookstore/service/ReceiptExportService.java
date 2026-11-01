package com.cengage.contenttaggingservice.unibookstore.service;

import java.time.Instant;

public interface ReceiptExportService {

    byte[] exportReceipts(Instant from, Instant to);
}
