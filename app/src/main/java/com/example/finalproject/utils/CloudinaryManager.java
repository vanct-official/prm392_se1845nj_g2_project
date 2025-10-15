package com.example.finalproject.utils;

import com.cloudinary.Cloudinary;
import com.cloudinary.utils.ObjectUtils;

import java.util.Map;

public class CloudinaryManager {
    private static Cloudinary cloudinary;

    public static Cloudinary getInstance() {
        if (cloudinary == null) {
            cloudinary = new Cloudinary(ObjectUtils.asMap(
                    "cloud_name", "dykflrbgy",
                    "api_key", "183287548354945",
                    "api_secret", "14csb10NDEEc35UakTZReIAZZHk"
            ));
        }
        return cloudinary;
    }
}
