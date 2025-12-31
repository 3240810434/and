package com.gxuwz.ccsa.ui.resident;

import android.app.Dialog;
import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.Gravity;
import android.view.ViewGroup;
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
    private List<CheckBox> yearCheckBoxes = new ArrayList<>();
    private List<CheckBox> monthCheckBoxes = new ArrayList<>();

    // 固定的年份和月份数据
    private final List<String> years = Arrays.asList("2023", "2024", "2025", "2026", "2027");
    private final List<String> months = Arrays.asList("1", "2", "3", "4", "5", "6", "7", "8", "9", "10", "11", "12");

    public interface OnFilterListener {
        void onConfirm(FilterData filterData);
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

        // 设置底部弹出样式
        Window window = getWindow();
        if (window != null) {
            window.setLayout(ViewGroup.LayoutParams.MATCH_PARENT, ViewGroup.LayoutParams.WRAP_CONTENT);
            window.setGravity(Gravity.BOTTOM);
            window.setBackgroundDrawable(new ColorDrawable(Color.TRANSPARENT));
        }

        initViews();
    }

    private void initViews() {
        yearContainer = findViewById(R.id.year_container);
        monthContainer = findViewById(R.id.month_container);

        // 动态添加年份选项
        for (String year : years) {
            CheckBox cb = createCheckBox(year + "年", year);
            yearContainer.addView(cb);
            yearCheckBoxes.add(cb);
        }

        // 动态添加月份选项
        for (String month : months) {
            CheckBox cb = createCheckBox(month + "月", month); // tag存纯数字
            monthContainer.addView(cb);
            monthCheckBoxes.add(cb);
        }

        findViewById(R.id.tv_close).setOnClickListener(v -> dismiss());

        findViewById(R.id.btn_reset).setOnClickListener(v -> {
            for (CheckBox cb : yearCheckBoxes) cb.setChecked(false);
            for (CheckBox cb : monthCheckBoxes) cb.setChecked(false);
        });

        findViewById(R.id.btn_confirm).setOnClickListener(v -> {
            FilterData data = new FilterData();

            List<String> selectedYears = new ArrayList<>();
            for (CheckBox cb : yearCheckBoxes) {
                if (cb.isChecked()) selectedYears.add((String) cb.getTag());
            }
            data.setSelectedYears(selectedYears);

            List<String> selectedMonths = new ArrayList<>();
            for (CheckBox cb : monthCheckBoxes) {
                if (cb.isChecked()) {
                    // 补齐为两位数，例如 "1" -> "01"，以便后续匹配
                    String m = (String) cb.getTag();
                    if (m.length() == 1) m = "0" + m;
                    selectedMonths.add(m);
                }
            }
            data.setSelectedMonths(selectedMonths);

            if (listener != null) {
                listener.onConfirm(data);
            }
            dismiss();
        });
    }

    private CheckBox createCheckBox(String text, String tag) {
        CheckBox cb = new CheckBox(context);
        cb.setText(text);
        cb.setTag(tag);
        cb.setTextSize(12);
        cb.setBackgroundResource(R.drawable.checkbox_selector); // 需确保有此背景selector，或使用系统默认
        cb.setButtonDrawable(null); // 去掉默认勾选框图标
        cb.setPadding(20, 10, 20, 10);
        cb.setGravity(Gravity.CENTER);

        GridLayout.LayoutParams params = new GridLayout.LayoutParams();
        params.width = 0;
        params.columnSpec = GridLayout.spec(GridLayout.UNDEFINED, 1f);
        params.setMargins(8, 8, 8, 8);
        cb.setLayoutParams(params);

        return cb;
    }
}