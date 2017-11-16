package com.example.joseph.sweepersd.limit;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

public class LimitParser {
    private static final String TAG = LimitParser.class.getSimpleName();

    public static Limit buildLimitFromLine(String limitString) {
        Limit result = null;

        String[] parsings = limitString.split("\t");
        if (parsings.length > 3) {
            String[] rangeParsings = parsings[1].split("-");
            if (rangeParsings.length == 2) {
                int range[] = new int[2];
                range[0] = Integer.parseInt(rangeParsings[0].trim());
                range[1] = Integer.parseInt(rangeParsings[1].trim());

                String street = parsings[0].trim().toLowerCase();
                String limit = parsings[2].trim().toLowerCase();
                result = new Limit();
                result.setStreet(street);
                result.setStartRange(range[0]);
                result.setEndRange(range[1]);
                result.setRawLimitString(limit);
            }
        }

        return result;
    }

    public static List<LimitSchedule> buildSchedulesFromLine(String limitString) {
        List<LimitSchedule> results = null;

        String[] parsings = limitString.split("\t");
        if (parsings.length > 3) {
            String[] rangeParsings = parsings[1].split("-");
            if (rangeParsings.length == 2) {
                List<LimitSchedule> schedules = new ArrayList<>();
                for (int j = 3; j < parsings.length; j++) {
                    if (parsings[j].contains("Not Posted")) {
                        continue;
                    }
                    if (!parsings[j].contains("Posted")) {
                        continue;
                    }
                    List<String> timeStrings = getTimeStrings(parsings[j]);
                    if (!timeStrings.isEmpty()) {
                        for (String timeString : timeStrings) {
                            String[] timeParsings = timeString.split("-");
                            int startTime = convertTimeStringToHour(timeParsings[0]);
                            int endTime = convertTimeStringToHour(timeParsings[1]);
                            if (startTime > -1 && endTime > -1) {
                                schedules.addAll(getDays(startTime, endTime, parsings[j]));
                            }
                        }
                    }
                }
                if (!schedules.isEmpty()) {
                    results = schedules;
                }
            }
        }

        return results;
    }

