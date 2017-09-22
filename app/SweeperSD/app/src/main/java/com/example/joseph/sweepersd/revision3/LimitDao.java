package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.Dao;
import android.arch.persistence.room.Delete;
import android.arch.persistence.room.Insert;
import android.arch.persistence.room.Query;

import java.util.List;

@Dao
public interface LimitDao {
    @Query("SELECT * FROM limits")
    List<Limit> getAll();

    @Query("SELECT * FROM limits WHERE street LIKE (:streetName)")
    List<Limit> getAllByStreet(String streetName);

    @Query("SELECT * FROM limits WHERE uid LIKE (:id)")
    List<Limit> get(int id);

    @Insert
    void insertAll(Limit... limits);

    @Delete
    void delete(Limit limit);
}
