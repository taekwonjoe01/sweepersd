package com.example.joseph.sweepersd;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Calendar;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.TimeZone;

/**
 * Created by joseph on 3/13/16.
 */
public class LimitManager {
    private static final String TAG = LimitManager.class.getSimpleName();

    private static List<Limit> mLoadedLimits;
    private static List<Limit> mLoadedPostedLimits;

    public static List<Limit> loadLimits(Context context) {
        mLoadedLimits = new ArrayList<>();
        mLoadedPostedLimits = new ArrayList<>();

        try {
            for (int i = 1; i < 10; i++) {
                String filename = "district" + i + ".txt";
                InputStream is = context.getAssets().open(filename);
                BufferedReader in=
                        new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str;

                while ((str=in.readLine()) != null) {
                    String[] parsings = str.split("\t");
                    if (parsings.length > 3) {
                        String[] rangeParsings = parsings[1].split("-");
                        int range[] = new int[2];
                        range[0] = Integer.parseInt(rangeParsings[0].trim());
                        range[1] = Integer.parseInt(rangeParsings[1].trim());

                        List<String> schedules = new ArrayList<>();
                        boolean acceptable = true;
                        for (int j = 3; j < parsings.length; j++) {
                            schedules.add(parsings[j]);
                            if (parsings[j].contains("Not Posted")) {
                                acceptable = false;
                            }
                            if (!parsings[j].contains("Posted")) {
                                acceptable = false;
                            }
                        }

                        Limit l = new Limit(parsings[0], range, parsings[2], schedules);

                        if (acceptable) {
                            mLoadedPostedLimits.add(l);
                        }
                        mLoadedLimits.add(l);
                    } else {
                        Log.e(TAG, "Parsed a bad line in " + filename);
                    }
                }

                in.close();
                is.close();
            }
        } catch (IOException e) {

        }

        Log.d(TAG, "Number of Limits posted: " + mLoadedPostedLimits.size());
        Log.d(TAG, "Looking for Beryl St...");
        for (Limit l : mLoadedPostedLimits) {
            if (l.getStreet().contains("BERYL")) {
                Log.d(TAG, "Found BERYL!");
            }
        }

        for (Limit l : mLoadedPostedLimits) {
            Log.d(TAG, "Street: " + l.getStreet());
            String s = "";
            for (String sc : l.getSchedules()) {
                s += sc;
            }
            Log.d(TAG, "Schedule: " + s);
            for (String schedule : l.getSchedules()) {
                String timeString = getTimeString(schedule);
                if (timeString == null) {
                    Log.e(TAG, "Parse Error on " + l.getStreet() + " :: " + l.getLimit() + " :: "
                            + l.getSchedules());
                } else {
                    String[] parsings = timeString.split("-");
                    int startTime = convertTimeStringToHour(parsings[0]);
                    int endTime = convertTimeStringToHour(parsings[1]);
                    if (startTime > -1 && endTime > -1) {
                        List<GregorianCalendar> days = new ArrayList<>();
                        for (int i = 0; i < l.getSchedules().size(); i++) {
                            days.addAll(getSweepingDates(startTime, endTime, l.getSchedules().get(i)));
                        }
                        for (GregorianCalendar d : days) {
                            Log.d(TAG, "month: " + d.get(Calendar.MONTH) +" day: " +
                                    d.get(Calendar.DAY_OF_MONTH) + " (" +
                                    d.get(Calendar.DAY_OF_WEEK) + ") time: " +
                                    d.get(Calendar.HOUR));
                        }
                    } else {
                        Log.e(TAG, "StartTime or endTime was -1: " + startTime + " " + endTime);
                    }
                }
            }
            Log.d(TAG, "~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~~");


            //getDateForLimit(l);
        }

        return mLoadedLimits;
    }

    public static List<Limit> getLimits() {
        return mLoadedLimits;
    }

    public static List<Limit> getPostedLimits() {
        return mLoadedPostedLimits;
    }

