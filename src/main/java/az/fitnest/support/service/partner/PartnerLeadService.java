package az.fitnest.support.service.partner;

import az.fitnest.support.dto.AdminPartnerLeadFilterOptionsResponse;
import az.fitnest.support.dto.AdminPartnerLeadPageResponse;
import az.fitnest.support.dto.AdminPartnerLeadResponse;
import az.fitnest.support.dto.AdminPartnerLeadUpdateRequest;
import az.fitnest.support.dto.PublicLandingPartnerApplicationRequest;
import az.fitnest.support.exception.BadRequestException;
import az.fitnest.support.exception.ResourceNotFoundException;
import az.fitnest.support.exception.TooManyRequestsException;
import az.fitnest.support.model.entity.PartnerLead;
import az.fitnest.support.model.enums.PartnerLeadStatus;
import az.fitnest.support.repository.PartnerLeadRepository;
import az.fitnest.support.repository.PartnerLeadSpecifications;
import az.fitnest.support.service.SupportNotificationMailer;
import az.fitnest.support.service.bmi.BmiLeadNormalizer;
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
import java.util.regex.Pattern;

@Slf4j
@Service
@RequiredArgsConstructor
public class PartnerLeadService {

    private static final Duration DUPLICATE_WINDOW = Duration.ofHours(24);
    private static final Duration IP_WINDOW = Duration.ofMinutes(10);
    private static final int IP_LIMIT = 8;
    private static final int MAX_PAGE_SIZE = 50;
    private static final ZoneId DISPLAY_ZONE = ZoneId.of("Asia/Baku");
    private static final Pattern EMAIL = Pattern.compile("^[^\\s@<>]{1,64}@[^\\s@<>]{1,255}$");

    private final PartnerLeadRepository repository;
    private final StringRedisTemplate redisTemplate;
    private final SupportNotificationMailer mailer;

    @Transactional
    public void createFromLanding(PublicLandingPartnerApplicationRequest request, String clientIp) {
        String phone = BmiLeadNormalizer.phone(request.phone());
        String ip = BmiLeadNormalizer.clientIp(clientIp);
        enforceIpLimit(ip);
        if (recentPhoneSeen(phone)) {
            return;
        }

        Instant now = Instant.now();
        if (repository.existsByPhoneAndCreatedAtAfter(phone, now.minus(DUPLICATE_WINDOW))) {
            rememberPhone(phone);
            return;
        }

        PartnerLead lead = new PartnerLead();
        lead.setGymName(BmiLeadNormalizer.cleanText(request.gymName(), 120));
        lead.setContactName(BmiLeadNormalizer.cleanText(request.contactName(), 80));
        lead.setPhone(phone);
        lead.setEmail(optionalEmail(request.email()));
        lead.setActivity(BmiLeadNormalizer.cleanText(request.activity(), 120));
        lead.setStatus(PartnerLeadStatus.NEW);
        lead.setCreatedAt(now);
        lead.setUpdatedAt(now);
        if (lead.getGymName().length() < 2 || lead.getContactName().length() < 2 || lead.getActivity().length() < 2) {
            throw new BadRequestException("Invalid partner application");
        }
        repository.save(lead);
        rememberPhone(phone);
        mailer.sendPartnerApplication(
                lead.getGymName(),
                lead.getContactName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getActivity()
        );
    }

    @Transactional(readOnly = true)
    public AdminPartnerLeadPageResponse list(
            int page,
            int size,
            String search,
            String status,
            String activity,
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
                PartnerLeadSpecifications.withFilters(
                        safeSearch,
                        PartnerLeadStatus.from(status),
                        blankToNull(activity),
                        blankToNull(assignee),
                        startOfDay(from),
                        startOfNextDay(to)
                ),
                pageable
        );
        return new AdminPartnerLeadPageResponse(
                result.getContent().stream().map(this::toResponse).toList(),
                result.getTotalElements(),
                result.getNumber() + 1,
                result.getSize()
        );
    }

    @Transactional(readOnly = true)
    public AdminPartnerLeadFilterOptionsResponse filterOptions() {
        var activities = repository.findDistinctActivities().stream()
                .filter(Objects::nonNull)
                .map(String::trim)
                .filter(value -> !value.isBlank())
                .distinct()
                .map(value -> new AdminPartnerLeadFilterOptionsResponse.ActivityOption(value, value))
                .toList();
        var assignees = new LinkedHashMap<Long, AdminPartnerLeadFilterOptionsResponse.AssigneeOption>();
        for (Object[] row : repository.findDistinctAssignees()) {
            if (row[0] instanceof Number id) {
                String name = Objects.toString(row[1], "—");
                assignees.putIfAbsent(id.longValue(), new AdminPartnerLeadFilterOptionsResponse.AssigneeOption(id.longValue(), name));
            }
        }
        return new AdminPartnerLeadFilterOptionsResponse(activities, List.copyOf(assignees.values()));
    }

    @Transactional
    public AdminPartnerLeadResponse update(long id, AdminPartnerLeadUpdateRequest request) {
        PartnerLead lead = repository.findById(id)
                .orElseThrow(() -> new ResourceNotFoundException("Partner application not found"));
        Instant now = Instant.now();
        boolean touched = false;

        if (request.status() != null && request.status() != lead.getStatus()) {
            lead.setStatus(request.status());
            if (request.status() != PartnerLeadStatus.NEW && request.status() != PartnerLeadStatus.REJECTED) {
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
            if (lead.getStatus() == PartnerLeadStatus.NEW) {
                lead.setStatus(PartnerLeadStatus.CONTACTED_WAITING);
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
        String key = "partner-lead:ip:" + ip;
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
            log.warn("Partner lead IP limiter unavailable");
        }
    }

    private boolean recentPhoneSeen(String phone) {
        try {
            Boolean exists = redisTemplate.hasKey("partner-lead:phone:" + phone);
            return Boolean.TRUE.equals(exists);
        } catch (Exception ex) {
            log.warn("Partner lead phone cache unavailable");
            return false;
        }
    }

    private void rememberPhone(String phone) {
        try {
            redisTemplate.opsForValue().set("partner-lead:phone:" + phone, "1", DUPLICATE_WINDOW);
        } catch (Exception ex) {
            log.warn("Partner lead phone cache unavailable");
        }
    }

    private AdminPartnerLeadResponse toResponse(PartnerLead lead) {
        return new AdminPartnerLeadResponse(
                lead.getId(),
                lead.getCreatedAt(),
                lead.getGymName(),
                lead.getContactName(),
                lead.getPhone(),
                lead.getEmail(),
                lead.getActivity(),
                lead.getStatus(),
                lead.getAssigneeUserId(),
                lead.getAssigneeName(),
                lead.getLastContactAt()
        );
    }

    private static String optionalEmail(String raw) {
        if (raw == null || raw.isBlank()) {
            return null;
        }
        String email = raw.trim().toLowerCase();
        if (!EMAIL.matcher(email).matches()) {
            throw new BadRequestException("Invalid email");
        }
        return email;
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
