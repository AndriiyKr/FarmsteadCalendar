package com.example.farmsteadcalendar.presentation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.farmsteadcalendar.R;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private FloatingActionButton fabAdd;
    private MaterialToolbar topAppBar;
    private DictionaryDatabase db;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        // Обов'язково вказуємо наш новий дизайн
        setContentView(R.layout.activity_main);

        // Знаходимо елементи на екрані
        calendarView = findViewById(R.id.calendarView);
        fabAdd = findViewById(R.id.fabAdd);
        topAppBar = findViewById(R.id.topAppBar);

        // Ініціалізація БД
        db = DictionaryDatabase.getInstance(this);

        // Обробка кнопок у верхньому хедері (Про додаток / Налаштування)
        topAppBar.setOnMenuItemClickListener(item -> {
            int id = item.getItemId();
            if (id == R.id.action_about) {
                Toast.makeText(this, "Farmstead Calendar v1.0", Toast.LENGTH_SHORT).show();
                return true;
            } else if (id == R.id.action_settings) {
                Toast.makeText(this, "Налаштування", Toast.LENGTH_SHORT).show();
                return true;
            }
            return false;
        });

        // Кнопка "+" внизу екрану
        fabAdd.setOnClickListener(v -> showBottomSheetMenu());

        // Запуск перевірки БД та завантаження подій у календар
        loadCalendarData();
    }

    private void showBottomSheetMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(view);

        Button btnTrees = view.findViewById(R.id.btnTrees);
        Button btnFlowers = view.findViewById(R.id.btnFlowers);
        Button btnVegetables = view.findViewById(R.id.btnVegetables);
        Button btnNote = view.findViewById(R.id.btnNote);

        if (btnFlowers != null) {
            btnFlowers.setOnClickListener(v -> {
                Toast.makeText(this, "Відкриваємо список квітів", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });
        }

        // Інші кнопки також можна налаштувати тут
        if (btnNote != null) btnNote.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnTrees != null) btnTrees.setOnClickListener(v -> bottomSheetDialog.dismiss());
        if (btnVegetables != null) btnVegetables.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
    }

    private void loadCalendarData() {
        // Перевірка DAL рівня та завантаження даних в окремому потоці
        new Thread(() -> {
            try {
                Log.d("DAL_CHECK", "Спроба підключення до БД...");
                List<Flower> flowers = db.dictionaryDao().getAllFlowers();
                List<EventDay> events = new ArrayList<>();

                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                if (flowers.isEmpty()) {
                    Log.d("DAL_CHECK", "БАЗА ЗНАЙДЕНА, АЛЕ ТАБЛИЦЯ flowers ПОРОЖНЯ!");
                } else {
                    Log.d("DAL_CHECK", "УСПІХ! Знайдено квітів: " + flowers.size());

                    // Проходимось по кожній квітці і створюємо для неї крапку
                    for (Flower f : flowers) {
                        Log.d("DAL_CHECK", "Квітка: " + f.name + " | Посадка: " + f.planting_start);

                        Calendar eventDate = parseDateForYear(f.planting_start, currentYear);
                        if (eventDate != null) {
                            // Додаємо подію з іконкою крапки
                            // Переконайтеся, що файл R.drawable.dot_marker_flowers існує
                            events.add(new EventDay(eventDate, R.drawable.dot_marker_flowers));
                        }
                    }
                }

                // Оновлюємо UI (календар) у головному потоці
                runOnUiThread(() -> {
                    calendarView.setEvents(events);
                    Log.d("CALENDAR", "Крапки успішно додано на календар");
                });

            } catch (Exception e) {
                Log.e("DAL_CHECK", "КРИТИЧНА ПОМИЛКА: " + e.getMessage());
                e.printStackTrace();
            }
        }).start();
    }

    // Метод для перетворення тексту з БД ("15.04") у формат Calendar
    private Calendar parseDateForYear(String dateText, int year) {
        if (dateText == null || dateText.isEmpty()) return null;
        try {
            String[] parts = dateText.split("[.\\-]");
            if (parts.length >= 2) {
                int day = Integer.parseInt(parts[0].trim());
                int month = Integer.parseInt(parts[1].trim()) - 1; // У Java місяці починаються з 0

                Calendar calendar = Calendar.getInstance();
                calendar.set(year, month, day, 0, 0, 0);
                calendar.set(Calendar.MILLISECOND, 0);
                return calendar;
            }
        } catch (Exception e) {
            Log.e("PARSE_ERROR", "Не вдалося розпарсити дату: " + dateText);
        }
        return null;
    }
}