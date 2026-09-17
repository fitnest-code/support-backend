package az.fitnest.support.dto;

import java.util.List;

public record AdminBmiLeadFilterOptionsResponse(
        List<GoalOption> goals,
        List<AssigneeOption> assignees
) {
    public record GoalOption(String code, String title) {}

    public record AssigneeOption(long id, String name) {}
}
