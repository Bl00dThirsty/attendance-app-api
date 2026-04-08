package com.example.attendance_app.config;

import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotNull;
import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.stereotype.Component;
import org.springframework.validation.annotation.Validated;

import java.time.LocalTime;

@Component
@Validated
@ConfigurationProperties(prefix = "app.attendance.rules")
public class AttendanceRulesProperties {

    @Min(value = 1, message = "maxRetroactiveMinutes must be >= 1")
    private long maxRetroactiveMinutes = 720;

    @Min(value = 0, message = "maxFutureToleranceSeconds must be >= 0")
    private long maxFutureToleranceSeconds = 0;

    private boolean duplicateGuardEnabled = true;

    private boolean singleCheckInPerDay = true;

    @Min(value = 0, message = "minMinutesBetweenCheckIns must be >= 0")
    private long minMinutesBetweenCheckIns = 0;

    private boolean enforceDailyWindow = true;

    @NotNull(message = "dailyWindowStart is required")
    private LocalTime dailyWindowStart = LocalTime.of(4, 0);

    @NotNull(message = "dailyWindowEnd is required")
    private LocalTime dailyWindowEnd = LocalTime.of(23, 0);

    public long getMaxRetroactiveMinutes() {
        return maxRetroactiveMinutes;
    }

    public void setMaxRetroactiveMinutes(long maxRetroactiveMinutes) {
        this.maxRetroactiveMinutes = maxRetroactiveMinutes;
    }

    public long getMaxFutureToleranceSeconds() {
        return maxFutureToleranceSeconds;
    }

    public void setMaxFutureToleranceSeconds(long maxFutureToleranceSeconds) {
        this.maxFutureToleranceSeconds = maxFutureToleranceSeconds;
    }

    public boolean isEnforceDailyWindow() {
        return enforceDailyWindow;
    }

    public void setEnforceDailyWindow(boolean enforceDailyWindow) {
        this.enforceDailyWindow = enforceDailyWindow;
    }

    public boolean isDuplicateGuardEnabled() {
        return duplicateGuardEnabled;
    }

    public void setDuplicateGuardEnabled(boolean duplicateGuardEnabled) {
        this.duplicateGuardEnabled = duplicateGuardEnabled;
    }

    public boolean isSingleCheckInPerDay() {
        return singleCheckInPerDay;
    }

    public void setSingleCheckInPerDay(boolean singleCheckInPerDay) {
        this.singleCheckInPerDay = singleCheckInPerDay;
    }

    public long getMinMinutesBetweenCheckIns() {
        return minMinutesBetweenCheckIns;
    }

    public void setMinMinutesBetweenCheckIns(long minMinutesBetweenCheckIns) {
        this.minMinutesBetweenCheckIns = minMinutesBetweenCheckIns;
    }

    public LocalTime getDailyWindowStart() {
        return dailyWindowStart;
    }

    public void setDailyWindowStart(LocalTime dailyWindowStart) {
        this.dailyWindowStart = dailyWindowStart;
    }

    public LocalTime getDailyWindowEnd() {
        return dailyWindowEnd;
    }

    public void setDailyWindowEnd(LocalTime dailyWindowEnd) {
        this.dailyWindowEnd = dailyWindowEnd;
    }
}
