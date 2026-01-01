package com.gxuwz.ccsa.ui.merchant;

import android.app.Dialog;
import android.content.Intent;
import android.content.SharedPreferences; // 必须导入
import android.os.Bundle;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.GridLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.db.AppDatabase;
import com.gxuwz.ccsa.model.Product;

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.List;

public class ProductManagementActivity extends AppCompatActivity {

    private RecyclerView recyclerView;
    private TextView tvEmpty;
    private MerchantProductAdapter adapter;
    private List<Product> productList = new ArrayList<>();

    // 修改1: 去掉硬编码 "= 1"，默认值设为 -1 表示未获取
    private int currentMerchantId = -1;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_product_management);

        ImageView btnBack = findViewById(R.id.btn_back);
        ImageView btnAdd = findViewById(R.id.btn_add);
        recyclerView = findViewById(R.id.recycler_view);
        tvEmpty = findViewById(R.id.tv_empty);

        btnBack.setOnClickListener(v -> finish());
        btnAdd.setOnClickListener(v -> showPublishTypeDialog());

        adapter = new MerchantProductAdapter();
        recyclerView.setLayoutManager(new GridLayoutManager(this, 2));
        recyclerView.setAdapter(adapter);
    }

    @Override
    protected void onResume() {
        super.onResume();
        // 修改2: 在每次页面显示时，动态获取当前登录的商家ID
        initMerchantData();
    }

    // 新增方法：获取商家ID并加载数据
    private void initMerchantData() {
        // 对应 MerchantLoginActivity 中的存储逻辑: getSharedPreferences("merchant_prefs", MODE_PRIVATE)
        SharedPreferences sp = getSharedPreferences("merchant_prefs", MODE_PRIVATE);
        currentMerchantId = sp.getInt("merchant_id", -1);

        if (currentMerchantId == -1) {
            // 如果获取失败（比如未登录），提示并关闭页面或显示空
            Toast.makeText(this, "获取商家信息失败，请重新登录", Toast.LENGTH_SHORT).show();
            finish(); // 或者跳转回登录页
            return;
        }

        // ID获取成功后，加载该ID对应的数据
        loadData();
    }

    private void loadData() {
        new Thread(() -> {
            // 使用动态获取的 currentMerchantId 查询数据库
            productList = AppDatabase.getInstance(this).productDao().getProductsByMerchantId(currentMerchantId);
            runOnUiThread(() -> {
                if (productList == null || productList.isEmpty()) {
                    recyclerView.setVisibility(View.GONE);
                    tvEmpty.setVisibility(View.VISIBLE);
                } else {
                    recyclerView.setVisibility(View.VISIBLE);
                    tvEmpty.setVisibility(View.GONE);
                    adapter.notifyDataSetChanged();
                }
            });
        }).start();
    }

    private void showPublishTypeDialog() {
        Dialog dialog = new Dialog(this, R.style.BottomDialogTheme);
        View view = LayoutInflater.from(this).inflate(R.layout.dialog_publish_type_selection, null);

        view.findViewById(R.id.btn_close).setOnClickListener(v -> dialog.dismiss());
        view.findViewById(R.id.card_goods).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, PhysicalProductEditActivity.class));
        });
        view.findViewById(R.id.card_service).setOnClickListener(v -> {
            dialog.dismiss();
            startActivity(new Intent(this, ServiceEditActivity.class));
        });

        dialog.setContentView(view);
        WindowManager.LayoutParams lp = dialog.getWindow().getAttributes();
        lp.gravity = Gravity.BOTTOM;
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = (int) (getResources().getDisplayMetrics().heightPixels * 0.4);
        dialog.getWindow().setAttributes(lp);
        dialog.show();
    }

    class MerchantProductAdapter extends RecyclerView.Adapter<MerchantProductAdapter.ViewHolder> {

        @NonNull
        @Override
        public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_product_card_merchant, parent, false);
            return new ViewHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
            Product product = productList.get(position);
            holder.tvName.setText(product.name);

            // 1. 设置封面
            if (product.imagePaths != null && !product.imagePaths.isEmpty()) {
                String firstImage = product.imagePaths.split(",")[0];
                Glide.with(ProductManagementActivity.this).load(firstImage).into(holder.ivCover);
            } else {
                holder.ivCover.setImageResource(R.drawable.shopping);
            }

            // 2. 设置价格
            try {
                if ("SERVICE".equals(product.type) && product.priceTableJson != null) {
                    JSONArray jsonArray = new JSONArray(product.priceTableJson);
                    if (jsonArray.length() > 0) {
                        JSONObject firstRow = jsonArray.getJSONObject(0);
                        String price = firstRow.optString("price");
                        String unit = firstRow.optString("unit");
                        holder.tvPrice.setText(price + "元/" + unit);
                    } else {
                        holder.tvPrice.setText("¥" + product.price);
                    }
                } else {
                    holder.tvPrice.setText("¥" + product.price);
                }
            } catch (Exception e) {
                holder.tvPrice.setText("¥" + product.price);
            }

            holder.itemView.setOnClickListener(v -> {
                Intent intent = new Intent(ProductManagementActivity.this, MerchantProductDetailActivity.class);
                intent.putExtra("product_id", product.id);
                startActivity(intent);
            });
        }

        @Override
        public int getItemCount() {
            return productList.size();
        }

        class ViewHolder extends RecyclerView.ViewHolder {
            ImageView ivCover;
            TextView tvName, tvPrice;

            public ViewHolder(@NonNull View itemView) {
                super(itemView);
                ivCover = itemView.findViewById(R.id.iv_product_cover);
                tvName = itemView.findViewById(R.id.tv_product_name);
                tvPrice = itemView.findViewById(R.id.tv_product_price);
            }
        }
    }
}