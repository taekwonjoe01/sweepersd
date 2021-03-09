package com.example.joseph.sweepersd.limit;

import androidx.room.Dao;
import androidx.room.Delete;
import androidx.room.Insert;
import androidx.room.Query;
import androidx.room.Update;

import java.util.List;

import static androidx.room.OnConflictStrategy.REPLACE;

@Dao
public interface LimitDao {
    @Query("SELECT * FROM limits")
    List<Limit> getAllLimits();

    @Query("SELECT * FROM limits WHERE street LIKE (:streetName)")
    List<Limit> getAllByStreet(String streetName);

    @Query("SELECT * FROM limitschedules WHERE limitId LIKE (:limitId)")
    List<LimitSchedule> getAllSchedulesByLimitId(long limitId);

    @Query("SELECT * FROM limitschedules")
    List<LimitSchedule> getAllSchedules();

    @Insert(onConflict = REPLACE)
    long[] insertLimits(List<Limit> limits);
    @Insert(onConflict = REPLACE)
    void insertLimitSchedules(List<LimitSchedule> schedules);

    @Insert(onConflict = REPLACE)
    long insertLimit(Limit limit);
    @Insert(onConflict = REPLACE)
    void insertLimitSchedule(LimitSchedule schedule);

    @Update(onConflict = REPLACE)
    void updateLimit(Limit limit);

    @Update(onConflict = REPLACE)
    void updateLimits(List<Limit> limits);

    @Delete
    void deleteAll(List<Limit> limit);
}
