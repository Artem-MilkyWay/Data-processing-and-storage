package org.example.api.dto;

public record CheckinResponse(
        String checkinId,
        String status,
        String seatNumber,
        String boardingPassUrl
) {}

