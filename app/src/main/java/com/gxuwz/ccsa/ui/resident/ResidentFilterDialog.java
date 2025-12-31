package com.gxuwz.ccsa.ui.resident;

import android.app.Dialog;
import android.content.Context;
import android.os.Bundle;
import android.view.Gravity;
import android.view.Window;
import android.view.WindowManager;
import android.widget.Button;
import android.widget.CheckBox;
import android.widget.GridLayout;
import android.widget.TextView;

import androidx.annotation.NonNull;

import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.model.FilterData;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

public class ResidentFilterDialog extends Dialog {

    private Context context;
    private OnFilterListener listener;

    private GridLayout yearContainer;
    private GridLayout monthContainer;

    // 年份数据 (2023-2027)
    private List<String> yearList = Arrays.asList("2023", "2024", "2025", "2026", "2027");
    // 月份数据
    private List<String> monthList = Arrays.asList("01", "02", "03", "04", "05", "06",
            "07", "08", "09", "10", "11", "12");

    private List<CheckBox> yearCheckboxes = new ArrayList<>();
    private List<CheckBox> monthCheckboxes = new ArrayList<>();

    public interface OnFilterListener {
        void onFilterApplied(FilterData filterData);
    }

    public ResidentFilterDialog(@NonNull Context context) {
        super(context);
        this.context = context;
    }

    public void setOnFilterListener(OnFilterListener listener) {
        this.listener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.dialog_resident_filter);

        setupDialog();
        initViews();
        setupCheckboxes();
        setupListeners();
    }

    private void setupDialog() {
        Window window = getWindow();
        if (window != null) {
            window.setBackgroundDrawableResource(android.R.color.transparent);
            window.setLayout(WindowManager.LayoutParams.MATCH_PARENT, WindowManager.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setWindowAnimations(R.style.BottomDialogAnimation); // 需确保style存在，或者移除这行
        }
    }

    private void initViews() {
        yearContainer = findViewById(R.id.year_container);
        monthContainer = findViewById(R.id.month_container);
    }

    private void setupCheckboxes() {
        // 设置年份复选框
        for (String year : yearList) {
            CheckBox checkBox = createCheckbox(year);
            yearContainer.addView(checkBox);
            yearCheckboxes.add(checkBox);
        }

        // 设置月份复选框
        for (String month : monthList) {
            CheckBox checkBox = createCheckbox(month + "月");
            monthContainer.addView(checkBox);
            monthCheckboxes.add(checkBox);
        }
    }

    private CheckBox createCheckbox(String text) {
        CheckBox checkBox = new CheckBox(context);
        checkBox.setText(text);
        checkBox.setTextSize(14);
        checkBox.setPadding(10, 10, 10, 10);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.setMargins(10, 5, 10, 5);
        params.width = GridLayout.LayoutParams.WRAP_CONTENT;
        params.height = GridLayout.LayoutParams.WRAP_CONTENT;
        checkBox.setLayoutParams(params);

        // 使用通用的复选框样式
        checkBox.setButtonDrawable(R.drawable.checkbox_selector);
        return checkBox;
    }

    private void setupListeners() {
        findViewById(R.id.tv_close).setOnClickListener(v -> dismiss());
        findViewById(R.id.btn_reset).setOnClickListener(v -> resetFilters());
        findViewById(R.id.btn_confirm).setOnClickListener(v -> applyFilters());
    }

    private void resetFilters() {
        for (CheckBox cb : yearCheckboxes) cb.setChecked(false);
        for (CheckBox cb : monthCheckboxes) cb.setChecked(false);
    }

    private void applyFilters() {
        FilterData filterData = new FilterData();

        List<String> selectedYears = new ArrayList<>();
        for (CheckBox cb : yearCheckboxes) {
            if (cb.isChecked()) selectedYears.add(cb.getText().toString());
        }
        filterData.setSelectedYears(selectedYears);

        List<String> selectedMonths = new ArrayList<>();
        for (int i = 0; i < monthCheckboxes.size(); i++) {
            if (monthCheckboxes.get(i).isChecked()) {
                selectedMonths.add(monthList.get(i)); // 保存数字格式 "01", "02" 等
            }
        }
        filterData.setSelectedMonths(selectedMonths);

        if (listener != null) {
            listener.onFilterApplied(filterData);
        }
        dismiss();
    }
}