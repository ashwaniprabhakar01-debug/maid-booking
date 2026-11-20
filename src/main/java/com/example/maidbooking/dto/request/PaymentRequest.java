package com.example.maidbooking.dto.request;

import com.example.maidbooking.enums.PaymentMethod;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PaymentRequest {

    @NotNull
    private PaymentMethod method;

    @NotBlank
    private String instrument;
}
