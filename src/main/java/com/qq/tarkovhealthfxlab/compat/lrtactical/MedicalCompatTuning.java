package com.qq.tarkovhealthfxlab.compat.lrtactical;

/** Central compatibility timing values; ready to be backed by server config later. */
public record MedicalCompatTuning(int ibuprofenAnalgesiaDurationTicks) {
    public static final int DEFAULT_IBUPROFEN_ANALGESIA_DURATION_TICKS = 20 * 120;
    public static final MedicalCompatTuning DEFAULT = new MedicalCompatTuning(
            DEFAULT_IBUPROFEN_ANALGESIA_DURATION_TICKS
    );

    public MedicalCompatTuning {
        ibuprofenAnalgesiaDurationTicks = Math.max(1, ibuprofenAnalgesiaDurationTicks);
    }
}
