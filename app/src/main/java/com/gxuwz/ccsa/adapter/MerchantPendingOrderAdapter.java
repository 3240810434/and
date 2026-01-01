package com.gxuwz.ccsa.adapter;

import android.graphics.Color;
import android.graphics.drawable.GradientDrawable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.model.Order;
import java.util.List;

public class MerchantPendingOrderAdapter extends RecyclerView.Adapter<MerchantPendingOrderAdapter.ViewHolder> {
    private List<Order> list;
    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onAccept(Order order);
        // 新增取消回调
        void onCancel(Order order);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public MerchantPendingOrderAdapter(List<Order> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_merchant_pending_order, parent, false);
        return new ViewHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Order order = list.get(position);

        holder.tvOrderNo.setText("订单号: " + order.orderNo);
        holder.tvStatus.setText(order.status);
        holder.tvCreateTime.setText("下单: " + order.createTime);

        Glide.with(holder.itemView.getContext())
                .load(order.productImageUrl)
                .placeholder(R.drawable.ic_launcher_background)
                .into(holder.ivProductImg);

        holder.tvProductName.setText(order.productName);
        holder.tvTags.setText("标签: " + (order.tags == null ? "无" : order.tags));

        boolean isService = "服务".equals(order.productType) || "SERVICE".equalsIgnoreCase(order.productType);
        if (isService) {
            holder.llServiceInfo.setVisibility(View.VISIBLE);
            holder.llPhysicalInfo.setVisibility(View.GONE);
            holder.tvServicePriceUnit.setText("价格: ¥" + order.unitPrice + "/" + order.productUnit + " × " + order.serviceCount);
            holder.tvServiceType.setText("服务类型: 上门服务");
        } else {
            holder.llPhysicalInfo.setVisibility(View.VISIBLE);
            holder.llServiceInfo.setVisibility(View.GONE);
            holder.tvPhysicalSpec.setText("规格: " + order.selectedSpec);
            holder.tvPhysicalDelivery.setText("方式: " + (order.deliveryMethod == null ? "商家配送" : order.deliveryMethod));
        }

        holder.tvResidentInfo.setText(order.residentName + "  " + order.residentPhone);
        holder.tvAddress.setText("地址: " + order.address);
        holder.tvPayAmount.setText("¥" + order.payAmount);
        holder.tvPayMethod.setText(order.paymentMethod);

        // 接单按钮事件
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(order);
        });

        // 取消按钮事件 - 仅在待接单状态显示
        if ("待接单".equals(order.status)) {
            holder.btnCancel.setVisibility(View.VISIBLE);

            // 设置取消按钮颜色为 #C41E3A，并保持圆角
            GradientDrawable bg = new GradientDrawable();
            bg.setShape(GradientDrawable.RECTANGLE);
            bg.setColor(Color.parseColor("#C41E3A"));
            bg.setCornerRadius(15); // 设置适当的圆角
            holder.btnCancel.setBackground(bg);
            holder.btnCancel.setTextColor(Color.WHITE);

            holder.btnCancel.setOnClickListener(v -> {
                if (listener != null) listener.onCancel(order);
            });
        } else {
            holder.btnCancel.setVisibility(View.GONE);
        }
    }

    @Override
    public int getItemCount() {
        return list == null ? 0 : list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNo, tvStatus, tvCreateTime;
        ImageView ivProductImg;
        TextView tvProductName, tvTags;
        LinearLayout llPhysicalInfo, llServiceInfo;
        TextView tvPhysicalSpec, tvPhysicalDelivery, tvServicePriceUnit, tvServiceType;
        TextView tvResidentInfo, tvAddress, tvPayAmount, tvPayMethod;
        Button btnAccept, btnCancel;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvOrderNo = itemView.findViewById(R.id.tv_order_no);
            tvStatus = itemView.findViewById(R.id.tv_order_status);
            tvCreateTime = itemView.findViewById(R.id.tv_create_time);
            ivProductImg = itemView.findViewById(R.id.iv_product_img);
            tvProductName = itemView.findViewById(R.id.tv_product_name);
            tvTags = itemView.findViewById(R.id.tv_tags);
            llPhysicalInfo = itemView.findViewById(R.id.ll_physical_info);
            tvPhysicalSpec = itemView.findViewById(R.id.tv_physical_spec);
            tvPhysicalDelivery = itemView.findViewById(R.id.tv_physical_delivery);
            llServiceInfo = itemView.findViewById(R.id.ll_service_info);
            tvServicePriceUnit = itemView.findViewById(R.id.tv_service_price_unit);
            tvServiceType = itemView.findViewById(R.id.tv_service_type);
            tvResidentInfo = itemView.findViewById(R.id.tv_resident_info);
            tvAddress = itemView.findViewById(R.id.tv_address);
            tvPayAmount = itemView.findViewById(R.id.tv_pay_amount);
            tvPayMethod = itemView.findViewById(R.id.tv_pay_method);
            btnAccept = itemView.findViewById(R.id.btn_accept_order);
            btnCancel = itemView.findViewById(R.id.btn_cancel_order);
        }
    }
}