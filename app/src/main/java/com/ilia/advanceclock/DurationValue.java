package com.ilia.advanceclock;

import java.math.BigInteger;

/** Exact duration arithmetic, independent of field length and device scheduling limits. */
final class DurationValue {
    private static final BigInteger SIXTY = BigInteger.valueOf(60);
    final BigInteger hours;
    final int minutes, seconds;
    final BigInteger millis;

    DurationValue(String h, String m, String s) {
        BigInteger total = number(h).multiply(BigInteger.valueOf(3600))
                .add(number(m).multiply(SIXTY)).add(number(s));
        BigInteger[] split = total.divideAndRemainder(BigInteger.valueOf(3600));
        hours = split[0];
        minutes = split[1].intValue() / 60;
        seconds = split[1].intValue() % 60;
        millis = total.multiply(BigInteger.valueOf(1000));
    }

    long schedulableMillis(long now) {
        long value = millis.longValueExact();
        Math.addExact(now, value);
        return value;
    }

    private static BigInteger number(String raw) {
        String text = raw == null ? "" : raw.trim();
        if (text.isEmpty()) return BigInteger.ZERO;
        StringBuilder ascii = new StringBuilder(text.length());
        for (int i = 0; i < text.length(); i++) {
            int digit = Character.digit(text.charAt(i), 10);
            if (digit < 0) throw new NumberFormatException("Unsigned duration expected");
            ascii.append((char) ('0' + digit));
        }
        return new BigInteger(ascii.toString());
    }
}
