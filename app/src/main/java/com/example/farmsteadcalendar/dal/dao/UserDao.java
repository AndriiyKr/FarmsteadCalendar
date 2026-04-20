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

    @Query("SELECT * FROM notes WHERE startDate <= :dateStr AND endDate >= :dateStr")
    List<Note> getNotesByDate(String dateStr);

    @Query("SELECT * FROM notes")
    List<Note> getAllNotes();

    @Query("DELETE FROM user_plants WHERE plant_id = :plantId AND category = :category")
    void deleteUserPlant(int plantId, String category);

    @Query("DELETE FROM notes WHERE id = :noteId")
    void deleteNote(int noteId);

}
