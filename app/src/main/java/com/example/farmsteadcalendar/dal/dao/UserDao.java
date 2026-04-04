package com.example.farmsteadcalendar.dal.dao;

import androidx.room.Dao;
import androidx.room.Insert;
import androidx.room.Query;
import com.example.farmsteadcalendar.dal.entities.*;
import java.util.List;

@Dao
public interface UserDao {
    @Insert
    void addNote(Note note);

    @Insert
    void addUserPlant(UserPlant userPlant);

    @Query("SELECT * FROM user_plants")
    List<UserPlant> getMyPlants();

    @Query("SELECT * FROM notes WHERE date = :dateStr")
    List<Note> getNotesByDate(String dateStr);
}
