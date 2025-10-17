package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ProgressBar;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.AdminReviewAdapter;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class AdminReviewsFragment extends Fragment {

    private static final String TAG = "AdminReviewsFragment";
    private RecyclerView rvReviews;
    private ProgressBar progressBar;
    private FirebaseFirestore db;
    private AdminReviewAdapter adapter;
    private List<Map<String, Object>> reviewList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reviews, container, false);

        rvReviews = view.findViewById(R.id.rvReviews);
        progressBar = view.findViewById(R.id.progressBar);

        db = FirebaseFirestore.getInstance();
        rvReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminReviewAdapter(getContext(), reviewList);
        rvReviews.setAdapter(adapter);

        loadReviews();

        return view;
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Chưa có đánh giá nào.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    Log.d(TAG, "Total reviews: " + querySnapshot.size());

                    List<Task<Map<String, Object>>> tasks = new ArrayList<>();

                    for (var doc : querySnapshot) {
                        Map<String, Object> review = doc.getData();
                        review.put("id", doc.getId());

                        String tourId = (String) review.get("tourId");
                        String userId = (String) review.get("userId");

                        Log.d(TAG, "Processing review - tourId: " + tourId + ", userId: " + userId);

                        // Kiểm tra null trước khi query
                        if (tourId == null || tourId.isEmpty()) {
                            Log.w(TAG, "Review " + doc.getId() + " has null/empty tourId");
                            review.put("tourName", "(Không có tourId)");
                        }

                        if (userId == null || userId.isEmpty()) {
                            Log.w(TAG, "Review " + doc.getId() + " has null/empty userId");
                            review.put("userName", "(Không có userId)");
                        }

                        // Tạo các tasks để fetch dữ liệu
                        Task<DocumentSnapshot> tourTask = (tourId != null && !tourId.isEmpty())
                                ? db.collection("tours").document(tourId).get()
                                : Tasks.forResult(null);

                        Task<DocumentSnapshot> userTask = (userId != null && !userId.isEmpty())
                                ? db.collection("users").document(userId).get()
                                : Tasks.forResult(null);

                        Task<Map<String, Object>> reviewTask = Tasks.whenAllSuccess(tourTask, userTask)
                                .continueWith(task -> {
                                    if (task.isSuccessful()) {
                                        List<Object> results = task.getResult();
                                        DocumentSnapshot tourDoc = (DocumentSnapshot) results.get(0);
                                        DocumentSnapshot userDoc = (DocumentSnapshot) results.get(1);

                                        // Lấy tên tour
                                        if (tourDoc != null && tourDoc.exists()) {
                                            String tourName = tourDoc.getString("title");
                                            Log.d(TAG, "Tour found - ID: " + tourId + ", Title: " + tourName);
                                            review.put("tourName", tourName != null ? tourName : "(Không có tên tour)");
                                        } else {
                                            Log.w(TAG, "Tour not found - ID: " + tourId);
                                            if (!review.containsKey("tourName")) {
                                                review.put("tourName", "(Tour không tồn tại)");
                                            }
                                        }

                                        // Lấy tên user
                                        if (userDoc != null && userDoc.exists()) {
                                            String firstname = userDoc.getString("firstname");
                                            String lastname = userDoc.getString("lastname");

                                            Log.d(TAG, "User found - ID: " + userId + ", Firstname: " + firstname + ", Lastname: " + lastname);

                                            String fullName = "";
                                            if (firstname != null && !firstname.isEmpty()) {
                                                fullName = firstname;
                                            }
                                            if (lastname != null && !lastname.isEmpty()) {
                                                fullName = fullName.isEmpty() ? lastname : fullName + " " + lastname;
                                            }

                                            if (fullName.isEmpty()) {
                                                fullName = "(Không có tên)";
                                            }

                                            review.put("userName", fullName);
                                        } else {
                                            Log.w(TAG, "User not found - ID: " + userId);
                                            if (!review.containsKey("userName")) {
                                                review.put("userName", "(User không tồn tại)");
                                            }
                                        }
                                    } else {
                                        Log.e(TAG, "Error fetching data", task.getException());
                                        if (!review.containsKey("tourName")) {
                                            review.put("tourName", "(Lỗi tải tour)");
                                        }
                                        if (!review.containsKey("userName")) {
                                            review.put("userName", "(Lỗi tải user)");
                                        }
                                    }
                                    return review;
                                });

                        tasks.add(reviewTask);
                    }

                    // Đợi tất cả tasks hoàn thành
                    Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
                        for (int i = 0; i < tasks.size(); i++) {
                            if (tasks.get(i).isSuccessful()) {
                                reviewList.add(tasks.get(i).getResult());
                            } else {
                                Log.e(TAG, "Task " + i + " failed", tasks.get(i).getException());
                            }
                        }

                        Log.d(TAG, "Final review list size: " + reviewList.size());
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    Log.e(TAG, "Error loading reviews", e);
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}