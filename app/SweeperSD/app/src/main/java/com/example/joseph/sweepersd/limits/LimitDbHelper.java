package com.example.joseph.sweepersd.limits;

import android.content.ContentValues;
import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.joseph.sweepersd.limits.LimitDbReaderContract.ImportedLimitEntry;
import com.example.joseph.sweepersd.limits.LimitDbReaderContract.ImportedScheduleEntry;
import com.example.joseph.sweepersd.limits.LimitDbReaderContract.PersonalLimitEntry;
import com.example.joseph.sweepersd.limits.LimitDbReaderContract.PersonalScheduleEntry;

import java.util.List;

/**
 * Created by joseph on 8/27/16.
 */
public class LimitDbHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "limit_database";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    private static final String SQL_CREATE_IMPORTED_LIMITS =
            "CREATE TABLE " + ImportedLimitEntry.TABLE_NAME + " (" +
                    ImportedLimitEntry._ID + " INTEGER PRIMARY KEY," +
                    ImportedLimitEntry.COLUMN_STREET_NAME + TEXT_TYPE + COMMA_SEP +
                    ImportedLimitEntry.COLUMN_RANGE + TEXT_TYPE + COMMA_SEP +
                    ImportedLimitEntry.COLUMN_LIMIT + TEXT_TYPE + " )";

    private static final String SQL_DELETE_IMPORTED_LIMITS =
            "DROP TABLE IF EXISTS " + ImportedLimitEntry.TABLE_NAME;

    private static final String SQL_CREATE_IMPORTED_SCHEDULES =
            "CREATE TABLE " + ImportedScheduleEntry.TABLE_NAME + " (" +
                    ImportedScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    ImportedScheduleEntry.COLUMN_LIMIT_ID + INTEGER_TYPE + COMMA_SEP +
                    ImportedScheduleEntry.COLUMN_START_TIME + INTEGER_TYPE + COMMA_SEP +
                    ImportedScheduleEntry.COLUMN_END_TIME + INTEGER_TYPE + COMMA_SEP +
                    ImportedScheduleEntry.COLUMN_DAY + INTEGER_TYPE + " )";

    private static final String SQL_DELETE_IMPORTED_SCHEDULES =
            "DROP TABLE IF EXISTS " + ImportedScheduleEntry.TABLE_NAME;

    private static final String SQL_CREATE_PERSONAL_LIMITS =
            "CREATE TABLE " + PersonalLimitEntry.TABLE_NAME + " (" +
                    PersonalLimitEntry._ID + " INTEGER PRIMARY KEY," +
                    PersonalLimitEntry.COLUMN_STREET_NAME + TEXT_TYPE + COMMA_SEP +
                    PersonalLimitEntry.COLUMN_RANGE + TEXT_TYPE + COMMA_SEP +
                    PersonalLimitEntry.COLUMN_LIMIT + TEXT_TYPE + " )";

    private static final String SQL_DELETE_PERSONAL_LIMITS =
            "DROP TABLE IF EXISTS " + PersonalLimitEntry.TABLE_NAME;

    private static final String SQL_CREATE_PERSONAL_SCHEDULES =
            "CREATE TABLE " + PersonalScheduleEntry.TABLE_NAME + " (" +
                    PersonalScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    PersonalScheduleEntry.COLUMN_LIMIT_ID + INTEGER_TYPE + COMMA_SEP +
                    PersonalScheduleEntry.COLUMN_START_TIME + INTEGER_TYPE + COMMA_SEP +
                    PersonalScheduleEntry.COLUMN_END_TIME + INTEGER_TYPE + COMMA_SEP +
                    PersonalScheduleEntry.COLUMN_DAY + INTEGER_TYPE + " )";

    private static final String SQL_DELETE_PERSONAL_SCHEDULES =
            "DROP TABLE IF EXISTS " + PersonalScheduleEntry.TABLE_NAME;

    private final Context mContext;


    public LimitDbHelper(Context context) {
        super(context, DATABASE_NAME, null, VERSION);
        mContext = context;
    }

    public List<Limit> getAllLimits() {
        return null;
    }

    public List<Limit> getLimitsForStreet(String street) {
        return null;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        loadImportedLimits(db);
        db.execSQL(SQL_CREATE_PERSONAL_LIMITS);
        db.execSQL(SQL_CREATE_PERSONAL_SCHEDULES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        reloadImportedLimits(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        super.onDowngrade(db, oldVersion, newVersion);
    }

    private void reloadImportedLimits(SQLiteDatabase db) {
        db.execSQL(SQL_DELETE_IMPORTED_LIMITS);
        db.execSQL(SQL_DELETE_IMPORTED_SCHEDULES);
        loadImportedLimits(db);
    }

    private void loadImportedLimits(SQLiteDatabase db) {
        db.execSQL(SQL_CREATE_IMPORTED_LIMITS);
        db.execSQL(SQL_CREATE_IMPORTED_SCHEDULES);

        getWritableDatabase();

        List<Limit> importedLimits = LimitImporter.importLimits(mContext);
        for (Limit limit : importedLimits) {
            ContentValues limitValues = new ContentValues();
            limitValues.put(ImportedLimitEntry.COLUMN_STREET_NAME, limit.getStreet());
            String range = limit.getRange()[0] + "-" + limit.getRange()[1];
            limitValues.put(ImportedLimitEntry.COLUMN_RANGE, range);
            limitValues.put(ImportedLimitEntry.COLUMN_LIMIT, limit.getLimit());
            // insert book
            long id = db.insert(ImportedLimitEntry.TABLE_NAME, null, limitValues);

            for (LimitSchedule schedule : limit.getSchedules()) {
                ContentValues scheduleValues = new ContentValues();
                scheduleValues.put(ImportedScheduleEntry.COLUMN_LIMIT_ID, id);
                scheduleValues.put(ImportedScheduleEntry.COLUMN_START_TIME, schedule.getStartHour());
                scheduleValues.put(ImportedScheduleEntry.COLUMN_END_TIME, schedule.getEndHour());
                scheduleValues.put(ImportedScheduleEntry.COLUMN_DAY, schedule.getDay());

                db.insert(ImportedScheduleEntry.TABLE_NAME, null, scheduleValues);
            }
        }

        close();
    }
}
