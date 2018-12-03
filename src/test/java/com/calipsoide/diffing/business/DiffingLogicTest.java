package com.calipsoide.diffing.business;

import org.junit.jupiter.api.Test;

import java.util.Arrays;
import java.util.List;

import static com.calipsoide.diffing.business.DiffReport.Status.*;
import static org.apache.commons.lang3.RandomUtils.nextBytes;
import static org.assertj.core.api.Assertions.assertThat;

class DiffingLogicTest {

    private final DiffingLogic logic = new DiffingLogic();

    @Test
    void lengthMismatch() {
        final byte[] leftBytes = nextBytes(32);
        final byte[] rightBytes = nextBytes(leftBytes.length - 1);
        final BinaryData leftData = BinaryData.of(leftBytes);
        final BinaryData rightData = BinaryData.of(rightBytes);
        final DiffReport report = logic.diff(leftData, rightData);
        assertThat(report.getStatus()).isEqualTo(LENGTH_MISMATCH);
        assertThat(report.getInsights()).isEmpty();
    }

    @Test
    void lengthMismatchOnEmptySide() {
        final BinaryData data = BinaryData.of(nextBytes(32));
        final BinaryData empty = BinaryData.empty();
        final DiffReport left = logic.diff(data, empty);
        assertThat(left.getStatus()).isEqualTo(LENGTH_MISMATCH);
        assertThat(left.getInsights()).isEmpty();
        final DiffReport right = logic.diff(empty, data);
        assertThat(right.getStatus()).isEqualTo(LENGTH_MISMATCH);
        assertThat(right.getInsights()).isEmpty();
    }

    @Test
    void emptySides() {
        final BinaryData leftData = BinaryData.empty();
        final BinaryData rightData = BinaryData.empty();
        final DiffReport report = logic.diff(leftData, rightData);
        assertThat(report.getStatus()).isEqualTo(EQUAL);
        assertThat(report.getInsights()).isEmpty();
    }

    @Test
    void sidesEqual() {
        final byte[] leftBytes = nextBytes(32);
        final byte[] rightBytes = Arrays.copyOf(leftBytes, leftBytes.length);
        final BinaryData leftData = BinaryData.of(leftBytes);
        final BinaryData rightData = BinaryData.of(rightBytes);
        final DiffReport report = logic.diff(leftData, rightData);
        assertThat(report.getStatus()).isEqualTo(EQUAL);
        assertThat(report.getInsights()).isEmpty();
    }

    @Test
    void manyDifferences() {
        final byte[] leftBytes = nextBytes(16);
        final byte[] rightBytes = Arrays.copyOf(leftBytes, leftBytes.length);
        // create some differences between the arrays
        // offset 3, length 2
        rightBytes[3] = (byte) ~rightBytes[3];
        rightBytes[4] = (byte) ~rightBytes[4];
        // offset 7, length 1
        rightBytes[7] = (byte) ~rightBytes[7];
        // offset 9, length 3
        rightBytes[9] = (byte) ~rightBytes[9];
        rightBytes[10] = (byte) ~rightBytes[10];
        rightBytes[11] = (byte) ~rightBytes[11];
        // offset 14, length 2
        rightBytes[14] = (byte) ~rightBytes[14];
        rightBytes[15] = (byte) ~rightBytes[15];
        final BinaryData leftData = BinaryData.of(leftBytes);
        final BinaryData rightData = BinaryData.of(rightBytes);
        final DiffReport report = logic.diff(leftData, rightData);
        assertThat(report.getStatus()).isEqualTo(NOT_EQUAL);
        final List<DiffInsight> insights = report.getInsights();
        assertThat(insights).hasSize(4);
        // now check the results for each difference
        final DiffInsight first = insights.get(0);
        assertThat(first.getOffset()).isEqualTo(3);
        assertThat(first.getLength()).isEqualTo(2);
        final DiffInsight second = insights.get(1);
        assertThat(second.getOffset()).isEqualTo(7);
        assertThat(second.getLength()).isEqualTo(1);
        final DiffInsight third = insights.get(2);
        assertThat(third.getOffset()).isEqualTo(9);
        assertThat(third.getLength()).isEqualTo(3);
        final DiffInsight forth = insights.get(3);
        assertThat(forth.getOffset()).isEqualTo(14);
        assertThat(forth.getLength()).isEqualTo(2);
    }

    @Test
    void completelyDifferentData() {
        final byte[] leftBytes = nextBytes(32);
        final byte[] rightBytes = Arrays.copyOf(leftBytes, leftBytes.length);
        // ensure completely different data by changing every byte in the array
        for (int i = 0; i < rightBytes.length; i++) {
            rightBytes[i] = (byte) ~rightBytes[i];
        }
        final BinaryData leftData = BinaryData.of(leftBytes);
        final BinaryData rightData = BinaryData.of(rightBytes);
        final DiffReport report = logic.diff(leftData, rightData);
        assertThat(report.getStatus()).isEqualTo(NOT_EQUAL);
        final List<DiffInsight> insights = report.getInsights();
        assertThat(insights).hasSize(1);
        final DiffInsight difference = insights.get(0);
        assertThat(difference.getOffset()).isEqualTo(0);
        assertThat(difference.getLength()).isEqualTo(leftBytes.length);
    }

}