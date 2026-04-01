package com.example.farmsteadcalendar.dal.database;

import android.content.Context;
import androidx.room.Database;
import androidx.room.Room;
import androidx.room.RoomDatabase;
import com.example.farmsteadcalendar.dal.entities.*;
import com.example.farmsteadcalendar.dal.dao.DictionaryDao;

@Database(entities = {Flower.class, Tree.class, Vegetable.class}, version = 1)
public abstract class DictionaryDatabase extends RoomDatabase {

    private static DictionaryDatabase instance;

    public abstract DictionaryDao dictionaryDao();

    public static synchronized DictionaryDatabase getInstance(Context context) {
        if (instance == null) {
            instance = Room.databaseBuilder(
                            context.getApplicationContext(),
                            DictionaryDatabase.class,
                            "dictionary.db"
                    )
                    .createFromAsset("databases/dictionary.db")
                    .build();
        }
        return instance;
    }
}