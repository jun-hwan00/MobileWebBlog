package com.cookandriod.photoviewer;

import android.graphics.Bitmap;

public class PostData {
    private Bitmap image;
    private String title;
    private String text;
    private String date;

    public PostData(Bitmap image, String title, String text, String date) {
        this.image = image;
        this.title = title;
        this.text = text;
        this.date = date;
    }

    public Bitmap getImage() {
        return image;
    }

    public String getTitle() {
        return title;
    }

    public String getText() {
        return text;
    }

    public String getDate() {
        return date;
    }
}