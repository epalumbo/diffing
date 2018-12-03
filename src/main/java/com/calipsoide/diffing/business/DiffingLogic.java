package com.calipsoide.diffing.business;

import com.calipsoide.diffing.business.DiffReport.Status;
import org.springframework.stereotype.Component;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

import static com.calipsoide.diffing.business.DiffReport.Status.*;
import static java.util.stream.IntStream.range;

/**
 * Encapsulates the diff logic that this application applies on two streams of binary data.
 * Therefore, if API contract stays the same, diff logic could be evolved in complexity without
 * changing the rest of the system.
 */
@Component
class DiffingLogic {

    /**
     * Implements the diff algorithm between two binary data instances.
     * <p>
     * Diff is actually computed only if both sides are of equal size, otherwise
     * {@link Status#LENGTH_MISMATCH} is returned. If both sides are equal,
     * report results in status {@link Status#EQUAL}. In other case, {@link Status#NOT_EQUAL}
     * is returned, along with some insights (offset, length) on where the differences are.
     *
     * @param leftData
     * @param rightData
     * @return a {@link DiffReport} containing diff results
     */
    DiffReport diff(BinaryData leftData, BinaryData rightData) {
        if (leftData.getLength() == rightData.getLength()) {
            final int length = leftData.getLength();
            final DiffCounter counter = new DiffCounter();
            range(0, length)
                    .mapToObj(index -> leftData.getByteAt(index) == rightData.getByteAt(index))
                    .forEach(counter::count);
            final List<DiffInsight> insights = counter.results();
            final Status status = insights.isEmpty() ? EQUAL : NOT_EQUAL;
            return DiffReport.of(status, insights);
        } else {
            return DiffReport.of(LENGTH_MISMATCH);
        }
    }

    private static class DiffCounter {

        private List<DiffInsight> insights;

        private DiffInsight current;

        private int index;

        private DiffCounter() {
            index = 0;
            insights = new ArrayList<>();
        }

        private void ok() {
            Optional.ofNullable(current).ifPresent(insights::add);
            current = null;
        }

        private List<DiffInsight> results() {
            ok();
            return insights;
        }

        private void count(boolean equal) {
            if (equal) {
                ok();
            } else if (current == null) {
                // new difference found
                current = new DiffInsight(index, 1);
            } else {
                // increment current difference by one in length
                current = new DiffInsight(current.getOffset(), current.getLength() + 1);
            }
            index++;
        }

    }
}
