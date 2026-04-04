package com.example.farmsteadcalendar.presentation;

import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.farmsteadcalendar.R;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.database.UserDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import com.example.farmsteadcalendar.dal.entities.Tree;
import com.example.farmsteadcalendar.dal.entities.UserPlant;
import com.example.farmsteadcalendar.dal.entities.Vegetable;
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

    // Дві наші бази даних
    private DictionaryDatabase db;       // Довідник (Read-only)
    private UserDatabase userDb;         // База користувача (Читання/Запис)

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        // Знаходимо елементи на екрані
        calendarView = findViewById(R.id.calendarView);
        fabAdd = findViewById(R.id.fabAdd);
        topAppBar = findViewById(R.id.topAppBar);

        // Ініціалізація БД
        db = DictionaryDatabase.getInstance(this);
        userDb = UserDatabase.getInstance(this);

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

        // Запуск завантаження подій у календар
        loadCalendarData();

        // Обробка кліку по дню на календарі
        calendarView.setOnDayClickListener(eventDay -> {
            android.content.Intent intent = new android.content.Intent(MainActivity.this, DayDetailActivity.class);
            // Передаємо дату в мілісекундах
            intent.putExtra("SELECTED_DATE", eventDay.getCalendar().getTimeInMillis());
            startActivity(intent);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        // Оновлюємо крапки на календарі щоразу, коли повертаємось на головний екран
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

        // Прив'язуємо кнопки до виклику діалогу з відповідною категорією
        if (btnFlowers != null) {
            btnFlowers.setOnClickListener(v -> {
                showPlantSelectionDialog("flowers");
                bottomSheetDialog.dismiss();
            });
        }

        if (btnTrees != null) {
            btnTrees.setOnClickListener(v -> {
                showPlantSelectionDialog("trees");
                bottomSheetDialog.dismiss();
            });
        }

        if (btnVegetables != null) {
            btnVegetables.setOnClickListener(v -> {
                showPlantSelectionDialog("vegetables");
                bottomSheetDialog.dismiss();
            });
        }

        if (btnNote != null) {
            btnNote.setOnClickListener(v -> {
                Toast.makeText(this, "Додавання нотаток в розробці", Toast.LENGTH_SHORT).show();
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    // Метод, який витягує список назв рослин з Довідника і показує діалог вибору
    private void showPlantSelectionDialog(String category) {
        new Thread(() -> {
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            try {
                // Завантажуємо дані залежно від категорії
                if (category.equals("flowers")) {
                    List<Flower> list = db.dictionaryDao().getAllFlowers();
                    for (Flower f : list) { names.add(f.name); ids.add(f.id); }
                } else if (category.equals("trees")) {
                    List<Tree> list = db.dictionaryDao().getAllTrees();
                    for (Tree t : list) { names.add(t.name); ids.add(t.id); }
                } else if (category.equals("vegetables")) {
                    List<Vegetable> list = db.dictionaryDao().getAllVegetables();
                    for (Vegetable v : list) { names.add(v.name); ids.add(v.id); }
                }

                // Показуємо список у головному потоці
                runOnUiThread(() -> {
                    if (names.isEmpty()) {
                        Toast.makeText(this, "Список порожній", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Оберіть рослину")
                            .setItems(names.toArray(new String[0]), (dialog, which) -> {
                                // Коли користувач обрав рослину — зберігаємо її ID та категорію в його БД
                                savePlantToUserList(ids.get(which), category, names.get(which));
                            })
                            .show();
                });
            } catch (Exception e) {
                Log.e("DIALOG_ERROR", "Помилка завантаження списку: " + e.getMessage());
            }
        }).start();
    }

    // Збереження вибору в таблицю user_plants
    private void savePlantToUserList(int plantId, String category, String plantName) {
        new Thread(() -> {
            try {
                UserPlant up = new UserPlant();
                up.plant_id = plantId;
                up.category = category;

                // Записуємо в базу користувача
                userDb.userDao().addUserPlant(up);

                runOnUiThread(() -> {
                    Toast.makeText(this, plantName + " додано до вашого списку!", Toast.LENGTH_SHORT).show();
                    // ОНОВЛЮЄМО КАЛЕНДАР ОДРАЗУ ПІСЛЯ ДОДАВАННЯ РОСЛИНИ
                    loadCalendarData();
                });
            } catch (Exception e) {
                Log.e("SAVE_ERROR", "Помилка збереження рослини: " + e.getMessage());
            }
        }).start();
    }

    private void loadCalendarData() {
        new Thread(() -> {
            try {
                List<EventDay> events = new ArrayList<>();
                int currentYear = Calendar.getInstance().get(Calendar.YEAR);

                List<UserPlant> myPlants = userDb.userDao().getMyPlants();

                for (UserPlant up : myPlants) {
                    if ("flowers".equals(up.category)) {
                        Flower f = db.dictionaryDao().getFlowerById(up.plant_id);
                        if (f != null) {
                            addEventPeriod(events, f.planting_start, f.planting_end, currentYear, R.drawable.dot_planting);
                        }
                    } else if ("trees".equals(up.category)) {
                        Tree t = db.dictionaryDao().getTreeById(up.plant_id);
                        if (t != null) {
                            addEventPeriod(events, t.planting_start, t.planting_end, currentYear, R.drawable.dot_planting);
                            addEventPeriod(events, t.pruning_start, t.pruning_end, currentYear, R.drawable.dot_pruning);
                            addEventPeriod(events, t.blooming_start, t.blooming_end, currentYear, R.drawable.dot_blooming);
                            addEventPeriod(events, t.ripening_start, t.ripening_end, currentYear, R.drawable.dot_ripening);
                            addEventPeriod(events, t.fertilizing_start, t.fertilizing_end, currentYear, R.drawable.dot_fertilizing);
                        }
                    } else if ("vegetables".equals(up.category)) {
                        Vegetable v = db.dictionaryDao().getVegetableById(up.plant_id);
                        if (v != null) {
                            addEventPeriod(events, v.planting_start, v.planting_end, currentYear, R.drawable.dot_planting);
                            addEventPeriod(events, v.maturity_start, v.maturity_end, currentYear, R.drawable.dot_ripening);
                            addEventPeriod(events, v.fertilizing_start, v.fertilizing_end, currentYear, R.drawable.dot_fertilizing);
                        }
                    }
                }

                runOnUiThread(() -> {
                    calendarView.setEvents(events);
                });

            } catch (Exception e) {
                Log.e("CALENDAR_LOAD", "Помилка: " + e.getMessage());
            }
        }).start();
    }

    // Заповнює крапками кожен день між початком і кінцем періоду
    private void addEventPeriod(List<EventDay> events, String startStr, String endStr, int year, int iconRes) {
        if (startStr == null || startStr.trim().isEmpty() || endStr == null || endStr.trim().isEmpty()) return;

        Calendar startCal = parseMonthDay(startStr.trim(), year);
        Calendar endCal = parseMonthDay(endStr.trim(), year);

        if (startCal != null && endCal != null) {
            // Якщо період переходить на наступний рік (наприклад, з листопада по лютий)
            if (endCal.before(startCal)) {
                endCal.add(Calendar.YEAR, 1);
            }

            Calendar current = (Calendar) startCal.clone();
            // Цикл: додаємо крапку, поки поточний день не стане більшим за кінцевий
            while (!current.after(endCal)) {
                events.add(new EventDay((Calendar) current.clone(), iconRes));
                current.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
    }

    // Парсер для формату "09-30" (місяць-день)
    private Calendar parseMonthDay(String dateText, int year) {
        try {
            String[] parts = dateText.split("-");
            if (parts.length == 2) {
                int month = Integer.parseInt(parts[0].trim()) - 1; // У Java місяці 0-11
                int day = Integer.parseInt(parts[1].trim());

                Calendar cal = Calendar.getInstance();
                cal.set(year, month, day, 0, 0, 0);
                cal.set(Calendar.MILLISECOND, 0);
                return cal;
            }
        } catch (Exception e) {
            Log.e("PARSE_ERROR", "Не вдалося розпарсити: " + dateText);
        }
        return null;
    }
}