package com.cengage.contenttaggingservice.unibookstore.config;

import org.junit.jupiter.api.Test;
import org.quartz.CronTrigger;
import org.quartz.JobDataMap;
import org.quartz.JobDetail;
import org.quartz.Trigger;

import static org.assertj.core.api.Assertions.assertThat;

class QuartzConfigTest {

    private final QuartzConfig config = new QuartzConfig();

    private JobDataMap mergedAsQuartzWould(JobDetail job, Trigger trigger) {
        JobDataMap merged = new JobDataMap(job.getJobDataMap());
        merged.putAll(trigger.getJobDataMap());
        return merged;
    }

    @Test
    void lowStockJobAndTriggersHaveExplicitNameAndGroupAndTriggersPointToTheJob() {
        JobDetail lowStockJob = config.lowStockDigestJobDetail();
        Trigger daily = config.lowStockDailyTrigger(lowStockJob);
        Trigger weekly = config.lowStockWeeklyTrigger(lowStockJob);

        assertThat(lowStockJob.getKey().getName()).isEqualTo("lowStockDigestJob");
        assertThat(lowStockJob.getKey().getGroup()).isEqualTo("catalog");
        assertThat(daily.getJobKey()).isEqualTo(lowStockJob.getKey());
        assertThat(weekly.getJobKey()).isEqualTo(lowStockJob.getKey());
        assertThat(daily.getKey()).isNotEqualTo(weekly.getKey());
    }

    @Test
    void triggerJobDataOverridesJobDefaultsAndKeepsUnsetKeys() {
        JobDetail lowStockJob = config.lowStockDigestJobDetail();

        JobDataMap weekly = mergedAsQuartzWould(lowStockJob, config.lowStockWeeklyTrigger(lowStockJob));
        assertThat(weekly.getInt("threshold")).isEqualTo(25);     // overridden by trigger
        assertThat(weekly.getString("channel")).isEqualTo("SSE"); // overridden by trigger
        assertThat(weekly.getBoolean("onlyEnabled")).isTrue();    // kept from job default

        JobDataMap daily = mergedAsQuartzWould(lowStockJob, config.lowStockDailyTrigger(lowStockJob));
        assertThat(daily.getInt("threshold")).isEqualTo(5);       // overridden by trigger
        assertThat(daily.getString("channel")).isEqualTo("LOG");  // kept from job default
    }

    @Test
    void cronTriggersDefineMisfireInstructionAndExplicitTimeZone() {
        JobDetail lowStockJob = config.lowStockDigestJobDetail();
        CronTrigger daily = (CronTrigger) config.lowStockDailyTrigger(lowStockJob);
        CronTrigger weekly = (CronTrigger) config.lowStockWeeklyTrigger(lowStockJob);

        assertThat(daily.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(weekly.getMisfireInstruction()).isEqualTo(CronTrigger.MISFIRE_INSTRUCTION_FIRE_ONCE_NOW);
        assertThat(daily.getTimeZone().getID()).isEqualTo("UTC");
        assertThat(weekly.getTimeZone().getID()).isEqualTo("UTC");
    }
}
