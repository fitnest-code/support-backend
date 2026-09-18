package az.fitnest.support.model.enums;

import java.util.Locale;

public enum PartnerLeadStatus {
    NEW,
    CONTACTED_WAITING,
    REJECTED,
    AWAITING_DETAILS,
    APPROVED,
    CONTRACT_SIGNED;

    public static PartnerLeadStatus from(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        try {
            return PartnerLeadStatus.valueOf(raw.trim().toUpperCase(Locale.ROOT));
        } catch (IllegalArgumentException ignored) {
            return null;
        }
    }
}
