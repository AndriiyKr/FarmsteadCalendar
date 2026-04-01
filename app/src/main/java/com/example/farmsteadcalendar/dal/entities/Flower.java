package com.example.farmsteadcalendar.dal.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "flowers")
public class Flower {
    @PrimaryKey
    public int id;
    public String name;
    public String planting_start;
    public String planting_end;
}