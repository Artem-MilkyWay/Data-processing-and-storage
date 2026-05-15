package org.example.service;

public enum BookingClass {
    Economy,
    Comfort,
    Business;

    public static BookingClass parse(String raw) {
        try {
            return BookingClass.valueOf(
                    raw.substring(0, 1).toUpperCase() +
                            raw.substring(1).toLowerCase()
            );
        } catch (Exception e) {
            throw new IllegalArgumentException(
                    "bookingClass must be one of Economy, Comfort, Business"
            );
        }
    }
}

