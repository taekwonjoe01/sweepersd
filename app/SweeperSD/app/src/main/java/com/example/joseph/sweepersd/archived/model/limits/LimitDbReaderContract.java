package com.example.joseph.sweepersd.archived.model.limits;

import android.provider.BaseColumns;

/**
 * Created by joseph on 8/27/16.
 */
public final class LimitDbReaderContract {

    private LimitDbReaderContract () { }

    public static class ImportedLimitEntry implements BaseColumns {
        public static final String TABLE_NAME = "imported_limits";
        public static final String COLUMN_STREET_NAME = "street_name";
        public static final String COLUMN_RANGE = "range";
        public static final String COLUMN_LIMIT = "limit_desc";
    }

    public static class ImportedScheduleEntry implements BaseColumns {
        public static final String TABLE_NAME = "imported_schedules";
        public static final String COLUMN_LIMIT_ID = "limit_id";
        public static final String COLUMN_START_TIME = "start_time";
        public static final String COLUMN_END_TIME = "end_time";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_WEEK = "week";
    }

    public static class PersonalLimitEntry implements BaseColumns {
        public static final String TABLE_NAME = "personal_limits";
        public static final String COLUMN_STREET_NAME = "street_name";
        public static final String COLUMN_RANGE = "range";
        public static final String COLUMN_LIMIT = "limit_desc";
    }

    public static class PersonalScheduleEntry implements BaseColumns {
        public static final String TABLE_NAME = "personal_schedules";
        public static final String COLUMN_LIMIT_ID = "limit_id";
        public static final String COLUMN_START_TIME = "start_time";
        public static final String COLUMN_END_TIME = "end_time";
        public static final String COLUMN_DAY = "day";
        public static final String COLUMN_WEEK = "week";
    }
}
