package com.example.finalproject.adapter;

import android.content.Context;

import com.bumptech.glide.Glide;
import com.denzcoskun.imageslider.interfaces.ItemClickListener;
import com.denzcoskun.imageslider.models.SlideModel;
import com.denzcoskun.imageslider.constants.ScaleTypes;
import com.denzcoskun.imageslider.ImageSlider;
import com.example.finalproject.R;

import java.util.ArrayList;
import java.util.List;

public class TourImageSliderAdapter {

    private final Context context;
    private final ImageSlider imageSlider;

    public TourImageSliderAdapter(Context context, ImageSlider imageSlider, List<String> imageUrls) {
        this.context = context;
        this.imageSlider = imageSlider;
        setupImageSlider(imageUrls);
    }

    private void setupImageSlider(List<String> imageUrls) {
        List<SlideModel> slideModels = new ArrayList<>();

        if (imageUrls != null && !imageUrls.isEmpty()) {
            for (String url : imageUrls) {
                slideModels.add(new SlideModel(url, ScaleTypes.CENTER_CROP));
            }
        } else {
            // Nếu không có ảnh, hiển thị ảnh mặc định
            slideModels.add(new SlideModel(R.drawable.ic_image_placeholder, ScaleTypes.CENTER_CROP));
        }

        // Set danh sách ảnh vào slider
        imageSlider.setImageList(slideModels, ScaleTypes.CENTER_CROP);

        // Thiết lập auto slide 3 giây/lần
        imageSlider.startSliding(3000);

        // Bắt sự kiện click vào từng ảnh
        imageSlider.setItemClickListener(new ItemClickListener() {
            @Override
            public void onItemSelected(int i) {
                // Có thể mở ảnh lớn hoặc hiển thị chi tiết tại đây
            }

            @Override
            public void doubleClick(int i) {
                // Không bắt buộc
            }
        });
    }
}
