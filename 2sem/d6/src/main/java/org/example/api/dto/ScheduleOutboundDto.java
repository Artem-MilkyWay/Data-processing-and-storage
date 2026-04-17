package org.example.api.dto;

import java.util.List;

public record ScheduleOutboundDto(
        List<String> daysOfWeek,
        String departureTime,
        String flightNo,
        String destination
) {}

