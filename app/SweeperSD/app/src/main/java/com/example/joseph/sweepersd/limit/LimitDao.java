package com.example.joseph.sweepersd.limit;

import android.arch.lifecycle.LiveData;
import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface LimitDao {
    @Query("SELECT * from limits WHERE isPosted IS 1 LIMIT 999")
    LiveData<List<LimitModel>> loadLimitModels();

    @Query("SELECT * FROM limits")
    LiveData<List<Limit>> getAllLimitsLiveData();

    @Query("SELECT * FROM limits")
    List<Limit> getAllLimits();

    @Query("SELECT * FROM limits WHERE isPosted IS 1")
    LiveData<List<Limit>> getAllPostedLimitsLiveData();

    @Query("SELECT * FROM limits WHERE isPosted IS 1")
    List<Limit> getAllPostedLimits();

    @Query("SELECT * FROM limitSchedules WHERE limitId LIKE :uid")
    LiveData<List<LimitSchedule>> getLimitSchedulesLiveData(long uid);

    @Query("SELECT * FROM limitSchedules WHERE limitId LIKE :uid")
    List<LimitSchedule> getLimitSchedules(long uid);

    @Query("SELECT * FROM limits WHERE street LIKE (:streetName)")
    List<Limit> getAllByStreet(String streetName);

    @Query("SELECT * FROM limits WHERE uid LIKE (:uid)")
    LiveData<Limit> getLimitLiveData(long uid);

    @Insert(onConflict = REPLACE)
    long[] insertLimits(List<Limit> limits);
    @Insert(onConflict = REPLACE)
    void insertLimitSchedules(List<LimitSchedule> schedules);

    @Update(onConflict = REPLACE)
    void updateLimit(Limit limit);

    @Update(onConflict = REPLACE)
    void updateLimits(List<Limit> limits);

    @Delete
    void deleteAll(List<Limit> limit);
}
