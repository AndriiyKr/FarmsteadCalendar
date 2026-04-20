package com.example.farmsteadcalendar.dal.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;

@Entity(tableName = "notes")
public class Note {
    @PrimaryKey(autoGenerate = true) public int id;
    public String startDate;   // yyyy-MM-dd
    public String endDate;     // yyyy-MM-dd
    public String content;
    public String colorHex;    // колір нотатки (наприклад #FF5722)
}