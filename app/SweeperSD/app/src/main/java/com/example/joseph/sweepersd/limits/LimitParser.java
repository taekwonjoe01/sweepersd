package com.example.joseph.sweepersd.limits;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.List;

/**
 * Created by joseph on 8/27/16.
 */
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
                                List<Integer> days = getDays(parsings[j]);
                                for (Integer d : days) {
                                    schedules.add(new LimitSchedule(startTime, endTime, d));
                                }
                            }
                        }
                    }
                }
                if (!schedules.isEmpty()) {
                    result = new Limit(parsings[0], range, parsings[2], schedules);
                }
            }
        }

        return result;
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

    static List<Integer> getDays(String schedule) {
        List<Integer> results = new ArrayList<>();
        if (schedule != null) {
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
                    results.add(weekdayNumber);
                }
            }
        }
        return results;
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
    static String convertHourToTimeString(int hour) {
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
}
