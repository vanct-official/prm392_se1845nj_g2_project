package com.example.finalproject.adapter.admin;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalproject.R;

import java.util.List;
import java.util.Map;

public class AdminInvitationsAdapter extends RecyclerView.Adapter<AdminInvitationsAdapter.ViewHolder> {

    private final List<Map<String, Object>> list;

    public AdminInvitationsAdapter(List<Map<String, Object>> list) {
        this.list = list;
    }

    @NonNull
    @Override
    public ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_admin_invitation, parent, false);
        return new ViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull ViewHolder holder, int position) {
        Map<String, Object> data = list.get(position);

        String guideName = (String) data.get("guideName");
        String tourName = (String) data.get("tourName");
        String status = (String) data.get("status");

        holder.tvGuideName.setText("Hướng dẫn viên: " + (guideName != null ? guideName : "Không rõ"));
        holder.tvTourName.setText("Tour: " + (tourName != null ? tourName : "Không rõ"));
        holder.tvStatus.setText("Trạng thái: " + (status != null ? status : "Chưa rõ"));
    }

    @Override
    public int getItemCount() {
        return list.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        TextView tvGuideName, tvTourName, tvStatus;

        public ViewHolder(@NonNull View itemView) {
            super(itemView);
            tvGuideName = itemView.findViewById(R.id.tvGuideName);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvStatus = itemView.findViewById(R.id.tvStatus);
        }
    }
}
