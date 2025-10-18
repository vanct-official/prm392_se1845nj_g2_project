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
import com.example.finalproject.adapter.AdminReviewAdapter;
import com.example.finalproject.entity.Review;
import com.google.android.gms.tasks.Task;
import com.google.android.gms.tasks.Tasks;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

import java.util.ArrayList;
import java.util.List;

public class AdminReviewsFragment extends Fragment {

    private RecyclerView recyclerViewReviews;
    private ProgressBar progressBar;
    private EditText edtSearch;
    private FirebaseFirestore db;
    private AdminReviewAdapter adapter;
    private List<Review> reviewList = new ArrayList<>();

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

        // tìm kiếm theo tên user hoặc tour
        edtSearch.addTextChangedListener(new TextWatcher() {
            @Override public void beforeTextChanged(CharSequence s, int start, int count, int after) {}
            @Override public void onTextChanged(CharSequence s, int start, int before, int count) {
                adapter.filter(s.toString());
            }
            @Override public void afterTextChanged(Editable s) {}
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
                    if (querySnapshot.isEmpty()) {
                        progressBar.setVisibility(View.GONE);
                        Toast.makeText(getContext(), "Chưa có đánh giá nào", Toast.LENGTH_SHORT).show();
                        return;
                    }

                    List<Task<Review>> tasks = new ArrayList<>();
                    for (DocumentSnapshot doc : querySnapshot) {
                        Review review = new Review();
                        review.setId(doc.getId());
                        review.setComment(doc.getString("comment"));
                        review.setUserId(doc.getString("userId"));
                        review.setTourId(doc.getString("tourId"));
                        review.setRating(doc.get("rating"));
                        review.setCreatedAt(doc.getTimestamp("createdAt"));

                        Task<DocumentSnapshot> tourTask = (review.getTourId() != null)
                                ? db.collection("tours").document(review.getTourId()).get()
                                : Tasks.forResult(null);

                        Task<DocumentSnapshot> userTask = (review.getUserId() != null)
                                ? db.collection("users").document(review.getUserId()).get()
                                : Tasks.forResult(null);

                        Task<Review> combined = Tasks.whenAllSuccess(tourTask, userTask)
                                .continueWith(task -> {
                                    List<Object> results = task.getResult();
                                    DocumentSnapshot tourDoc = (DocumentSnapshot) results.get(0);
                                    DocumentSnapshot userDoc = (DocumentSnapshot) results.get(1);

                                    if (tourDoc != null && tourDoc.exists()) {
                                        review.setTourName(tourDoc.getString("title"));
                                    } else {
                                        review.setTourName("(Tour không tồn tại)");
                                    }

                                    if (userDoc != null && userDoc.exists()) {
                                        String firstname = userDoc.getString("firstname");
                                        String lastname = userDoc.getString("lastname");
                                        String fullName = "";
                                        if (firstname != null) fullName += firstname;
                                        if (lastname != null)
                                            fullName = fullName.isEmpty() ? lastname : fullName + " " + lastname;

                                        review.setUserName(fullName.isEmpty() ? "(Không có tên)" : fullName);
                                        review.setUserAvatar(userDoc.getString("avatar"));
                                    } else {
                                        review.setUserName("(User không tồn tại)");
                                        review.setUserAvatar(null);
                                    }
                                    return review;
                                });

                        tasks.add(combined);
                    }

                    Tasks.whenAllComplete(tasks).addOnCompleteListener(t -> {
                        for (Task<Review> rTask : tasks) {
                            if (rTask.isSuccessful()) reviewList.add(rTask.getResult());
                        }
                        adapter.notifyDataSetChanged();
                        progressBar.setVisibility(View.GONE);
                    });
                })
                .addOnFailureListener(e -> {
                    progressBar.setVisibility(View.GONE);
                    Toast.makeText(getContext(), "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show();
                });
    }
}
