package az.fitnest.support.repository;

import az.fitnest.support.model.entity.BmiLead;
import az.fitnest.support.model.enums.BmiLeadStatus;
import org.springframework.data.jpa.domain.Specification;

import java.time.Instant;

public final class BmiLeadSpecifications {

    private BmiLeadSpecifications() {
    }

    public static Specification<BmiLead> withFilters(
            String search,
            BmiLeadStatus status,
            String goalCode,
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
                        // ignore unparseable padded ids
                    }
                    ors.add(cb.like(root.get("phone"), "%" + digits + "%"));
                } else {
                    String pattern = "%" + trimmed.toLowerCase(java.util.Locale.ROOT)
                            .replace("\\", "\\\\")
                            .replace("%", "\\%")
                            .replace("_", "\\_") + "%";
                    ors.add(cb.like(cb.lower(root.get("goalTitle")), pattern, '\\'));
                    ors.add(cb.like(cb.lower(root.get("assigneeName")), pattern, '\\'));
                    ors.add(cb.like(cb.lower(root.get("email")), pattern, '\\'));
                }
                predicates.add(cb.or(ors.toArray(jakarta.persistence.criteria.Predicate[]::new)));
            }

            if (status != null) {
                predicates.add(cb.equal(root.get("status"), status));
            }
            if (goalCode != null && !goalCode.isBlank()) {
                predicates.add(cb.equal(root.get("goalCode"), goalCode));
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
