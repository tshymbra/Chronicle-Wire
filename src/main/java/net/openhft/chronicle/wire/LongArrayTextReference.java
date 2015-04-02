package net.openhft.chronicle.wire;

import net.openhft.chronicle.bytes.Byteable;
import net.openhft.chronicle.bytes.Bytes;
import net.openhft.chronicle.bytes.BytesStore;
import net.openhft.chronicle.core.OS;
import net.openhft.chronicle.core.values.LongArrayValues;

/*
The format for a long array in text is
{ capacity: 12345678901234567890, values: [ 12345678901234567890, ... ] }
 */

public class LongArrayTextReference implements LongArrayValues, Byteable {
    static final byte[] SECTION1 = "{ capacity: ".getBytes();
    static final byte[] SECTION2 = ", values: [ ".getBytes();
    static final byte[] SECTION3 = " ] }\n".getBytes();
    static final byte[] ZERO = "00000000000000000000".getBytes();
    static final byte[] SEP = ", ".getBytes();

    private static final int DIGITS = ZERO.length;
    private static final int CAPACITY = SECTION1.length;
    private static final int VALUES = CAPACITY + DIGITS + SECTION2.length;
    private static final int VALUE_SIZE = DIGITS + SEP.length;

    private BytesStore bytes;
    private long offset;
    private long length = VALUES;

    @Override
    public long getCapacity() {
        return (length - VALUES) / VALUE_SIZE;
    }

    @Override
    public long getValueAt(long index) {
        return bytes.parseLong(VALUES + offset + index * VALUE_SIZE);
    }

    @Override
    public void setValueAt(long index, long value) {
        bytes.append(VALUES + offset + index * VALUE_SIZE, value, DIGITS);
    }

    @Override
    public long getVolatileValueAt(long index) {
        OS.memory().loadFence();
        return getValueAt(index);
    }

    @Override
    public void setOrderedValueAt(long index, long value) {
        setValueAt(index, value);
        OS.memory().storeFence();
    }

    @Override
    public void bytesStore(BytesStore bytes, long offset, long length) {
        if (length != peakLength(bytes, offset))
            throw new IllegalArgumentException(length + " != " + peakLength(bytes, offset));
        this.bytes = bytes;
        this.offset = offset;
        this.length = length;
    }

    public static long peakLength(BytesStore bytes, long offset) {
        return (bytes.parseLong(offset + CAPACITY) * VALUE_SIZE) + VALUES + SECTION3.length - SEP.length;
    }

    @Override
    public BytesStore bytesStore() {
        return bytes;
    }

    @Override
    public long offset() {
        return offset;
    }

    @Override
    public long maxSize() {
        return length;
    }

    public String toString() {
        return "value: " + getValueAt(0) + " ...";
    }

    public static void write(Bytes bytes, long capacity) {
        bytes.write(SECTION1);
        bytes.append(capacity, 20);
        bytes.write(SECTION2);
        for (long i = 0; i < capacity; i++) {
            if (i > 0)
                bytes.append(", ");
            bytes.write(ZERO);
        }
        bytes.write(SECTION3);
    }

}