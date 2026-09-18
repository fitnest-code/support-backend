package az.fitnest.support.dto;

import com.fasterxml.jackson.annotation.JsonIgnoreProperties;
import jakarta.validation.constraints.AssertTrue;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;

import java.math.BigDecimal;

@JsonIgnoreProperties(ignoreUnknown = true)
public record PublicLandingBmiLeadRequest(
        @NotBlank @Size(max = 20) String phone,
        @NotBlank @Size(min = 2, max = 64) String goalCode,
        @NotBlank @Size(min = 2, max = 120) String goalTitle,
        @NotNull @DecimalMin("80.0") @DecimalMax("250.0") BigDecimal heightCm,
        @NotNull @DecimalMin("25.0") @DecimalMax("300.0") BigDecimal weightKg,
        @Min(10) @Max(100) Integer age,
        @Size(max = 16) String gender,
        @NotNull @AssertTrue Boolean consent,
        @Size(max = 120) String email,
        @Size(max = 0) String website
) {}
