package com.example.farmsteadcalendar.presentation;

import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.core.widget.NestedScrollView;
import androidx.fragment.app.Fragment;
import com.example.farmsteadcalendar.R;
import com.example.farmsteadcalendar.dal.database.DictionaryDatabase;
import com.example.farmsteadcalendar.dal.database.UserDatabase;
import com.example.farmsteadcalendar.dal.entities.Flower;
import com.example.farmsteadcalendar.dal.entities.Note;
import com.example.farmsteadcalendar.dal.entities.UserPlant;
import java.text.SimpleDateFormat;
import java.util.Calendar;
import java.util.List;
import java.util.Locale;

public class DayDetailFragment extends Fragment {

    private static final String ARG_DATE_MS = "date_ms";
    private Calendar currentDate;

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

        // Навігація: плавний скрол до розділів
        btnNavPlants.setOnClickListener(v -> scrollView.smoothScrollTo(0, titlePlants.getTop()));
        btnNavNotes.setOnClickListener(v -> scrollView.smoothScrollTo(0, titleNotes.getTop()));

        // Встановлюємо дату
        tvDayNumber.setText(String.valueOf(currentDate.get(Calendar.DAY_OF_MONTH)));
        String monthName = currentDate.getDisplayName(Calendar.MONTH, Calendar.LONG, new Locale("uk", "UA"));
        if (monthName != null) tvMonthName.setText(monthName);

        // Форматуємо поточну дату для порівняння з нотатками (напр. "2026-04-15")
        SimpleDateFormat sdf = new SimpleDateFormat("yyyy-MM-dd", Locale.getDefault());
        String dateStringForDb = sdf.format(currentDate.getTime());

        // Завантажуємо дані з БД
        new Thread(() -> {
            try {
                // 1. Завантаження Нотаток
                UserDatabase userDb = UserDatabase.getInstance(requireContext()); // Припускаємо що такий метод є
                List<Note> dailyNotes = userDb.userDao().getNotesByDate(dateStringForDb);

                // 2. Завантаження Рослин (Складніше)
                DictionaryDatabase dictDb = DictionaryDatabase.getInstance(requireContext());
                List<UserPlant> myPlants = userDb.userDao().getMyPlants();
                StringBuilder plantsHtml = new StringBuilder();

                for (UserPlant up : myPlants) {
                    if ("flowers".equals(up.category)) {
                        Flower f = dictDb.dictionaryDao().getFlowerById(up.plant_id);
                        if (f != null && isDateInPeriod(currentDate, f.planting_start, f.planting_end)) {
                            plantsHtml.append("🌸 ").append(f.name).append("\nПеріод посадки: ").append(f.planting_start).append("-").append(f.planting_end).append("\n\n");
                        }
                    }
                    // Тут аналогічно треба додати перевірку для "trees" і "vegetables"
                }

                // Оновлюємо UI
                requireActivity().runOnUiThread(() -> {
                    // Виводимо рослини
                    if (plantsHtml.length() > 0) {
                        TextView tvP = new TextView(getContext());
                        tvP.setText(plantsHtml.toString());
                        tvP.setTextSize(16f);
                        containerPlants.addView(tvP);
                    } else {
                        TextView emptyP = new TextView(getContext());
                        emptyP.setText("На цей день немає запланованих робіт з вибраними рослинами.");
                        emptyP.setTextColor(Color.GRAY);
                        containerPlants.addView(emptyP);
                    }

                    // Виводимо нотатки
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

    // Хелпер для перевірки, чи входить поточний день у період типу "15.04" - "20.05"
    private boolean isDateInPeriod(Calendar date, String startStr, String endStr) {
        if (startStr == null || startStr.isEmpty()) return false;
        try {
            // Спрощена логіка. Для повноцінної перевірки треба розпарсити дні і місяці.
            int currentMonth = date.get(Calendar.MONTH) + 1;
            int currentDay = date.get(Calendar.DAY_OF_MONTH);

            String[] sParts = startStr.split("\\.");
            int sDay = Integer.parseInt(sParts[0].trim());
            int sMonth = Integer.parseInt(sParts[1].trim());

            if (endStr == null || endStr.isEmpty()) {
                return (currentMonth == sMonth && currentDay == sDay);
            }

            String[] eParts = endStr.split("\\.");
            int eDay = Integer.parseInt(eParts[0].trim());
            int eMonth = Integer.parseInt(eParts[1].trim());

            // Рахуємо абстрактний "день року" для порівняння
            int currentVal = currentMonth * 100 + currentDay;
            int startVal = sMonth * 100 + sDay;
            int endVal = eMonth * 100 + eDay;

            return currentVal >= startVal && currentVal <= endVal;

        } catch (Exception e) {
            return false;
        }
    }
}