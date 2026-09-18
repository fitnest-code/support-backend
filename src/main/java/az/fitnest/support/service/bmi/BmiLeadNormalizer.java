package az.fitnest.support.service.bmi;

import az.fitnest.support.exception.BadRequestException;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.LinkedHashSet;
import java.util.Locale;
import java.util.regex.Pattern;

public final class BmiLeadNormalizer {

    static final Pattern PHONE_E164 = Pattern.compile("^\\+994(10|50|51|55|60|70|77|99)\\d{7}$");
    static final Pattern GOAL_CODE = Pattern.compile("^[A-Za-z0-9][A-Za-z0-9_-]{1,63}$");
    static final Pattern CONTROL = Pattern.compile("[\\p{Cntrl}<>]");
    static final Pattern EMAIL = Pattern.compile("^[^\\s@<>]{1,64}@[^\\s@<>]{1,255}$");

    private BmiLeadNormalizer() {
    }

    public static String phone(String raw) {
        if (raw == null) {
            throw new BadRequestException("Invalid phone");
        }
        String digits = raw.replaceAll("\\D", "");
        if (digits.startsWith("00")) {
            digits = digits.substring(2);
        }
        if (digits.length() == 9) {
            digits = "994" + digits;
        }
        if (digits.length() == 10 && digits.startsWith("0")) {
            digits = "994" + digits.substring(1);
        }
        String e164 = "+" + digits;
        if (!PHONE_E164.matcher(e164).matches()) {
            throw new BadRequestException("Invalid phone");
        }
        return e164;
    }

    public static String goalCode(String raw) {
        String cleaned = CONTROL.matcher(raw == null ? "" : raw.trim()).replaceAll("");
        LinkedHashSet<String> codes = new LinkedHashSet<>();
        for (String part : cleaned.split("[,+]+")) {
            String code = part.trim();
            if (code.isEmpty()) {
                continue;
            }
            if (!GOAL_CODE.matcher(code).matches()) {
                throw new BadRequestException("Invalid goal");
            }
            codes.add(code);
        }
        if (codes.isEmpty()) {
            throw new BadRequestException("Invalid goal");
        }
        String joined = String.join(",", codes);
        return joined.length() > 255 ? joined.substring(0, 255) : joined;
    }

    public static String goalTitle(String raw) {
        String cleaned = cleanText(raw, 500);
        if (cleaned.length() < 2) {
            throw new BadRequestException("Invalid goal");
        }
        return cleaned;
    }

    public static String gender(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String value = raw.trim().toUpperCase(Locale.ROOT);
        if ("M".equals(value) || "MALE".equals(value) || "KİŞİ".equals(value) || "KISI".equals(value)) {
            return "MALE";
        }
        if ("F".equals(value) || "FEMALE".equals(value) || "QADIN".equals(value)) {
            return "FEMALE";
        }
        throw new BadRequestException("Invalid gender");
    }

    public static BigDecimal bmi(BigDecimal heightCm, BigDecimal weightKg) {
        if (heightCm == null || weightKg == null) {
            throw new BadRequestException("Invalid body metrics");
        }
        BigDecimal heightM = heightCm.divide(BigDecimal.valueOf(100), 6, RoundingMode.HALF_UP);
        if (heightM.compareTo(BigDecimal.ZERO) <= 0) {
            throw new BadRequestException("Invalid body metrics");
        }
        return weightKg.divide(heightM.multiply(heightM), 1, RoundingMode.HALF_UP);
    }

    public static String assigneeName(String raw) {
        String cleaned = cleanText(raw, 80);
        return cleaned.isBlank() ? null : cleaned;
    }

    public static String clientIp(String raw) {
        if (raw == null || raw.isBlank() || raw.length() > 45) {
            return "unknown";
        }
        String first = raw.split(",")[0].trim();
        if (first.length() > 45 || first.indexOf('\r') >= 0 || first.indexOf('\n') >= 0) {
            return "unknown";
        }
        return first;
    }

    public static String optionalEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String email = raw.trim().toLowerCase(Locale.ROOT);
        if (!EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Invalid email");
        }
        return email.length() > 120 ? email.substring(0, 120) : email;
    }

    public static String cleanText(String raw, int max) {
        if (raw == null) {
            return "";
        }
        String trimmed = CONTROL.matcher(raw).replaceAll(" ").trim().replaceAll("\\s+", " ");
        return trimmed.length() > max ? trimmed.substring(0, max) : trimmed;
    }
}
