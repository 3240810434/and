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
import com.gxuwz.ccsa.model.PropertyFeeBill;
import com.gxuwz.ccsa.model.User;
import com.gxuwz.ccsa.util.SharedPreferencesUtil;

import org.json.JSONObject;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

public class PaymentDashboardActivity extends AppCompatActivity {

    private PieChart pieChart;
    private RecyclerView recyclerView;
    private TextView tvTotalYearly;
    private TextView tvFilter;
    private PaymentRecordAdapter adapter;
    private User currentUser;

    // 原始数据
    private List<PaymentRecord> allRecords = new ArrayList<>();
    // 账单数据映射 Map<BillId, PropertyFeeBill>
    private Map<Long, PropertyFeeBill> billMap = new HashMap<>();

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

        // 确保你的layout文件中有这个ID的TextView作为筛选按钮
        tvFilter = findViewById(R.id.tv_filter);

        findViewById(R.id.iv_back).setOnClickListener(v -> finish());

        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentRecordAdapter(this, new ArrayList<>());
        recyclerView.setAdapter(adapter);

        tvFilter.setOnClickListener(v -> showFilterDialog());
    }

    private void showFilterDialog() {
        ResidentFilterDialog dialog = new ResidentFilterDialog(this);
        dialog.setOnFilterListener(this::applyFilter);
        dialog.show();
    }

    private void loadData() {
        new Thread(() -> {
            AppDatabase db = AppDatabase.getInstance(this);

            // 1. 获取该用户的所有缴费记录
            allRecords = db.paymentRecordDao().getByPhone(currentUser.getPhone());

            // 2. 获取所有的账单信息，建立 BillId -> Bill 的映射
            // 这样做是为了筛选时能直接查到某条记录对应的账单是哪年哪月的
            List<PropertyFeeBill> allBills = db.propertyFeeBillDao().getAll(); // 假设Dao有getAll方法，或者查询相关账单
            for (PropertyFeeBill bill : allBills) {
                billMap.put(bill.getId(), bill);
            }

            // 默认显示所有数据
            runOnUiThread(() -> applyFilter(new FilterData()));
        }).start();
    }

    /**
     * 核心筛选逻辑修改：
     * 不再使用 record.getPayTime() (支付时间)
     * 而是使用 billMap.get(record.getBillId()) 获取账单的 year 和 month (账单周期)
     */
    private void applyFilter(FilterData filterData) {
        List<PaymentRecord> filteredList = new ArrayList<>();
        double totalAmount = 0;

        List<String> selectedYears = filterData.getSelectedYears();
        List<String> selectedMonths = filterData.getSelectedMonths();

        for (PaymentRecord record : allRecords) {
            // 获取该记录对应的账单
            PropertyFeeBill bill = billMap.get(record.getBillId());

            if (bill != null) {
                String billYear = bill.getYear(); // 账单所属年份，如 "2025"
                String billMonth = bill.getMonth(); // 账单所属月份，如 "01" 或 "1"

                // 统一格式化月份，确保 "1" 和 "01" 能匹配
                if (billMonth != null && billMonth.length() == 1) {
                    billMonth = "0" + billMonth;
                }

                // 1. 年份筛选
                boolean yearMatch = (selectedYears == null || selectedYears.isEmpty())
                        || selectedYears.contains(billYear);

                // 2. 月份筛选
                boolean monthMatch = (selectedMonths == null || selectedMonths.isEmpty())
                        || selectedMonths.contains(billMonth);

                if (yearMatch && monthMatch) {
                    filteredList.add(record);
                    totalAmount += record.getAmount();
                }
            } else {
                // 如果找不到对应的账单（极其罕见的情况），可以选择不显示或者默认显示
                // 这里选择不显示，因为无法判断其周期
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
        // 初始化统计类别
        typeMap.put("物业费", 0.0);
        typeMap.put("维修金", 0.0);
        typeMap.put("公摊水电", 0.0);
        typeMap.put("电梯费", 0.0);
        typeMap.put("其他", 0.0);

        for (PaymentRecord r : records) {
            boolean parsed = false;
            // 解析费用详情快照
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

                    // 计算是否有剩余金额归为其他
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
        pieChart.setCenterText("费用构成");

        // 图例设置
        Legend l = pieChart.getLegend();
        l.setVerticalAlignment(Legend.LegendVerticalAlignment.BOTTOM);
        l.setHorizontalAlignment(Legend.LegendHorizontalAlignment.CENTER);
        l.setOrientation(Legend.LegendOrientation.HORIZONTAL);
        l.setDrawInside(false);
        l.setWordWrapEnabled(true);

        pieChart.invalidate();
    }
}