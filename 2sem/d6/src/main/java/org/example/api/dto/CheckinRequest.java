package org.example.api.dto;

import jakarta.validation.constraints.NotBlank;

public record CheckinRequest(

        @NotBlank
        String bookingId,

        @NotBlank
        String flightNo

) {}