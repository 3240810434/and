package com.gxuwz.ccsa.ui.resident;

import android.graphics.Color;
import android.os.Bundle;
import android.view.View;
import android.widget.TextView;

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
import com.gxuwz.ccsa.model.FilterData;
import com.gxuwz.ccsa.model.PaymentRecord;
import com.gxuwz.ccsa.model.User;
import com.gxuwz.ccsa.util.SharedPreferencesUtil;

import org.json.JSONObject;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class PaymentDashboardActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private TextView tvTotalYearly;
    private TextView tvFilter; // 筛选按钮
    private PaymentRecordAdapter adapter;
    private User currentUser;

    // 原始数据
    private List<PaymentRecord> allRecords = new ArrayList<>();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_dashboard);

        currentUser = SharedPreferencesUtil.getUser(this);
        initViews();
        loadData();
    }

    private void initViews() {
        pieChart = findViewById(R.id.chart_pie);
        recyclerView = findViewById(R.id.recycler_view_records);
        tvTotalYearly = findViewById(R.id.tv_total_yearly);
        tvFilter = findViewById(R.id.tv_filter); // 获取右上角筛选按钮

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        // 初始化列表
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentRecordAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        // 设置筛选按钮点击事件
        tvFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        ResidentFilterDialog dialog = new ResidentFilterDialog(this);
        dialog.setOnFilterListener(this::applyFilter);
        dialog.show();
    }

    private void loadData() {
        new Thread(() -> {
            allRecords = AppDatabase.getInstance(this)
                    .paymentRecordDao()
                    .getByPhone(currentUser.getPhone());

            // 默认显示所有数据
            runOnUiThread(() -> applyFilter(new FilterData()));
        }).start();
    }

    /**
     * 根据筛选条件刷新界面
     * @param filterData 筛选数据
     */
    private void applyFilter(FilterData filterData) {
        List<PaymentRecord> filteredList = new ArrayList<>();
        double totalAmount = 0;
        SimpleDateFormat sdfYear = new SimpleDateFormat("yyyy", Locale.getDefault());
        SimpleDateFormat sdfMonth = new SimpleDateFormat("MM", Locale.getDefault());

        List<String> selectedYears = filterData.getSelectedYears();
        List<String> selectedMonths = filterData.getSelectedMonths();

        for (PaymentRecord record : allRecords) {
            Date date = new Date(record.getPayTime());
            String rYear = sdfYear.format(date);
            String rMonth = sdfMonth.format(date);

            // 年份筛选：如果未选中任何年份，默认视为全选；否则检查是否包含
            boolean yearMatch = (selectedYears == null || selectedYears.isEmpty()) || selectedYears.contains(rYear);

            // 月份筛选：如果未选中任何月份，默认视为全选；否则检查是否包含
            boolean monthMatch = (selectedMonths == null || selectedMonths.isEmpty()) || selectedMonths.contains(rMonth);

            if (yearMatch && monthMatch) {
                filteredList.add(record);
                totalAmount += record.getAmount();
            }
        }

        // 更新UI
        adapter.updateData(filteredList);
        tvTotalYearly.setText(String.format("¥ %.2f", totalAmount));
        updatePieChart(filteredList);
    }

    private void updatePieChart(List<PaymentRecord> records) {
        if (records.isEmpty()) {
            pieChart.clear();
            pieChart.setNoDataText("当前筛选条件下无数据");
            pieChart.invalidate();
            return;
        }

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

        PieDataSet set = new PieDataSet(entries, "");
        set.setColors(ColorTemplate.MATERIAL_COLORS);
        set.setValueTextSize(12f);
        set.setValueTextColor(Color.WHITE);
        set.setValueFormatter(new PercentFormatter(pieChart));

        PieData data = new PieData(set);
        pieChart.setData(data);
        pieChart.setUsePercentValues(true);
        pieChart.getDescription().setEnabled(false);
        pieChart.setCenterText("费用构成"); // 固定中心文字

        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true);

        pieChart.invalidate();
    }
}