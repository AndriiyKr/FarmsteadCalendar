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
import androidx.viewpager2.widget.ViewPager2;

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

public class DayDetailActivity extends AppCompatActivity {

    private long initialSelectedDateMs;
    private Calendar selectedDate;
    private ViewPager2 viewPager;
    private DictionaryDatabase db;
    private UserDatabase userDb;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        viewPager = findViewById(R.id.viewPager);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        // Ініціалізація БД
        db = DictionaryDatabase.getInstance(this);
        userDb = UserDatabase.getInstance(this);

        // Кнопка НАЗАД
        topAppBar.setNavigationOnClickListener(v -> finish()); // Закриває вікно і повертає до календаря

        initialSelectedDateMs = getIntent().getLongExtra("SELECTED_DATE", Calendar.getInstance().getTimeInMillis());

        // Отримуємо дату, на яку клікнув користувач
        Calendar baseDate = Calendar.getInstance();
        baseDate.setTimeInMillis(initialSelectedDateMs);

        // Встановлюємо заголовок (наприклад, рік)
        topAppBar.setTitle("Огляд дня");

        // Налаштовуємо горталку
        DayPagerAdapter adapter = new DayPagerAdapter(this, baseDate);
        viewPager.setAdapter(adapter);

        // Переходимо на позицію 5000, яка відповідає вибраній даті
        viewPager.setCurrentItem(5000, false);
        updateSelectedDateForPosition(5000);

        viewPager.registerOnPageChangeCallback(new ViewPager2.OnPageChangeCallback() {
            @Override
            public void onPageSelected(int position) {
                updateSelectedDateForPosition(position);
            }
        });

