package com.cengage.contenttaggingservice.unibookstore.config;

import com.cengage.contenttaggingservice.unibookstore.job.LowStockDigestJob;
import org.quartz.CronScheduleBuilder;
import org.quartz.JobBuilder;
import org.quartz.JobDetail;
import org.quartz.Trigger;
import org.quartz.TriggerBuilder;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

import java.util.TimeZone;

/**
 * Schedules the low-stock digest job. book-management-service owns the stock, so the
 * job runs here on an in-memory Quartz scheduler (no shared job-store tables).
 */
@Configuration
public class QuartzConfig {

    private static final String CATALOG_GROUP = "catalog";
    private static final TimeZone SCHEDULE_ZONE = TimeZone.getTimeZone("Europe/Kyiv");

    @Bean
    public JobDetail lowStockDigestJobDetail() {
        return JobBuilder.newJob(LowStockDigestJob.class)
                .withIdentity("lowStockDigestJob", CATALOG_GROUP)
                .usingJobData("threshold", 10)
                .usingJobData("onlyEnabled", true)
                .usingJobData("channel", "LOG")
                .storeDurably()
                .build();
    }

    @Bean
    public Trigger lowStockDailyTrigger(JobDetail lowStockDigestJobDetail) {
        return TriggerBuilder.newTrigger()
                .forJob(lowStockDigestJobDetail)
                .withIdentity("lowStockDailyTrigger", CATALOG_GROUP)
                .usingJobData("threshold", 5)
                .withSchedule(CronScheduleBuilder.cronSchedule("0 0 11 * * ?")
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
