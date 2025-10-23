package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.text.Editable;
import android.text.TextWatcher;
import android.util.Log;
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
import com.example.finalproject.adapter.admin.AdminReviewAdapter;
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
    private RecyclerView recyclerViewReviews;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private FirebaseFirestore db;
    private AdminReviewAdapter adapter;
    private List<Map<String, Object>> reviewList = new ArrayList<>();
    private List<Map<String, Object>> originalList = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_reviews, container, false);

        recyclerViewReviews = view.findViewById(R.id.recyclerViewReviews);
        progressBar = view.findViewById(R.id.progressBar);
        edtSearch = view.findViewById(R.id.edtSearch);

        db = FirebaseFirestore.getInstance();
        recyclerViewReviews.setLayoutManager(new LinearLayoutManager(getContext()));

        adapter = new AdminReviewAdapter(getContext(), reviewList);
        recyclerViewReviews.setAdapter(adapter);

        loadReviews();

        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override
            public void beforeTextChanged(CharSequence s, int start, int count, int after) {}

            @Override
            public void onTextChanged(CharSequence s, int start, int before, int count) {
                filterReviews(s.toString());
            }

            @Override
            public void afterTextChanged(Editable s) {}
        });

        return view;
    }

    private void loadReviews() {
        progressBar.setVisibility(View.VISIBLE);

        db.collection("reviews")
                .orderBy("createdAt", Query.Direction.DESCENDING)
                .get()
                .addOnSuccessListener(querySnapshot -> {
                    reviewList.clear();
                    originalList.clear();

                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Chưa có đánh giá nào.", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Task<Map<String, Object>>> tasks = new ArrayList<>();

                    for (DocumentSnapshot doc : querySnapshot) {
                        Map<String, Object> review = doc.getData();
                        if (review == null) continue;
                        review.put("id", doc.getId());

                        String tourId = (String) review.get("tourId");
                        String userId = (String) review.get("userId");

                        Task<DocumentSnapshot> tourTask = (tourId != null && !tourId.isEmpty())
                                ? db.collection("tours").document(tourId).get()
                                : Tasks.forResult(null);

                        Task<DocumentSnapshot> userTask = (userId != null && !userId.isEmpty())
                                ? db.collection("users").document(userId).get()
                                : Tasks.forResult(null);

                        Task<Map<String, Object>> reviewTask = Tasks.whenAllSuccess(tourTask, userTask)
                                .continueWith(task -> {
                                    List<Object> results = task.getResult();
                                    DocumentSnapshot tourDoc = (DocumentSnapshot) results.get(0);
                                    DocumentSnapshot userDoc = (DocumentSnapshot) results.get(1);

                                    // ✅ Lấy thông tin tour
                                    if (tourDoc != null && tourDoc.exists()) {
                                        String tourName = tourDoc.getString("title");
                                        review.put("tourName", tourName != null ? tourName : "(Không có tên tour)");
                                    } else {
                                        review.put("tourName", "(Không có tên tour)");
                                    }

                                    // ✅ Lấy thông tin user (bao gồm avatarUrl)
                                    if (userDoc != null && userDoc.exists()) {
                                        String firstname = userDoc.getString("firstname");
                                        String lastname = userDoc.getString("lastname");
                                        String avatarUrl = userDoc.getString("avatarUrl"); // ✅ LẤY AVATAR

                                        String fullName = (firstname != null ? firstname : "") + " " + (lastname != null ? lastname : "");
                                        review.put("userName", fullName.trim().isEmpty() ? "(Không có tên user)" : fullName.trim());
                                        review.put("avatarUrl", avatarUrl != null ? avatarUrl : ""); // ✅ THÊM VÀO REVIEW

                                        // ✅ Debug log
                                        Log.d(TAG, "User: " + fullName + ", Avatar: " + avatarUrl);
                                    } else {
                                        review.put("userName", "(Không có tên user)");
                                        review.put("avatarUrl", ""); // ✅ THÊM TRƯỜNG RỖNG NẾU KHÔNG CÓ USER
                                    }

                                    return review;
                                });

                        tasks.add(reviewTask);
                    }

                    Tasks.whenAllComplete(tasks).addOnCompleteListener(task -> {
                        for (Task<Map<String, Object>> t : tasks) {
                            if (t.isSuccessful()) {
                                reviewList.add(t.getResult());
                            }
                        }
                        originalList.addAll(reviewList);
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);

                        // ✅ Log tổng số review
                        Log.d(TAG, "Loaded " + reviewList.size() + " reviews");
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Log.e(TAG, "Error loading reviews", e);
                    Toast.makeText(getContext(), "Lỗi tải review: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }

    private void filterReviews(String query) {
        if (originalList == null || originalList.isEmpty()) return;

        if (query.trim().isEmpty()) {
            adapter.filterList(new ArrayList<>(originalList));
            return;
        }

        List<Map<String, Object>> filteredList = new ArrayList<>();
        String lowerQuery = query.toLowerCase();

        for (Map<String, Object> review : originalList) {
            String comment = review.get("comment") != null ? review.get("comment").toString().toLowerCase() : "";
            String tourName = review.get("tourName") != null ? review.get("tourName").toString().toLowerCase() : "";
            String userName = review.get("userName") != null ? review.get("userName").toString().toLowerCase() : "";

            if (comment.contains(lowerQuery) || tourName.contains(lowerQuery) || userName.contains(lowerQuery)) {
                filteredList.add(review);
            }
        }

        adapter.filterList(filteredList);
    }
}