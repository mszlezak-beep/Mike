package com.example.pdf;

/**
 * Image placement data for images supplied as Base64.
 */
public class Base64Image {
    private final String base64;
    private final float x;
    private final float y;
    private final float width;
    private final float height;

    public Base64Image(String base64, float x, float y, float width, float height) {
        if (width <= 0.0F || height <= 0.0F) {
            throw new IllegalArgumentException("Image width and height must be positive.");
        }

        this.base64 = base64;
        this.x = x;
        this.y = y;
        this.width = width;
        this.height = height;
    }

    public String base64() {
        return base64;
    }

    public String getBase64() {
        return base64;
    }

    public float x() {
        return x;
    }

    public float getX() {
        return x;
    }

    public float y() {
        return y;
    }

    public float getY() {
        return y;
    }

    public float width() {
        return width;
    }

    public float getWidth() {
        return width;
    }

    public float height() {
        return height;
    }

    public float getHeight() {
        return height;
    }
}
