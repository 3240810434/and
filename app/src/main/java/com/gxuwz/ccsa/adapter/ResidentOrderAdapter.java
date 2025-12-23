package com.gxuwz.ccsa.adapter;

import android.content.Context;
import android.content.Intent;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.model.Order;
import com.gxuwz.ccsa.ui.resident.ResidentApplyAfterSalesActivity;
import com.gxuwz.ccsa.ui.resident.PublishReviewActivity;

import java.util.List;

public class ResidentOrderAdapter extends RecyclerView.Adapter<ResidentOrderAdapter.OrderViewHolder> {

    private List<Order> orderList;
    private OnOrderCancelListener cancelListener;

    // 定义回调接口
    public interface OnOrderCancelListener {
        void onCancelOrder(Order order);
    }

    // 构造函数增加 Listener 参数
    public ResidentOrderAdapter(List<Order> orderList, OnOrderCancelListener listener) {
        this.orderList = orderList;
        this.cancelListener = listener;
    }

    public void updateList(List<Order> newList) {
        this.orderList = newList;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public OrderViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_resident_order_card, parent, false);
        return new OrderViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull OrderViewHolder holder, int position) {
        Order order = orderList.get(position);
        Context context = holder.itemView.getContext();

        holder.tvMerchantName.setText(order.merchantName != null ? order.merchantName : "未知商家");
        holder.tvStatus.setText(order.status);

        // 重置按钮可见性
        holder.btnReview.setVisibility(View.GONE);
        holder.btnApply.setVisibility(View.GONE);
        holder.btnCancel.setVisibility(View.GONE);
        holder.layoutActionButtons.setVisibility(View.VISIBLE); // 默认显示容器，下面根据情况隐藏

        // 状态逻辑处理
        if ("待接单".equals(order.status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#FF9800")); // Orange
            // 显示取消按钮
            holder.btnCancel.setVisibility(View.VISIBLE);
            holder.btnCancel.setOnClickListener(v -> {
                if (cancelListener != null) {
                    cancelListener.onCancelOrder(order);
                }
            });

        } else if ("已完成".equals(order.status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#4CAF50")); // Green
            // 已完成订单显示售后和评价逻辑（保持原有逻辑）

            // 售后按钮逻辑
            holder.btnApply.setVisibility(View.VISIBLE);
            switch (order.afterSalesStatus) {
                case 0: // 无售后 -> 显示“申请售后”
                    holder.btnApply.setText("申请售后");
                    holder.btnApply.setTextColor(Color.parseColor("#666666"));
                    holder.btnApply.setOnClickListener(v -> {
                        Intent intent = new Intent(context, ResidentApplyAfterSalesActivity.class);
                        intent.putExtra("orderId", order.id);
                        context.startActivity(intent);
                    });
                    break;
                case 1: // 待处理
                    holder.btnApply.setText("售后待处理");
                    holder.btnApply.setTextColor(Color.parseColor("#FF9800"));
                    holder.btnApply.setOnClickListener(v -> Toast.makeText(context, "您的申请正在等待商家处理", Toast.LENGTH_SHORT).show());
                    break;
                case 2: // 协商中
                    holder.btnApply.setText("售后协商中");
                    holder.btnApply.setTextColor(Color.RED);
                    holder.btnApply.setOnClickListener(v -> Toast.makeText(context, "请查看消息并与商家沟通", Toast.LENGTH_SHORT).show());
                    break;
                case 3: // 成功
                    holder.btnApply.setText("售后成功");
                    holder.btnApply.setTextColor(Color.parseColor("#4CAF50"));
                    holder.btnApply.setOnClickListener(null);
                    break;
                case 4: // 关闭
                    holder.btnApply.setText("售后已关闭");
                    holder.btnApply.setTextColor(Color.GRAY);
                    holder.btnApply.setOnClickListener(null);
                    break;
            }

            // 评价按钮逻辑
            holder.btnReview.setVisibility(View.VISIBLE);
            if (order.isReviewed == 1) {
                holder.btnReview.setText("已评价");
                holder.btnReview.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#FFD700")));
                holder.btnReview.setEnabled(false);
            } else {
                holder.btnReview.setText("评价");
                holder.btnReview.setBackgroundTintList(ColorStateList.valueOf(Color.parseColor("#32CD32")));
                holder.btnReview.setEnabled(true);
                holder.btnReview.setOnClickListener(v -> {
                    Intent intent = new Intent(context, PublishReviewActivity.class);
                    try {
                        int pId = Integer.parseInt(order.productId);
                        intent.putExtra("product_id", pId);
                        intent.putExtra("order_id", order.id);
                        context.startActivity(intent);
                    } catch (NumberFormatException e) {
                        e.printStackTrace();
                        Toast.makeText(context, "数据异常", Toast.LENGTH_SHORT).show();
                    }
                });
            }

        } else if ("配送中".equals(order.status)) {
            holder.tvStatus.setTextColor(Color.parseColor("#2196F3")); // Blue
            // 配送中一般不给取消，也不给评价
            holder.layoutActionButtons.setVisibility(View.GONE);

        } else {
            // 其他状态 (如 已取消)
            holder.tvStatus.setTextColor(Color.GRAY);
            holder.layoutActionButtons.setVisibility(View.GONE);
        }

        // 基础数据绑定
        holder.tvProductName.setText(order.productName);
        if ("服务".equals(order.productType) || "SERVICE".equals(order.productType)) {
            holder.tvSpecInfo.setText("服务数量：" + order.serviceCount);
        } else {
            holder.tvSpecInfo.setText("规格：" + (order.selectedSpec != null ? order.selectedSpec : "默认"));
        }
        holder.tvPayAmount.setText("¥ " + order.payAmount);
        holder.tvPayMethod.setText(order.paymentMethod != null ? order.paymentMethod : "在线支付");
        holder.tvCreateTime.setText("下单时间：" + order.createTime);
        holder.tvOrderNo.setText("订单号：" + order.orderNo);

        if (order.productImageUrl != null) {
            Glide.with(context)
                    .load(order.productImageUrl)
                    .placeholder(R.drawable.ic_launcher_background)
                    .into(holder.ivProductImg);
        }
    }

    @Override
    public int getItemCount() {
        return orderList == null ? 0 : orderList.size();
    }

    static class OrderViewHolder extends RecyclerView.ViewHolder {
        TextView tvMerchantName, tvStatus, tvProductName, tvSpecInfo;
        TextView tvPayAmount, tvPayMethod, tvCreateTime, tvOrderNo;
        ImageView ivProductImg;
        LinearLayout layoutActionButtons;
        TextView btnEvaluate, btnApply, btnReview, btnCancel;

        public OrderViewHolder(@NonNull View itemView) {
            super(itemView);
            tvMerchantName = itemView.findViewById(R.id.tv_merchant_name);
            tvStatus = itemView.findViewById(R.id.tv_status);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvSpecInfo = itemView.findViewById(R.id.tv_spec_info);
            tvPayAmount = itemView.findViewById(R.id.tv_pay_amount);
            tvPayMethod = itemView.findViewById(R.id.tv_pay_method);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            tvOrderNo = itemView.findViewById(R.id.tv_order_no);
            ivProductImg = itemView.findViewById(R.id.iv_product_img);
            layoutActionButtons = itemView.findViewById(R.id.layout_action_buttons);

            btnEvaluate = itemView.findViewById(R.id.btn_evaluate);
            btnApply = itemView.findViewById(R.id.btn_apply);
            btnReview = itemView.findViewById(R.id.btn_review);
            btnCancel = itemView.findViewById(R.id.btn_cancel); // 新增
        }
    }
}