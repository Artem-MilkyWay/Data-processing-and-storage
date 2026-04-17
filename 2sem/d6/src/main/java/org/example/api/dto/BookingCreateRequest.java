package org.example.api.dto;

import jakarta.validation.Valid;
import jakarta.validation.constraints.Email;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotEmpty;
import jakarta.validation.constraints.NotNull;

import java.time.LocalDate;
import java.util.List;

public class BookingCreateRequest {
    @Valid
    @NotNull
    public Passenger passenger;

    @NotEmpty
    public List<@NotBlank String> route;

    @NotBlank
    public String bookingClass;

    @NotNull
    public LocalDate departureDate;

    public static class Passenger {
        @NotBlank
        public String firstName;
        @NotBlank
        public String lastName;
        @Email
        @NotBlank
        public String email;
    }
}

