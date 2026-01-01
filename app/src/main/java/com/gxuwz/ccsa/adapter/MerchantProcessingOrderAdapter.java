package com.gxuwz.ccsa.adapter;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;
import com.bumptech.glide.Glide;
import com.gxuwz.ccsa.R;
import com.gxuwz.ccsa.model.Order;
import java.util.List;

public class MerchantProcessingOrderAdapter extends RecyclerView.Adapter<MerchantProcessingOrderAdapter.ViewHolder> {

    protected List<Order> list;
    protected Context context;

    private OnOrderActionListener listener;

    public interface OnOrderActionListener {
        void onFinishOrder(Order order);
    }

    public void setOnOrderActionListener(OnOrderActionListener listener) {
        this.listener = listener;
    }

    public MerchantProcessingOrderAdapter(Context context, List<Order> list) {
        this.context = context;
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

        Glide.with(context).load(order.productImageUrl).placeholder(R.drawable.ic_launcher_background).into(holder.ivProductImg);
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

        // 关键修改：隐藏取消按钮
        if (holder.btnCancel != null) {
            holder.btnCancel.setVisibility(View.GONE);
        }

        holder.btnAccept.setText("标记完成");
        holder.btnAccept.setOnClickListener(v -> {
            new AlertDialog.Builder(context)
                    .setTitle("确认完成")
                    .setMessage("确定该订单已送达或服务已完成吗？")
                    .setPositiveButton("确定", (dialog, which) -> {
                        if (listener != null) listener.onFinishOrder(order);
                    })
                    .setNegativeButton("取消", null)
                    .show();
        });
    }

    @Override
    public int getItemCount() { return list == null ? 0 : list.size(); }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvOrderNo, tvStatus, tvCreateTime, tvProductName, tvTags;
        ImageView ivProductImg;
        LinearLayout llPhysicalInfo, llServiceInfo;
        TextView tvPhysicalSpec, tvPhysicalDelivery, tvServicePriceUnit, tvServiceType;
        TextView tvResidentInfo, tvAddress, tvPayAmount, tvPayMethod;
        public Button btnAccept;
        public Button btnCancel; // 确保包含这个按钮

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
            btnCancel = itemView.findViewById(R.id.btn_cancel_order); // 绑定ID以便隐藏
        }
    }
}