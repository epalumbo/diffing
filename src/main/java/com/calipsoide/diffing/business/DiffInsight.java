package com.calipsoide.diffing.business;

import static com.google.common.base.Preconditions.checkArgument;

/**
 * Basic information to help the client understand where the difference is located.
 */
public class DiffInsight {

    private final int offset;

    private final int length;

    public DiffInsight(int offset, int length) {
        checkArgument(offset >= 0, "offset cannot be negative");
        checkArgument(length >= 0, "length cannot be negative");
        this.offset = offset;
        this.length = length;
    }

    public int getOffset() {
        return offset;
    }

    public int getLength() {
        return length;
    }

}
