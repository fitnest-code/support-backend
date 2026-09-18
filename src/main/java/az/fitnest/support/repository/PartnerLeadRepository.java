package az.fitnest.support.repository;

import az.fitnest.support.model.entity.PartnerLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface PartnerLeadRepository extends JpaRepository<PartnerLead, Long>, JpaSpecificationExecutor<PartnerLead> {

    boolean existsByPhoneAndCreatedAtAfter(String phone, Instant createdAfter);

    @Query("select distinct p.activity from PartnerLead p where p.activity is not null")
    List<String> findDistinctActivities();

    @Query("""
            select distinct p.assigneeUserId, p.assigneeName
            from PartnerLead p
            where p.assigneeUserId is not null
            """)
    List<Object[]> findDistinctAssignees();
}