        // Кнопка "+"
        fabAdd.setOnClickListener(v -> showBottomSheetMenu());
    }

    private void updateSelectedDateForPosition(int position) {
        Calendar c = Calendar.getInstance();
        c.setTimeInMillis(initialSelectedDateMs);
        c.add(Calendar.DAY_OF_YEAR, position - 5000);
        selectedDate = c;
    }
    private void showBottomSheetMenu() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(view);

        Button btnTrees = view.findViewById(R.id.btnTrees);
        Button btnFlowers = view.findViewById(R.id.btnFlowers);
        Button btnVegetables = view.findViewById(R.id.btnVegetables);
        Button btnNote = view.findViewById(R.id.btnNote);

        if (btnNote != null) {
            btnNote.setOnClickListener(v -> {
                showNoteAddDialog();
                bottomSheetDialog.dismiss();
            });
        }

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

        bottomSheetDialog.show();
    }

    public void openNoteEditor(Note note) {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_note_editor, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvNoteTitle);
        if (tvTitle == null) {
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
        tvTitle.setText("Редагування нотатки");

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

        etContent.setText(note.content);
        btnStartDate.setText(note.startDate);
        btnEndDate.setText(note.endDate);

        final String[] currentColor = {note.colorHex != null ? note.colorHex : "#455A64"};

        highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);

        final String[] currentStartDate = {note.startDate};
        final String[] currentEndDate = {note.endDate};

        btnColor1.setOnClickListener(v -> {
            currentColor[0] = "#455A64";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor2.setOnClickListener(v -> {
            currentColor[0] = "#6D4C41";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor3.setOnClickListener(v -> {
            currentColor[0] = "#00838F";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor4.setOnClickListener(v -> {
            currentColor[0] = "#3949AB";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor5.setOnClickListener(v -> {
            currentColor[0] = "#5D4037";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });


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

            updateNote(note.id, currentStartDate[0], currentEndDate[0], content, currentColor[0], bottomSheetDialog);
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                userDb.userDao().deleteNote(note.id);
                runOnUiThread(() -> {
                    Toast.makeText(this, "Нотатка видалена", Toast.LENGTH_SHORT).show();
                    bottomSheetDialog.dismiss();
                    refreshCurrentDay();
                });
            }).start();
        });

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
        new android.app.DatePickerDialog(this, (dialog, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            callback.onDateSelected(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    private void updateNote(int noteId, String startDate, String endDate, String content, String colorHex, BottomSheetDialog dialog) {
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
                    dialog.dismiss();
                    refreshCurrentDay();
                });
            } catch (Exception e) {
                Log.e("UPDATE_NOTE_ERR", "Помилка оновлення", e);
            }
        }).start();
    }

    interface DatePickerCallback {
        void onDateSelected(String date);
    }

    private void showNoteAddDialog() {
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_note_editor, null);
        bottomSheetDialog.setContentView(view);

        TextView tvTitle = view.findViewById(R.id.tvNoteTitle);
        if (tvTitle == null) {
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
        tvTitle.setText("Створення нової нотатки");

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

        btnDelete.setVisibility(View.GONE);

        final String[] currentColor = {"#455A64"};
        highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);

        if (selectedDate == null) {
            Toast.makeText(this, "Не вдалося визначити дату", Toast.LENGTH_SHORT).show();
            return;
        }

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStr = sdf.format(selectedDate.getTime());
        btnStartDate.setText(dateStr);
        btnEndDate.setText(dateStr);

        final String[] currentStartDate = {dateStr};
        final String[] currentEndDate = {dateStr};

        btnColor1.setOnClickListener(v -> {
            currentColor[0] = "#455A64";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor2.setOnClickListener(v -> {
            currentColor[0] = "#6D4C41";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor3.setOnClickListener(v -> {
            currentColor[0] = "#00838F";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor4.setOnClickListener(v -> {
            currentColor[0] = "#3949AB";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });
        btnColor5.setOnClickListener(v -> {
            currentColor[0] = "#5D4037";
            highlightColorButton(currentColor[0], btnColor1, btnColor2, btnColor3, btnColor4, btnColor5);
        });


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

            saveNote(currentStartDate[0], currentEndDate[0], content, currentColor[0]);
            bottomSheetDialog.dismiss();
        });

        btnCancel.setOnClickListener(v -> bottomSheetDialog.dismiss());

        bottomSheetDialog.show();
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
                    refreshCurrentDay();
                });
            } catch (Exception e) {
                Log.e("SAVE_NOTE_ERR", "Помилка збереження нотатки", e);
                runOnUiThread(() ->
                        Toast.makeText(this, "Помилка при збереженні", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void showPlantSelectionDialog(String category) {
        new Thread(() -> {
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();

            try {
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

                runOnUiThread(() -> {
                    if (names.isEmpty()) {
                        Toast.makeText(this, "Список порожній", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    new AlertDialog.Builder(this)
                            .setTitle("Оберіть рослину")
                            .setItems(names.toArray(new String[0]), (dialog, which) -> {
                                savePlantToUserList(ids.get(which), category, names.get(which));
                            })
                            .show();
                });
            } catch (Exception e) {
                Log.e("DIALOG_ERROR", "Помилка завантаження списку: " + e.getMessage());
            }
        }).start();
    }

    private void savePlantToUserList(int plantId, String category, String plantName) {
        new Thread(() -> {
            try {
                // 1. Отримуємо список усіх рослин, які вже є у користувача
                List<UserPlant> existingPlants = userDb.userDao().getMyPlants();

                // 2. Перевіряємо, чи є серед них рослина з таким самим ID та категорією
                boolean isAlreadyAdded = false;
                for (UserPlant p : existingPlants) {
                    if (p.plant_id == plantId && p.category.equals(category)) {
                        isAlreadyAdded = true;
                        break;
                    }
                }

                if (isAlreadyAdded) {
                    // Повідомляємо, що рослина вже існує
                    runOnUiThread(() ->
                            Toast.makeText(this, plantName + " вже є у вашому списку!", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    // 3. Якщо дублікатів немає — зберігаємо
                    UserPlant up = new UserPlant();
                    up.plant_id = plantId;
                    up.category = category;

                    userDb.userDao().addUserPlant(up);

                    runOnUiThread(() -> {
                        Toast.makeText(this, plantName + " додано!", Toast.LENGTH_SHORT).show();
                        refreshCurrentDay();
                    });
                }
            } catch (Exception e) {
                Log.e("SAVE_ERROR", "Помилка збереження: " + e.getMessage());
            }
        }).start();
    }

    private void refreshCurrentDay() {
        if (viewPager == null || selectedDate == null) {
            return;
        }

        DayPagerAdapter adapter = new DayPagerAdapter(this, selectedDate);
        viewPager.setAdapter(adapter);
        viewPager.setCurrentItem(5000, false);
    }

    private void showColorPickerDialog(ColorPickerCallback callback) {
        android.widget.LinearLayout colorLayout = new android.widget.LinearLayout(this);
        colorLayout.setOrientation(android.widget.LinearLayout.VERTICAL);
        colorLayout.setPadding(24, 24, 24, 24);

        final android.widget.EditText etColor = new android.widget.EditText(this);
        etColor.setHint("#RRGGBB");
        etColor.setText("#455A64");
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

    interface ColorPickerCallback {
        void onColorSelected(String color);
    }
}