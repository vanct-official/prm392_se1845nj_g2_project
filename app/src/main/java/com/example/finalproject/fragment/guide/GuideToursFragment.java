package com.example.finalproject.fragment.guide;

import android.content.Intent;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageButton;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.activity.guide.GuideTourInvitationsActivity;
import com.example.finalproject.adapter.guide.ToursAdapter;
import com.example.finalproject.entity.Tour;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.Query;

public class GuideToursFragment extends Fragment {

    private ToursAdapter adapter;
    private TextView tvEmpty;
    private RecyclerView rv;

    @Nullable
    @Override
    public View onCreateView(@NonNull LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        return inflater.inflate(R.layout.fragment_guide_tours, container, false);
    }

    @Override
    public void onViewCreated(@NonNull View v, @Nullable Bundle savedInstanceState) {
        rv = v.findViewById(R.id.rvTours);
        tvEmpty = v.findViewById(R.id.tvEmpty);
        rv.setLayoutManager(new LinearLayoutManager(requireContext()));
        rv.setItemAnimator(null); // ⚡ tránh lỗi "Inconsistency detected"

        // 🔹 Nút xem danh sách lời mời
        ImageButton btnInvitations = v.findViewById(R.id.btnInvitations);
        btnInvitations.setOnClickListener(view -> {
            if (adapter != null) adapter.stopListening(); // tạm dừng lắng nghe trước khi chuyển màn
            Intent intent = new Intent(requireContext(), GuideTourInvitationsActivity.class);
            startActivity(intent);
        });

        String uid = FirebaseAuth.getInstance().getCurrentUser() != null
                ? FirebaseAuth.getInstance().getCurrentUser().getUid()
                : null;

        if (uid == null) {
            tvEmpty.setText("Bạn chưa đăng nhập.");
            tvEmpty.setVisibility(View.VISIBLE);
            return;
        }

        Query q = FirebaseFirestore.getInstance()
                .collection("tours")
                .whereArrayContains("guideIds", uid);

        FirestoreRecyclerOptions<Tour> options = new FirestoreRecyclerOptions.Builder<Tour>()
                .setQuery(q, snapshot -> {
                    Tour t = snapshot.toObject(Tour.class);
                    if (t != null) t.setId(snapshot.getId());
                    return t;
                })
                .build();

        adapter = new ToursAdapter(options, (id, model) -> {
            // TODO: mở TourDetailActivity nếu có
        });

        rv.setAdapter(adapter);

        adapter.registerAdapterDataObserver(new RecyclerView.AdapterDataObserver() {
            private void check() {
                boolean empty = adapter.getItemCount() == 0;
                tvEmpty.setVisibility(empty ? View.VISIBLE : View.GONE);
            }
            @Override public void onChanged() { check(); }
            @Override public void onItemRangeInserted(int positionStart, int itemCount) { check(); }
            @Override public void onItemRangeRemoved(int positionStart, int itemCount) { check(); }
        });
    }

    @Override
    public void onStart() {
        super.onStart();
        if (adapter != null) adapter.startListening();
    }

    @Override
    public void onResume() {
        super.onResume();
        if (adapter != null) adapter.startListening(); // đảm bảo cập nhật lại sau khi quay về
    }

    @Override
    public void onPause() {
        super.onPause();
        if (adapter != null) adapter.stopListening();
    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();
        if (adapter != null) {
            adapter.stopListening();
            adapter = null;
        }
    }
}
