package com.example.joseph.sweepersd.model.limits;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;

import com.example.joseph.sweepersd.model.AddressValidatorManager;

import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.Semaphore;

/**
 * SQLiteOpenHelper for Limits. This object protects access to the limit database and may block if
 * multiple LimitDbHelper objects exist and attempt to access the database at the same time.
 */
public class LimitDbHelper extends SQLiteOpenHelper {
    private static final int VERSION = 1;
    private static final String DATABASE_NAME = "limit_database";

    private static final String TEXT_TYPE = " TEXT";
    private static final String INTEGER_TYPE = " INTEGER";
    private static final String COMMA_SEP = ",";

    private static final String SORT_ORDER_ASCENDING = " ASC";
    private static final String SORT_ORDER_DESCENDING = " DESC";

    private static final String SQL_CREATE_IMPORTED_LIMITS =
            "CREATE TABLE " + LimitDbReaderContract.ImportedLimitEntry.TABLE_NAME + " (" +
                    LimitDbReaderContract.ImportedLimitEntry._ID + " INTEGER PRIMARY KEY," +
                    LimitDbReaderContract.ImportedLimitEntry.COLUMN_STREET_NAME + TEXT_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedLimitEntry.COLUMN_RANGE + TEXT_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedLimitEntry.COLUMN_LIMIT + TEXT_TYPE + " )";

    private static final String SQL_DELETE_IMPORTED_LIMITS =
            "DROP TABLE IF EXISTS " + LimitDbReaderContract.ImportedLimitEntry.TABLE_NAME;

    private static final String SQL_CREATE_IMPORTED_SCHEDULES =
            "CREATE TABLE " + LimitDbReaderContract.ImportedScheduleEntry.TABLE_NAME + " (" +
                    LimitDbReaderContract.ImportedScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_LIMIT_ID + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_START_TIME + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_END_TIME + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_DAY + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_WEEK + INTEGER_TYPE + " )";

    private static final String SQL_DELETE_IMPORTED_SCHEDULES =
            "DROP TABLE IF EXISTS " + LimitDbReaderContract.ImportedScheduleEntry.TABLE_NAME;

    private static final String SQL_CREATE_PERSONAL_LIMITS =
            "CREATE TABLE " + LimitDbReaderContract.PersonalLimitEntry.TABLE_NAME + " (" +
                    LimitDbReaderContract.PersonalLimitEntry._ID + " INTEGER PRIMARY KEY," +
                    LimitDbReaderContract.PersonalLimitEntry.COLUMN_STREET_NAME + TEXT_TYPE + COMMA_SEP +
                    LimitDbReaderContract.PersonalLimitEntry.COLUMN_RANGE + TEXT_TYPE + COMMA_SEP +
                    LimitDbReaderContract.PersonalLimitEntry.COLUMN_LIMIT + TEXT_TYPE + " )";

    private static final String SQL_DELETE_PERSONAL_LIMITS =
            "DROP TABLE IF EXISTS " + LimitDbReaderContract.PersonalLimitEntry.TABLE_NAME;

    private static final String SQL_CREATE_PERSONAL_SCHEDULES =
            "CREATE TABLE " + LimitDbReaderContract.PersonalScheduleEntry.TABLE_NAME + " (" +
                    LimitDbReaderContract.PersonalScheduleEntry._ID + " INTEGER PRIMARY KEY," +
                    LimitDbReaderContract.PersonalScheduleEntry.COLUMN_LIMIT_ID + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.PersonalScheduleEntry.COLUMN_START_TIME + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.PersonalScheduleEntry.COLUMN_END_TIME + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.PersonalScheduleEntry.COLUMN_DAY + INTEGER_TYPE + COMMA_SEP +
                    LimitDbReaderContract.ImportedScheduleEntry.COLUMN_WEEK + INTEGER_TYPE + " )";

    private static final String SQL_DELETE_PERSONAL_SCHEDULES =
            "DROP TABLE IF EXISTS " + LimitDbReaderContract.PersonalScheduleEntry.TABLE_NAME;

    private static final Semaphore DB_SEMAPHORE = new Semaphore(1);

    private final Context mContext;
    private final LimitImporter mLimitImporter;