    public static String getTimeString(String schedule) {
        String result = null;
        String[] parsings = schedule.split("\\(");
        for (int i = 1; i < parsings.length; i++) {
            String p = parsings[i];
            String[] parsings2 = p.split("\\)");
            if (parsings2.length == 2) {
                if (result != null) {
                    String[] temp = parsings2[0].split("-");
                    String[] temp2 = result.split("-");
                    int startTime = convertTimeStringToHour(temp[0]);
                    int endTime = convertTimeStringToHour(temp[1]);
                    int startTime2 = convertTimeStringToHour(temp2[0]);
                    int endTime2 = convertTimeStringToHour(temp2[1]);
                    int minStart = Math.min(startTime, startTime2);
                    int maxEnd = Math.max(endTime, endTime2);
                    String startString = convertHourToTimeString(minStart);
                    String endString = convertHourToTimeString(maxEnd);
                    result = startString + " - " + endString;
                } else {
                    result = parsings2[0];
                }
            } else {
                Log.w(TAG, "Failed to get time string from: " + schedule);
            }
        }
        if (parsings.length == 2) {

        } else {
            Log.w(TAG, "Failed to get time string from: " + schedule);
        }
        return result;
    }

    /**
     * Example: "7pm", "10am".
     * @param time
     * @return
     */
    public static int convertTimeStringToHour(String time) {
        int result = -1;
        String t = time.trim().toLowerCase();
        int base = 0;
        if (t.contains("pm")) {
            base = 12;
        }
        t = t.replace("pm", "");
        t = t.replace("am", "");

        try {
            result = Integer.parseInt(t) + base;
        } catch (NumberFormatException e) {
            Log.d(TAG, "Failed to parse time from: " + time);
        }
        return result;
    }

    public static List<GregorianCalendar> getSweepingDates(int startTime, int endTime, String schedule) {
        List<GregorianCalendar> results = new ArrayList<>();
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
                List<GregorianCalendar> potentialDays = new ArrayList<>();
                List<GregorianCalendar> potentialResults = new ArrayList<>();
                Calendar calendar = Calendar.getInstance(TimeZone.getDefault());

                for (int j = 0; j < 28; j++) {
                    int dow = calendar.get(Calendar.DAY_OF_WEEK);

                    if (dow == weekdayNumber) {
                        GregorianCalendar c = new GregorianCalendar(calendar.get(Calendar.YEAR),
                                calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH),
                                startTime, 0, 0);
                        potentialDays.add(c);
                    }

                    calendar.add(Calendar.DATE, 1);
                }

                potentialResults.addAll(refineDays(words, i, potentialDays));
                if (potentialResults.isEmpty()) {
                    results.addAll(potentialDays);
                } else {
                    results.addAll(potentialResults);
                }
            }
        }
        return results;
    }

    private static String convertHourToTimeString(int hour) {
        String suffix = hour > 12 ? "pm" : "am";
        int result = hour > 12 ? (hour - 12) : hour;
        return result + suffix;
    }

    private static int getDay(String word) {
        int result = 0;
        final String monday = "mon";
        final String tuesday = "tue";
        final String wednesday = "wed";
        final String thursday = "thu";
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
            case friday:
                result = Calendar.FRIDAY;
                break;
            case saturday:
                result = Calendar.SATURDAY;
                break;

        }
        return result;
    }

    private static List<GregorianCalendar> refineDays(List<String> words, int index,
                                               List<GregorianCalendar> unrefinedDays) {
        List<GregorianCalendar> refinedDays = new ArrayList<>();
        String prevWord = getPreviousWord(words, index);
        if (prevWord != null) {
            int prefix = getPrefix(prevWord);
            if (prefix > 0) {
                for (GregorianCalendar day : unrefinedDays) {
                    int dom = day.get(Calendar.DAY_OF_MONTH);
                    int p = dom - ((prefix - 1) * 7);
                    if (p > 0 && p < 8) {
                        refinedDays.add(day);
                    }
                }
                refinedDays.addAll(refineDays(words, index - 1, unrefinedDays));
            } else if (prevWord.equals("&")) {
                refinedDays.addAll(refineDays(words, index - 1, unrefinedDays));
            }
        }
        return refinedDays;
    }

    private static String getPreviousWord(List<String> words, int position) {
        String result = null;
        if (position > 0) {
            result = words.get(position - 1);
        }
        return result;
    }

    private static int getPrefix(String word) {
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
}
