package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import java.text.SimpleDateFormat;
import java.util.List;
import java.util.Locale;

public class Tour {


    private String id;
    private String title;
    private String destination;
    private String status;
    private double price;
    private String itinerary;
    private String description;
    private List<String> images;
    private Timestamp start_date;
    private Timestamp end_date;
    private List<String> guideIds;


    public Tour() {}


    public String getId() {
        return id;
    }

    public void setId(String id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public void setTitle(String title) {
        this.title = title;
    }

    public String getDestination() {
        return destination;
    }

    public void setDestination(String destination) {
        this.destination = destination;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public double getPrice() {
        return price;
    }

    public void setPrice(double price) {
        this.price = price;
    }

    public String getItinerary() {
        return itinerary;
    }

    public void setItinerary(String itinerary) {
        this.itinerary = itinerary;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public List<String> getImages() {
        return images;
    }

    public void setImages(List<String> images) {
        this.images = images;
    }

    public Timestamp getStart_date() {
        return start_date;
    }

    public void setStart_date(Timestamp start_date) {
        this.start_date = start_date;
    }

    public Timestamp getEnd_date() {
        return end_date;
    }

    public void setEnd_date(Timestamp end_date) {
        this.end_date = end_date;
    }

    public List<String> getGuideIds() {
        return guideIds;
    }

    public void setGuideIds(List<String> guideIds) {
        this.guideIds = guideIds;
    }

    // ====================================================================
    // 🔹 Các phương thức tương thích cho adapter & hiển thị UI
    // ====================================================================

    // 👉 Tên tour (tương thích với getTourName())
    public String getTourName() {
        return title;
    }

    // 👉 Địa điểm (tương thích với getLocation())
    public String getLocation() {
        return destination;
    }

    // 👉 Mô tả hiển thị (ưu tiên description, fallback itinerary)
    public String getDescriptionText() {
        if (description != null && !description.isEmpty()) {
            return description;
        }
        return itinerary;
    }

    // 👉 Ngày bắt đầu định dạng dd/MM/yyyy
    public String getStartDate() {
        if (start_date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(start_date.toDate());
        }
        return "";
    }

    // 👉 Khoảng thời lượng tour (ví dụ: “2 ngày”)
    public String getDuration() {
        if (start_date != null && end_date != null) {
            long diff = end_date.toDate().getTime() - start_date.toDate().getTime();
            long days = diff / (1000 * 60 * 60 * 24);
            if (days <= 0) days = 1;
            return days + " ngày";
        }
        return "Không xác định";
    }

    // 👉 Giá dưới dạng Long (dành cho adapter cũ)
    public Long getPriceAsLong() {
        return (long) price;
    }

    // 👉 Format giá đẹp (ví dụ: 1.500.000 ₫)
    public String getFormattedPrice() {
        java.text.NumberFormat nf = java.text.NumberFormat.getInstance(new Locale("vi", "VN"));
        return nf.format(price) + " ₫";
    }

    // 👉 Gộp ngày bắt đầu – kết thúc (ví dụ: 05/11/2025 - 06/11/2025)
    public String getFormattedPeriod() {
        if (start_date != null && end_date != null) {
            SimpleDateFormat sdf = new SimpleDateFormat("dd/MM/yyyy", Locale.getDefault());
            return sdf.format(start_date.toDate()) + " - " + sdf.format(end_date.toDate());
        }
        return "";
    }
}
