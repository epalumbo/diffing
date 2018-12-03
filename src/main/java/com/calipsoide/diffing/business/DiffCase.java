package com.calipsoide.diffing.business;

/**
 * Represents the diff entity.
 * Contains both sides of data, internal persistence ID, external resource ID and related diff results.
 */
public class DiffCase {

    private String id;

    private String name;

    private BinaryData leftData;

    private BinaryData rightData;

    private DiffReport report;

    private DiffCase(String id, String name, BinaryData leftData, BinaryData rightData, DiffReport report) {
        this.id = id;
        this.name = name;
        this.leftData = leftData;
        this.rightData = rightData;
        this.report = report;
    }

    public static Builder builder() {
        return new Builder();
    }

    public String getId() {
        return id;
    }

    public String getName() {
        return name;
    }

    public BinaryData getLeftData() {
        return leftData;
    }

    public BinaryData getRightData() {
        return rightData;
    }

    public DiffReport getReport() {
        return report;
    }

    /**
     * Generates a builder out of this instance's data.
     *
     * @return a open builder to modify any state and easily generate a copy
     */
    Builder copy() {
        return builder()
                .withId(id)
                .withName(name)
                .withLeftData(leftData)
                .withRightData(rightData)
                .withReport(report);
    }

    public static class Builder {

        private String id;

        private String name;

        private BinaryData leftData;

        private BinaryData rightData;

        private DiffReport report;

        public Builder withId(String id) {
            this.id = id;
            return this;
        }

        public Builder withName(String name) {
            this.name = name;
            return this;
        }

        public Builder withLeftData(BinaryData leftData) {
            this.leftData = leftData;
            return this;
        }

        public Builder withRightData(BinaryData rightData) {
            this.rightData = rightData;
            return this;
        }

        public Builder withReport(DiffReport report) {
            this.report = report;
            return this;
        }

        public DiffCase build() {
            return new DiffCase(id, name, leftData, rightData, report);
        }

    }

}
