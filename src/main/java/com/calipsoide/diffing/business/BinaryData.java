package com.calipsoide.diffing.business;

import static com.google.common.base.Preconditions.checkArgument;
import static com.google.common.base.Preconditions.checkNotNull;
import static com.google.common.io.BaseEncoding.base64;

/**
 * Convenient wrapper for byte arrays.
 * Represents the binary data on which diffs can be executed.
 */
public class BinaryData {

    private final byte[] bytes;

    private BinaryData(byte[] bytes) {
        this.bytes = checkNotNull(bytes, "cannot create binary data read no bytes");
    }

    public static BinaryData empty() {
        return new BinaryData(new byte[0]);
    }

    public static BinaryData of(byte[] bytes) {
        return new BinaryData(bytes);
    }

    /**
     * Get bytes from a base64 string.
     *
     * @param data base64 string to be decoded
     * @return a new instance of {@link BinaryData} containing the bytes decoded from the base64 string
     */
    public static BinaryData read(String data) {
        return of(base64().decode(data));
    }

    public byte[] getBytes() {
        return bytes;
    }

    public int getLength() {
        return bytes.length;
    }

    public byte getByteAt(int position) {
        checkArgument(position >= 0 && position < bytes.length, "invalid position");
        return bytes[position];
    }

}
