package az.fitnest.support.dto;

import java.util.List;

public record AdminPartnerLeadPageResponse(
        List<AdminPartnerLeadResponse> items,
        long total,
        int page,
        int pageSize
) {}
