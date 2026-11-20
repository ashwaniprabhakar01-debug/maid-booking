package com.example.maidbooking.dto.request;

import com.example.maidbooking.enums.Gender;
import com.example.maidbooking.enums.ServiceType;
import jakarta.validation.Valid;
import jakarta.validation.constraints.DecimalMax;
import jakarta.validation.constraints.DecimalMin;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;
import lombok.Getter;
import lombok.Setter;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

@Getter
@Setter
public class CreateMaidRequest {

    @NotBlank
    private String name;

    @NotNull
    private Gender gender;

    @NotBlank
    private String locality;

    @NotEmpty
    private Set<@NotNull ServiceType> services;

    @NotNull
    @Positive
    private BigDecimal pricePerHour;

    @DecimalMin("0.0")
    @DecimalMax("5.0")
    private Double rating;

    @NotEmpty
    private List<@NotNull @Valid AvailabilityWindowRequest> availability;
}
