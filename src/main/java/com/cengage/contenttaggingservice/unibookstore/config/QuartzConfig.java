package com.cengage.contenttaggingservice.unibookstore.config;

import com.cengage.contenttaggingservice.unibookstore.job.LowStockDigestJob;
import com.cengage.contenttaggingservice.unibookstore.job.ReceiptRecoveryJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

@Configuration
public class QuartzConfig {

    private static final String RECEIPTS_GROUP = "receipts";
    private static final String CATALOG_GROUP = "catalog";
    private static final TimeZone SCHEDULE_ZONE = TimeZone.getTimeZone("UTC");

    @Bean
    public JobDetail receiptRecoveryJobDetail() {
        return JobBuilder.newJob(ReceiptRecoveryJob.class)
                .withIdentity("receiptRecoveryJob", RECEIPTS_GROUP)
                .usingJobData("maxAgeMinutes", 15)
                .usingJobData("batchSize", 50)
                .usingJobData("maxAttempts", 3)
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger receiptRecoveryTrigger(JobDetail receiptRecoveryJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(receiptRecoveryJobDetail)
                .withIdentity("receiptRecoveryTrigger", RECEIPTS_GROUP)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0/10 * * * ?")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public JobDetail lowStockDigestJobDetail() {
        return JobBuilder.newJob(LowStockDigestJob.class)
                .withIdentity("lowStockDigestJob", CATALOG_GROUP)
                .usingJobData("threshold", 10)
                .usingJobData("onlyEnabled", true)
                .usingJobData("channel", "LOG")
                .storeDurably()
                .requestRecovery(true)
                .build();
    }

    @Bean
    public Trigger lowStockDailyTrigger(JobDetail lowStockDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(lowStockDigestJobDetail)
                .withIdentity("lowStockDailyTrigger", CATALOG_GROUP)
                .usingJobData("threshold", 5)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 9 * * ?")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }

    @Bean
    public Trigger lowStockWeeklyTrigger(JobDetail lowStockDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(lowStockDigestJobDetail)
                .withIdentity("lowStockWeeklyTrigger", CATALOG_GROUP)
                .usingJobData("threshold", 25)
                .usingJobData("channel", "SSE")
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 8 ? * MON")
                        .inTimeZone(SCHEDULE_ZONE)
                        .withMisfireHandlingInstructionFireAndProceed())
                .build();
    }
}
