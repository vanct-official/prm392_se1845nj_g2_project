package com.example.finalproject.entity;

import com.google.firebase.Timestamp;
import java.util.List;

public class Tour {
    private String id;
    private String description;
    private String destination;
    private String duration;
    private String itinerary;
    private Long price;
    private Timestamp createdAt;
    private Timestamp end_date;
    private List<String> guideIds;
    private List<String> images;

    public Tour() {}

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
}
