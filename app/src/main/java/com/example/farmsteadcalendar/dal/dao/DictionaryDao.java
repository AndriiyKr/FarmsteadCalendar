package com.example.farmsteadcalendar.dal.dao;

import androidx.room.Dao;
import androidx.room.Query;
import com.example.farmsteadcalendar.dal.entities.*;
import java.util.List;

@Dao
public interface DictionaryDao {
    @Query("SELECT * FROM flowers")
    List<Flower> getAllFlowers();

    @Query("SELECT * FROM trees")
    List<Tree> getAllTrees();

    @Query("SELECT * FROM vegetables")
    List<Vegetable> getAllVegetables();
}
