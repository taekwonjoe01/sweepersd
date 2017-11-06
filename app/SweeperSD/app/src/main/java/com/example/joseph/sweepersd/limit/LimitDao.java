package com.example.joseph.sweepersd.limit;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;
import android.arch.persistence.room.Update;

import java.util.List;

import static android.arch.persistence.room.OnConflictStrategy.REPLACE;

@Dao
public interface LimitDao {
    @Query("SELECT * FROM limits")
    List<Limit> getAllLimits();

    @Query("SELECT * FROM limits WHERE street LIKE (:streetName)")
    List<Limit> getAllByStreet(String streetName);

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
