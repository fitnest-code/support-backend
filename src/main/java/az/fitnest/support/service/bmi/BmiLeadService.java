package az.fitnest.support.service.bmi;

import az.fitnest.support.dto.AdminBmiLeadFilterOptionsResponse;
import az.fitnest.support.dto.AdminBmiLeadPageResponse;
import az.fitnest.support.dto.AdminBmiLeadResponse;
import az.fitnest.support.dto.AdminBmiLeadUpdateRequest;
import az.fitnest.support.dto.PublicLandingBmiLeadRequest;
import az.fitnest.support.exception.BadRequestException;
import az.fitnest.support.exception.ResourceNotFoundException;
import az.fitnest.support.exception.TooManyRequestsException;
import az.fitnest.support.model.entity.BmiLead;
import az.fitnest.support.model.enums.BmiLeadStatus;
import az.fitnest.support.repository.BmiLeadRepository;
import az.fitnest.support.repository.BmiLeadSpecifications;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Sort;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.Duration;
import java.time.Instant;
import java.time.LocalDate;
import java.time.ZoneId;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Objects;

@Slf4j
@Service
@RequiredArgsConstructor
public class BmiLeadService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofHours(24);
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);
    private static final int IP_LIMIT = 8;
    private static final int MAX_PAGE_SIZE = 50;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Baku");

    private final BmiLeadRepository repository;
    private final StringRedisTemplate redisTemplate;

    @Transactional
    public void createFromLanding(PublicLandingBmiLeadRequest request, String clientIp) {
        if (Boolean.FALSE.equals(request.consent())) {
            throw new BadRequestException("Consent required");
        }
        String phone = BmiLeadNormalizer.phone(request.phone());
        String ip = BmiLeadNormalizer.clientIp(clientIp);
        enforceIpLimit(ip);
        if (recentPhoneSeen(phone)) {
            return;
        }

        Instant now = Instant.now();
        if (repository.existsByPhoneAndCreatedAtAfter(phone, now.minus(DUPLICATE_WINDOW))) {
            rememberPhone(phone);
            log.info("BMI lead duplicate suppressed phoneEnding={}", phone.substring(phone.length() - 4));
            return;
        }

        BmiLead lead = new BmiLead();
        lead.setPhone(phone);
        lead.setGoalCode(BmiLeadNormalizer.goalCode(request.goalCode()));
        lead.setGoalTitle(BmiLeadNormalizer.goalTitle(request.goalTitle()));
        lead.setHeightCm(request.heightCm());
        lead.setWeightKg(request.weightKg());
        lead.setBmi(BmiLeadNormalizer.bmi(request.heightCm(), request.weightKg()));
        lead.setAge(request.age());
        lead.setGender(BmiLeadNormalizer.gender(request.gender()));
        lead.setStatus(BmiLeadStatus.NEW);
        lead.setCreatedAt(now);
        lead.setUpdatedAt(now);
        repository.save(lead);
        rememberPhone(phone);
    }

    @Transactional(readOnly = true)
    public AdminBmiLeadPageResponse list(
            int page,
            int size,
            String search,
            String status,
            String goalCode,
            String assignee,
            String from,
            String to
    ) {
        int safePage = Math.max(page, 0);
        int safeSize = Math.min(Math.max(size, 1), MAX_PAGE_SIZE);
        String safeSearch = search == null ? null : search.trim();
        if (safeSearch != null && safeSearch.length() > 80) {
            safeSearch = safeSearch.substring(0, 80);
        }
        var pageable = PageRequest.of(safePage, safeSize, Sort.by(Sort.Direction.DESC, "createdAt"));
        var result = repository.findAll(
                BmiLeadSpecifications.withFilters(
                        safeSearch,
                        BmiLeadStatus.from(status),
                        blankToNull(goalCode),
                        blankToNull(assignee),
                        startOfDay(from),
                        startOfNextDay(to)
                ),
                pageable
        );
        return new AdminBmiLeadPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getNumber() + 1,
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminBmiLeadFilterOptionsResponse filterOptions() {
        var goals = new LinkedHashMap<String, AdminBmiLeadFilterOptionsResponse.GoalOption>();
        for (Object[] row : repository.findDistinctGoals()) {
            String code = Objects.toString(row[0], "");
            String title = Objects.toString(row[1], "");
            if (!code.isBlank()) {
                goals.putIfAbsent(code, new AdminBmiLeadFilterOptionsResponse.GoalOption(code, title));
            }
        }
        var assignees = new LinkedHashMap<Long, AdminBmiLeadFilterOptionsResponse.AssigneeOption>();
        for (Object[] row : repository.findDistinctAssignees()) {
            if (row[0] instanceof Number id) {
                String name = Objects.toString(row[1], "—");
                assignees.putIfAbsent(id.longValue(), new AdminBmiLeadFilterOptionsResponse.AssigneeOption(id.longValue(), name));
            }
        }
        return new AdminBmiLeadFilterOptionsResponse(
                List.copyOf(goals.values()),
                List.copyOf(assignees.values())
        );
    }

    @Transactional
    public AdminBmiLeadResponse update(long id, AdminBmiLeadUpdateRequest request) {
        BmiLead lead = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("BMI request not found"));
        Instant now = Instant.now();
        boolean touched = false;

        if (request.status() != null && request.status() != lead.getStatus()) {
            lead.setStatus(request.status());
            if (request.status() != BmiLeadStatus.NEW && request.status() != BmiLeadStatus.CLOSED) {
                lead.setLastContactAt(now);
            }
            touched = true;
        }

        if (Boolean.TRUE.equals(request.unassign())) {
            lead.setAssigneeUserId(null);
            lead.setAssigneeName(null);
            touched = true;
        } else if (request.assigneeUserId() != null) {
            if (request.assigneeUserId() <= 0) {
                throw new BadRequestException("Invalid assignee");
            }
            String name = BmiLeadNormalizer.assigneeName(request.assigneeName());
            if (name == null) {
                throw new BadRequestException("Invalid assignee");
            }
            lead.setAssigneeUserId(request.assigneeUserId());
            lead.setAssigneeName(name);
            if (lead.getStatus() == BmiLeadStatus.NEW) {
                lead.setStatus(BmiLeadStatus.CONTACTING);
                lead.setLastContactAt(now);
            }
            touched = true;
        }

        if (Boolean.TRUE.equals(request.touched())) {
            lead.setLastContactAt(now);
            touched = true;
        }

        if (!touched) {
            throw new BadRequestException("No changes");
        }
        lead.setUpdatedAt(now);
        return toResponse(repository.save(lead));
    }

    private void enforceIpLimit(String ip) {
        String key = "bmi-lead:ip:" + ip;
        try {
            Long count = redisTemplate.opsForValue().increment(key);
            if (count != null && count == 1L) {
                redisTemplate.expire(key, IP_WINDOW);
            }
            if (count != null && count > IP_LIMIT) {
                throw new TooManyRequestsException("Too many requests");
            }
        } catch (TooManyRequestsException ex) {
            throw ex;
        } catch (Exception ex) {
            log.warn("BMI lead IP limiter unavailable");
        }
    }

    private boolean recentPhoneSeen(String phone) {
        try {
            Boolean exists = redisTemplate.hasKey("bmi-lead:phone:" + phone);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.warn("BMI lead phone cache unavailable");
            return false;
        }
    }

    private void rememberPhone(String phone) {
        try {
            redisTemplate.opsForValue().set("bmi-lead:phone:" + phone, "1", DUPLICATE_WINDOW);
        } catch (Exception ex) {
            log.warn("BMI lead phone cache unavailable");
        }
    }

    private AdminBmiLeadResponse toResponse(BmiLead lead) {
        return new AdminBmiLeadResponse(
                lead.getId(),
                lead.getCreatedAt(),
                lead.getPhone(),
                lead.getGoalCode(),
                lead.getGoalTitle(),
                lead.getBmi(),
                lead.getStatus(),
                lead.getAssigneeUserId(),
                lead.getAssigneeName(),
                lead.getLastContactAt(),
                lead.getAge(),
                lead.getGender(),
                lead.getHeightCm(),
                lead.getWeightKg()
        );
    }

    private static String blankToNull(String value) {
        if (value == null || value.isBlank()) {
            return null;
        }
        return value.trim();
    }

    private static Instant startOfDay(String isoDate) {
        LocalDate date = parseDate(isoDate);
        return date == null ? null : date.atStartOfDay(DISPLAY_ZONE).toInstant();
    }

    private static Instant startOfNextDay(String isoDate) {
        LocalDate date = parseDate(isoDate);
        return date == null ? null : date.plusDays(1).atStartOfDay(DISPLAY_ZONE).toInstant();
    }

    private static LocalDate parseDate(String isoDate) {
        if (isoDate == null || isoDate.isBlank()) {
            return null;
        }
        try {
            return LocalDate.parse(isoDate.trim());
        } catch (Exception ex) {
            throw new BadRequestException("Invalid date");
        }
    }
}
