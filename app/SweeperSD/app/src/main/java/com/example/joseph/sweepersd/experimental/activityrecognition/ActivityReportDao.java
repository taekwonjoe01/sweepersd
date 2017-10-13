package com.example.joseph.sweepersd.experimental.activityrecognition;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface ActivityReportDao {
    @Query("SELECT * FROM activityreports")
    LiveData<List<ActivityReport>> getAllActivityReportsLiveData();

    @Query("SELECT * FROM activityreports")
    List<ActivityReport> getAllActivityReports();

    @Insert(onConflict = REPLACE)
    long addActivityReport(ActivityReport activityReport);

    @Delete
    void deleteAll(List<ActivityReport> limit);
}
