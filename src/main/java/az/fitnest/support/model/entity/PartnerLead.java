package az.fitnest.support.model.entity;

import az.fitnest.support.model.enums.PartnerLeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.Instant;

@Entity
@Table(
        name = "partner_leads",
        indexes = {
                @Index(name = "idx_partner_leads_status_created", columnList = "status, created_at"),
                @Index(name = "idx_partner_leads_phone", columnList = "phone"),
                @Index(name = "idx_partner_leads_assignee", columnList = "assignee_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class PartnerLead extends BaseEntity {

    @Column(name = "gym_name", nullable = false, length = 120)
    private String gymName;

    @Column(name = "contact_name", nullable = false, length = 80)
    private String contactName;

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "activity", nullable = false, length = 120)
    private String activity;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 32)
    private PartnerLeadStatus status;

    @Column(name = "assignee_user_id")
    private Long assigneeUserId;

    @Column(name = "assignee_name", length = 80)
    private String assigneeName;

    @Column(name = "last_contact_at")
    private Instant lastContactAt;

    @Column(name = "created_at", nullable = false, updatable = false)
    private Instant createdAt;

    @Column(name = "updated_at", nullable = false)
    private Instant updatedAt;
}
