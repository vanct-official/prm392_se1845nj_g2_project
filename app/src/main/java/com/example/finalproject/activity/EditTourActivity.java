package com.example.finalproject.activity;

import android.app.DatePickerDialog;
import android.os.Bundle;
import android.widget.Button;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.appcompat.app.AppCompatActivity;

import com.example.finalproject.R;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.FirebaseFirestore;

import java.util.Calendar;
import java.util.HashMap;
import java.util.Map;

public class EditTourActivity extends AppCompatActivity {

    private EditText etDescription, etLocation, etSeats, etPrice, etDeposit, etStartDate, etEndDate, etGuideId;
    private Button btnSave;
    private FirebaseFirestore db;
    private String tourId;

    @Override
    protected void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_edit_tour);

        db = FirebaseFirestore.getInstance();
        tourId = getIntent().getStringExtra("tourId");

        etDescription = findViewById(R.id.etDescription);
        etLocation = findViewById(R.id.etLocation);
        etSeats = findViewById(R.id.etSeats);
        etPrice = findViewById(R.id.etPrice);
        etDeposit = findViewById(R.id.etDeposit);
        etStartDate = findViewById(R.id.etStartDate);
        etEndDate = findViewById(R.id.etEndDate);
        etGuideId = findViewById(R.id.etGuideId);
        btnSave = findViewById(R.id.btnSave);

        loadTourData();

        // Chọn ngày
        etStartDate.setOnClickListener(v -> showDatePicker(etStartDate));
        etEndDate.setOnClickListener(v -> showDatePicker(etEndDate));

        btnSave.setOnClickListener(v -> saveChanges());
    }

    private void showDatePicker(EditText target) {
        Calendar calendar = Calendar.getInstance();
        DatePickerDialog dialog = new DatePickerDialog(this,
                (view, year, month, dayOfMonth) ->
                        target.setText(dayOfMonth + "/" + (month + 1) + "/" + year),
                calendar.get(Calendar.YEAR),
                calendar.get(Calendar.MONTH),
                calendar.get(Calendar.DAY_OF_MONTH));
        dialog.show();
    }

    private void loadTourData() {
        DocumentReference docRef = db.collection("tours").document(tourId);
        docRef.get().addOnSuccessListener(doc -> {
            if (doc.exists()) {
                etDescription.setText(doc.getString("description"));
                etLocation.setText(doc.getString("location"));
                etSeats.setText(String.valueOf(doc.getLong("availableSeats")));
                etPrice.setText(String.valueOf(doc.getDouble("price")));
                etDeposit.setText(String.valueOf(doc.getDouble("deposit")));
                etStartDate.setText(doc.getString("startDate"));
                etEndDate.setText(doc.getString("endDate"));
                etGuideId.setText(((java.util.List<String>) doc.get("guideIds")).get(0));
            }
        }).addOnFailureListener(e ->
                Toast.makeText(this, "Lỗi tải dữ liệu: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }

    private void saveChanges() {
        Map<String, Object> updates = new HashMap<>();
        updates.put("description", etDescription.getText().toString());
        updates.put("location", etLocation.getText().toString());
        updates.put("availableSeats", Integer.parseInt(etSeats.getText().toString()));
        updates.put("price", Double.parseDouble(etPrice.getText().toString()));
        updates.put("deposit", Double.parseDouble(etDeposit.getText().toString()));
        updates.put("startDate", etStartDate.getText().toString());
        updates.put("endDate", etEndDate.getText().toString());
        updates.put("guideIds", java.util.Collections.singletonList(etGuideId.getText().toString()));

        db.collection("tours").document(tourId)
                .update(updates)
                .addOnSuccessListener(aVoid ->
                        Toast.makeText(this, "Cập nhật thành công!", Toast.LENGTH_SHORT).show())
                .addOnFailureListener(e ->
                        Toast.makeText(this, "Lỗi: " + e.getMessage(), Toast.LENGTH_SHORT).show());
    }
}
