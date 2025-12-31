package com.gxuwz.ccsa.ui.resident;

import android.content.DialogInterface;
import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.AdapterView;
import android.widget.ArrayAdapter;
import android.widget.Spinner;
import android.widget.TextView;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.github.mikephil.charting.charts.PieChart;
import com.github.mikephil.charting.components.Legend;
import com.github.mikephil.charting.data.PieData;
import com.github.mikephil.charting.data.PieDataSet;
import com.github.mikephil.charting.data.PieEntry;
import com.github.mikephil.charting.formatter.PercentFormatter;
import com.github.mikephil.charting.utils.ColorTemplate;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.adapter.PaymentRecordAdapter;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.PaymentRecord;
import com.gxuwz.ccsa.model.User;
import com.gxuwz.ccsa.util.SharedPreferencesUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentDashboardActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private TextView tvTotalYearly;
    private TextView tvMonthFilter;
    private Spinner spYear;
    private PaymentRecordAdapter adapter;
    private User currentUser;

    // 原始数据
    private List<PaymentRecord> allRecords = new ArrayList<>();

    // 筛选状态
    private int currentSelectedYear;
    // 修复：使用整型记录当前选中的月份，-1 代表“全年”，0-11 代表 1月-12月
    private int currentSelectedMonth = -1;

    // 标签，增加“全年”选项用于单选逻辑
    private final String[] monthLabels = new String[]{
            "全年",
            "1月", "2月", "3月", "4月", "5月", "6月",
            "7月", "8月", "9月", "10月", "11月", "12月"
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_dashboard);

        // 修复：默认选中当前月份，而不是全选
        Calendar calendar = Calendar.getInstance();
        currentSelectedMonth = calendar.get(Calendar.MONTH); // 获取当前月份 (0-11)

        currentUser = SharedPreferencesUtil.getUser(this);
        initViews();

        // 更新一次筛选器文本，显示当前月份
        updateMonthFilterText();

        loadData();
    }

    private void initViews() {
        pieChart = findViewById(R.id.chart_pie);
        recyclerView = findViewById(R.id.recycler_view_records);
        tvTotalYearly = findViewById(R.id.tv_total_yearly);
        spYear = findViewById(R.id.sp_year);
        tvMonthFilter = findViewById(R.id.tv_month_filter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 初始化列表
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentRecordAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 点击月份筛选
        tvMonthFilter.setOnClickListener(v -> showMonthFilterDialog());

        // 初始化年份选择器
        setupYearSpinner();
    }

    private void setupYearSpinner() {
        int currentYear = Calendar.getInstance().get(Calendar.YEAR);
        currentSelectedYear = currentYear;
        // 提供最近5年的选项
        String[] years = {
                String.valueOf(currentYear),
                String.valueOf(currentYear - 1),
                String.valueOf(currentYear - 2),
                String.valueOf(currentYear - 3),
                String.valueOf(currentYear - 4)
        };
        ArrayAdapter<String> adapter = new ArrayAdapter<>(this, android.R.layout.simple_spinner_item, years);
        adapter.setDropDownViewResource(android.R.layout.simple_spinner_dropdown_item);

        spYear.setAdapter(adapter);
        spYear.setOnItemSelectedListener(new AdapterView.OnItemSelectedListener() {
            @Override
            public void onItemSelected(AdapterView<?> parent, View view, int position, long id) {
                currentSelectedYear = Integer.parseInt(years[position]);
                updateUI(); // 年份改变时刷新
            }
            @Override
            public void onNothingSelected(AdapterView<?> parent) {}
        });
    }

    /**
     * 修复：改为单选对话框，明确“选择某一月”或“全年”
     */
    private void showMonthFilterDialog() {
        // 对话框中的选中索引：如果 currentSelectedMonth 是 -1 (全年)，对应索引 0
        // 如果是 0 (1月)，对应索引 1，以此类推
        int checkedItemIndex = currentSelectedMonth + 1;

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle("选择月份");
        builder.setSingleChoiceItems(monthLabels, checkedItemIndex, (dialog, which) -> {
            // update selection immediately
            if (which == 0) {
                currentSelectedMonth = -1; // 全年
            } else {
                currentSelectedMonth = which - 1; // 0-11
            }

            updateMonthFilterText();
            updateUI();
            dialog.dismiss(); // 单选模式下，选择即关闭，体验更好
        });

        builder.setNegativeButton("取消", null);
        builder.show();
    }

    private void updateMonthFilterText() {
        if (currentSelectedMonth == -1) {
            tvMonthFilter.setText("全年数据 ▼");
        } else {
            // currentSelectedMonth 是 0-11，显示时 +1
            tvMonthFilter.setText((currentSelectedMonth + 1) + "月 ▼");
        }
    }

    private void loadData() {
        new Thread(() -> {
            allRecords = AppDatabase.getInstance(this)
                    .paymentRecordDao()
                    .getByPhone(currentUser.getPhone());
            runOnUiThread(this::updateUI);
        }).start();
    }

    /**
     * 核心刷新方法 - 修复联动逻辑
     */
    private void updateUI() {
        // 1. 准备数据容器
        List<PaymentRecord> filteredList = new ArrayList<>();
        double totalAmount = 0;
        Calendar cal = Calendar.getInstance();

        // 2. 遍历所有记录进行归类
        for (PaymentRecord record : allRecords) {
            cal.setTimeInMillis(record.getPayTime());
            int recordYear = cal.get(Calendar.YEAR);
            int recordMonth = cal.get(Calendar.MONTH); // 0-11

            // 年份必须匹配
            if (recordYear == currentSelectedYear) {
                // 修复逻辑：如果是全年(-1) 或者 月份匹配，才加入统计
                if (currentSelectedMonth == -1 || recordMonth == currentSelectedMonth) {
                    filteredList.add(record);
                    totalAmount += record.getAmount();
                }
            }
        }

        // 3. 更新视图
        // 更新列表
        adapter.updateData(filteredList);

        // 更新总金额文本
        tvTotalYearly.setText(String.format("¥ %.2f", totalAmount));

        // 更新饼图 (基于筛选后的数据，实现图表联动)
        if (filteredList.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText(currentSelectedMonth == -1 ? "本年度无数据" : "本月无数据");
            pieChart.invalidate();
        } else {
            updatePieChart(filteredList);
        }
    }

    private void updatePieChart(List<PaymentRecord> records) {
        Map<String, Double> typeMap = new LinkedHashMap<>();
        String[] keys = {"物业费", "维修金", "公摊水电", "电梯费", "其他"};
        for(String k : keys) typeMap.put(k, 0.0);

        for (PaymentRecord r : records) {
            boolean parsed = false;
            if (r.getFeeDetailsSnapshot() != null && !r.getFeeDetailsSnapshot().isEmpty()) {
                try {
                    JSONObject json = new JSONObject(r.getFeeDetailsSnapshot());
                    double property = json.optDouble("property", 0);
                    double maintenance = json.optDouble("maintenance", 0);
                    double utility = json.optDouble("utility", 0);
                    double elevator = json.optDouble("elevator", 0);

                    if (property > 0) typeMap.put("物业费", typeMap.get("物业费") + property);
                    if (maintenance > 0) typeMap.put("维修金", typeMap.get("维修金") + maintenance);
                    if (utility > 0) typeMap.put("公摊水电", typeMap.get("公摊水电") + utility);
                    if (elevator > 0) typeMap.put("电梯费", typeMap.get("电梯费") + elevator);

                    double subTotal = property + maintenance + utility + elevator;
                    if (r.getAmount() > subTotal + 0.01) {
                        typeMap.put("其他", typeMap.get("其他") + (r.getAmount() - subTotal));
                    }
                    parsed = true;
                } catch (Exception e) {
                    e.printStackTrace();
                }
            }

            if (!parsed) {
                typeMap.put("其他", typeMap.get("其他") + r.getAmount());
            }
        }

        List<PieEntry> entries = new ArrayList<>();
        for (Map.Entry<String, Double> entry : typeMap.entrySet()) {
            if (entry.getValue() > 0.01) {
                entries.add(new PieEntry(entry.getValue().floatValue(), entry.getKey()));
            }
        }

        // 如果没有具体费用细则，饼图可能为空，需处理
        if (entries.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("无详细费用构成");
            return;
        }

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);
        set.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);

        // 动态设置中心文字
        if (currentSelectedMonth == -1) {
            pieChart.setCenterText("全年费用");
        } else {
            pieChart.setCenterText((currentSelectedMonth + 1) + "月费用");
        }

        // 设置图例
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true);

        pieChart.invalidate();
    }
}