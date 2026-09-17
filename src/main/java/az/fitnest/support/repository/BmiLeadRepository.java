package az.fitnest.support.repository;

import az.fitnest.support.model.entity.BmiLead;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.data.jpa.repository.Query;

import java.time.Instant;
import java.util.List;

public interface BmiLeadRepository extends JpaRepository<BmiLead, Long>, JpaSpecificationExecutor<BmiLead> {

    boolean existsByPhoneAndCreatedAtAfter(String phone, Instant createdAfter);

    @Query("select distinct b.goalCode, b.goalTitle from BmiLead b")
    List<Object[]> findDistinctGoals();

    @Query("""
            select distinct b.assigneeUserId, b.assigneeName
            from BmiLead b
            where b.assigneeUserId is not null
            """)
    List<Object[]> findDistinctAssignees();
}
