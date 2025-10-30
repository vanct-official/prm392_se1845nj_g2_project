package com.example.finalproject.fragment.admin;

import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import androidx.annotation.NonNull;
import androidx.fragment.app.Fragment;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.admin.AdminPaymentAdapter;
import com.example.finalproject.entity.Payment;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class AdminPaymentsFragment extends Fragment {

    private RecyclerView recyclerAdminPayments;
    private AdminPaymentAdapter adapter;
    private List<Payment> paymentList = new ArrayList<>();
    private Map<String, String> userMap = new HashMap<>();
    private FirebaseFirestore db;

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_admin_payments, container, false);

        recyclerAdminPayments = view.findViewById(R.id.recyclerAdminPayments);
        recyclerAdminPayments.setLayoutManager(new LinearLayoutManager(getContext()));

        db = FirebaseFirestore.getInstance();

        // Gọi hàm load users trước, sau đó mới load payments
        loadUsersThenPayments();

        return view;
    }

    private void loadUsersThenPayments() {
        db.collection("users")
                .get()
                .addOnSuccessListener(query -> {
                    for (DocumentSnapshot doc : query) {
                        String fullName = doc.getString("firstname") + " " + doc.getString("lastname");
                        userMap.put(doc.getId(), fullName.trim());
                    }
                    loadPayments(); // Sau khi có userMap mới load payment
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }

    private void loadPayments() {
        db.collection("payments")
                .get()
                .addOnSuccessListener(queryDocumentSnapshots -> {
                    paymentList.clear();
                    for (QueryDocumentSnapshot doc : queryDocumentSnapshots) {
                        Payment payment = doc.toObject(Payment.class);
                        if (payment != null) {
                            payment.setId(doc.getId()); // ✅ Gán Firestore document ID
                            paymentList.add(payment);
                        }
                    }

                    // Khởi tạo adapter sau khi có cả payment và userMap
                    adapter = new AdminPaymentAdapter(requireContext(), paymentList, userMap);
                    recyclerAdminPayments.setAdapter(adapter);
                    adapter.notifyDataSetChanged();
                })
                .addOnFailureListener(e -> e.printStackTrace());
    }
}
