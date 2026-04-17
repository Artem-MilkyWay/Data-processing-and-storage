package org.example.service;

public enum BookingClass {
    Economy,
    Comfort,
    Business;

    public static BookingClass parse(String raw) {
        for (var v : values()) {
            if (v.name().equalsIgnoreCase(raw)) return v;
        }
        throw new IllegalArgumentException("bookingClass must be one of Economy, Comfort, Business");
    }
}

