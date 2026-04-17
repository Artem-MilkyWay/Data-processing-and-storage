package org.example.api.dto;

import java.util.List;

public record ScheduleInboundDto(
        List<String> daysOfWeek,
        String arrivalTime,
        String flightNo,
        String origin
) {}

