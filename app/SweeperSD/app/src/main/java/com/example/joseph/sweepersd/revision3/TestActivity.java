package com.example.joseph.sweepersd.revision3;

import android.arch.persistence.room.Room;
import android.os.Bundle;
import android.app.Activity;

import com.example.joseph.sweepersd.R;

public class TestActivity extends Activity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_test);

        AppDatabase db = Room.databaseBuilder(getApplicationContext(),
                AppDatabase.class, "app-database").build();

        // TODO make this database access a singleton. Each build call is expensive.

    }

}
