package com.example.joseph.sweepersd.experimental.activityrecognition;

import androidx.lifecycle.LiveData;
import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;

import java.util.List;

import static androidx.room.OnConflictStrategy.REPLACE;

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
