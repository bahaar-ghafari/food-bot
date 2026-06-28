package com.foodbot.lang;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;

class TimeFormatTest {

    @Test
    void belowAnHourShowsMinutes() {
        assertEquals("10 min", TimeFormat.format(10, Lang.EN));
        assertEquals("59 min", TimeFormat.format(59, Lang.EN));
    }

    @Test
    void exactHourShowsHourOnly() {
        assertEquals("1 hour", TimeFormat.format(60, Lang.EN));
        assertEquals("2 hours", TimeFormat.format(120, Lang.EN));
    }

    @Test
    void hourAndMinutesCombine() {
        assertEquals("1 hour 30 min", TimeFormat.format(90, Lang.EN));
        assertEquals("2 hours 15 min", TimeFormat.format(135, Lang.EN));
    }

    @Test
    void exactDayShowsDayOnly() {
        assertEquals("1 day", TimeFormat.format(1440, Lang.EN));
        assertEquals("2 days", TimeFormat.format(2880, Lang.EN));
    }

    @Test
    void dayHourAndMinutesCombine() {
        assertEquals("1 day 1 hour 5 min", TimeFormat.format(1505, Lang.EN));
    }

    @Test
    void persianUsesSameUnitWordRegardlessOfCount() {
        assertEquals("10 دقیقه", TimeFormat.format(10, Lang.FA));
        assertEquals("1 ساعت", TimeFormat.format(60, Lang.FA));
        assertEquals("2 ساعت", TimeFormat.format(120, Lang.FA));
        assertEquals("1 روز", TimeFormat.format(1440, Lang.FA));
    }

    @Test
    void persianJoinsPartsWithAnd() {
        assertEquals("1 ساعت و 30 دقیقه", TimeFormat.format(90, Lang.FA));
    }
}
