package az.fitnest.support.dto;

import az.fitnest.support.model.enums.BmiLeadStatus;
import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record AdminBmiLeadUpdateRequest(
        BmiLeadStatus status,
        Long assigneeUserId,
        @Size(max = 80) String assigneeName,
        Boolean unassign,
        Boolean touched
) {}
