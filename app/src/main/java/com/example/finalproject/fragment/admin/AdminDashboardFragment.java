package com.example.finalproject.fragment.admin;

import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;

import com.example.finalproject.R;
import com.example.finalproject.activity.admin.AddPromotionAdminActivity;
import com.example.finalproject.activity.admin.AddTourAdminActivity;
import com.github.mikephil.charting.charts.BarChart;
import com.github.mikephil.charting.components.Description;
import com.github.mikephil.charting.data.BarData;
import com.github.mikephil.charting.data.BarDataSet;
import com.github.mikephil.charting.data.BarEntry;
import com.github.mikephil.charting.formatter.ValueFormatter;
import com.google.firebase.Timestamp;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.DateFormatSymbols;
import java.text.DecimalFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminDashboardFragment extends Fragment {
    // Các thành phần hiển thị
    private TextView tvTotalUsers, tvTotalBookings, tvTotalTours, tvTotalGuides, tvTotalReviews, tvTotalPromotions;
    private TextView tvTotalRevenue, tvTopTours;
    private BarChart barChart;
    private ProgressBar progressBar;
    private FirebaseFirestore db;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_dashboard, container, false);

        db = FirebaseFirestore.getInstance();

        // Gán các View
        tvTotalUsers = view.findViewById(R.id.tvTotalUsers);
        tvTotalBookings = view.findViewById(R.id.tvTotalBookings);
        tvTotalTours = view.findViewById(R.id.tvTotalTours);
        tvTotalGuides = view.findViewById(R.id.tvTotalGuides);
        tvTotalReviews = view.findViewById(R.id.tvTotalReviews);
        tvTotalPromotions = view.findViewById(R.id.tvTotalPromotions);
        tvTotalRevenue = view.findViewById(R.id.tvTotalRevenue);
        tvTopTours = view.findViewById(R.id.tvTopTours);
        barChart = view.findViewById(R.id.barChart);
        progressBar = view.findViewById(R.id.progressBar);

        // 🔹 Thao tác nhanh
        Button btnAddTour = view.findViewById(R.id.btnAddTour);
        Button btnAddPromotion = view.findViewById(R.id.btnAddPromotion);

        btnAddTour.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddTourAdminActivity.class);
            startActivity(intent);
        });

        btnAddPromotion.setOnClickListener(v -> {
            Intent intent = new Intent(getActivity(), AddPromotionAdminActivity.class);
            startActivity(intent);
        });

        loadDashboardStats();

        return view;
    }

    // ===========================================================
    // 🔄 Load tất cả dữ liệu Dashboard
    // ===========================================================
    private void loadDashboardStats() {
        progressBar.setVisibility(View.VISIBLE);

        loadBasicStats();
        loadTotalRevenue();
        loadTop5Tours();
        loadRevenueByMonth();
    }

    // ===========================================================
    // 📦 Thống kê số lượng các đối tượng cơ bản
    // ===========================================================
    private void loadBasicStats() {
        // Tổng user
        db.collection("users").get().addOnSuccessListener(snap ->
                tvTotalUsers.setText(String.valueOf(snap.size()))
        );

        // Tổng booking
        db.collection("bookings").get().addOnSuccessListener(snap ->
                tvTotalBookings.setText(String.valueOf(snap.size()))
        );

        // Tổng tour
        db.collection("tours").get().addOnSuccessListener(snap ->
                tvTotalTours.setText(String.valueOf(snap.size()))
        );

        // ✅ Tổng hướng dẫn viên (lấy từ users có role = "guide")
        db.collection("users")
                .whereEqualTo("role", "guide")
                .get()
                .addOnSuccessListener(snap ->
                        tvTotalGuides.setText(String.valueOf(snap.size()))
                )
                .addOnFailureListener(e ->
                        Log.e("GUIDE_COUNT", "Lỗi khi lấy hướng dẫn viên: " + e.getMessage())
                );

        // Tổng review
        db.collection("reviews").get().addOnSuccessListener(snap ->
                tvTotalReviews.setText(String.valueOf(snap.size()))
        );

        // Tổng promotion
        db.collection("promotions").get().addOnSuccessListener(snap ->
                tvTotalPromotions.setText(String.valueOf(snap.size()))
        );
    }

    // ===========================================================
    // 💰 Tổng doanh thu
    // ===========================================================
    private void loadTotalRevenue() {
        db.collection("payments")
                .whereEqualTo("status", "success")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    double total = 0;
                    for (DocumentSnapshot doc : querySnapshot) {
                        Object amountObj = doc.get("amount");

                        if (amountObj != null) {
                            try {
                                // Firestore có thể lưu amount là String hoặc Number
                                if (amountObj instanceof String) {
                                    String cleaned = ((String) amountObj).replaceAll("[^0-9.]", ""); // loại bỏ ký tự lạ
                                    if (!cleaned.isEmpty()) {
                                        total += Double.parseDouble(cleaned);
                                    }
                                } else if (amountObj instanceof Number) {
                                    total += ((Number) amountObj).doubleValue();
                                }
                            } catch (Exception e) {
                                Log.e("REVENUE_PARSE", "Lỗi parse amount: " + e.getMessage());
                            }
                        } else {
                            Log.w("REVENUE_NULL", "amount is null in " + doc.getId());
                        }
                    }

                    DecimalFormat formatter = new DecimalFormat("#,###");
                    tvTotalRevenue.setText(formatter.format(total) + " ₫");
                    progressBar.setVisibility(View.GONE);

                    Log.d("TOTAL_REVENUE", "Tổng doanh thu = " + total);
                })
                .addOnFailureListener(e -> {
                    Toast.makeText(getContext(), "Lỗi tải doanh thu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                    Log.e("TOTAL_REVENUE", "Firestore error", e);
                });
    }

    // ===========================================================
    // 🏆 Top 5 tour được đặt nhiều nhất
    // ===========================================================
    private void loadTop5Tours() {
        db.collection("bookings").get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<String, Integer> countMap = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        String tourId = doc.getString("tourId");
                        if (tourId != null) {
                            countMap.put(tourId, countMap.getOrDefault(tourId, 0) + 1);
                        }
                    }

                    List<Map.Entry<String, Integer>> sortedList = new ArrayList<>(countMap.entrySet());
                    sortedList.sort((a, b) -> b.getValue() - a.getValue());

                    StringBuilder topToursText = new StringBuilder();
                    int limit = Math.min(5, sortedList.size());

                    for (int i = 0; i < limit; i++) {
                        String tourId = sortedList.get(i).getKey();
                        int count = sortedList.get(i).getValue();

                        int finalI = i;
                        db.collection("tours").document(tourId).get()
                                .addOnSuccessListener(tourDoc -> {
                                    String tourName = tourDoc.getString("title");
                                    topToursText.append(finalI + 1)
                                            .append(". ")
                                            .append(tourName != null ? tourName : "(Không rõ)")
                                            .append(" — ")
                                            .append(count)
                                            .append(" lượt đặt\n");
                                    tvTopTours.setText(topToursText.toString());
                                });
                    }
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải top tours: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    // ===========================================================
    // 📈 Biểu đồ doanh thu theo tháng
    // ===========================================================
    private void loadRevenueByMonth() {
        db.collection("payments")
                .whereEqualTo("status", "success")
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    Map<Integer, Double> monthlyRevenue = new HashMap<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Timestamp timestamp = doc.getTimestamp("paymentTime");
                        if (timestamp != null) {
                            Calendar cal = Calendar.getInstance();
                            cal.setTime(timestamp.toDate());
                            int month = cal.get(Calendar.MONTH);
                            double amount = Double.parseDouble(doc.get("amount").toString());
                            monthlyRevenue.put(month, monthlyRevenue.getOrDefault(month, 0.0) + amount);
                        }
                    }

                    List<BarEntry> entries = new ArrayList<>();
                    for (int i = 0; i < 12; i++) {
                        entries.add(new BarEntry(i, monthlyRevenue.getOrDefault(i, 0.0).floatValue()));
                    }

                    BarDataSet dataSet = new BarDataSet(entries, "Doanh thu theo tháng");
                    dataSet.setColor(Color.parseColor("#4F46E5"));
                    dataSet.setValueTextColor(Color.BLACK);

                    BarData barData = new BarData(dataSet);
                    barData.setBarWidth(0.8f);

                    barChart.setData(barData);
                    Description desc = new Description();
                    desc.setText("");
                    barChart.setDescription(desc);
                    barChart.getXAxis().setGranularity(1f);
                    barChart.getXAxis().setValueFormatter(new ValueFormatter() {
                        @Override
                        public String getAxisLabel(float value, com.github.mikephil.charting.components.AxisBase axis) {
                            int monthIndex = (int) value;
                            if (monthIndex >= 0 && monthIndex < 12) {
                                return new DateFormatSymbols().getShortMonths()[monthIndex];
                            }
                            return "";
                        }
                    });

                    barChart.getAxisRight().setEnabled(false);
                    barChart.animateY(1000);
                    barChart.invalidate();
                })
                .addOnFailureListener(e ->
                        Toast.makeText(getContext(), "Lỗi tải biểu đồ: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}