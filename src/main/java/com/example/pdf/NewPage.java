package com.example.pdf;

import java.util.List;
import java.util.Objects;

/**
 * A page to append to the output PDF.
 */
public class NewPage {
    private final String headerText;
    private final List<Base64Image> images;

    public NewPage(String headerText, List<? extends Base64Image> images) {
        this.headerText = headerText;
        this.images = List.copyOf(Objects.requireNonNull(images, "images must not be null"));
    }

    public String headerText() {
        return headerText;
    }

    public String getHeaderText() {
        return headerText;
    }

    public List<Base64Image> images() {
        return images;
    }

    public List<Base64Image> getImages() {
        return images;
    }
}
