package az.fitnest.support.controller;

import az.fitnest.support.dto.PublicLandingPartnerApplicationRequest;
import az.fitnest.support.service.partner.PartnerLeadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/v1/public/landing")
@RequiredArgsConstructor
@Tag(name = "Landing Public Partner", description = "Public partner application used by the website.")
public class PublicLandingPartnerApplicationController {

    private final PartnerLeadService partnerLeadService;

    @Operation(summary = "Public partner application")
    @PostMapping("/partner-applications")
    public ResponseEntity<Void> create(
            @Valid @RequestBody PublicLandingPartnerApplicationRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = firstHeader(httpRequest, "X-Client-IP", "X-Forwarded-For");
        partnerLeadService.createFromLanding(request, clientIp);
        return ResponseEntity.status(HttpStatus.CREATED).build();
    }

    private static String firstHeader(HttpServletRequest request, String... names) {
        for (String name : names) {
            String value = request.getHeader(name);
            if (value != null && !value.isBlank()) {
                return value;
            }
        }
        return request.getRemoteAddr();
    }
}
