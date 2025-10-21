package com.example.finalproject.adapter.handlepicture;

import android.content.Context;
import android.net.Uri;
import android.view.ViewGroup;
import android.widget.ImageView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import java.util.List;

public class SelectedImagesAdapter extends RecyclerView.Adapter<SelectedImagesAdapter.ViewHolder> {

    private final Context context;
    private final List<Uri> images;
    private final OnRemoveClickListener listener;

    public interface OnRemoveClickListener {
        void onRemoveClick(int position);
    }

    public SelectedImagesAdapter(Context context, List<Uri> images, OnRemoveClickListener listener) {
        this.context = context;
        this.images = images;
        this.listener = listener;
    }

    @NonNull
    @Override
    public SelectedImagesAdapter.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        ImageView iv = new ImageView(context);
        RecyclerView.LayoutParams params = new RecyclerView.LayoutParams(100, 100);
        params.setMargins(8, 8, 8, 8);
        iv.setLayoutParams(params);
        iv.setScaleType(ImageView.ScaleType.CENTER_CROP);
        return new ViewHolder(iv);
    }

    @Override
    public void onBindViewHolder(@NonNull SelectedImagesAdapter.ViewHolder holder, int position) {
        Glide.with(context).load(images.get(position)).into(holder.imageView);
        holder.imageView.setOnLongClickListener(v -> {
            if (listener != null) listener.onRemoveClick(position);
            return true;
        });
    }

    @Override
    public int getItemCount() {
        return images.size();
    }

    static class ViewHolder extends RecyclerView.ViewHolder {
        ImageView imageView;
        ViewHolder(@NonNull ImageView itemView) {
            super(itemView);
            imageView = itemView;
        }
    }
}
