package com.ilia.advanceclock;

import org.junit.Test;
import static org.junit.Assert.*;

public final class DurationValueTest {
    @Test public void secondsCarryIntoMinutes() {
        DurationValue value = new DurationValue("0", "0", "240");
        assertEquals("0", value.hours.toString()); assertEquals(4, value.minutes); assertEquals(0, value.seconds);
    }
    @Test public void thousandMinutesAndHundredThousandSecondsAreExact() {
        DurationValue value = new DurationValue("0", "1000", "100000");
        assertEquals("44", value.hours.toString()); assertEquals(26, value.minutes); assertEquals(40, value.seconds);
        assertEquals(160_000_000L, value.schedulableMillis(0));
    }
    @Test public void oneMillionHoursIsSupportedWithoutOverflow() {
        DurationValue value = new DurationValue("1000000", "0", "0");
        assertEquals(3_600_000_000_000L, value.schedulableMillis(0));
    }
    @Test public void localizedDigitsAndMixedUnitsCarryExactly() {
        DurationValue value = new DurationValue("۱", "۶۰", "١٢١");
        assertEquals("2", value.hours.toString()); assertEquals(2, value.minutes); assertEquals(1, value.seconds);
    }
    @Test public void arbitraryLengthValuesNormalizeWithoutWrapping() {
        DurationValue value = new DurationValue("999999999999999999999999", "60", "60");
        assertEquals("1000000000000000000000000", value.hours.toString()); assertEquals(1, value.minutes);
        try { value.schedulableMillis(0); fail("Must not wrap a device deadline"); }
        catch (ArithmeticException expected) {}
    }
}
