package com.gxuwz.ccsa.adapter;

import android.content.Context;
import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.View;
import androidx.annotation.NonNull;
import com.gxuwz.ccsa.model.Order;
import java.util.List;

public class MerchantCompletedOrderAdapter extends MerchantProcessingOrderAdapter {

    public MerchantCompletedOrderAdapter(Context context, List<Order> list) {
        super(context, list);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        super.onBindViewHolder(holder, position); // 先调用父类方法填充基本数据，同时父类会隐藏 btnCancel

        Order order = list.get(position);

        // 覆盖按钮样式和逻辑
        holder.btnAccept.setText("已完成");
        holder.btnAccept.setEnabled(false); // 不可点击

        // 设置绿色、稍微椭圆的背景
        GradientDrawable bg = new GradientDrawable();
        bg.setShape(GradientDrawable.RECTANGLE);
        bg.setColor(Color.parseColor("#4CAF50")); // 绿色
        bg.setCornerRadius(50); // 较大的圆角半径使其看起来像椭圆/胶囊
        holder.btnAccept.setBackground(bg);

        holder.btnAccept.setTextColor(Color.WHITE);

        // 移除点击事件
        holder.btnAccept.setOnClickListener(null);
    }
}