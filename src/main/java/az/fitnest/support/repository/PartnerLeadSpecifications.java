package az.fitnest.support.repository;

import az.fitnest.support.model.entity.PartnerLead;
import az.fitnest.support.model.enums.PartnerLeadStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class PartnerLeadSpecifications {

    private PartnerLeadSpecifications() {
    }

    public static Specification<PartnerLead> withFilters(
            String search,
            PartnerLeadStatus status,
            String activity,
            String assignee,
            Instant createdFrom,
            Instant createdTo
    ) {
        return (root, query, cb) -> {
            var predicates = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();

            if (search != null && !search.isBlank()) {
                String trimmed = search.trim();
                String digits = trimmed.replaceAll("\\D", "");
                var ors = new java.util.ArrayList<jakarta.persistence.criteria.Predicate>();
                if (digits.matches("\\d{1,18}")) {
                    try {
                        ors.add(cb.equal(root.get("id"), Long.parseLong(digits)));
                    } catch (NumberFormatException ignored) {
                    }
                    ors.add(cb.like(root.get("phone"), "%" + digits + "%"));
                } else {
                    String pattern = "%" + trimmed.toLowerCase(java.util.Locale.ROOT)
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_") + "%";
                    ors.add(cb.like(cb.lower(root.get("gymName")), pattern, '\\'));
                    ors.add(cb.like(cb.lower(root.get("contactName")), pattern, '\\'));
                    ors.add(cb.like(cb.lower(root.get("email")), pattern, '\\'));
                    ors.add(cb.like(cb.lower(root.get("activity")), pattern, '\\'));
                }
                predicates.add(cb.or(ors.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (activity != null && !activity.isBlank()) {
                predicates.add(cb.equal(root.get("activity"), activity));
            }
            if (assignee != null && !assignee.isBlank()) {
                if ("unassigned".equalsIgnoreCase(assignee)) {
                    predicates.add(cb.isNull(root.get("assigneeUserId")));
                } else if (assignee.matches("\\d{1,18}")) {
                    predicates.add(cb.equal(root.get("assigneeUserId"), Long.parseLong(assignee)));
                }
            }
            if (createdFrom != null) {
                predicates.add(cb.greaterThanOrEqualTo(root.get("createdAt"), createdFrom));
            }
            if (createdTo != null) {
                predicates.add(cb.lessThan(root.get("createdAt"), createdTo));
            }

            return cb.and(predicates.toArray(jakarta.persistence.criteria.Predicate[]::new));
        };
    }
}
