package com.gxuwz.ccsa.ui.resident;

import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.view.View;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;
import androidx.appcompat.app.AlertDialog; // 引入 AlertDialog
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

        // 【修复关键点】：这里不再传入 null，而是传入一个实现了 OnOrderCancelListener 的匿名内部类
        adapter = new ResidentOrderAdapter(null, new ResidentOrderAdapter.OnOrderCancelListener() {
            @Override
            public void onCancelOrder(Order order) {
                // 收到 Adapter 的取消点击回调，显示确认弹窗
                showCancelDialog(order);
            }
        });

        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        loadData();
    }

    /**
     * 显示取消确认弹窗
     */
    private void showCancelDialog(Order order) {
        new AlertDialog.Builder(this)
                .setTitle("提示")
                .setMessage("确定要取消该订单吗？")
                .setPositiveButton("确定", (dialog, which) -> {
                    // 用户点击确定，执行取消操作
                    cancelOrderInDb(order);
                })
                .setNegativeButton("再想想", null)
                .show();
    }

    /**
     * 在数据库中执行取消操作
     */
    private void cancelOrderInDb(Order order) {
        new Thread(() -> {
            // 1. 修改订单状态
            order.status = "已取消";

            // 2. 更新数据库
            db.orderDao().update(order);

            // 3. 回到主线程刷新 UI
            runOnUiThread(() -> {
                Toast.makeText(ResidentOrdersActivity.this, "订单已取消", Toast.LENGTH_SHORT).show();
                loadData(); // 重新加载数据，刷新列表显示
            });
        }).start();
    }

    private void loadData() {
        new Thread(() -> {
            List<Order> allOrders = db.orderDao().getOrdersByResident(String.valueOf(userId));
            List<Order> activeOrders = new ArrayList<>();

            // 过滤逻辑：只显示 待接单 和 配送中 (以及刚被取消的)，排除 已完成
            // 注意：因为 Adapter 中对 "已取消" 状态有灰色显示的逻辑，所以这里保留它在列表中，
            // 方便用户确认它确实变为了“已取消”。如果不希望显示已取消的，可以在这里加判断。
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