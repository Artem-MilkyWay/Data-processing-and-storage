package org.example.api.dto;

public record RouteLegDto(
        String flightNo,
        String from,
        String to,
        String departureTime,
        String arrivalTime
) {}

