package com.gxuwz.ccsa.ui.resident;

import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.adapter.ResidentOrderAdapter;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.Order;
import java.util.ArrayList;
import java.util.List;

public class ResidentOrdersActivity extends AppCompatActivity {
    private RecyclerView recyclerView;
    private ResidentOrderAdapter adapter;
    private AppDatabase db;
    private long userId;
    private TextView tvEmpty;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_resident_orders);

        // 初始化控件
        TextView tvTitle = findViewById(R.id.tv_title);
        TextView btnCompleted = findViewById(R.id.btn_completed_orders);
        TextView btnAfterSales = findViewById(R.id.btn_after_sales);
        ImageView btnBack = findViewById(R.id.btn_back);

        // 返回按钮逻辑（虽然布局里gone了，保留逻辑以防万一）
        if(btnBack != null) {
            btnBack.setOnClickListener(v -> finish());
        }

        // 跳转到已完成订单页面
        btnCompleted.setOnClickListener(v -> {
            Intent intent = new Intent(ResidentOrdersActivity.this, ResidentCompletedOrdersActivity.class);
            startActivity(intent);
        });

        // 跳转到售后页面
        btnAfterSales.setOnClickListener(v -> {
            Intent intent = new Intent(ResidentOrdersActivity.this, ResidentAfterSalesActivity.class);
            startActivity(intent);
        });

        userId = getSharedPreferences("user_prefs", MODE_PRIVATE).getLong("user_id", -1);
        db = AppDatabase.getInstance(this);

        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);

        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        adapter = new ResidentOrderAdapter(null);
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            List<Order> allOrders = db.orderDao().getOrdersByResident(String.valueOf(userId));
            List<Order> activeOrders = new ArrayList<>();

            // 过滤逻辑：只显示 待接单 和 配送中，排除 已完成
            if (allOrders != null) {
                for (Order order : allOrders) {
                    if (!"已完成".equals(order.status)) {
                        activeOrders.add(order);
                    }
                }
            }

            runOnUiThread(() -> {
                if (!activeOrders.isEmpty()) {
                    adapter.updateList(activeOrders);
                    recyclerView.setVisibility(View.VISIBLE);
                    if (tvEmpty != null) tvEmpty.setVisibility(View.GONE);
                } else {
                    recyclerView.setVisibility(View.GONE);
                    if (tvEmpty != null) {
                        tvEmpty.setText("暂无进行中的订单");
                        tvEmpty.setVisibility(View.VISIBLE);
                    }
                }
            });
        }).start();
    }
}