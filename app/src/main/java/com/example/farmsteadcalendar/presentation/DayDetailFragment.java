package com.example.farmsteadcalendar.presentation;

import android.graphics.Color;
import android.graphics.Typeface;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;

import com.example.farmsteadcalendar.R;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.database.UserDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import com.example.farmsteadcalendar.dal.entities.Note;
import com.example.farmsteadcalendar.dal.entities.Tree;
import com.example.farmsteadcalendar.dal.entities.UserPlant;
import com.example.farmsteadcalendar.dal.entities.Vegetable;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DayDetailFragment extends Fragment {

    private static final String ARG_DATE_MS = "date_ms";
    private Calendar currentDate;

    // Внутрішній клас для зручного групування робіт по одній рослині
    private static class PlantTask {
        String name;
        StringBuilder actions;
        int plantId;
        String category;

        PlantTask(String n, String a, int id, String c) {
            name = n;
            actions = new StringBuilder(a);
            plantId = id;
            category = c;
        }
    }

    public static DayDetailFragment newInstance(long dateMs) {
        DayDetailFragment fragment = new DayDetailFragment();
        Bundle args = new Bundle();
        args.putLong(ARG_DATE_MS, dateMs);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        currentDate = Calendar.getInstance();
        if (getArguments() != null) {
            currentDate.setTimeInMillis(getArguments().getLong(ARG_DATE_MS));
        }
    }

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_day_detail, container, false);

        TextView tvDayNumber = view.findViewById(R.id.tvDayNumber);
        TextView tvMonthName = view.findViewById(R.id.tvMonthName);
        LinearLayout containerPlants = view.findViewById(R.id.containerPlants);
        LinearLayout containerNotes = view.findViewById(R.id.containerNotes);
        NestedScrollView scrollView = view.findViewById(R.id.scrollView);
        Button btnNavPlants = view.findViewById(R.id.btnNavPlants);
        Button btnNavNotes = view.findViewById(R.id.btnNavNotes);
        TextView titlePlants = view.findViewById(R.id.titlePlants);
        TextView titleNotes = view.findViewById(R.id.titleNotes);

        btnNavPlants.setOnClickListener(v -> scrollView.smoothScrollTo(0, titlePlants.getTop()));
        btnNavNotes.setOnClickListener(v -> scrollView.smoothScrollTo(0, titleNotes.getTop()));

        tvDayNumber.setText(String.valueOf(currentDate.get(Calendar.DAY_OF_MONTH)));
        String monthName = currentDate.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("uk", "UA"));
        if (monthName != null) tvMonthName.setText(monthName);

        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStringForDb = sdf.format(currentDate.getTime());

        new Thread(() -> {
            try {
                UserDatabase userDb = UserDatabase.getInstance(requireContext());
                List<Note> dailyNotes = userDb.userDao().getNotesByDate(dateStringForDb);

                DictionaryDatabase dictDb = DictionaryDatabase.getInstance(requireContext());
                List<UserPlant> myPlants = userDb.userDao().getMyPlants();

                List<PlantTask> activeTasks = new ArrayList<>();

                // Збираємо всі роботи на цей день у список
                for (UserPlant up : myPlants) {
                    if ("flowers".equals(up.category)) {
                        Flower f = dictDb.dictionaryDao().getFlowerById(up.plant_id);
                        if (f != null && isDateInPeriod(currentDate, f.planting_start, f.planting_end)) {
                            addTask(activeTasks, f.name, "Період садіння", f.id, up.category);
                        }
                    } else if ("trees".equals(up.category)) {
                        Tree t = dictDb.dictionaryDao().getTreeById(up.plant_id);
                        if (t != null) {
                            if (isDateInPeriod(currentDate, t.planting_start, t.planting_end)) addTask(activeTasks, t.name, "Період садіння", t.id, up.category);
                            if (isDateInPeriod(currentDate, t.pruning_start, t.pruning_end)) addTask(activeTasks, t.name, "Період обрізки", t.id, up.category);
                            if (isDateInPeriod(currentDate, t.blooming_start, t.blooming_end)) addTask(activeTasks, t.name, "Період цвітіння", t.id, up.category);
                            if (isDateInPeriod(currentDate, t.ripening_start, t.ripening_end)) addTask(activeTasks, t.name, "Період дозрівання", t.id, up.category);
                            if (isDateInPeriod(currentDate, t.fertilizing_start, t.fertilizing_end)) addTask(activeTasks, t.name, "Період удобрення", t.id, up.category);
                        }
                    } else if ("vegetables".equals(up.category)) {
                        Vegetable v = dictDb.dictionaryDao().getVegetableById(up.plant_id);
                        if (v != null) {
                            if (isDateInPeriod(currentDate, v.planting_start, v.planting_end)) addTask(activeTasks, v.name, "Період садіння", v.id, up.category);
                            if (isDateInPeriod(currentDate, v.maturity_start, v.maturity_end)) addTask(activeTasks, v.name, "Період збору врожаю", v.id, up.category);
                            if (isDateInPeriod(currentDate, v.fertilizing_start, v.fertilizing_end)) addTask(activeTasks, v.name, "Період удобрення", v.id, up.category);
                        }
                    }
                }

                requireActivity().runOnUiThread(() -> {
                    containerPlants.removeAllViews();

                    // Відмальовуємо рослини
                    if (!activeTasks.isEmpty()) {
                        for (PlantTask task : activeTasks) {
                            containerPlants.addView(createPlantView(task));
                        }
                    } else {
                        TextView emptyP = new TextView(getContext());
                        emptyP.setText("На цей день немає запланованих робіт.");
                        emptyP.setTextColor(Color.GRAY);
                        containerPlants.addView(emptyP);
                    }

                    // Відмальовуємо нотатки
                    containerNotes.removeAllViews();
                    if (dailyNotes != null && !dailyNotes.isEmpty()) {
                        for (Note note : dailyNotes) {
                            TextView tvN = new TextView(getContext());
                            tvN.setText("• " + note.content);
                            tvN.setTextSize(16f);
                            tvN.setPadding(0, 0, 0, 16);
                            containerNotes.addView(tvN);
                        }
                    } else {
                        TextView emptyN = new TextView(getContext());
                        emptyN.setText("Немає нотаток на цей день.");
                        emptyN.setTextColor(Color.GRAY);
                        containerNotes.addView(emptyN);
                    }
                });

            } catch (Exception e) {
                Log.e("DAY_DETAIL", "Помилка завантаження", e);
            }
        }).start();

        return view;
    }

    // Хелпер: додає дію до існуючої рослини або створює нову
    private void addTask(List<PlantTask> tasks, String name, String action, int id, String category) {
        for (PlantTask t : tasks) {
            if (t.plantId == id && t.category.equals(category)) {
                t.actions.append("\n").append(action);
                return;
            }
        }
        tasks.add(new PlantTask(name, action, id, category));
    }

    // Створення красивого блоку з кнопкою Видалити
    private View createPlantView(PlantTask task) {
        // Головний контейнер-рядок
        LinearLayout row = new LinearLayout(getContext());
        row.setOrientation(LinearLayout.HORIZONTAL);
        row.setLayoutParams(new LinearLayout.LayoutParams(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT));
        row.setPadding(0, 0, 0, 32); // Відступ знизу
        row.setGravity(android.view.Gravity.CENTER_VERTICAL);

        // Колонка з текстами
        LinearLayout textCol = new LinearLayout(getContext());
        textCol.setOrientation(LinearLayout.VERTICAL);
        textCol.setLayoutParams(new LinearLayout.LayoutParams(0, ViewGroup.LayoutParams.WRAP_CONTENT, 1.0f));

        // Назва (Жирним)
        TextView tvName = new TextView(getContext());
        tvName.setText(task.name);
        tvName.setTextSize(18f);
        tvName.setTypeface(null, Typeface.BOLD);
        tvName.setTextColor(Color.parseColor("#212121"));

        // Періоди дій (Звичайним)
        TextView tvAction = new TextView(getContext());
        tvAction.setText(task.actions.toString());
        tvAction.setTextSize(14f);
        tvAction.setTextColor(Color.parseColor("#757575"));

        textCol.addView(tvName);
        textCol.addView(tvAction);

        // Кнопка видалення
        Button btnDelete = new Button(getContext(), null, android.R.attr.borderlessButtonStyle);
        btnDelete.setText("Видалити");
        btnDelete.setTextColor(Color.parseColor("#F44336")); // Червоний

        // Логіка натискання кнопки
        btnDelete.setOnClickListener(v -> {
            new Thread(() -> {
                try {
                    // Видаляємо з бази
                    UserDatabase.getInstance(requireContext()).userDao().deleteUserPlant(task.plantId, task.category);

                    // Прибираємо рядок з екрану
                    requireActivity().runOnUiThread(() -> {
                        ((ViewGroup) row.getParent()).removeView(row);
                        Toast.makeText(getContext(), task.name + " видалено з відстеження", Toast.LENGTH_SHORT).show();
                    });
                } catch (Exception e) {
                    Log.e("DELETE_ERR", "Помилка видалення", e);
                }
            }).start();
        });

        row.addView(textCol);
        row.addView(btnDelete);

        return row;
    }

    private boolean isDateInPeriod(Calendar targetDate, String startStr, String endStr) {
        if (startStr == null || startStr.isEmpty() || endStr == null || endStr.isEmpty()) return false;
        try {
            Calendar date = (Calendar) targetDate.clone();
            date.set(Calendar.HOUR_OF_DAY, 0); date.set(Calendar.MINUTE, 0); date.set(Calendar.SECOND, 0); date.set(Calendar.MILLISECOND, 0);

            int currentYear = date.get(Calendar.YEAR);

            String[] sParts = startStr.split("-");
            Calendar startCal = Calendar.getInstance();
            startCal.set(currentYear, Integer.parseInt(sParts[0].trim()) - 1, Integer.parseInt(sParts[1].trim()), 0, 0, 0);
            startCal.set(Calendar.MILLISECOND, 0);

            String[] eParts = endStr.split("-");
            Calendar endCal = Calendar.getInstance();
            endCal.set(currentYear, Integer.parseInt(eParts[0].trim()) - 1, Integer.parseInt(eParts[1].trim()), 0, 0, 0);
            endCal.set(Calendar.MILLISECOND, 0);

            if (endCal.before(startCal)) {
                if (date.get(Calendar.MONTH) <= endCal.get(Calendar.MONTH)) {
                    startCal.add(Calendar.YEAR, -1);
                } else {
                    endCal.add(Calendar.YEAR, 1);
                }
            }

            return !date.before(startCal) && !date.after(endCal);
        } catch (Exception e) {
            return false;
        }
    }
}