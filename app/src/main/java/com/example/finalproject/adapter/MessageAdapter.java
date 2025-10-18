package com.example.finalproject.adapter;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.example.finalproject.R;
import com.example.finalproject.entity.Message;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class MessageAdapter extends RecyclerView.Adapter<MessageAdapter.MessageViewHolder> {
    private final List<Message> messageList;
    private final String currentUserId;

    public MessageAdapter(List<Message> messageList, String currentUserId) {
        this.messageList = messageList;
        this.currentUserId = currentUserId;
    }

    @Override
    public int getItemViewType(int position) {
        return messageList.get(position).getSenderId().equals(currentUserId) ? 1 : 0;
    }

    @NonNull
    @Override
    public MessageViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        int layout = (viewType == 1)
                ? R.layout.item_message_right
                : R.layout.item_message_left;
        View view = LayoutInflater.from(parent.getContext()).inflate(layout, parent, false);
        return new MessageViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull MessageViewHolder holder, int position) {
        Message msg = messageList.get(position);

        // Text message
        if (msg.getContent() != null && !msg.getContent().isEmpty()) {
            holder.txtMessage.setText(msg.getContent());
            holder.txtMessage.setVisibility(View.VISIBLE);
        } else {
            holder.txtMessage.setVisibility(View.GONE);
        }

        // Image message
        if (msg.getImageUrl() != null && !msg.getImageUrl().isEmpty()) {
            holder.imgMessage.setVisibility(View.VISIBLE);
            Glide.with(holder.itemView.getContext())
                    .load(msg.getImageUrl())
                    .placeholder(R.drawable.ic_image_placeholder)
                    .into(holder.imgMessage);
        } else {
            holder.imgMessage.setVisibility(View.GONE);
        }

        // Timestamp
        if (msg.getTimestamp() != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("HH:mm - dd/MM/yyyy", Locale.getDefault());
            holder.txtTime.setText(sdf.format(msg.getTimestamp().toDate()));
        }
    }

    @Override
    public int getItemCount() {
        return messageList.size();
    }

    public static class MessageViewHolder extends RecyclerView.ViewHolder {
        TextView txtMessage, txtTime;
        ImageView imgMessage;

        public MessageViewHolder(@NonNull View itemView) {
            super(itemView);
            txtMessage = itemView.findViewById(R.id.txtMessage);
            txtTime = itemView.findViewById(R.id.txtTime);
            imgMessage = itemView.findViewById(R.id.imgMessage);
        }
    }
}
