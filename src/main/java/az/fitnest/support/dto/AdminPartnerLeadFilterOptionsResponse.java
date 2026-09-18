package az.fitnest.support.dto;

import java.util.List;

public record AdminPartnerLeadFilterOptionsResponse(
        List<ActivityOption> activities,
        List<AssigneeOption> assignees
) {
    public record ActivityOption(String value, String label) {}

    public record AssigneeOption(long id, String name) {}
}
