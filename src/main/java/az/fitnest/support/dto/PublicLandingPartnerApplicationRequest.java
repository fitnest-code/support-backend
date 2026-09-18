package az.fitnest.support.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicLandingPartnerApplicationRequest(
        @NotBlank @Size(min = 2, max = 120) String gymName,
        @NotBlank @Size(min = 2, max = 80) String contactName,
        @NotBlank @Size(max = 20) String phone,
        @Size(max = 120) String email,
        @NotBlank @Size(min = 2, max = 120) String activity,
        @Size(max = 0) String website
) {}
