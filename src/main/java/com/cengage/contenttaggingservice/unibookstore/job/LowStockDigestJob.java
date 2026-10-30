package com.cengage.contenttaggingservice.unibookstore.job;

import com.cengage.contenttaggingservice.unibookstore.service.LowStockService;
import lombok.extern.slf4j.Slf4j;
import org.quartz.DisallowConcurrentExecution;
import org.quartz.JobDataMap;
import org.quartz.JobExecutionContext;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.scheduling.quartz.QuartzJobBean;

@Slf4j
@DisallowConcurrentExecution
public class LowStockDigestJob extends QuartzJobBean {

    @Autowired
    private LowStockService lowStockService;

    @Override
    protected void executeInternal(JobExecutionContext context) {
        JobDataMap data = context.getMergedJobDataMap();
        int threshold = data.getInt("threshold");
        boolean onlyEnabled = data.getBoolean("onlyEnabled");
        String channel = data.getString("channel");

        try {
            lowStockService.reportLowStock(threshold, onlyEnabled, channel);
        } catch (RuntimeException ex) {
            log.error("LowStockDigestJob failed; recording error and waiting for the next scheduled run", ex);
        }
    }
}
