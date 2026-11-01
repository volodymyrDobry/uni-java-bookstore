package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.dto.LowStockDigestResponse;
import com.cengage.contenttaggingservice.unibookstore.service.LowStockDigestService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

/**
 * Scheduled catalogue job that records a low-stock digest. It lives in
 * book-management-service because this service owns the stock data; each run stores
 * the digest so the admin UI can display the history.
 */
@Slf4j
@DisallowConcurrentExecution
public class LowStockDigestJob extends QuartzJobBean {

    @Autowired
    private LowStockDigestService lowStockDigestService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        int threshold = data.getInt("threshold");
        boolean onlyEnabled = data.getBoolean("onlyEnabled");
        String channel = data.getString("channel");

        try {
            LowStockDigestResponse digest = lowStockDigestService.recordDigest(threshold, onlyEnabled, channel);
            log.info("Low-stock digest [channel={}, threshold={}, onlyEnabled={}]: {} book(s) recorded as digest {}",
                    channel, threshold, onlyEnabled, digest.itemCount(), digest.id());
        } catch (RuntimeException ex) {
            log.error("LowStockDigestJob failed; recording error and waiting for the next scheduled run", ex);
        }
    }
}
