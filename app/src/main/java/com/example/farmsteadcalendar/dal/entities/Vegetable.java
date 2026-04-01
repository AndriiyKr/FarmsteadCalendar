package com.example.farmsteadcalendar.dal.entities;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
@Entity(tableName = "vegetables")
public class Vegetable {
    @PrimaryKey public int id;
    public String name;
    public String planting_start;
    public String planting_end;
    public String maturity_start;
    public String maturity_end;
    public String fertilizing_start;
    public String fertilizing_end;
}