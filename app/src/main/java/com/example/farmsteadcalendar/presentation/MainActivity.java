package com.example.farmsteadcalendar.presentation;

import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.RectF;
import android.graphics.Typeface;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
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
import com.google.android.material.dialog.MaterialAlertDialogBuilder;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class MainActivity extends AppCompatActivity {

    private CalendarView calendarView;
    private FloatingActionButton fabAdd;
    private MaterialToolbar topAppBar;
    private LinearLayout legendContainer;

    private Integer filterPlantId = null;
    private String filterCategory = null;
    private Map<String, Boolean> filterPeriodSelections = new HashMap<>();
    private final Map<String, Drawable> drawableCache = new HashMap<>();

    private final Object reloadLock = new Object();
    private boolean isReloadInProgress = false;

    private DictionaryDatabase db;
    private UserDatabase userDb;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        calendarView = findViewById(R.id.calendarView);
        fabAdd = findViewById(R.id.fabAdd);
        topAppBar = findViewById(R.id.topAppBar);
        legendContainer = findViewById(R.id.legendContainer);

        db = DictionaryDatabase.getInstance(this);
        userDb = UserDatabase.getInstance(this);

        buildLegend();



        fabAdd.setOnClickListener(v -> showBottomSheetMenu());

        // Завантажуємо дані на весь рік
        requestCalendarReload();

        calendarView.setOnDayClickListener(eventDay -> {
            if (eventDay != null && eventDay.getCalendar() != null) {
                android.content.Intent intent = new android.content.Intent(MainActivity.this, DayDetailActivity.class);
                intent.putExtra("SELECTED_DATE", eventDay.getCalendar().getTimeInMillis());
                startActivity(intent);
            }
        });

        calendarView.setOnCalendarDayLongClickListener((CalendarDay day) -> {
            Calendar target = (day != null && day.getCalendar() != null) ? day.getCalendar() : Calendar.getInstance();
            String dateStr = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(target.getTime());
            showNoteEditorBottomSheet(dateStr, dateStr, null);
        });
    }

    @Override
    protected void onResume() {
        super.onResume();
        //requestCalendarReload();
    }

    private void requestCalendarReload() {
        synchronized (reloadLock) {
            if (isReloadInProgress) return;
            isReloadInProgress = true;
        }
        loadCalendarData();
    }

    private void loadCalendarData() {
        new Thread(() -> {
            try {
                // Очищаємо кеш малюнків перед завантаженням нових даних
                drawableCache.clear();

                Map<String, List<Integer>> dateColorMap = new HashMap<>();
                Calendar now = Calendar.getInstance();
                int currentYear = now.get(Calendar.YEAR);

                Calendar rangeStart = Calendar.getInstance();
                rangeStart.set(currentYear, Calendar.JANUARY, 1); // Тільки цей рік

                Calendar rangeEnd = Calendar.getInstance();
                rangeEnd.set(currentYear, Calendar.DECEMBER, 31);

                List<UserPlant> myPlants = userDb.userDao().getMyPlants();

                for (UserPlant up : myPlants) {
                    if (filterCategory != null && !filterCategory.equals(up.category)) continue;
                    if (filterPlantId != null && up.plant_id != filterPlantId) continue;

                    for (int y = currentYear; y <= currentYear; y++) {
                        if ("flowers".equals(up.category)) {
                            Flower f = db.dictionaryDao().getFlowerById(up.plant_id);
                            if (f != null && shouldShowPeriod("Період садіння")) {
                                addPeriodToColorMap(dateColorMap, f.planting_start, f.planting_end, y, Color.parseColor("#4CAF50"), rangeStart, rangeEnd);
                            }
                        } else if ("trees".equals(up.category)) {
                            Tree t = db.dictionaryDao().getTreeById(up.plant_id);
                            if (t != null) {
                                if (shouldShowPeriod("Період садіння")) addPeriodToColorMap(dateColorMap, t.planting_start, t.planting_end, y, Color.parseColor("#4CAF50"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період обрізки")) addPeriodToColorMap(dateColorMap, t.pruning_start, t.pruning_end, y, Color.parseColor("#FF9800"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період цвітіння")) addPeriodToColorMap(dateColorMap, t.blooming_start, t.blooming_end, y, Color.parseColor("#E91E63"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період дозрівання")) addPeriodToColorMap(dateColorMap, t.ripening_start, t.ripening_end, y, Color.parseColor("#FFC107"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період удобрення")) addPeriodToColorMap(dateColorMap, t.fertilizing_start, t.fertilizing_end, y, Color.parseColor("#9C27B0"), rangeStart, rangeEnd);
                            }
                        } else if ("vegetables".equals(up.category)) {
                            Vegetable v = db.dictionaryDao().getVegetableById(up.plant_id);
                            if (v != null) {
                                if (shouldShowPeriod("Період садіння")) addPeriodToColorMap(dateColorMap, v.planting_start, v.planting_end, y, Color.parseColor("#4CAF50"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період збору врожаю")) addPeriodToColorMap(dateColorMap, v.maturity_start, v.maturity_end, y, Color.parseColor("#2196F3"), rangeStart, rangeEnd);
                                if (shouldShowPeriod("Період удобрення")) addPeriodToColorMap(dateColorMap, v.fertilizing_start, v.fertilizing_end, y, Color.parseColor("#9C27B0"), rangeStart, rangeEnd);
                            }
                        }
                    }
                }

                List<Note> notes = userDb.userDao().getAllNotes();
                for (Note note : notes) {
                    int colorInt = Color.parseColor(note.colorHex != null && !note.colorHex.trim().isEmpty() ? note.colorHex : "#455A64");
                    addNoteToColorMap(dateColorMap, note.startDate, note.endDate, colorInt, rangeStart, rangeEnd);
                }

                List<EventDay> events = new ArrayList<>();
                for (Map.Entry<String, List<Integer>> entry : dateColorMap.entrySet()) {
                    Calendar cal = parseIsoDate(entry.getKey());
                    Drawable drawable = createCompositeDrawable(entry.getValue());
                    if (cal != null && drawable != null) {
                        events.add(new EventDay(cal, drawable));
                    }
                }

                runOnUiThread(() -> {
                    calendarView.setEvents(events);
                    synchronized (reloadLock) { isReloadInProgress = false; }
                });

            } catch (Exception e) {
                Log.e("CALENDAR_LOAD", "Error: " + e.getMessage());
                synchronized (reloadLock) { isReloadInProgress = false; }
            }
        }).start();
    }

    private void addPeriodToColorMap(Map<String, List<Integer>> map, String startStr, String endStr, int year, int color, Calendar vStart, Calendar vEnd) {
        if (startStr == null || endStr == null) return;
        // Support comma-separated multiple ranges: startStr = "MM-dd,MM-dd", endStr = "MM-dd,MM-dd"
        String[] starts = startStr.split(",");
        String[] ends = endStr.split(",");

        int pairs = Math.min(starts.length, ends.length);
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        for (int i = 0; i < pairs; i++) {
            String s = starts[i].trim();
            String e = ends[i].trim();
            if (s.isEmpty() || e.isEmpty()) continue;
            Calendar startCal = parseMonthDay(s, year);
            Calendar endCal = parseMonthDay(e, year);
            if (startCal == null || endCal == null) continue;

            if (endCal.before(startCal)) endCal.add(Calendar.YEAR, 1);

            Calendar curr = (Calendar) startCal.clone();
            while (!curr.after(endCal)) {
                if (!curr.before(vStart) && !curr.after(vEnd)) {
                    String key = sdf.format(curr.getTime());
                    map.computeIfAbsent(key, k -> new ArrayList<>()).add(color);
                }
                curr.add(Calendar.DAY_OF_MONTH, 1);
            }
        }
    }

    private void addNoteToColorMap(Map<String, List<Integer>> map, String startDate, String endDate, int color, Calendar vStart, Calendar vEnd) {
        Calendar start = parseIsoDate(startDate);
        Calendar end = parseIsoDate(endDate);
        if (start == null || end == null) return;

        // 1. Якщо нотатка повністю поза межами нашого вікна — ігноруємо її
        if (end.before(vStart) || start.after(vEnd)) {
            return;
        }

        // 2. Якщо нотатка починається раніше вікна, починаємо малювати з початку вікна
        Calendar actualStart = start.before(vStart) ? (Calendar) vStart.clone() : start;

        // 3. Якщо нотатка закінчується пізніше вікна, закінчуємо в кінці вікна
        Calendar actualEnd = end.after(vEnd) ? (Calendar) vEnd.clone() : end;

        Calendar curr = (Calendar) actualStart.clone();
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());

        while (!curr.after(actualEnd)) {
            String key = sdf.format(curr.getTime());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(color);
            curr.add(Calendar.DAY_OF_MONTH, 1);

            // Додатковий захист: якщо щось пішло не так, не даємо циклу зробити більше 2000 ітерацій
            if (map.size() > 5000) break;
        }
    }

    private Drawable createCompositeDrawable(List<Integer> colors) {
        if (colors == null || colors.isEmpty()) return null;

        // Створюємо унікальний ключ для комбінації кольорів (наприклад, "green_orange_red")
        List<Integer> unique = new ArrayList<>(new LinkedHashSet<>(colors));
        StringBuilder keyBuilder = new StringBuilder();
        for (Integer c : unique) keyBuilder.append(c).append("_");
        String key = keyBuilder.toString();

        // Якщо така іконка вже є в кеші — повертаємо її
        if (drawableCache.containsKey(key)) {
            return drawableCache.get(key);
        }

        int count = Math.min(unique.size(), 4);
        int width = dpToPx(18), height = dpToPx(18);
        int lineHeight = dpToPx(2), gap = dpToPx(2);
        int totalH = count * lineHeight + (count - 1) * gap;
        int top = (height - totalH) / 2;

        Bitmap bmp = Bitmap.createBitmap(width, height, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(bmp);
        Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG);

        for (int i = 0; i < count; i++) {
            paint.setColor(unique.get(i));
            int y = top + i * (lineHeight + gap);
            canvas.drawRoundRect(new RectF(dpToPx(2), y, width - dpToPx(2), y + lineHeight), lineHeight, lineHeight, paint);
        }

        Drawable drawable = new BitmapDrawable(getResources(), bmp);
        drawableCache.put(key, drawable); // Зберігаємо в кеш
        return drawable;
    }


    private Calendar parseIsoDate(String dateText) {
        try {
            String[] p = dateText.split("-");
            Calendar cal = Calendar.getInstance();
            cal.set(Integer.parseInt(p[0]), Integer.parseInt(p[1]) - 1, Integer.parseInt(p[2]), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) { return null; }
    }

    private Calendar parseMonthDay(String dateText, int year) {
        try {
            String[] p = dateText.split("-");
            Calendar cal = Calendar.getInstance();
            cal.set(year, Integer.parseInt(p[0]) - 1, Integer.parseInt(p[1]), 0, 0, 0);
            cal.set(Calendar.MILLISECOND, 0);
            return cal;
        } catch (Exception e) { return null; }
    }

    private void showBottomSheetMenu() {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        dialog.setContentView(view);

        view.findViewById(R.id.btnFlowers).setOnClickListener(v -> { showPlantSelectionDialog("flowers"); dialog.dismiss(); });
        view.findViewById(R.id.btnTrees).setOnClickListener(v -> { showPlantSelectionDialog("trees"); dialog.dismiss(); });
        view.findViewById(R.id.btnVegetables).setOnClickListener(v -> { showPlantSelectionDialog("vegetables"); dialog.dismiss(); });
        view.findViewById(R.id.btnNote).setOnClickListener(v -> {
            String today = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault()).format(new java.util.Date());
            showNoteEditorBottomSheet(today, today, null);
            dialog.dismiss();
        });
        dialog.show();
    }

    private void showPlantSelectionDialog(String category) {
        new Thread(() -> {
            List<String> names = new ArrayList<>();
            List<Integer> ids = new ArrayList<>();
            if (category.equals("flowers")) {
                for (Flower f : db.dictionaryDao().getAllFlowers()) { names.add(f.name); ids.add(f.id); }
            } else if (category.equals("trees")) {
                for (Tree t : db.dictionaryDao().getAllTrees()) { names.add(t.name); ids.add(t.id); }
            } else if (category.equals("vegetables")) {
                for (Vegetable v : db.dictionaryDao().getAllVegetables()) { names.add(v.name); ids.add(v.id); }
            }

            runOnUiThread(() -> {
                if (names.isEmpty()) return;
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                        .setTitle("🌿 Оберіть рослину")
                        .setIcon(R.drawable.ic_launcher_foreground)
                        .setItems(names.toArray(new String[0]), (d, w) -> savePlantToUserList(ids.get(w), category, names.get(w)))
                        .setNegativeButton("Скасувати", null);
                styleDialog(builder.show());
            });
        }).start();
    }

    private void savePlantToUserList(int plantId, String category, String name) {
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
                    // Повідомляємо користувача, що рослина вже є у списку
                    runOnUiThread(() ->
                            Toast.makeText(this, name + " вже додано раніше!", Toast.LENGTH_SHORT).show()
                    );
                } else {
                    // 3. Якщо рослини немає — додаємо її
                    UserPlant up = new UserPlant();
                    up.plant_id = plantId;
                    up.category = category;

                    userDb.userDao().addUserPlant(up);

                    runOnUiThread(() -> {
                        Toast.makeText(this, name + " додано до вашого саду!", Toast.LENGTH_SHORT).show();
                        requestCalendarReload();
                    });
                }
            } catch (Exception e) {
                Log.e("SAVE_ERROR", "Помилка при перевірці або збереженні: " + e.getMessage());
                runOnUiThread(() ->
                        Toast.makeText(this, "Помилка при збереженні", Toast.LENGTH_SHORT).show()
                );
            }
        }).start();
    }

    private void buildLegend() {
        legendContainer.removeAllViews();

        // Контейнер для заголовка та кнопки
        LinearLayout header = new LinearLayout(this);
        header.setOrientation(LinearLayout.HORIZONTAL);
        header.setLayoutParams(new LinearLayout.LayoutParams(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT));
        header.setGravity(android.view.Gravity.CENTER_VERTICAL);
        header.setPadding(0, 0, 0, dpToPx(8));

        // Текст заголовка
        TextView title = new TextView(this);
        title.setText("📋 Легенда робіт:");
        title.setTextSize(16f);
        title.setTypeface(null, Typeface.BOLD);
        title.setTextColor(Color.BLACK);
        title.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1f));

        // Кнопка фільтра
        Button btnFilter = new Button(this, null, android.R.attr.borderlessButtonStyle);
        btnFilter.setText("Фільтр");
        btnFilter.setTextColor(Color.parseColor("#4CAF50")); // Зелений колір як у вас був
        btnFilter.setOnClickListener(v -> showFilterDialog());

        header.addView(title);
        header.addView(btnFilter);
        legendContainer.addView(header);

        // Дані для елементів легенди
        String[] actions = {"🌱 Період садіння", "✂️ Період обрізки", "🌸 Період цвітіння", "🌾 Період дозрівання", "🧪 Період удобрення", "🌽 Період збору врожаю"};
        int[] colors = {
                Color.parseColor("#4CAF50"), Color.parseColor("#FF9800"),
                Color.parseColor("#E91E63"), Color.parseColor("#FFC107"),
                Color.parseColor("#9C27B0"), Color.parseColor("#2196F3")
        };

        // Додаємо кожен рядок легенди
        for (int i = 0; i < actions.length; i++) {
            LinearLayout item = new LinearLayout(this);
            item.setOrientation(LinearLayout.HORIZONTAL);
            item.setGravity(android.view.Gravity.CENTER_VERTICAL);
            item.setPadding(0, dpToPx(4), 0, dpToPx(4));
            item.setLayoutParams(new LinearLayout.LayoutParams(
                    ViewGroup.LayoutParams.MATCH_PARENT,
                    ViewGroup.LayoutParams.WRAP_CONTENT));

            // Кольоровий квадратик
            View box = new View(this);
            LinearLayout.LayoutParams boxParams = new LinearLayout.LayoutParams(dpToPx(14), dpToPx(14));
            boxParams.rightMargin = dpToPx(12);
            box.setLayoutParams(boxParams);
            box.setBackgroundColor(colors[i]);

            // Текст дії
            TextView txt = new TextView(this);
            txt.setText(actions[i]);
            txt.setTextSize(14f);
            txt.setTextColor(Color.parseColor("#212121"));

            item.addView(box);
            item.addView(txt);
            legendContainer.addView(item);
        }
    }

    // Переконайтеся, що цей метод виглядає саме так
    private int dpToPx(int dp) {
        return (int) (dp * getResources().getDisplayMetrics().density);
    }

    private boolean shouldShowPeriod(String period) {
        if (filterPlantId == null) return true;
        return filterPeriodSelections.getOrDefault(period, true);
    }

    private void showFilterDialog() {
        String[] cats = {"Усі категорії", "🌲 Дерева", "🌸 Квіти", "🌽 Овочі"};
        MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this);
        builder.setTitle("📁 Фільтрація за категорією")
                .setIcon(R.drawable.ic_launcher_foreground)
                .setItems(cats, (d, w) -> {
                    if (w == 0) { // Варіант "Усі"
                        filterCategory = null;
                        filterPlantId = null;
                        filterPeriodSelections.clear();
                        requestCalendarReload();
                    } else {
                        // Визначаємо ключ категорії
                        String selectedCat = w == 1 ? "trees" : (w == 2 ? "flowers" : "vegetables");

                        // Відкриваємо діалог вибору конкретної рослини з цієї категорії
                        showPlantFilterSelection(selectedCat);
                    }
                })
                .setNegativeButton("Скасувати", null);
        styleDialog(builder.show());
    }

    private void showPlantFilterSelection(String category) {
        new Thread(() -> {
            try {
                List<UserPlant> allMyPlants = userDb.userDao().getMyPlants();
                List<UserPlant> filteredUserPlants = new ArrayList<>();
                List<String> names = new ArrayList<>();

                for (UserPlant up : allMyPlants) {
                    if (up.category.equals(category)) {
                        String name = "Рослина #" + up.plant_id;
                        if (category.equals("flowers")) {
                            Flower f = db.dictionaryDao().getFlowerById(up.plant_id);
                            if (f != null) name = "🌸 " + f.name;
                        } else if (category.equals("trees")) {
                            Tree t = db.dictionaryDao().getTreeById(up.plant_id);
                            if (t != null) name = "🌲 " + t.name;
                        } else if (category.equals("vegetables")) {
                            Vegetable v = db.dictionaryDao().getVegetableById(up.plant_id);
                            if (v != null) name = "🌽 " + v.name;
                        }
                        names.add(name);
                        filteredUserPlants.add(up);
                    }
                }

                runOnUiThread(() -> {
                    if (filteredUserPlants.isEmpty()) {
                        Toast.makeText(this, "У вашому списку немає рослин цієї категорії", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    names.add(0, "✓ Усі рослини");

                    MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                            .setTitle("🌿 Вибір рослини")
                            .setIcon(R.drawable.ic_launcher_foreground)
                            .setItems(names.toArray(new String[0]), (dialog, which) -> {
                                if (which == 0) {
                                    // Якщо обрано "Усі", просто показуємо все без вибору періодів
                                    filterCategory = category;
                                    filterPlantId = null;
                                    filterPeriodSelections.clear();
                                    requestCalendarReload();
                                } else {
                                    // Якщо обрана конкретна рослина — відкриваємо вибір періодів
                                    UserPlant selected = filteredUserPlants.get(which - 1);
                                    showPeriodSelectionDialog(selected);
                                }
                            })
                            .setNegativeButton("Скасувати", null);
                    styleDialog(builder.show());
                });
            } catch (Exception e) {
                Log.e("FILTER_ERR", "Помилка: " + e.getMessage());
            }
        }).start();
    }

    private void showPeriodSelectionDialog(UserPlant up) {
        new Thread(() -> {
            List<String> periods = new ArrayList<>();
            // Визначаємо доступні періоди залежно від категорії рослини
            if (up.category.equals("trees")) {
                periods.add("🌱 Період садіння");
                periods.add("✂️ Період обрізки");
                periods.add("🌸 Період цвітіння");
                periods.add("🌾 Період дозрівання");
                periods.add("🧪 Період удобрення");
            } else if (up.category.equals("flowers")) {
                periods.add("🌱 Період садіння");
            } else if (up.category.equals("vegetables")) {
                periods.add("🌱 Період садіння");
                periods.add("🌽 Період збору врожаю");
                periods.add("🧪 Період удобрення");
            }

            String[] items = periods.toArray(new String[0]);
            boolean[] checked = new boolean[items.length];

            // Заповнюємо стан прапорців (за замовчуванням true)
            for (int i = 0; i < items.length; i++) {
                checked[i] = filterPeriodSelections.getOrDefault(items[i].replaceAll("^[^\\p{L}]+ ", ""), true);
            }

            runOnUiThread(() -> {
                MaterialAlertDialogBuilder builder = new MaterialAlertDialogBuilder(this)
                        .setTitle("⏰ Оберіть періоди")
                        .setIcon(R.drawable.ic_launcher_foreground)
                        .setMultiChoiceItems(items, checked, (dialog, which, isChecked) -> {
                            checked[which] = isChecked;
                        })
                        .setPositiveButton("✓ Застосувати", (dialog, which) -> {
                            filterCategory = up.category;
                            filterPlantId = up.plant_id;
                            filterPeriodSelections.clear();
                            for (int i = 0; i < items.length; i++) {
                                String periodKey = items[i].replaceAll("^[^\\p{L}]+ ", "");
                                filterPeriodSelections.put(periodKey, checked[i]);
                            }
                            requestCalendarReload();
                        })
                        .setNegativeButton("✕ Скасувати", null);
                styleDialog(builder.show());
            });
        }).start();
    }

    private void styleDialog(androidx.appcompat.app.AlertDialog dialog) {
        if (dialog != null) {
            // Налаштування кольорів тексту
            int titleId = android.R.id.title;
            TextView title = dialog.findViewById(titleId);
            if (title != null) {
                title.setTextColor(ContextCompat.getColor(this, R.color.dialogTitleText));
                title.setTextSize(18f);
                title.setTypeface(null, android.graphics.Typeface.BOLD);
            }

            // Налаштування список (items)
            android.widget.ListView listView = dialog.getListView();
            if (listView != null) {
                listView.setDividerHeight(1);
                listView.setDivider(new android.graphics.drawable.ColorDrawable(
                        ContextCompat.getColor(this, R.color.colorPrimary)));
            }

            // Налаштування кнопок
            Button positiveButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_POSITIVE);
            Button negativeButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEGATIVE);
            Button neutralButton = dialog.getButton(androidx.appcompat.app.AlertDialog.BUTTON_NEUTRAL);

            if (positiveButton != null) {
                positiveButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
                positiveButton.setTypeface(null, android.graphics.Typeface.BOLD);
            }
            if (negativeButton != null) {
                negativeButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            }
            if (neutralButton != null) {
                neutralButton.setTextColor(ContextCompat.getColor(this, R.color.colorPrimary));
            }
        }
    }

    private void showNoteEditorBottomSheet(String start, String end, Note existing) {
        BottomSheetDialog dialog = new BottomSheetDialog(this);
        View v = getLayoutInflater().inflate(R.layout.bottom_sheet_note_editor, null);
        dialog.setContentView(v);

        // Ініціалізація елементів UI
        TextView tvTitle = v.findViewById(R.id.tvNoteTitle);
        EditText et = v.findViewById(R.id.etNoteContent);
        Button btnSave = v.findViewById(R.id.btnSaveNote);
        Button btnDelete = v.findViewById(R.id.btnDeleteNote);
        Button btnCancel = v.findViewById(R.id.btnCancelNote);
        Button btnStartDate = v.findViewById(R.id.btnStartDate);
        Button btnEndDate = v.findViewById(R.id.btnEndDate);

        // Використовуємо масиви для збереження стану дат всередині лямбда-виразів
        final String[] currentStart = {existing != null ? existing.startDate : start};
        final String[] currentEnd = {existing != null ? existing.endDate : end};

        // Налаштування початкового вигляду
        if (btnStartDate != null) btnStartDate.setText(currentStart[0]);
        if (btnEndDate != null) btnEndDate.setText(currentEnd[0]);

        if (existing == null) {
            if (tvTitle != null) tvTitle.setText("Створення нової нотатки");
            if (btnDelete != null) btnDelete.setVisibility(View.GONE);
            et.setText("");
        } else {
            if (tvTitle != null) tvTitle.setText("Редагування нотатки");
            if (btnDelete != null) btnDelete.setVisibility(View.VISIBLE);
            et.setText(existing.content);
        }

        // Обробники для вибору дат
        if (btnStartDate != null) {
            btnStartDate.setOnClickListener(view -> showDatePicker(date -> {
                currentStart[0] = date;
                btnStartDate.setText(date);
            }));
        }

        if (btnEndDate != null) {
            btnEndDate.setOnClickListener(view -> showDatePicker(date -> {
                currentEnd[0] = date;
                btnEndDate.setText(date);
            }));
        }

        // Кнопка СКАСУВАТИ
        if (btnCancel != null) {
            btnCancel.setOnClickListener(view -> dialog.dismiss());
        }

        // Кнопка ВИДАЛИТИ
        if (btnDelete != null) {
            btnDelete.setOnClickListener(view -> {
                new Thread(() -> {
                    if (existing != null) {
                        userDb.userDao().deleteNote(existing.id);
                        runOnUiThread(() -> {
                            Toast.makeText(this, "Видалено", Toast.LENGTH_SHORT).show();
                            requestCalendarReload();
                            dialog.dismiss();
                        });
                    }
                }).start();
            });
        }

        // Кнопка ЗБЕРЕГТИ
        btnSave.setOnClickListener(view -> {
            String content = et.getText().toString().trim();
            if (content.isEmpty()) {
                Toast.makeText(this, "Нотатка не може бути порожньою", Toast.LENGTH_SHORT).show();
                return;
            }

            Calendar startCal = parseIsoDate(currentStart[0]);
            Calendar endCal = parseIsoDate(currentEnd[0]);

            if (startCal != null && endCal != null) {
                // Перевірка на логічну помилку в датах
                if (endCal.before(startCal)) {
                    Toast.makeText(this, "Дата завершення не може бути раніше початку!", Toast.LENGTH_SHORT).show();
                    return;
                }
                // Перевірка на занадто великий діапазон (захист продуктивності)
                if (endCal.get(Calendar.YEAR) - startCal.get(Calendar.YEAR) > 10) {
                    Toast.makeText(this, "Період не може перевищувати 10 років!", Toast.LENGTH_SHORT).show();
                    return;
                }
            }

            Note n = new Note();
            if (existing != null) n.id = existing.id;
            n.startDate = currentStart[0];
            n.endDate = currentEnd[0];
            n.content = content;
            n.colorHex = (existing != null) ? existing.colorHex : "#455A64";

            new Thread(() -> {
                try {
                    if (existing != null) {
                        userDb.userDao().deleteNote(existing.id);
                    }
                    userDb.userDao().addNote(n);
                    runOnUiThread(() -> {
                        Toast.makeText(this, "Збережено", Toast.LENGTH_SHORT).show();
                        requestCalendarReload();
                        dialog.dismiss();
                    });
                } catch (Exception e) {
                    Log.e("SAVE_NOTE_ERR", "Помилка збереження: " + e.getMessage());
                }
            }).start();
        });

        dialog.show();
    }

    private void showDatePicker(DatePickerCallback callback) {
        Calendar calendar = Calendar.getInstance();
        new android.app.DatePickerDialog(this, (dialog, year, month, dayOfMonth) -> {
            String date = String.format(Locale.getDefault(), "%04d-%02d-%02d", year, month + 1, dayOfMonth);
            callback.onDateSelected(date);
        }, calendar.get(Calendar.YEAR), calendar.get(Calendar.MONTH), calendar.get(Calendar.DAY_OF_MONTH)).show();
    }

    interface DatePickerCallback {
        void onDateSelected(String date);
    }
}