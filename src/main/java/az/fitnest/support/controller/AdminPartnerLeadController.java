package az.fitnest.support.controller;

import az.fitnest.support.dto.AdminPartnerLeadFilterOptionsResponse;
import az.fitnest.support.dto.AdminPartnerLeadPageResponse;
import az.fitnest.support.dto.AdminPartnerLeadResponse;
import az.fitnest.support.dto.AdminPartnerLeadUpdateRequest;
import az.fitnest.support.service.partner.PartnerLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.security.SecurityRequirement;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/admin/partner-applications")
@RequiredArgsConstructor
@Tag(name = "Partner Applications Admin", description = "Partner applications submitted from the website.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminPartnerLeadController {

    private final PartnerLeadService partnerLeadService;

    @Operation(summary = "List partner applications")
    @GetMapping
    public ResponseEntity<AdminPartnerLeadPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String activity,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(partnerLeadService.list(page, size, search, status, activity, assignee, from, to));
    }

    @Operation(summary = "Partner application filter options")
    @GetMapping("/filter-options")
    public ResponseEntity<AdminPartnerLeadFilterOptionsResponse> filterOptions() {
        return ResponseEntity.ok(partnerLeadService.filterOptions());
    }

    @Operation(summary = "Update partner application")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminPartnerLeadResponse> update(
            @PathVariable long id,
            @Valid @RequestBody AdminPartnerLeadUpdateRequest request
    ) {
        return ResponseEntity.ok(partnerLeadService.update(id, request));
    }
}
