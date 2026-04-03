package com.example.farmsteadcalendar.dal.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "trees")
public class Tree {
    @PrimaryKey public int id;
    public String name;
    public String planting_start;
    public String planting_end;
    public String blooming_start;
    public String blooming_end;
    public String ripening_start;
    public String ripening_end;
    public String pruning_start;
    public String pruning_end;
    public String fertilizing_start;
    public String fertilizing_end;
}