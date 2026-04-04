package com.example.farmsteadcalendar.presentation;

import android.os.Bundle;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;
import androidx.appcompat.app.AppCompatActivity;
import androidx.viewpager2.widget.ViewPager2;
import com.example.farmsteadcalendar.R;
import com.google.android.material.appbar.MaterialToolbar;
import com.google.android.material.bottomsheet.BottomSheetDialog;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import java.util.Calendar;

public class DayDetailActivity extends AppCompatActivity {

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_day_detail);

        MaterialToolbar topAppBar = findViewById(R.id.topAppBar);
        ViewPager2 viewPager = findViewById(R.id.viewPager);
        FloatingActionButton fabAdd = findViewById(R.id.fabAdd);

        // Кнопка НАЗАД
        topAppBar.setNavigationOnClickListener(v -> finish()); // Закриває вікно і повертає до календаря

        // Отримуємо дату, на яку клікнув користувач
        long selectedDateMs = getIntent().getLongExtra("SELECTED_DATE", Calendar.getInstance().getTimeInMillis());
        Calendar selectedDate = Calendar.getInstance();
        selectedDate.setTimeInMillis(selectedDateMs);

        // Встановлюємо заголовок (наприклад, рік)
        topAppBar.setTitle("Огляд дня");

        // Налаштовуємо горталку
        DayPagerAdapter adapter = new DayPagerAdapter(this, selectedDate);
        viewPager.setAdapter(adapter);

        // Переходимо на позицію 5000, яка відповідає вибраній даті
        viewPager.setCurrentItem(5000, false);

        // Кнопка "+"
        fabAdd.setOnClickListener(v -> showBottomSheetMenu());
    }

    private void showBottomSheetMenu() {
        // Використовуємо те саме меню, що і в MainActivity
        BottomSheetDialog bottomSheetDialog = new BottomSheetDialog(this);
        View view = getLayoutInflater().inflate(R.layout.bottom_sheet_menu, null);
        bottomSheetDialog.setContentView(view);

        // (Тут скопіюйте логіку кнопок з MainActivity)
        bottomSheetDialog.show();
    }
}