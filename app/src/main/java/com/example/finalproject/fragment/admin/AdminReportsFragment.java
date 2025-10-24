package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.admin.AdminReportAdapter;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Map;

public class AdminReportsFragment extends Fragment {

    private RecyclerView rvReports;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private FirebaseFirestore db;
    private AdminReportAdapter adapter;
    private final List<Map<String, Object>> reportList = new ArrayList<>();
    private final List<Map<String, Object>> filteredList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reports, container, false);

        rvReports = view.findViewById(R.id.rvReports);
        progressBar = view.findViewById(R.id.progressBar);
        edtSearch = view.findViewById(R.id.edtSearch);

        db = FirebaseFirestore.getInstance();

        rvReports.setLayoutManager(new LinearLayoutManager(getContext()));
        adapter = new AdminReportAdapter(getContext(), filteredList);
        rvReports.setAdapter(adapter);

        loadReports();

        // Tìm kiếm tour theo tên
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReports(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadReports() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("reports")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reportList.clear();
                    // Nếu không có report thì hide progress
                    if (querySnapshot.isEmpty()) {
                        filteredList.clear();
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                        return;
                    }

                    // Đếm số report đã xử lý để biết khi nào xong
                    final int total = querySnapshot.size();
                    final int[] doneCount = {0};

                    for (var doc : querySnapshot) {
                        Map<String, Object> report = doc.getData();
                        report.put("id", doc.getId());

                        String tourId = (String) report.get("tourId");

                        if (tourId != null && !tourId.isEmpty()) {
                            // Lấy thông tin tour
                            db.collection("tours").document(tourId).get().addOnSuccessListener(tourDoc -> {
                                if (tourDoc.exists()) {
                                    // LẤY TÊN TOUR TỪ tours (ghi đè nếu report có trường cũ)
                                    String tourName = tourDoc.getString("tourName");
                                    if (tourName != null) report.put("tourName", tourName);

                                    // LẤY MẢNG ẢNH TỪ tours
                                    List<String> images = (List<String>) tourDoc.get("images");
                                    report.put("tourImages", images != null ? images : new ArrayList<String>());

                                    // LẤY guideId từ tours (có thể tên trường là guideId hoặc guide)
                                    String guideId = null;
                                    if (tourDoc.contains("guideId")) guideId = tourDoc.getString("guideId");
                                    else if (tourDoc.contains("guide")) guideId = tourDoc.getString("guide");

                                    if (guideId != null && !guideId.isEmpty()) {
                                        // Lấy thông tin hướng dẫn viên từ users
                                        db.collection("users").document(guideId).get().addOnSuccessListener(guideDoc -> {
                                            if (guideDoc.exists()) {
                                                String guideFirst = guideDoc.getString("firstname");
                                                String guideLast = guideDoc.getString("lastname");
                                                String guideName = ((guideLast != null ? guideLast : "") + " " + (guideFirst != null ? guideFirst : "")).trim();
                                                String guideAvatar = guideDoc.getString("avatarUrl");

                                                report.put("guideName", guideName.isEmpty() ? guideDoc.getString("username") : guideName);
                                                report.put("guideAvatar", guideAvatar);
                                            }
                                            // thêm report sau khi đã có cả tour + guide (nếu có)
                                            synchronized (reportList) {
                                                reportList.add(report);
                                            }
                                            doneCount[0]++;
                                            if (doneCount[0] == total) {
                                                // khi xong tất cả
                                                filteredList.clear();
                                                filteredList.addAll(reportList);
                                                adapter.notifyDataSetChanged();
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        }).addOnFailureListener(e -> {
                                            // dù lỗi lấy guide, vẫn thêm report (có tour)
                                            synchronized (reportList) {
                                                reportList.add(report);
                                            }
                                            doneCount[0]++;
                                            if (doneCount[0] == total) {
                                                filteredList.clear();
                                                filteredList.addAll(reportList);
                                                adapter.notifyDataSetChanged();
                                                progressBar.setVisibility(View.GONE);
                                            }
                                        });
                                    } else {
                                        // nếu không có guideId thì thêm report luôn
                                        synchronized (reportList) {
                                            reportList.add(report);
                                        }
                                        doneCount[0]++;
                                        if (doneCount[0] == total) {
                                            filteredList.clear();
                                            filteredList.addAll(reportList);
                                            adapter.notifyDataSetChanged();
                                            progressBar.setVisibility(View.GONE);
                                        }
                                    }
                                } else {
                                    // tour doc không tồn tại, vẫn thêm report (không có tour info)
                                    synchronized (reportList) {
                                        reportList.add(report);
                                    }
                                    doneCount[0]++;
                                    if (doneCount[0] == total) {
                                        filteredList.clear();
                                        filteredList.addAll(reportList);
                                        adapter.notifyDataSetChanged();
                                        progressBar.setVisibility(View.GONE);
                                    }
                                }
                            }).addOnFailureListener(e -> {
                                // lỗi khi lấy tour -> thêm report dù thiếu dữ liệu
                                synchronized (reportList) {
                                    reportList.add(report);
                                }
                                doneCount[0]++;
                                if (doneCount[0] == total) {
                                    filteredList.clear();
                                    filteredList.addAll(reportList);
                                    adapter.notifyDataSetChanged();
                                    progressBar.setVisibility(View.GONE);
                                }
                            });
                        } else {
                            // không có tourId
                            synchronized (reportList) {
                                reportList.add(report);
                            }
                            doneCount[0]++;
                            if (doneCount[0] == total) {
                                filteredList.clear();
                                filteredList.addAll(reportList);
                                adapter.notifyDataSetChanged();
                                progressBar.setVisibility(View.GONE);
                            }
                        }
                    }
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải báo cáo: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterReports(String query) {
        filteredList.clear();
        if (query.isEmpty()) {
            filteredList.addAll(reportList);
        } else {
            for (Map<String, Object> report : reportList) {
                String tourName = (String) report.get("tourName");
                if (tourName != null && tourName.toLowerCase(Locale.getDefault()).contains(query.toLowerCase(Locale.getDefault()))) {
                    filteredList.add(report);
                }
            }
        }
        adapter.notifyDataSetChanged();
    }
}
