package com.example.farmsteadcalendar.presentation;

import android.os.Bundle;
import android.util.Log;
import androidx.appcompat.app.AppCompatActivity;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Вкажи свій layout, якщо він є (напр. R.layout.activity_main)
        // setContentView(R.layout.activity_main);

        // Перевірка DAL рівня в окремому потоці (Room забороняє запити в Main Thread)
        new Thread(() -> {
            try {
                Log.d("DAL_CHECK", "Спроба підключення до БД...");
                DictionaryDatabase db = DictionaryDatabase.getInstance(this);

                // Спробуємо отримати список квітів
                List<Flower> flowers = db.dictionaryDao().getAllFlowers();

                if (flowers.isEmpty()) {
                    Log.d("DAL_CHECK", "БАЗА ЗНАЙДЕНА, АЛЕ ТАБЛИЦЯ flowers ПОРОЖНЯ!");
                } else {
                    Log.d("DAL_CHECK", "УСПІХ! Знайдено квітів: " + flowers.size());
                    for (Flower f : flowers) {
                        Log.d("DAL_CHECK", "Квітка: " + f.name);
                    }
                }
            } catch (Exception e) {
                // Якщо файл не знайдено або структура крива - помилка буде тут
                Log.e("DAL_CHECK", "КРИТИЧНА ПОМИЛКА: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();


    }
}