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

    // 处理接单逻辑（封装原逻辑）
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

    // 【新增】显示取消订单对话框
    private void showCancelDialog(Order order) {
        final EditText etReason = new EditText(this);
        etReason.setHint("请输入取消原因（必填）");

        // 设置一点内边距让输入框更好看
        int padding = (int) (16 * getResources().getDisplayMetrics().density);
        etReason.setPadding(padding, padding, padding, padding);

        new AlertDialog.Builder(this)
                .setTitle("取消订单")
                .setMessage("请填写取消订单的原因：")
                .setView(etReason)
                .setPositiveButton("确定", null) // 设置为null以覆盖默认关闭行为
                .setNegativeButton("返回", (dialog, which) -> dialog.dismiss())
                .create()
                .show()
                // 获取按钮并重写点击事件，实现输入校验
                .getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
                    String reason = etReason.getText().toString().trim();
                    if (TextUtils.isEmpty(reason)) {
                        Toast.makeText(PendingOrdersActivity.this, "必须填写取消原因！", Toast.LENGTH_SHORT).show();
                    } else {
                        // 执行取消逻辑
                        performCancelOrder(order, reason);
                        // 关闭对话框（需要手动维护引用或在此处无法直接关闭，简单起见重新获取Dialog对象或让Thread处理关闭不合适，
                        // 这里因为是在OnClickListener内部，无法直接调dialog.dismiss()除非它是final。
                        // 由于AlertDialog.Builder链式调用问题，建议将Dialog提取为变量，或者强转 v.getContext()。
                        // 最简单的修复：使用Dialog接口
                    }
                    // 注意：这里需要正确关闭Dialog。
                    // 修正写法：
                });
    }

    // 由于lambda作用域问题，上面代码无法直接dismiss dialog，这里重写一个完整的方法
    private void showCancelDialogFixed(Order order) {
        final EditText etReason = new EditText(this);
        etReason.setHint("请输入取消原因（必填）");
        // ... (padding设置同上)

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("确认取消订单？")
                .setView(etReason)
                .setPositiveButton("确定", null)
                .setNegativeButton("返回", (d, w) -> d.dismiss())
                .create();

        dialog.show();

        dialog.getButton(AlertDialog.BUTTON_POSITIVE).setOnClickListener(v -> {
            String reason = etReason.getText().toString().trim();
            if (TextUtils.isEmpty(reason)) {
                Toast.makeText(PendingOrdersActivity.this, "必须填写取消原因！", Toast.LENGTH_SHORT).show();
                return;
            }
            dialog.dismiss();
            performCancelOrder(order, reason);
        });
    }

    // 实际上我们在 loadData 里调用的是 showCancelDialog，所以替换为这个实现：
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
                loadData(); // 刷新列表，订单将消失（因为loadData只查‘待接单’）
            });
        }).start();
    }

    // 重写 showCancelDialog 以匹配 adapter 调用
    private void showCancelDialog(Order order) {
        showCancelDialogFixed(order);
    }
}