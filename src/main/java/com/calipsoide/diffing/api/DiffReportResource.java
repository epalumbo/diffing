package com.calipsoide.diffing.api;

import com.fasterxml.jackson.annotation.JsonInclude;

/**
 * Represents the API contract (response body) of the endpoint that returns information regarding some diff operation.
 * It has only public fields to avoid hidden logic here.
 */
public class DiffReportResource {

    public final String status;

    @JsonInclude(JsonInclude.Include.NON_EMPTY)
    public final DiffInsightResource[] insights;

    public DiffReportResource(String status, DiffInsightResource[] insights) {
        this.status = status;
        this.insights = insights;
    }

    public static class DiffInsightResource {

        public final int offset;

        public final int length;

        public DiffInsightResource(int offset, int length) {
            this.offset = offset;
            this.length = length;
        }

    }

}
