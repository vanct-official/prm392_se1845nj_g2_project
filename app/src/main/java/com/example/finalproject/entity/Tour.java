package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import java.util.List;

public class Tour {
    private String id;
    private String description;     // Mô tả hoặc tên tour
    private String destination;     // Địa điểm
    private String duration;
    private String itinerary;
    private Long price;
    private Timestamp createdAt;    // Ngày bắt đầu
    private Timestamp end_date;
    private List<String> guideIds;
    private List<String> images;

    public Tour() {}

    // ===== Getter & Setter gốc =====
    public String getId() { return id; }
    public void setId(String id) { this.id = id; }

    public String getDescription() { return description; }
    public void setDescription(String description) { this.description = description; }

    public String getDestination() { return destination; }
    public void setDestination(String destination) { this.destination = destination; }

    public String getDuration() { return duration; }
    public void setDuration(String duration) { this.duration = duration; }

    public String getItinerary() { return itinerary; }
    public void setItinerary(String itinerary) { this.itinerary = itinerary; }

    public Long getPrice() { return price; }
    public void setPrice(Long price) { this.price = price; }

    public Timestamp getCreatedAt() { return createdAt; }
    public void setCreatedAt(Timestamp createdAt) { this.createdAt = createdAt; }

    public Timestamp getEnd_date() { return end_date; }
    public void setEnd_date(Timestamp end_date) { this.end_date = end_date; }

    public List<String> getGuideIds() { return guideIds; }
    public void setGuideIds(List<String> guideIds) { this.guideIds = guideIds; }

    public List<String> getImages() { return images; }
    public void setImages(List<String> images) { this.images = images; }

    // ===== Getter ảo để tương thích với Adapter / Fragment =====
    // (Không thay đổi dữ liệu Firestore, chỉ để hiển thị thuận tiện)

    // Tên tour: dùng description (hoặc itinerary nếu bạn muốn khác)
    public String getTourName() {
        return description != null ? description : "Chưa có tên";
    }

    // Ngày bắt đầu: dùng createdAt
    public String getStartDate() {
        if (createdAt != null)
            return createdAt.toDate().toString();
        else
            return "Chưa xác định";
    }

    // Địa điểm: dùng destination
    public String getLocation() {
        return destination != null ? destination : "Không rõ địa điểm";
    }
}
