package com.calipsoide.diffing.business;

import com.google.common.collect.ImmutableList;

import java.util.List;
import java.util.Optional;

import static com.google.common.base.Preconditions.checkNotNull;

/**
 * Represents the diff results, providing {@link Status} and a list of {@link DiffInsight}s if applicable.
 */
public class DiffReport {

    public enum Status {
        EQUAL, LENGTH_MISMATCH, NOT_EQUAL
    }

    private final Status status;

    private final List<DiffInsight> insights;

    private DiffReport(Status status, List<DiffInsight> insights) {
        this.status = status;
        this.insights = insights;
    }

    static DiffReport of(Status status) {
        return new DiffReport(status, ImmutableList.of());
    }

    public static DiffReport of(Status status, List<DiffInsight> insights) {
        return new DiffReport(
                checkNotNull(status, "diff report status required"),
                Optional.ofNullable(insights).orElseGet(ImmutableList::of));
    }

    public List<DiffInsight> getInsights() {
        return insights;
    }

    public Status getStatus() {
        return status;
    }

}
