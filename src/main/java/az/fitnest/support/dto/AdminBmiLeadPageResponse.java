package az.fitnest.support.dto;

import java.util.List;

public record AdminBmiLeadPageResponse(
        List<AdminBmiLeadResponse> items,
        long total,
        int page,
        int pageSize
) {}
