package com.cengage.contenttaggingservice.unibookstore.dto;

public record ReceiptDownload(
        String filename,
        byte[] content
) {
}
