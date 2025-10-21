package com.example.finalproject.adapter.guide;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.Tour;
import com.firebase.ui.firestore.FirestoreRecyclerAdapter;
import com.firebase.ui.firestore.FirestoreRecyclerOptions;


public class ToursAdapter extends FirestoreRecyclerAdapter<Tour, ToursAdapter.TourVH> {

    public interface OnItemClick {
        void onClick(String tourId, Tour model);
    }

    private final OnItemClick onItemClick;

    public ToursAdapter(@NonNull FirestoreRecyclerOptions<Tour> options, OnItemClick onItemClick) {
        super(options);
        this.onItemClick = onItemClick;
    }

    @Override
    protected void onBindViewHolder(@NonNull TourVH holder, int position, @NonNull Tour model) {
        holder.tvDestination.setText(model.getDestination());
        holder.tvDuration.setText(model.getDuration());
        Long price = model.getPrice();
        holder.tvPrice.setText(price != null ? String.format("%,d ₫", price) : "—");
        holder.tvDesc.setText(model.getDescription());

        String cover = (model.getImages() != null && !model.getImages().isEmpty())
                ? model.getImages().get(0) : null;
        Glide.with(holder.itemView.getContext())
                .load(cover)
                .placeholder(R.mipmap.ic_launcher)
                .into(holder.imgCover);

        holder.itemView.setOnClickListener(v -> {
            String id = getSnapshots().getSnapshot(position).getId();
            if (onItemClick != null) onItemClick.onClick(id, model);
        });
    }

    @NonNull
    @Override
    public TourVH onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_tour, parent, false);
        return new TourVH(v);
    }

    static class TourVH extends RecyclerView.ViewHolder {
        ImageView imgCover;
        TextView tvDestination, tvDuration, tvPrice, tvDesc;
        public TourVH(@NonNull View itemView) {
            super(itemView);
            imgCover = itemView.findViewById(R.id.imgCover);
            tvDestination = itemView.findViewById(R.id.tvDestination);
            tvDuration   = itemView.findViewById(R.id.tvDuration);
            tvPrice      = itemView.findViewById(R.id.tvPrice);
            tvDesc       = itemView.findViewById(R.id.tvDesc);
        }
    }
}
