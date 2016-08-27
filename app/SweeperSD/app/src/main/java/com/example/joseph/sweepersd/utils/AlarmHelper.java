package com.example.joseph.sweepersd.utils;

import android.content.Context;

import com.example.joseph.sweepersd.limits.Limit;
import com.example.joseph.sweepersd.SweepingPosition;
import com.example.joseph.sweepersd.alarms.Alarm;
import com.google.android.gms.maps.model.LatLng;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileInputStream;
import java.io.FileNotFoundException;
import java.io.FileWriter;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Helper class for loading and saving alarms to disk.
 */
public class AlarmHelper {
    private static final String TAG = AlarmHelper.class.getSimpleName();

    private static final String FILE_SWEEPING_LOCATIONS = "sweeping_locations.txt";
    private static final String FILE_ALARM_DETAILS = "alarm_details.txt";

    private static final String SEPARATOR_SWEEPING_LOCATIONS = "::";

    public static List<Alarm> loadAlarms(Context context) {
        List<Alarm> results = new ArrayList<>();

        String pathToAlarms = context.getFilesDir() + "/alarms/";
        File alarmsDir = new File(pathToAlarms);
        if (alarmsDir.exists()) {
            File[] alarmDirs = alarmsDir.listFiles();
            for (File alarmDir : alarmDirs) {
                try {
                    File alarmDetailsFile = new File(alarmDir.getAbsolutePath() + "/" +
                            FILE_ALARM_DETAILS);
                    File sweepingLocationsFile = new File(alarmDir.getAbsolutePath() + "/" +
                            FILE_SWEEPING_LOCATIONS);

                    InputStream ADis = new FileInputStream(alarmDetailsFile);

                    LatLng center = null;
                    int radius = 0;

                    if (ADis != null) {
                        InputStreamReader inputStreamReader = new InputStreamReader(ADis);
                        BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                        String receiveString = bufferedReader.readLine();
                        String[] parsings = receiveString.split(",");
                        center = new LatLng(Double.parseDouble(parsings[0]),
                                Double.parseDouble(parsings[1]));

                        receiveString = bufferedReader.readLine();
                        radius = Integer.parseInt(receiveString);

                        ADis.close();
                    }

                    List<SweepingPosition> positions = null;
                    if (sweepingLocationsFile.exists()) {
                        InputStream SLis = new FileInputStream(sweepingLocationsFile);
                        if (SLis != null) {
                            positions = new ArrayList<>();
                            InputStreamReader inputStreamReader = new InputStreamReader(SLis);
                            BufferedReader bufferedReader = new BufferedReader(inputStreamReader);
                            String receiveString = "";

                            while ((receiveString = bufferedReader.readLine()) != null) {
                                String[] parsings = receiveString.split(
                                        SEPARATOR_SWEEPING_LOCATIONS);
                                String latLngData = parsings[0];
                                LatLng latLng = new LatLng(
                                        Double.parseDouble(latLngData.split(",")[0]),
                                        Double.parseDouble(latLngData.split(",")[1]));
                                String address = null;
                                if (parsings.length > 1) {
                                    address = parsings[1];
                                }
                                positions.add(new SweepingPosition(latLng, address));
                            }
                            if (positions.isEmpty()) {
                                positions = null;
                            }

                            SLis.close();
                        }
                    }

                    results.add(new Alarm(Long.parseLong(alarmDir.getName()), center,
                            radius, positions));
                } catch (FileNotFoundException e) {
                } catch (IOException e) {
                }
            }
        }


        return results;
    }

    public static void saveAlarm(Context context, Alarm alarm) {
        Long tsLong = alarm.getTimestamp();
        String ts = tsLong.toString();
        String path = context.getFilesDir() + "/alarms/" + ts;
        File alarmDir = new File(path);
        alarmDir.mkdirs();
        try {
            File alarmDetailsFile = new File(alarmDir.getAbsolutePath() + "/" +
                    FILE_ALARM_DETAILS);
            File sweepingLocationsFile = new File(alarmDir.getAbsolutePath() + "/" +
                    FILE_SWEEPING_LOCATIONS);
            alarmDetailsFile.createNewFile();
            sweepingLocationsFile.createNewFile();

            FileWriter ADwriter = new FileWriter(alarmDetailsFile);

            if (ADwriter != null) {
                String LatLng = alarm.getCenter().latitude + "," + alarm.getCenter().longitude;
                ADwriter.write(LatLng + "\n");

                ADwriter.write(alarm.getRadius() + "\n");

                ADwriter.close();
            }

            if (alarm.getSweepingPositions() != null) {
                FileWriter SLwriter = new FileWriter(sweepingLocationsFile);
                if (SLwriter != null) {
                    for (SweepingPosition pos : alarm.getSweepingPositions()) {
                        String LatLng = pos.getLatLng().latitude + "," + pos.getLatLng().longitude;
                        String address = pos.getAddress();

                        if (address != null) {
                            SLwriter.write(LatLng + SEPARATOR_SWEEPING_LOCATIONS + address + "\n");
                        } else {

                            SLwriter.write(LatLng + "\n");
                        }
                    }

                    SLwriter.close();
                }
            }
        }
        catch (FileNotFoundException e) {
        } catch (IOException e) {
        }
    }

    public static void deleteAlarm(Context context, Alarm alarm) {
        Long tsLong = alarm.getTimestamp();
        String ts = tsLong.toString();
        String path = context.getFilesDir() + "/alarms/" + ts;
        File alarmDir = new File(path);
        if (alarmDir.exists()) {
            if (alarmDir.isDirectory()) {
                String[] children = alarmDir.list();
                for (int i = 0; i < children.length; i++) {
                    new File(alarmDir, children[i]).delete();
                }
            }
            alarmDir.delete();
        }
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
