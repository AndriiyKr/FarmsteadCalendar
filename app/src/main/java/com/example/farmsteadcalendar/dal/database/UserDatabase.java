package com.example.farmsteadcalendar.dal.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.farmsteadcalendar.dal.entities.*;
import com.example.farmsteadcalendar.dal.dao.UserDao;

@Database(entities = {Note.class, UserPlant.class}, version = 3)
public abstract class UserDatabase extends RoomDatabase {

    private static UserDatabase instance;

    public abstract UserDao userDao();

    public static synchronized UserDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            UserDatabase.class,
                            "user_data.db"
                    )
                    .fallbackToDestructiveMigration()
                    .allowMainThreadQueries()
                    .build();
        }
        return instance;
    }
}