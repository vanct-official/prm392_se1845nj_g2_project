package com.example.finalproject.activity.customer;

import android.os.Bundle;
import android.widget.Toast;
import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.example.finalproject.R;
import com.example.finalproject.adapter.PaymentAdapter;
import com.example.finalproject.entity.Payment;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.QueryDocumentSnapshot;

import java.util.ArrayList;
import java.util.List;

public class PaymentHistoryActivity extends AppCompatActivity {
    private RecyclerView recyclerPayments;
    private PaymentAdapter adapter;
    private List<Payment> paymentList = new ArrayList<>();
    private FirebaseFirestore db;
    private String currentUserId;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_payment_history);

        recyclerPayments = findViewById(R.id.recyclerPayments);
        recyclerPayments.setLayoutManager(new LinearLayoutManager(this));
        adapter = new PaymentAdapter(paymentList);
        recyclerPayments.setAdapter(adapter);

        db = FirebaseFirestore.getInstance();
        currentUserId = FirebaseAuth.getInstance().getCurrentUser().getUid();

        loadPaymentHistory();
    }

    private void loadPaymentHistory() {
        db.collection("payments")
                .whereEqualTo("userId", currentUserId)
                .get()
                .addOnCompleteListener(task -> {
                    if (task.isSuccessful()) {
                        paymentList.clear();
                        for (QueryDocumentSnapshot doc : task.getResult()) {
                            Payment payment = doc.toObject(Payment.class);
                            payment.setId(doc.getId()); // 🔥 Gán document ID từ Firestore vào đối tượng Payment
                            paymentList.add(payment);
                        }
                        adapter.notifyDataSetChanged();
                    } else {
                        Toast.makeText(this, "Error loading payments", Toast.LENGTH_SHORT).show();
                    }
                });
    }
}
