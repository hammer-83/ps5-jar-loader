package org.ps5jb.sdk.include.machine.pmap;

public class PageMapEntryMask {
    /*
     * Page-directory and page-table entries follow this format, with a few
     * of the fields not present here and there, depending on a lot of things.
     */

    /** P Valid */
    public static final PageMapEntryMask X86_PG_V = new PageMapEntryMask(0x001, "X86_PG_V");
    /** R/W Read/Write */
    public static final PageMapEntryMask X86_PG_RW = new PageMapEntryMask(0x002, "X86_PG_RW");
    /** U/S User/Supervisor */
    public static final PageMapEntryMask X86_PG_U = new PageMapEntryMask(0x004, "X86_PG_U");
    /** PS Page size (0=4k,1=2M) */
    public static final PageMapEntryMask X86_PG_PS = new PageMapEntryMask(0x080, "X86_PG_PS");
    /** XO Execute-only */
    public static final PageMapEntryMask SCE_PG_XO = new PageMapEntryMask(1L << 58, "SCE_PG_XO");

    /*
     * Intel extended page table (EPT) bit definitions.
     */

    /** R Read */
    public static final PageMapEntryMask EPT_PG_READ = new PageMapEntryMask(0x001, "EPT_PG_READ");
    /** W Write */
    public static final PageMapEntryMask EPT_PG_WRITE = new PageMapEntryMask(0x002, "EPT_PG_WRITE");
    /** X Execute */
    public static final PageMapEntryMask EPT_PG_EXECUTE = new PageMapEntryMask(0x004, "EPT_PG_EXECUTE");
    public static final PageMapEntryMask EPT_PG_EMUL_V = new PageMapEntryMask(1L << 52, "EPT_PG_EMUL_V");
    public static final PageMapEntryMask EPT_PG_EMUL_RW = new PageMapEntryMask(1L << 53, "EPT_PG_EMUL_RW");
    public static final PageMapEntryMask PG_PHYS_FRAME = new PageMapEntryMask(0x000ffffffffff000L, "PG_PHYS_FRAME");
    public static final PageMapEntryMask PG_FRAME = new PageMapEntryMask(0x000fffffffffc000L, "PG_FRAME");
    public static final PageMapEntryMask PG_PS_FRAME = new PageMapEntryMask(0x000fffffffe00000L, "PG_PS_FRAME");

    private long value;

    private String name;

    /**
     * Default constructor. This class should not be instantiated manually,
     * use provided constants instead.
     *
     * @param value Numeric value of this instance.
     * @param name String representation of the constant.
     */
    private PageMapEntryMask(long value, String name) {
        this.value = value;
        this.name = name;
    }

    /**
     * Numeric value of this instance.
     *
     * @return Numeric value of the instance.
     */
    public long value() {
        return this.value;
    }

    @Override
    public String toString() {
        return name;
    }
}
