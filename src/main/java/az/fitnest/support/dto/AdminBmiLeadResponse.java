package az.fitnest.support.dto;

import az.fitnest.support.model.enums.BmiLeadStatus;

import java.math.BigDecimal;
import java.time.Instant;

public record AdminBmiLeadResponse(
        long id,
        Instant createdAt,
        String phone,
        String email,
        String goalCode,
        String goalTitle,
        BigDecimal bmi,
        BmiLeadStatus status,
        Long assigneeUserId,
        String assigneeName,
        Instant lastContactAt,
        Integer age,
        String gender,
        BigDecimal heightCm,
        BigDecimal weightKg
) {}
