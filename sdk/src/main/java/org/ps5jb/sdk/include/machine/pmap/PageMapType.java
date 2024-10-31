package org.ps5jb.sdk.include.machine.pmap;

import org.ps5jb.sdk.res.ErrorMessages;

/**
 * FreeBSD <code>pmap_type</code> enum.
 */
public final class PageMapType implements Comparable {
    /** Regular x86 page tables. */
    public static final PageMapType PT_X86 = new PageMapType(0, "PT_X86");
    /** Intel's nested page tables. */
    public static final PageMapType PT_EPT = new PageMapType(1, "PT_EPT");
    /** AMD's nested page tables. */
    public static final PageMapType PT_RVI = new PageMapType(2, "PT_RVI");

    /** All possible PageMapType values. */
    private static final PageMapType[] values = new PageMapType[] {
            PT_X86,
            PT_EPT,
            PT_RVI
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
    private PageMapType(int value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Get all possible values for PageMapType.
     *
     * @return Array of PageMapType possible values.
     */
    public static PageMapType[] values() {
        return values;
    }

    /**
     * Convert a numeric value into a PageMapType constant.
     *
     * @param value Number to convert
     * @return PageMapType constant corresponding to the given value.
     * @throws IllegalArgumentException If value does not correspond to any PageMapType.
     */
    public static PageMapType valueOf(int value) {
        for (PageMapType pageMapType : values) {
            if (value == pageMapType.value()) {
                return pageMapType;
            }
        }

        throw new IllegalArgumentException(ErrorMessages.getClassErrorMessage(PageMapType.class,"invalidValue", Integer.toString(value)));
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public int value() {
        return this.value;
    }

    @Override
    public int compareTo(Object o) {
        return this.value - ((PageMapType) o).value;
    }

    @Override
    public boolean equals(Object o) {
        boolean result;
        if (o instanceof PageMapType) {
            result = value == ((PageMapType) o).value;
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
