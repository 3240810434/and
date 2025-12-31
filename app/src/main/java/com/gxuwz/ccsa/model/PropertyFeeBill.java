package com.gxuwz.ccsa.model;

import androidx.room.Entity;
import androidx.room.PrimaryKey;
import java.util.Date;

@Entity(tableName = "property_fee_bill")
public class PropertyFeeBill {
    @PrimaryKey(autoGenerate = true)
    private long id;
    private String community;
    private String building;
    private String roomNumber;
    private String phone;
    private double totalAmount;
    private int status; // 0-未缴，1-已缴
    private String periodStart;
    private String periodEnd;
    private long standardId;
    private Date paymentTime;
    private long createTime;

    // 新增：账单所属年份和月份，用于筛选
    private String year;
    private String month;

    // 无参构造函数
    public PropertyFeeBill() {
    }

    // 更新后的构造函数，包含 year 和 month
    public PropertyFeeBill(String community, String building, String roomNumber, String phone,
                           double totalAmount, String periodStart, String periodEnd,
                           int status, long standardId, long createTime, String year, String month) {
        this.community = community;
        this.building = building;
        this.roomNumber = roomNumber;
        this.phone = phone;
        this.totalAmount = totalAmount;
        this.periodStart = periodStart;
        this.periodEnd = periodEnd;
        this.status = status;
        this.standardId = standardId;
        this.createTime = createTime;
        this.year = year;
        this.month = month;
    }

    // 所有getter和setter方法...
    public long getId() { return id; }
    public void setId(long id) { this.id = id; }

    public String getCommunity() { return community; }
    public void setCommunity(String community) { this.community = community; }

    public String getBuilding() { return building; }
    public void setBuilding(String building) { this.building = building; }

    public String getRoomNumber() { return roomNumber; }
    public void setRoomNumber(String roomNumber) { this.roomNumber = roomNumber; }

    public String getPhone() { return phone; }
    public void setPhone(String phone) { this.phone = phone; }

    public double getTotalAmount() { return totalAmount; }
    public void setTotalAmount(double totalAmount) { this.totalAmount = totalAmount; }

    public int getStatus() { return status; }
    public void setStatus(int status) { this.status = status; }

    public String getPeriodStart() { return periodStart; }
    public void setPeriodStart(String periodStart) { this.periodStart = periodStart; }

    public String getPeriodEnd() { return periodEnd; }
    public void setPeriodEnd(String periodEnd) { this.periodEnd = periodEnd; }

    public long getStandardId() { return standardId; }
    public void setStandardId(long standardId) { this.standardId = standardId; }

    public Date getPaymentTime() { return paymentTime; }
    public void setPaymentTime(Date paymentTime) { this.paymentTime = paymentTime; }

    public long getCreateTime() { return createTime; }
    public void setCreateTime(long createTime) { this.createTime = createTime; }

    // 新增的方法
    public String getYear() { return year; }
    public void setYear(String year) { this.year = year; }

    public String getMonth() { return month; }
    public void setMonth(String month) { this.month = month; }
}