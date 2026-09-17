package az.fitnest.support.controller;

import az.fitnest.support.dto.AdminBmiLeadFilterOptionsResponse;
import az.fitnest.support.dto.AdminBmiLeadPageResponse;
import az.fitnest.support.dto.AdminBmiLeadResponse;
import az.fitnest.support.dto.AdminBmiLeadUpdateRequest;
import az.fitnest.support.service.bmi.BmiLeadService;
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
@RequestMapping("/api/v1/admin/bmi-requests")
@RequiredArgsConstructor
@Tag(name = "BMI Admin", description = "BMI support request queue for FitNest admins.")
@SecurityRequirement(name = "bearerAuth")
@PreAuthorize("hasRole('ADMIN')")
public class AdminBmiLeadController {

    private final BmiLeadService bmiLeadService;

    @Operation(summary = "List BMI requests")
    @GetMapping
    public ResponseEntity<AdminBmiLeadPageResponse> list(
            @RequestParam(defaultValue = "0") int page,
            @RequestParam(defaultValue = "10") int size,
            @RequestParam(required = false) String search,
            @RequestParam(required = false) String status,
            @RequestParam(required = false) String goalCode,
            @RequestParam(required = false) String assignee,
            @RequestParam(required = false) String from,
            @RequestParam(required = false) String to
    ) {
        return ResponseEntity.ok(bmiLeadService.list(page, size, search, status, goalCode, assignee, from, to));
    }

    @Operation(summary = "BMI request filter options")
    @GetMapping("/filter-options")
    public ResponseEntity<AdminBmiLeadFilterOptionsResponse> filterOptions() {
        return ResponseEntity.ok(bmiLeadService.filterOptions());
    }

    @Operation(summary = "Update BMI request")
    @PatchMapping("/{id}")
    public ResponseEntity<AdminBmiLeadResponse> update(
            @PathVariable long id,
            @Valid @RequestBody AdminBmiLeadUpdateRequest request
    ) {
        return ResponseEntity.ok(bmiLeadService.update(id, request));
    }
}
