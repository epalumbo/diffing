package com.calipsoide.diffing.persistence;

import org.springframework.data.annotation.Id;

import java.util.List;

/**
 * Object representation of a diff case's MongoDB document, used to persist state of the diff model (business package).
 * <p>
 * As we don't want to have any hidden logic here, document objects use just public fields to hold data.
 */
public class DiffCaseDocument {

    @Id
    public String id;

    public String name;

    public byte[] left;

    public byte[] right;

    public DiffReportDocument report;

    public static class DiffReportDocument {

        public String status;

        public List<DiffInsightDocument> insights;

    }

    public static class DiffInsightDocument {

        public int offset;

        public int length;

    }

}