    /**
     * Find and return the time strings from a string. The time string is identified as the
     * substring that exists within a parenthesis.
     * @param schedule
     * @return
     */
    static List<String> getTimeStrings(String schedule) {
        List<String> results = new ArrayList<>();
        if (schedule != null && schedule.contains("(") && schedule.contains(")")) {
            String[] parsings = schedule.split("\\(");
            if (parsings.length > 1) {
                for (int i = 1; i < parsings.length; i++) {
                    String p = parsings[i];

                    String[] parsings2 = p.split("\\)");
                    if (parsings2.length > 0) {
                        // We've validated that this String is inside parenthesis.

                        // Now validate it is proper format.
                        String timeString = parsings2[0];
                        String times[] = timeString.split("-");
                        if (times.length == 2) {
                            int startTime = convertTimeStringToHour(times[0]);
                            int endTime = convertTimeStringToHour(times[1]);
                            if (startTime != -1 && endTime != -1) {
                                results.add(timeString);
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    static List<LimitSchedule> getDays(int startTime, int endTime, String schedule) {
        List<LimitSchedule> results = null;
        if (schedule != null && startTime >= 0 && endTime < 24) {
            results = new ArrayList<>();
            String s = schedule.trim().toLowerCase();
            s = s.replace(",", " ");
            s = s.replace(";", " ");
            s = s.replace("  ", " ");
            s = s.trim();
            List<String> words = new ArrayList<>(Arrays.asList(s.split(" ")));
            for (int i = 0; i < words.size(); i++) {
                String word = words.get(i);
                int weekdayNumber = getDay(word);
                if (weekdayNumber > 0) {
                    List<LimitSchedule> schedulesForDay =
                            refineDays(startTime, endTime, weekdayNumber, words, i);
                    if (schedulesForDay.isEmpty()) {
                        for (int j = 1; j < 5; j++) {
                            LimitSchedule sched = new LimitSchedule();
                            sched.setStartHour(startTime);
                            sched.setStartMinute(0);
                            sched.setEndHour(endTime);
                            sched.setEndMinute(0);
                            sched.setWeekNumber(j);
                            sched.setDayNumber(weekdayNumber);
                            schedulesForDay.add(sched);
                        }
                    }
                    results.addAll(schedulesForDay);
                }
            }
        }
        return results;
    }

    private static List<LimitSchedule> refineDays(int startHour, int endHour, int weekdayNumber,
                                          List<String> words, int index) {
        //Log.d(TAG, "refineDays called on index " + index);
        List<LimitSchedule> results = new ArrayList<>();
        String prevWord = getPreviousWord(words, index);
        if (prevWord != null) {
            int prefix = getPrefix(prevWord);
            if (prefix > 0) {
                LimitSchedule sched = new LimitSchedule();
                sched.setStartHour(startHour);
                sched.setEndHour(endHour);
                sched.setDayNumber(weekdayNumber);
                sched.setWeekNumber(prefix);
                results.add(sched);
                results.addAll(refineDays(startHour, endHour, weekdayNumber, words, index - 1));
            } else if ("&".equals(prevWord) || getDay(prevWord) > 0) {
                results.addAll(refineDays(startHour, endHour, weekdayNumber, words, index - 1));
            }
        }
        return results;
    }

    static String getPreviousWord(List<String> words, int position) {
        String result = null;
        if (position > 0) {
            result = words.get(position - 1);
        }
        return result;
    }

    static int getPrefix(String word) {
        final String first = "1st";
        final String second = "2nd";
        final String third = "3rd";
        final String fourth = "4th";

        int result = 0;
        switch (word) {
            case first:
                result = 1;
                break;
            case second:
                result = 2;
                break;
            case third:
                result = 3;
                break;
            case fourth:
                result = 4;
                break;

        }
        return result;
    }

    public static String getPrefix(int number) {
        final String first = "1st";
        final String second = "2nd";
        final String third = "3rd";
        final String fourth = "4th";

        String result = null;
        switch (number) {
            case 1:
                result = first;
                break;
            case 2:
                result = second;
                break;
            case 3:
                result = third;
                break;
            case 4:
                result = fourth;
                break;

        }
        return result;
    }

    /**
     * Example: "7pm", "10am".
     * @param time
     * @return
     */
    static int convertTimeStringToHour(String time) {
        int result = -1;
        String t = time.trim().toLowerCase();
        if (time.contains("am") || time.contains("pm")) {
            int base = 0;
            if (t.contains("pm")) {
                base = 12;
            }
            t = t.replace("pm", "");
            t = t.replace("am", "");

            try {
                int hour = Integer.parseInt(t);
                if (hour > 0 && hour < 13) {
                    result = Integer.parseInt(t) + base;
                }
            } catch (NumberFormatException e) {
                //Log.d(TAG, "Failed to parse time from: " + time);
            }
        }
        return result;
    }

    /**
     * Convert an integer value from 0 to 23 to a human readable time string with am/pm
     * @param hour
     * @return
     */
    public static String convertHourToTimeString(int hour) {
        if (hour >= 0 && hour < 24) {
            String suffix = hour > 12 ? "pm" : "am";
            int result = hour > 12 ? (hour - 12) : hour;
            return result + suffix;
        } else {
            return null;
        }
    }

    /**
     * Convert a word string to an integer number representing that day of the week.
     *
     * The integer value corresponding to the day is based on Calendar.DAY values.
     *
     * @param word
     * @return
     */
    static int getDay(String word) {
        word = word.trim();

        int result = 0;
        final String monday = "mon";
        final String tuesday = "tue";
        final String wednesday = "wed";
        final String thursday = "thu";
        final String thursday2 = "thur";
        final String friday = "fri";
        final String saturday = "sat";
        final String sunday = "sun";
        switch (word) {
            case sunday:
                result = Calendar.SUNDAY;
                break;
            case monday:
                result = Calendar.MONDAY;
                break;
            case tuesday:
                result = Calendar.TUESDAY;
                break;
            case wednesday:
                result = Calendar.WEDNESDAY;
                break;
            case thursday:
                result = Calendar.THURSDAY;
                break;
            case thursday2:
                result = Calendar.THURSDAY;
                break;
            case friday:
                result = Calendar.FRIDAY;
                break;
            case saturday:
                result = Calendar.SATURDAY;
                break;

        }
        return result;
    }

    /**
     * Convert a word string to an integer number representing that day of the week.
     *
     * The integer value corresponding to the day is based on Calendar.DAY values.
     *
     * @param day
     * @return
     */
    public static String getDay(int day) {
        String result = null;
        final String monday = "mon";
        final String tuesday = "tue";
        final String wednesday = "wed";
        final String thursday = "thu";
        final String friday = "fri";
        final String saturday = "sat";
        final String sunday = "sun";
        switch (day) {
            case Calendar.SUNDAY:
                result = sunday;
                break;
            case Calendar.MONDAY:
                result = monday;
                break;
            case Calendar.TUESDAY:
                result = tuesday;
                break;
            case Calendar.WEDNESDAY:
                result = wednesday;
                break;
            case Calendar.THURSDAY:
                result = thursday;
                break;
            case Calendar.FRIDAY:
                result = friday;
                break;
            case Calendar.SATURDAY:
                result = saturday;
                break;

        }
        return result;
    }
}
