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

    @Query("SELECT * FROM flowers WHERE id = :id")
    Flower getFlowerById(int id);

    @Query("SELECT * FROM trees WHERE id = :id")
    Tree getTreeById(int id);

    @Query("SELECT * FROM vegetables WHERE id = :id")
    Vegetable getVegetableById(int id);
}
