package com.example.joseph.sweepersd.archived.model.limits;

import org.junit.Test;

import java.util.Calendar;
import java.util.List;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertNotEquals;
import static org.junit.Assert.assertNotNull;
import static org.junit.Assert.assertNull;
import static org.junit.Assert.assertTrue;

/**
 * To work on unit tests, switch the Test Artifact in the Build Variants view.
 */
public class TestLimitParser {
    @Test
    public void testGetDay() throws Exception {
        // Test a valid sunday
        String sunday = "sun";

        int sun = LimitParser.getDay(sunday);
        assertEquals(Calendar.SUNDAY, sun);

        // Test a valid monday
        String monday = "mon";

        int mon = LimitParser.getDay(monday);
        assertEquals(Calendar.MONDAY, mon);

        // Test a valid tuesday
        String tuesday = "tue";

        int tue = LimitParser.getDay(tuesday);
        assertEquals(Calendar.TUESDAY, tue);

        // Test a valid wednesday
        String wednesday = "wed";

        int wed = LimitParser.getDay(wednesday);
        assertEquals(Calendar.WEDNESDAY, wed);

        // Test a valid thursday
        String thursday = "thu";

        int thu = LimitParser.getDay(thursday);
        assertEquals(Calendar.THURSDAY, thu);

        // Test a valid thursday
        thursday = "thur";

        int thur = LimitParser.getDay(thursday);
        assertEquals(Calendar.THURSDAY, thur);

        // Test a valid friday
        String friday = "fri";

        int fri = LimitParser.getDay(friday);
        assertEquals(Calendar.FRIDAY, fri);

        // Test a valid saturday
        String saturday = "sat";

        int sat = LimitParser.getDay(saturday);
        assertEquals(Calendar.SATURDAY, sat);

        // Test invalid day
        String invalid = "inv";

        int inv = LimitParser.getDay(invalid);
        assertEquals(0, inv);

        // Test valid day with extra space
        String saturdaySpace = " sat";

        int satSp = LimitParser.getDay(saturdaySpace);
        assertEquals(Calendar.SATURDAY, satSp);
        assertEquals(" sat", saturdaySpace);
    }

    @Test
    public void testConvertHourToTimeString() {
        // Test a valid time for am
        int am = 3;

        String amString = LimitParser.convertHourToTimeString(am);
        assertEquals("3am", amString);

        // Test a valid time for pm
        int pm = 13;

        String pmString = LimitParser.convertHourToTimeString(pm);
        assertEquals("1pm", pmString);

        // Test an invalid time for pm
        int inv = 24;

        String invString = LimitParser.convertHourToTimeString(inv);
        assertNull(invString);
    }

    @Test
    public void testConvertTimeStringToHour() {
        // Test a valid time for am
        String amString = "3am";

        int am = LimitParser.convertTimeStringToHour(amString);
        assertEquals(3, am);

        // Test a valid time for pm
        String pmString = "1pm";

        int pm = LimitParser.convertTimeStringToHour(pmString);
        assertEquals(13, pm);

        // Test an invalid time for pm
        String invString = "13pm";

        int inv = LimitParser.convertTimeStringToHour(invString);
        assertEquals(-1, inv);

        // Test an invalid time without am or pm
        String invString2 = "13";

        int inv2 = LimitParser.convertTimeStringToHour(invString2);
        assertEquals(-1, inv2);

        // Test an invalid time without am or pm
        String invString3 = "2";

        int inv3 = LimitParser.convertTimeStringToHour(invString3);
        assertEquals(-1, inv3);
    }

