/*
 * Copyright (c) 2026, WSO2 LLC. (http://www.wso2.com).
 *
 * WSO2 LLC. licenses this file to you under the Apache License,
 * Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License.
 * You may obtain a copy of the License at
 *
 *    http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing,
 * software distributed under the License is distributed on an
 * "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY
 * KIND, either express or implied. See the License for the
 * specific language governing permissions and limitations
 * under the License.
 */

package io.ballerina.lib.pdf.paint;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.Base64;
import java.util.HashMap;
import java.util.Map;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Decodes base64 data URL images into PDImageXObject for PDFBox rendering.
 */
public class ImageDecoder {

    private static final Pattern DATA_URL_PATTERN =
            Pattern.compile("(?:url\\(['\"]?)?data:image/(\\w+);base64,([A-Za-z0-9+/=\\s]+)['\"]?\\)?");

    private final PDDocument document;
    private final Map<String, PDImageXObject> cache = new HashMap<>();

    public ImageDecoder(PDDocument document) {
        this.document = document;
    }

    /**
     * Decodes a data URL (data:image/png;base64,...) into a PDImageXObject.
     * Returns null if the source is not a base64 data URL or decoding fails.
     */
    public PDImageXObject decode(String src) {
        if (src == null || !src.contains("base64")) return null;

        // Check cache
        PDImageXObject cached = cache.get(src);
        if (cached != null) return cached;

        Matcher m = DATA_URL_PATTERN.matcher(src);
        if (!m.find()) return null;

        String imageType = m.group(1);
        String base64Data = m.group(2).replaceAll("\\s+", "");

        try {
            byte[] imageBytes = Base64.getDecoder().decode(base64Data);
            BufferedImage bufferedImage = ImageIO.read(new ByteArrayInputStream(imageBytes));
            if (bufferedImage == null) return null;

            PDImageXObject image = org.apache.pdfbox.pdmodel.graphics.image.LosslessFactory
                    .createFromImage(document, bufferedImage);
            cache.put(src, image);
            return image;
        } catch (IOException | IllegalArgumentException e) {
            System.err.println("WARNING: Failed to decode base64 image: " + e.getMessage());
            return null;
        }
    }

    /**
     * Returns intrinsic dimensions of a decoded image, or defaults.
     */
    public float[] getImageDimensions(String src) {
        PDImageXObject image = decode(src);
        if (image != null) {
            return new float[]{image.getWidth(), image.getHeight()};
        }
        return new float[]{50, 50}; // default
    }
}
