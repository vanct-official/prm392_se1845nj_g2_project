package com.example.finalproject.adapter.guide;

import android.content.Context;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;
import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;
import com.example.finalproject.R;
import com.example.finalproject.entity.Tour;
import java.util.List;

public class GuideTourAdapter extends RecyclerView.Adapter<GuideTourAdapter.TourViewHolder> {

    private Context context;
    private List<Tour> tourList;

    public GuideTourAdapter(Context context, List<Tour> tourList) {
        this.context = context;
        this.tourList = tourList;
    }

    @NonNull
    @Override
    public TourViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(context).inflate(R.layout.item_tour_small, parent, false);
        return new TourViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull TourViewHolder holder, int position) {
        Tour tour = tourList.get(position);
        holder.tvTourName.setText(tour.getTourName());
        holder.tvStartDate.setText("Bắt đầu: " + tour.getStartDate());
        holder.tvLocation.setText("Địa điểm: " + tour.getLocation());
    }

    @Override
    public int getItemCount() {
        return tourList != null ? tourList.size() : 0;
    }

    static class TourViewHolder extends RecyclerView.ViewHolder {
        TextView tvTourName, tvStartDate, tvLocation;

        public TourViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTourName = itemView.findViewById(R.id.tvTourName);
            tvStartDate = itemView.findViewById(R.id.tvStartDate);
            tvLocation = itemView.findViewById(R.id.tvLocation);
        }
    }
}
