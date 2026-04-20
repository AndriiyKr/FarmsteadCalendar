package com.example.farmsteadcalendar.presentation;

import android.content.res.ColorStateList;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.EditText;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.content.ContextCompat;
import androidx.core.graphics.drawable.DrawableCompat;

import com.applandeo.materialcalendarview.CalendarDay;
import com.applandeo.materialcalendarview.CalendarView;
import com.applandeo.materialcalendarview.EventDay;
import com.example.farmsteadcalendar.R;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.database.UserDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import com.example.farmsteadcalendar.dal.entities.Note;
import com.example.farmsteadcalendar.dal.entities.Tree;
import com.example.farmsteadcalendar.dal.entities.UserPlant;
import com.example.farmsteadcalendar.dal.entities.Vegetable;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.button.MaterialButton;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

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
            if (eventDay != null && eventDay.getCalendar() != null) {
                Log.d("CALENDAR_CLICK", "Day clicked: " + eventDay.getCalendar().getTime());
                android.content.Intent intent = new android.content.Intent(MainActivity.this, DayDetailActivity.class);
                intent.putExtra("SELECTED_DATE", eventDay.getCalendar().getTimeInMillis());
                startActivity(intent);
            }
        });

        // Довге натискання по дню відкриває редактор нотатки для конкретної дати
        calendarView.setOnCalendarDayLongClickListener((CalendarDay day) -> {
            if (day != null && day.getCalendar() != null) {
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateStr = sdf.format(day.getCalendar().getTime());
                showNoteEditorBottomSheet(dateStr, dateStr, null);
                return;
            }

            Calendar today = Calendar.getInstance();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(today.getTime());
            showNoteEditorBottomSheet(dateStr, dateStr, null);
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
                Calendar today = Calendar.getInstance();
                SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
                String dateStr = sdf.format(today.getTime());
                showNoteEditorBottomSheet(dateStr, dateStr, null);
                bottomSheetDialog.dismiss();
            });
        }

        bottomSheetDialog.show();
    }

    private void showNoteEditorBottomSheet(String startDate, String endDate, Note existingNote) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_note_editor, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvNoteTitle);
        if (tvTitle == null) {
            // Додаємо заголовок програмно якщо його немає
            tvTitle = new android.widget.TextView(this);
            tvTitle.setLayoutParams(new LinearLayout.LayoutParams(
                LinearLayout.LayoutParams.MATCH_PARENT,
                LinearLayout.LayoutParams.WRAP_CONTENT
            ));
            tvTitle.setTextSize(20);
            tvTitle.setTypeface(null, android.graphics.Typeface.BOLD);
            tvTitle.setTextColor(getResources().getColor(android.R.color.black));
            tvTitle.setPadding(24, 24, 24, 0);
        }

        if (existingNote != null) {
            tvTitle.setText("Редагування нотатки");
        } else {
            tvTitle.setText("Створення нової нотатки");
        }

        EditText etContent = view.findViewById(R.id.etNoteContent);
        Button btnStartDate = view.findViewById(R.id.btnStartDate);
        Button btnEndDate = view.findViewById(R.id.btnEndDate);
        Button btnSave = view.findViewById(R.id.btnSaveNote);
        Button btnCancel = view.findViewById(R.id.btnCancelNote);
        Button btnDelete = view.findViewById(R.id.btnDeleteNote);

        MaterialButton btnColor1 = view.findViewById(R.id.btnColor1);
        MaterialButton btnColor2 = view.findViewById(R.id.btnColor2);
        MaterialButton btnColor3 = view.findViewById(R.id.btnColor3);
        MaterialButton btnColor4 = view.findViewById(R.id.btnColor4);
        MaterialButton btnColor5 = view.findViewById(R.id.btnColor5);
        MaterialButton btnColorCustom = view.findViewById(R.id.btnColorCustom);

        final String[] currentColor = {"#FF5722"};

        if (existingNote != null) {
            etContent.setText(existingNote.content);
            startDate = existingNote.startDate;
            endDate = existingNote.endDate;
            btnStartDate.setText(startDate);
            btnEndDate.setText(endDate);
            if (existingNote.colorHex != null) {
                currentColor[0] = existingNote.colorHex;
                btnColorCustom.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(existingNote.colorHex)));
                highlightColorButton(existingNote.colorHex, btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
            }
        } else {
            btnDelete.setVisibility(View.GONE);
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        }

        final String[] currentStartDate = {startDate};
        final String[] currentEndDate = {endDate};

        btnColor1.setOnClickListener(v -> {
            currentColor[0] = "#FF5722";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        });
        btnColor2.setOnClickListener(v -> {
            currentColor[0] = "#1E88E5";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        });
        btnColor3.setOnClickListener(v -> {
            currentColor[0] = "#43A047";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        });
        btnColor4.setOnClickListener(v -> {
            currentColor[0] = "#FFB300";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        });
        btnColor5.setOnClickListener(v -> {
            currentColor[0] = "#AB47BC";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        });

        btnColorCustom.setOnClickListener(v -> showColorPickerDialog(newColor -> {
            currentColor[0] = newColor;
            btnColorCustom.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor(newColor)));
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5, btnColorCustom);
        }));

        btnStartDate.setOnClickListener(v -> showDatePicker(date -> {
            currentStartDate[0] = date;
            btnStartDate.setText(date);
        }));

        btnEndDate.setOnClickListener(v -> showDatePicker(date -> {
            currentEndDate[0] = date;
            btnEndDate.setText(date);
        }));

        btnSave.setOnClickListener(v -> {
            String content = etContent.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Нотатка не може бути порожньою", Toast.LENGTH_SHORT).show();
                return;
            }

            if (existingNote != null) {
                updateNote(existingNote.id, currentStartDate[0], currentEndDate[0], content, currentColor[0]);
            } else {
                saveNote(currentStartDate[0], currentEndDate[0], content, currentColor[0]);
            }
            bottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        if (existingNote != null) {
            btnDelete.setOnClickListener(v -> {
                deleteNoteFromBottomSheet(existingNote.id, bottomSheetDialog);
            });
        }

        bottomSheetDialog.show();
    }

    private void highlightColorButton(String color, MaterialButton... buttons) {
        for (MaterialButton btn : buttons) {
            btn.setStrokeWidth(0);
            btn.setStrokeColor(null);
            btn.setScaleX(1f);
            btn.setScaleY(1f);
            btn.setAlpha(0.72f);
        }

        for (MaterialButton btn : buttons) {
            if (btn.getBackgroundTintList() == null) {
                continue;
            }

            int btnColorInt = btn.getBackgroundTintList().getDefaultColor();
            String btnColor = String.format(Locale.getDefault(), "#%06X", (0xFFFFFF & btnColorInt));
            if (btnColor.equalsIgnoreCase(color)) {
                btn.setStrokeColor(ColorStateList.valueOf(Color.parseColor("#212121")));
                btn.setStrokeWidth(4);
                btn.setScaleX(1.12f);
                btn.setScaleY(1.12f);
                btn.setAlpha(1f);
                break;
            }
        }
    }

    private void showDatePicker(DatePickerCallback callback) {
        Calendar calendar = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (view, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            callback.onDateSelected(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void showColorPickerDialog(DayDetailActivity.ColorPickerCallback callback) {
        android.view.View view = new android.view.View(this);
        android.widget.LinearLayout colorLayout = new android.widget.LinearLayout(this);
        colorLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        colorLayout.setPadding(24, 24, 24, 24);

        // Create a color picker using EditText for hex color
        final android.widget.EditText etColor = new android.widget.EditText(this);
        etColor.setHint("#RRGGBB");
        etColor.setText("#FF5722");
        etColor.setTextSize(14);
        etColor.setLayoutParams(new android.widget.LinearLayout.LayoutParams(
            android.widget.LinearLayout.LayoutParams.MATCH_PARENT,
            android.widget.LinearLayout.LayoutParams.WRAP_CONTENT
        ));
        colorLayout.addView(etColor);

        new AlertDialog.Builder(this)
            .setTitle("Виберіть колір")
            .setView(colorLayout)
            .setPositiveButton("OK", (dialog, which) -> {
                String hexColor = etColor.getText().toString().trim();
                if (hexColor.matches("#[0-9A-Fa-f]{6}")) {
                    callback.onColorSelected(hexColor);
                } else {
                    Toast.makeText(this, "Невірний формат кольору. Використовуйте #RRGGBB", Toast.LENGTH_SHORT).show();
                }
            })
            .setNegativeButton("Скасувати", null)
            .show();
    }

    private void updateNote(int noteId, String startDate, String endDate, String content, String colorHex) {
        new Thread(() -> {
            try {
                Note note = new Note();
                note.id = noteId;
                note.startDate = startDate;
                note.endDate = endDate;
                note.content = content;
                note.colorHex = colorHex;

                userDb.userDao().deleteNote(noteId);
                userDb.userDao().addNote(note);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Нотатка оновлена!", Toast.LENGTH_SHORT).show();
                    loadCalendarData();
                });
            } catch (Exception e) {
                Log.e("UPDATE_NOTE_ERR", "Помилка оновлення нотатки", e);
            }
        }).start();
    }

    private void deleteNoteFromBottomSheet(int noteId, BottomSheetDialog dialog) {
        new Thread(() -> {
            try {
                userDb.userDao().deleteNote(noteId);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Нотатка видалена", Toast.LENGTH_SHORT).show();
                    dialog.dismiss();
                    loadCalendarData();
                });
            } catch (Exception e) {
                Log.e("DELETE_NOTE_ERR", "Помилка видалення нотатки", e);
            }
        }).start();
    }

    interface DatePickerCallback {
        void onDateSelected(String date);
    }

    private void saveNote(String startDate, String endDate, String content, String colorHex) {
        new Thread(() -> {
            try {
                Note note = new Note();
                note.startDate = startDate;
                note.endDate = endDate;
                note.content = content;
                note.colorHex = colorHex;

                userDb.userDao().addNote(note);

                runOnUiThread(() -> {
                    Toast.makeText(this, "Нотатка додана!", Toast.LENGTH_SHORT).show();
                    loadCalendarData();
                });
            } catch (Exception e) {
                Log.e("SAVE_NOTE_ERR", "Помилка збереження нотатки", e);
                runOnUiThread(() -> Toast.makeText(this, "Помилка при збереженні", Toast.LENGTH_SHORT).show());
            }
        }).start();
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

                List<Note> notes = userDb.userDao().getAllNotes();
                for (Note note : notes) {
                    addEventDateRange(events, note.startDate, note.endDate, note.colorHex);
                }

                runOnUiThread(() -> {
                    calendarView.setEvents(events);
                });

            } catch (Exception e) {
                Log.e("CALENDAR_LOAD", "Помилка: " + e.getMessage());
            }
        }).start();
    }

    private void addEventDateRange(List<EventDay> events, String startDate, String endDate, String colorHex) {
        Calendar start = parseIsoDate(startDate);
        Calendar end = parseIsoDate(endDate);
        if (start == null || end == null) return;

        if (end.before(start)) {
            Calendar temp = start;
            start = end;
            end = temp;
        }

        Calendar current = (Calendar) start.clone();
        while (!current.after(end)) {
            events.add(new EventDay((Calendar) current.clone(), createTintedNoteDrawable(colorHex)));
            current.add(Calendar.DAY_OF_MONTH, 1);
        }
    }

    private android.graphics.drawable.Drawable createTintedNoteDrawable(String colorHex) {
        android.graphics.drawable.Drawable drawable = ContextCompat.getDrawable(this, R.drawable.dot_note);
        if (drawable == null) {
            return null;
        }

        android.graphics.drawable.Drawable wrapped = DrawableCompat.wrap(drawable.mutate());
        try {
            int color = Color.parseColor(colorHex != null && !colorHex.trim().isEmpty() ? colorHex : "#1E88E5");
            DrawableCompat.setTint(wrapped, color);
        } catch (Exception e) {
            DrawableCompat.setTint(wrapped, Color.parseColor("#1E88E5"));
        }
        return wrapped;
    }

    private Calendar parseIsoDate(String dateText) {
        if (dateText == null || dateText.trim().isEmpty()) return null;
        try {
            String[] parts = dateText.split("-");
            if (parts.length != 3) return null;

            int year = Integer.parseInt(parts[0].trim());
            int month = Integer.parseInt(parts[1].trim()) - 1;
            int day = Integer.parseInt(parts[2].trim());

            Calendar cal = Calendar.getInstance();
            cal.set(year, month, day, 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) {
            Log.e("PARSE_ISO_ERROR", "Не вдалося розпарсити: " + dateText, e);
            return null;
        }
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