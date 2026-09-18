package az.fitnest.support.dto;

import az.fitnest.support.model.enums.PartnerLeadStatus;

import java.time.Instant;

public record AdminPartnerLeadResponse(
        long id,
        Instant createdAt,
        String gymName,
        String contactName,
        String phone,
        String email,
        String activity,
        PartnerLeadStatus status,
        Long assigneeUserId,
        String assigneeName,
        Instant lastContactAt
) {}
