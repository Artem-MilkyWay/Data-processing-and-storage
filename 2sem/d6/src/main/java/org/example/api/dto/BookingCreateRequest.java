package org.example.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public record BookingCreateRequest(

        @Valid
        @NotNull
        Passenger passenger,

        @NotEmpty
        List<@NotBlank String> route,

        @NotBlank
        String bookingClass,

        @NotNull
        LocalDate departureDate

) {

    public record Passenger(

            @NotBlank
            String firstName,

            @NotBlank
            String lastName,

            @Email
            @NotBlank
            String email

    ) {}
}