    public interface LimitImporter {
        List<Limit> importLimits(Context context);
    }

    public LimitDbHelper(Context context) {
        this(context, null);
    }

    public LimitDbHelper(Context context, LimitImporter limitImporter) {
        super(context, DATABASE_NAME, null, VERSION);
        mContext = context;
        if (limitImporter == null) {
            mLimitImporter = new FileLimitImporter();
        } else {
            mLimitImporter = limitImporter;
        }
    }

    public List<Limit> getAllLimits() {
        List<Limit> results = null;
        try {
            DB_SEMAPHORE.acquire();

            SQLiteDatabase db = this.getReadableDatabase();

            results = getLimitsForSelection(db, null, null);

            db.close();
            DB_SEMAPHORE.release();
        } catch (InterruptedException e) {
            results = null;
        }

        return results;
    }

    public List<Limit> getLimitsForStreet(String street) {
        List<Limit> results = null;
        try {
            DB_SEMAPHORE.acquire();

            SQLiteDatabase db = this.getWritableDatabase();

            String selection = LimitDbReaderContract.ImportedLimitEntry.COLUMN_STREET_NAME + " LIKE ?";
            String streetToLower = street.toLowerCase();
            String[] selectionArgs = { streetToLower };

            results = getLimitsForSelection(db, selection, selectionArgs);

            db.close();
            DB_SEMAPHORE.release();
        } catch (InterruptedException e) {
            results = null;
        }

        return results;
    }

    public Limit getLimitForId(int id) {
        if (id < 0) return null;
        Limit result = null;
        try {
            DB_SEMAPHORE.acquire();

            SQLiteDatabase db = this.getReadableDatabase();

            String selection = LimitDbReaderContract.ImportedLimitEntry._ID + " = ?";
            String streetToLower = "" + id;
            String[] selectionArgs = { streetToLower };

            List<Limit> limits = getLimitsForSelection(db, selection, selectionArgs);
            if (limits.size() > 0) {
                result = limits.get(0);
            }

            db.close();
            DB_SEMAPHORE.release();
        } catch (InterruptedException e) {
            result = null;
        }
        return result;
    }

