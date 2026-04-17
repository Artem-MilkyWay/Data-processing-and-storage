package org.example.service;

import java.security.SecureRandom;

public final class Ids {
    private static final SecureRandom RND = new SecureRandom();
    private static final char[] ALNUM = "ABCDEFGHIJKLMNOPQRSTUVWXYZ0123456789".toCharArray();

    private Ids() {}

    public static String bookingRef() {
        return random(6);
    }

    public static String ticketNo() {
        return random(13);
    }

    public static String checkinId() {
        return random(10);
    }

    private static String random(int len) {
        var b = new StringBuilder(len);
        for (int i = 0; i < len; i++) {
            b.append(ALNUM[RND.nextInt(ALNUM.length)]);
        }
        return b.toString();
    }
}

