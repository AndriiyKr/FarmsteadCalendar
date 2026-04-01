package com.example.farmsteadcalendar.dal.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "user_plants")
public class UserPlant {
    @PrimaryKey(autoGenerate = true) public int id;
    public int plant_id;
    public String category;
}