package org.ps5jb.sdk.include.sys.rtprio;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * Process realtime-priority specifications to rtprio.
 */
public final class RtPrioType implements Comparable {
    /** Real time process. */
    public static final RtPrioType RTP_PRIO_REALTIME = new RtPrioType((short) 2, "RTP_PRIO_REALTIME");
    /** Time sharing process. */
    public static final RtPrioType RTP_PRIO_NORMAL = new RtPrioType((short) 3, "RTP_PRIO_NORMAL");
    /** Idle process. */
    public static final RtPrioType RTP_PRIO_IDLE = new RtPrioType((short) 4, "RTP_PRIO_IDLE");

    public static final RtPrioType RTP_PRIO_FIFO = new RtPrioType((short) (8 | RTP_PRIO_REALTIME.value), "RTP_PRIO_FIFO");

    /** All possible RtPrioType values. */
    private static final RtPrioType[] values = new RtPrioType[] {
            RTP_PRIO_REALTIME,
            RTP_PRIO_NORMAL,
            RTP_PRIO_IDLE,
            RTP_PRIO_FIFO
    };

    private short value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private RtPrioType(short value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for RtPrioType.
     *
     * @return Array of RtPrioType possible values.
     */
    public static RtPrioType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a RtPrioType constant.
     *
     * @param value Number to convert
     * @return RtPrioType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any CpuWhichType.
     */
    public static RtPrioType valueOf(short value) {
        for (RtPrioType rtPrioType : values) {
            if (value == rtPrioType.value()) {
                return rtPrioType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(RtPrioType.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public short value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((RtPrioType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof RtPrioType) {
            result = value == ((RtPrioType) o).value;
        } else {
            result = false;
        }
        return result;
    }

    @Override
    public int hashCode() {
        return value;
    }

    @Override
    public String toString() {
        return name;
    }
}
