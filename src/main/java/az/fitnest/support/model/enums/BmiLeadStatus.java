package az.fitnest.support.model.enums;

import java.util.Locale;

public enum BmiLeadStatus {
    NEW,
    CONTACTING,
    CONTACTED,
    CONVERTED,
    CLOSED;

    public static BmiLeadStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return BmiLeadStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
