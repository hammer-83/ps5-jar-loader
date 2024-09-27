package org.ps5jb.sdk.include.sys.rtprio;

/**
 * Wrapper for FreeBSD <code>rtprio</code> structure.
 */
public class RtPrio {
    public static final short RTP_PRIO_MIN = 0;
    public static final short RTP_PRIO_MAX = 31;

    public static final int RTP_LOOKUP = 0;
    public static final int RTP_SET = 1;

    private RtPrioType type;
    private short priority;

    /**
     * RtPrio constructor.
     *
     * @param type Scheduling class.
     * @param priority Priority value.
     */
    public RtPrio(RtPrioType type, short priority) {
        this.type = type;
        this.priority = priority;
    }

    /**
     * @return Scheduling class.
     */
    public RtPrioType getType() {
        return type;
    }

    /**
     * @return Priority value.
     */
    public short getPriority() {
        return priority;
    }

    @Override
    public String toString() {
        return getType().toString() + ": " + getPriority();
    }
}