    @Test
    public void testGetDays() {
        // Test a null string.
        List<LimitSchedule> nullDays = LimitParser.getDays(0, 0, null);
        assertNull(nullDays);

        nullDays = LimitParser.getDays(0, 24, "someGarbage");
        assertNull(nullDays);

        nullDays = LimitParser.getDays(-1, 0, "someGarbage");
        assertNull(nullDays);

        // Test a String
        String sample = "mon, tue, wed, saturday";// 3 are valid - saturday is not valid.
        int startTime = 10;
        int endTime = 13;
        List<LimitSchedule> sampleDays = LimitParser.getDays(startTime, endTime, sample);

        // sample string has no prefix (1st, 2nd, etc), therefore, all 4 days of month are presumed.
        assertEquals(12, sampleDays.size());
        boolean mondayFound = false;
        boolean tuesdayFound = false;
        boolean wednesdayFound = false;
        boolean saturdayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2) {
                mondayFound = true;
            } else if (sched.getDay() == 3) {
                tuesdayFound = true;
            } else if (sched.getDay() == 4) {
                wednesdayFound = true;
            } else if (sched.getDay() == 7) {
                saturdayFound = true;
            }
        }
        assertTrue(mondayFound);
        assertTrue(tuesdayFound);
        assertTrue(wednesdayFound);
        assertFalse(saturdayFound);

        // Test anothing String
        sample = "  sun; mon,,tue";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(12, sampleDays.size());
        boolean sundayFound = false;
        mondayFound = false;
        tuesdayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 1) {
                sundayFound = true;
            } else if (sched.getDay() == 2) {
                mondayFound = true;
            } else if (sched.getDay() == 3) {
                tuesdayFound = true;
            }
        }
        assertTrue(sundayFound);
        assertTrue(mondayFound);
        assertTrue(tuesdayFound);


        // Test anothing String
        sample = " 1st mon";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(1, sampleDays.size());
        mondayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 1) {
                mondayFound = true;
            }
        }
        assertTrue(mondayFound);

        // Test anothing String
        sample = " 2nd mon";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(1, sampleDays.size());
        mondayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 2) {
                mondayFound = true;
            }
        }
        assertTrue(mondayFound);

        // Test anothing String
        sample = " 3rd mon";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(1, sampleDays.size());
        mondayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 3) {
                mondayFound = true;
            }
        }
        assertTrue(mondayFound);

        // Test anothing String
        sample = " 4th mon";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(1, sampleDays.size());
        mondayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 4) {
                mondayFound = true;
            }
        }
        assertTrue(mondayFound);

        // Test anothing String
        sample = "garbage 1st & 3rd mon garbage garbage garbage";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(2, sampleDays.size());
        boolean firstMondayFound = false;
        boolean secondMondayFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 1) {
                firstMondayFound = true;
            } else if(sched.getDay() == 2 && sched.getWeekNumber() == 3) {
                secondMondayFound = true;
            }
        }
        assertTrue(firstMondayFound);
        assertTrue(secondMondayFound);

        // Test anothing String
        sample = "garbage 1st & 3rd mon, thur, fri garbage garbage garbage";
        sampleDays = LimitParser.getDays(startTime, endTime, sample);

        assertEquals(6, sampleDays.size());
        firstMondayFound = false;
        secondMondayFound = false;
        boolean firstThurFound = false;
        boolean secondThurFound = false;
        boolean firstFriFound = false;
        boolean secondFriFound = false;
        for (LimitSchedule sched : sampleDays) {
            if (sched.getDay() == 2 && sched.getWeekNumber() == 1) {
                firstMondayFound = true;
            } else if(sched.getDay() == 2 && sched.getWeekNumber() == 3) {
                secondMondayFound = true;
            } else if(sched.getDay() == 5 && sched.getWeekNumber() == 1) {
                firstThurFound = true;
            } else if(sched.getDay() == 5 && sched.getWeekNumber() == 3) {
                secondThurFound = true;
            } else if(sched.getDay() == 6 && sched.getWeekNumber() == 1) {
                firstFriFound = true;
            } else if(sched.getDay() == 6 && sched.getWeekNumber() == 3) {
                secondFriFound = true;
            }
        }
        assertTrue(firstMondayFound);
        assertTrue(secondMondayFound);
        assertTrue(firstThurFound);
        assertTrue(secondThurFound);
        assertTrue(firstFriFound);
        assertTrue(secondFriFound);
    }

    @Test
    public void testGetTimeString() {
        // Test a null string.
        String sample1 = null;
        List<String> sample1Result = LimitParser.getTimeStrings(sample1);

        assertEquals(0, sample1Result.size());

        // Test an invalid string.
        String sample2 = "every monday 7-10";
        List<String> sample2Result = LimitParser.getTimeStrings(sample2);

        assertEquals(0, sample2Result.size());

        // Test an invalid string.
        String sample3 = "every monday (7-10)";
        List<String> sample3Result = LimitParser.getTimeStrings(sample3);

        assertEquals(0, sample3Result.size());

        // Test a valid string.
        String sample4 = "every monday (7am-10am)";
        List<String> sample4Result = LimitParser.getTimeStrings(sample4);

        assertEquals(1, sample4Result.size());
        assertEquals("7am-10am", sample4Result.get(0));

        // Test a valid string.
        String sample5 = "every monday (7am-10am) mon";
        List<String> sample5Result = LimitParser.getTimeStrings(sample5);

        assertEquals(1, sample5Result.size());
        assertEquals("7am-10am", sample5Result.get(0));

        // Test a valid string.
        String sample6 = "(7am-10am) mon";
        List<String> sample6Result = LimitParser.getTimeStrings(sample6);

        assertEquals(1, sample6Result.size());
        assertEquals("7am-10am", sample6Result.get(0));

        // Test an invalid string.
        String sample7 = "(7am-13am) mon";
        List<String> sample7Result = LimitParser.getTimeStrings(sample7);

        assertEquals(0, sample7Result.size());
    }

    @Test
    public void testBuiltLimitFromLine() {
        String line1 = "ALMONDWOOD WY \t5030 - 5049 \tBROOKBURN DR - CARMEL KNOLLS DR \tNot Posted, Both Sides Odd Month 4th Wed";
        Limit limit1 = LimitParser.buildLimitFromLine(line1);
        assertNull(limit1);

        String line2 = "BERYL ST \t1000 - 1099 \tCASS ST - DAWES ST \tPosted (10am - 1pm), SS 1st Wed, NS 1st Thur";
        Limit limit2 = LimitParser.buildLimitFromLine(line2);
        assertNotNull(limit2);
        assertEquals("beryl st", limit2.getStreet());

        assertEquals(2, limit2.getSchedules().size());
        assertNotEquals(limit2.getSchedules().get(0).getDay(),
                limit2.getSchedules().get(1).getDay());
    }

    @Test
    public void testGetPrefix() {
        String value = "first";
        int prefix = LimitParser.getPrefix(value);
        assertEquals(0, prefix);

        value = "1st";
        prefix = LimitParser.getPrefix(value);
        assertEquals(1, prefix);

        value = "2nd";
        prefix = LimitParser.getPrefix(value);
        assertEquals(2, prefix);

        value = "3rd";
        prefix = LimitParser.getPrefix(value);
        assertEquals(3, prefix);

        value = "4th";
        prefix = LimitParser.getPrefix(value);
        assertEquals(4, prefix);

        value = "5th";
        prefix = LimitParser.getPrefix(value);
        assertEquals(0, prefix);
    }
}