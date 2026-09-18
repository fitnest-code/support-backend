package az.fitnest.support.model.entity;

import az.fitnest.support.model.enums.BmiLeadStatus;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Index;
import jakarta.persistence.Table;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.math.BigDecimal;
import java.time.Instant;

@Entity
@Table(
        name = "bmi_leads",
        indexes = {
                @Index(name = "idx_bmi_leads_status_created", columnList = "status, created_at"),
                @Index(name = "idx_bmi_leads_phone", columnList = "phone"),
                @Index(name = "idx_bmi_leads_goal_code", columnList = "goal_code"),
                @Index(name = "idx_bmi_leads_assignee", columnList = "assignee_user_id")
        }
)
@Getter
@Setter
@NoArgsConstructor
public class BmiLead extends BaseEntity {

    @Column(name = "phone", nullable = false, length = 16)
    private String phone;

    @Column(name = "email", length = 120)
    private String email;

    @Column(name = "goal_code", nullable = false, length = 255)
    private String goalCode;

    @Column(name = "goal_title", nullable = false, length = 500)
    private String goalTitle;

    @Column(name = "height_cm", nullable = false, precision = 6, scale = 2)
    private BigDecimal heightCm;

    @Column(name = "weight_kg", nullable = false, precision = 6, scale = 2)
    private BigDecimal weightKg;

    @Column(name = "bmi", nullable = false, precision = 5, scale = 1)
    private BigDecimal bmi;

    @Column(name = "age")
    private Integer age;

    @Column(name = "gender", length = 8)
    private String gender;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    private BmiLeadStatus status;

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
