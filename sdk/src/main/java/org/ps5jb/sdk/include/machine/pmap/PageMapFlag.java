package org.ps5jb.sdk.include.machine.pmap;

import java.util.ArrayList;
import java.util.List;

/**
 * Values for {@link PageMap#getFlags()}.
 */
public final class PageMapFlag implements Comparable {
    public static final PageMapFlag PMAP_NESTED_IPIMASK = new PageMapFlag(0xFF, "PMAP_NESTED_IPIMASK");
    /** Supports 2MB superpages. */
    public static final PageMapFlag PMAP_PDE_SUPERPAGE = new PageMapFlag(1 << 8, "PMAP_PDE_SUPERPAGE");
    /** Needs A/D bits emulation. */
    public static final PageMapFlag PMAP_EMULATE_AD_BITS = new PageMapFlag(1 << 9, "PMAP_EMULATE_AD_BITS");
    /** Execute only mappings ok. */
    public static final PageMapFlag PMAP_SUPPORTS_EXEC_ONLY = new PageMapFlag(1 << 10, "PMAP_SUPPORTS_EXEC_ONLY");

    /** All possible PageMapFlag values. */
    private static final PageMapFlag[] values = new PageMapFlag[] {
            PMAP_NESTED_IPIMASK,
            PMAP_PDE_SUPERPAGE,
            PMAP_EMULATE_AD_BITS,
            PMAP_SUPPORTS_EXEC_ONLY
    };

    private int value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private PageMapFlag(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for PageMapFlag.
     *
     * @return Array of PageMapFlag possible values.
     */
    public static PageMapFlag[] values() {
        return values;
    }

    /**
     * Convert a numeric value into an array of PageMapFlag constants.
     *
     * @param value Number to convert
     * @return Array of PageMapFlag constants ORed in the numeric value.
     */
    public static PageMapFlag[] valueOf(int value) {
        List result = new ArrayList();
        for (PageMapFlag flag : values) {
            if ((value & flag.value) == flag.value) {
                result.add(flag);
            }
        }

        return (PageMapFlag[]) result.toArray(new PageMapFlag[result.size()]);
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    /**
     * Combines an array of modes using a bitwise OR operator.
     *
     * @param flags Flags to combine.
     * @return Result of taking {@link #value()} of each mode and doing a bitwise OR operator on them.
     */
    public static short or(PageMapFlag... flags) {
        short result = 0;
        for (PageMapFlag flag : flags) {
            result |= flag.value;
        }
        return result;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((PageMapFlag) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof PageMapFlag) {
            result = value == ((PageMapFlag) o).value;
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