    public void updateLimit(Limit limit) {
        try {
            DB_SEMAPHORE.acquire();

            SQLiteDatabase db = getReadableDatabase();

            // New value for one column
            ContentValues limitValues = new ContentValues();
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_STREET_NAME, limit.getStreet());
            String range = limit.getRange()[0] + "-" + limit.getRange()[1];
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_RANGE, range);
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_LIMIT, limit.getLimit());

            // Which row to update, based on the title
            String selection = LimitDbReaderContract.ImportedLimitEntry._ID + " LIKE ?";
            String[] selectionArgs = { "" + limit.getId() };

            int count = db.update(
                    LimitDbReaderContract.ImportedLimitEntry.TABLE_NAME,
                    limitValues,
                    selection,
                    selectionArgs);

            db.close();
            DB_SEMAPHORE.release();
        } catch (InterruptedException e) {
        }
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

        List<Limit> importedLimits = mLimitImporter.importLimits(mContext);
        for (Limit limit : importedLimits) {
            ContentValues limitValues = new ContentValues();
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_STREET_NAME, limit.getStreet());
            String range = limit.getRange()[0] + "-" + limit.getRange()[1];
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_RANGE, range);
            limitValues.put(LimitDbReaderContract.ImportedLimitEntry.COLUMN_LIMIT, limit.getLimit());
            // insert book
            long id = db.insert(LimitDbReaderContract.ImportedLimitEntry.TABLE_NAME, null, limitValues);

            for (LimitSchedule schedule : limit.getSchedules()) {
                ContentValues scheduleValues = new ContentValues();
                scheduleValues.put(LimitDbReaderContract.ImportedScheduleEntry.COLUMN_LIMIT_ID, id);
                scheduleValues.put(LimitDbReaderContract.ImportedScheduleEntry.COLUMN_START_TIME, schedule.getStartHour());
                scheduleValues.put(LimitDbReaderContract.ImportedScheduleEntry.COLUMN_END_TIME, schedule.getEndHour());
                scheduleValues.put(LimitDbReaderContract.ImportedScheduleEntry.COLUMN_DAY, schedule.getDay());
                scheduleValues.put(LimitDbReaderContract.ImportedScheduleEntry.COLUMN_WEEK, schedule.getWeekNumber());

                db.insert(LimitDbReaderContract.ImportedScheduleEntry.TABLE_NAME, null, scheduleValues);
            }
        }

        AddressValidatorManager.getInstance(mContext).validateAddresses();
    }

    private List<Limit> getLimitsForSelection(SQLiteDatabase db,
                                              String selection, String[] selectionArgs) {
        List<Limit> results = new ArrayList<>();
        // Define a projection that specifies which columns from the database
        // you will actually use after this query.
        String[] limitProjection = {
                LimitDbReaderContract.ImportedLimitEntry._ID,
                LimitDbReaderContract.ImportedLimitEntry.COLUMN_STREET_NAME,
                LimitDbReaderContract.ImportedLimitEntry.COLUMN_RANGE,
                LimitDbReaderContract.ImportedLimitEntry.COLUMN_LIMIT
        };

        // How you want the results sorted in the resulting Cursor
        String sortOrder =
                LimitDbReaderContract.ImportedLimitEntry._ID + SORT_ORDER_ASCENDING;

        Cursor limitCursor = db.query(
                LimitDbReaderContract.ImportedLimitEntry.TABLE_NAME,                     // The table to query
                limitProjection,                               // The columns to return
                selection,                                // The columns for the WHERE clause
                selectionArgs,                            // The values for the WHERE clause
                null,                                     // don't group the rows
                null,                                     // don't filter by row groups
                sortOrder                                 // The sort order
        );
        limitCursor.moveToFirst();
        while (!limitCursor.isAfterLast()) {
            long id = limitCursor.getLong(0);

            String range = limitCursor.getString(2);
            String[] rangeSplit = range.split("-");
            int r[] = new int[2];
            r[0] = Integer.parseInt(rangeSplit[0]);
            r[1] = Integer.parseInt(rangeSplit[1]);

            List<LimitSchedule> schedules = getLimitSchedulesForLimit(db, id);

            results.add(new Limit((int) id,
                    limitCursor.getString(1), r, limitCursor.getString(3), schedules));

            limitCursor.moveToNext();
        }

        limitCursor.close();

        return results;
    }

    private List<LimitSchedule> getLimitSchedulesForLimit(SQLiteDatabase db, long limitId) {
        List<LimitSchedule> schedules = new ArrayList<>();

        String[] scheduleProjection = {
                LimitDbReaderContract.ImportedScheduleEntry._ID,
                LimitDbReaderContract.ImportedScheduleEntry.COLUMN_LIMIT_ID,
                LimitDbReaderContract.ImportedScheduleEntry.COLUMN_START_TIME,
                LimitDbReaderContract.ImportedScheduleEntry.COLUMN_END_TIME,
                LimitDbReaderContract.ImportedScheduleEntry.COLUMN_DAY,
                LimitDbReaderContract.ImportedScheduleEntry.COLUMN_WEEK
        };

        // Filter results WHERE "title" = 'My Title'
        String selection = LimitDbReaderContract.ImportedScheduleEntry.COLUMN_LIMIT_ID + " = ?";
        String[] selectionArgs = { "" + limitId };

        String sortOrder =
                LimitDbReaderContract.ImportedLimitEntry._ID + SORT_ORDER_ASCENDING;

        Cursor scheduleCursor = db.query(
                LimitDbReaderContract.ImportedScheduleEntry.TABLE_NAME,
                scheduleProjection,
                selection,
                selectionArgs,
                null,
                null,
                sortOrder
        );
        scheduleCursor.moveToFirst();
        while (!scheduleCursor.isAfterLast()) {
            int startTime = scheduleCursor.getInt(2);
            int endTime = scheduleCursor.getInt(3);
            int day = scheduleCursor.getInt(4);
            int week = scheduleCursor.getInt(5);

            schedules.add(new LimitSchedule(startTime, endTime, day, week));

            scheduleCursor.moveToNext();
        }

        scheduleCursor.close();

        return schedules;
    }
}
