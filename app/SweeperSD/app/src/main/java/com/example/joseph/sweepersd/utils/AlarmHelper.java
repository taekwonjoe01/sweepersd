package com.example.joseph.sweepersd.utils;

import android.content.Context;

import com.example.joseph.sweepersd.Limit;
import com.example.joseph.sweepersd.alarms.Alarm;

import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for loading and saving alarms to disk.
 */
public class AlarmHelper {
    private static final String TAG = AlarmHelper.class.getSimpleName();
    private static final String ALARM_FILE_NAME = "alarms.txt";

    public static List<Alarm> loadAlarms(Context context) {
        List<Alarm> results = new ArrayList<>();

        /*try {
            InputStream inputStream = context.openFileInput(ALARM_FILE_NAME);

            if (inputStream != null) {
                InputStreamReader inputStreamReader = new InputStreamReader(inputStream);
                BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                String receiveString = "";

                while ( (receiveString = bufferedReader.readLine()) != null ) {
                    String[] parsings = receiveString.split("\t");
                    if (parsings.length != 4) {
                        Log.e(TAG, "loadAlarm error: parsing length is not 4.");
                    }
                    String street = parsings[0];
                    String[] range = parsings[1].split(" ");
                    String limit = parsings[2];
                    String[] schedules = parsings[3].split(":&&:");

                    if (range.length != 2) {
                        Log.e(TAG, "loadAlarm error: range parsing length is not 2.");
                    }
                    int[] rangeInt = new int[2];
                    rangeInt[0] = Integer.parseInt(range[0]);
                    rangeInt[1] = Integer.parseInt(range[1]);

                    List<String> schedulesList = Arrays.asList(schedules);

                    Limit newLimit = new Limit(street, rangeInt, limit, schedulesList);
                    SweepingPosition ld = SweepingPosition.createFromLocation()
                    Alarm newAlarm = new Alarm(newLimit);
                }

                inputStream.close();
            }
        }
        catch (FileNotFoundException e) {
            Log.e("login activity", "File not found: " + e.toString());
        } catch (IOException e) {
            Log.e("login activity", "Can not read file: " + e.toString());
        }*/

        return results;
    }

    public static void saveAlarms(Context context, List<Alarm> alarms) {
        /*try {
            OutputStreamWriter outputStreamWriter = new OutputStreamWriter(
                    context.openFileOutput(ALARM_FILE_NAME, Context.MODE_PRIVATE));
            for (Alarm alarm : alarms) {
                Limit l = alarm.getLocationDetails();
                if (checkLimit(l)) {
                    String alarmString = "";
                    alarmString += l.getStreet() + "\t";
                    alarmString += l.getRange()[0] + " " + l.getRange()[1] + "\t";
                    alarmString += l.getLimit() + "\t";
                    for (int i = 0; i < l.getSchedules().size(); i++) {
                        String schedule = l.getSchedules().get(i);
                        alarmString += schedule;
                        if (i != l.getSchedules().size() - 1) {
                            alarmString += ":&&:";
                        }
                    }
                    alarmString += "\n";
                    outputStreamWriter.write(alarmString);
                }
            }
            outputStreamWriter.close();
        }
        catch (IOException e) {
            Log.e("Exception", "File write failed: " + e.toString());
        }*/
    }

    private static boolean checkLimit(Limit limit) {
        if (limit != null && limit.getStreet() != null && !limit.getStreet().isEmpty() &&
                limit.getLimit() != null && !limit.getLimit().isEmpty() &&
                !limit.getSchedules().isEmpty()) {
            return true;
        }
        return false;
    }
}
