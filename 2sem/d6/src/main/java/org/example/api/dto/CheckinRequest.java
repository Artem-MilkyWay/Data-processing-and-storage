package org.example.api.dto;

import jakarta.validation.constraints.NotBlank;

public class CheckinRequest {
    @NotBlank
    public String bookingId;
    @NotBlank
    public String flightNo;
}

