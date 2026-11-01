package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.service.ReceiptRecoveryService;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@DisallowConcurrentExecution
public class ReceiptRecoveryJob extends QuartzJobBean {

    @Autowired
    private ReceiptRecoveryService receiptRecoveryService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        receiptRecoveryService.recoverPendingReceipts(
                data.getInt("maxAgeMinutes"), data.getInt("batchSize"), data.getInt("maxAttempts"));
    }
}
