package az.fitnest.support.controller;

import az.fitnest.support.dto.PublicLandingBmiLeadRequest;
import az.fitnest.support.service.bmi.BmiLeadService;
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
@Tag(name = "Landing Public BMI", description = "Public BMI support request used by the website. Landing key required at gateway.")
public class PublicLandingBmiLeadController {

    private final BmiLeadService bmiLeadService;

    @Operation(summary = "Public BMI support request", description = "Stores a BMI page lead. No auth. Landing frontend only.")
    @PostMapping("/bmi-requests")
    public ResponseEntity<Void> create(
            @Valid @RequestBody PublicLandingBmiLeadRequest request,
            HttpServletRequest httpRequest
    ) {
        String clientIp = firstHeader(httpRequest, "X-Client-IP", "X-Forwarded-For");
        bmiLeadService.createFromLanding(request, clientIp);
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
