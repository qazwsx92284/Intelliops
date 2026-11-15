package org.intllps.intelliops.dto;

public record DashboardStats(
        long total,
        long low,
        long medium,
        long high,
        long critical
) {
}

