package az.fitnest.support.dto;

import az.fitnest.support.model.enums.PartnerLeadStatus;
import jakarta.validation.constraints.Size;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminPartnerLeadUpdateRequest(
        PartnerLeadStatus status,
        Long assigneeUserId,
        @Size(max = 80) String assigneeName,
        Boolean unassign,
        Boolean touched
) {}
