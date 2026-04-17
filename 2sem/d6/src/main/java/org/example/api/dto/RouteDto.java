package org.example.api.dto;

import java.util.List;

public record RouteDto(
        List<RouteLegDto> route,
        int totalConnections,
        String bookingClass
) {}

