package com.gxuwz.ccsa.ui.merchant;

import android.os.Bundle;
import android.text.TextUtils;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.adapter.MerchantPendingOrderAdapter;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.Notification;
import com.gxuwz.ccsa.model.Order;

import java.util.Date;
import java.util.List;

public class PendingOrdersActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private AppDatabase db;
    private int merchantId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_pending_orders);

        merchantId = getSharedPreferences("merchant_prefs", MODE_PRIVATE).getInt("merchant_id", -1);

        if (merchantId == -1) {
            Toast.makeText(this, "登录状态异常，请重新登录", Toast.LENGTH_SHORT).show();
            finish();
            return;
        }

        db = AppDatabase.getInstance(this);

        recyclerView = findViewById(R.id.recycler_view);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));

        findViewById(R.id.btn_back).setOnClickListener(v -> finish());

        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            // 查询待接单列表
            List<Order> orders = db.orderDao().getPendingOrdersByMerchant(String.valueOf(merchantId));

            runOnUiThread(() -> {
                MerchantPendingOrderAdapter adapter = new MerchantPendingOrderAdapter(orders);

                adapter.setOnOrderActionListener(new MerchantPendingOrderAdapter.OnOrderActionListener() {
                    @Override
                    public void onAccept(Order order) {
                        handleAcceptOrder(order);
                    }

                    @Override
                    public void onCancel(Order order) {
                        showCancelDialog(order);
                    }
                });

                recyclerView.setAdapter(adapter);
            });
        }).start();
    }

    private void handleAcceptOrder(Order order) {
        new Thread(() -> {
            order.status = "配送中";
            db.orderDao().update(order);
            runOnUiThread(() -> {
                Toast.makeText(PendingOrdersActivity.this, "已接单，开始配送", Toast.LENGTH_SHORT).show();
                loadData();
            });
        }).start();
    }

    // 显示取消订单对话框
    private void showCancelDialog(Order order) {
        final EditText etReason = new EditText(this);
        etReason.setHint("请输入取消原因（必填）");

        // 设置输入框边距
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etReason.setPadding(padding, padding, padding, padding);

        // 创建Dialog对象，这里不要直接链式调用show()，否则无法获取Button
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("取消订单")
                .setMessage("请填写取消订单的原因：")
                .setView(etReason)
                // 设置PositiveButton为null，稍后重新定义OnClickListener以阻止默认的自动关闭行为
                .setPositiveButton("确定", null)
                .setNegativeButton("返回", (d, which) -> d.dismiss())
                .create();

        dialog.show();

        // 获取确定按钮并设置点击事件，进行非空校验
        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (TextUtils.isEmpty(reason)) {
                Toast.makeText(PendingOrdersActivity.this, "必须填写取消原因！", Toast.LENGTH_SHORT).show();
            } else {
                dialog.dismiss();
                performCancelOrder(order, reason);
            }
        });
    }

    // 执行取消逻辑：更新订单状态 + 发送通知
    private void performCancelOrder(Order order, String reason) {
        new Thread(() -> {
            // 1. 更新订单状态
            order.status = "已取消";
            db.orderDao().update(order);

            // 2. 创建发送给居民的通知
            Notification notification = new Notification();
            notification.setRecipientPhone(order.residentPhone);
            notification.setTitle("订单取消通知");
            notification.setContent("您的订单【" + order.productName + "】已被商家取消。原因：" + reason);
            notification.setType(1); // 1-系统/订单类通知
            notification.setCreateTime(new Date());
            notification.setRead(false);
            notification.setPublisher("商家通知");
            notification.setCommunity(order.address); // 使用订单地址作为社区标识

            db.notificationDao().insert(notification);

            runOnUiThread(() -> {
                Toast.makeText(PendingOrdersActivity.this, "订单已取消", Toast.LENGTH_SHORT).show();
                loadData(); // 刷新列表，该订单将从待接单列表中消失
            });
        }).start();
    }
}