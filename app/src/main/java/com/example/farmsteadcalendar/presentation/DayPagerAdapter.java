package com.example.farmsteadcalendar.presentation;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.fragment.app.FragmentActivity;
import androidx.viewpager2.adapter.FragmentStateAdapter;
import java.util.Calendar;

public class DayPagerAdapter extends FragmentStateAdapter {
    private final Calendar startDate;

    public DayPagerAdapter(@NonNull FragmentActivity fragmentActivity, Calendar selectedDate) {
        super(fragmentActivity);
        // Зберігаємо дату, зміщену на 5000 днів назад, щоб можна було гортати і в минуле, і в майбутнє
        this.startDate = (Calendar) selectedDate.clone();
        this.startDate.add(Calendar.DAY_OF_YEAR, -5000);
    }

    @NonNull
    @Override
    public Fragment createFragment(int position) {
        Calendar day = (Calendar) startDate.clone();
        day.add(Calendar.DAY_OF_YEAR, position);
        return DayDetailFragment.newInstance(day.getTimeInMillis());
    }

    @Override
    public int getItemCount() {
        return 10000; // Дозволяє гортати на 13 років вперед і назад
    }
}