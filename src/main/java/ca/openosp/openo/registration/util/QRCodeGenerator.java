/**
 * Copyright (c) 2025. OpenOSP. All Rights Reserved.
 * This software is published under the GPL GNU General Public License.
 * This program is free software; you can redistribute it and/or
 * modify it under the terms of the GNU General Public License
 * as published by the Free Software Foundation; either version 2
 * of the License, or (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA 02111-1307, USA.
 *
 * This software was written for OpenOSP - Open Source Patient Registration Module.
 *
 * @since 2025-12-29
 */
package ca.openosp.openo.registration.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.EncodeHintType;
import com.google.zxing.WriterException;
import com.google.zxing.common.ByteMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import com.google.zxing.qrcode.decoder.ErrorCorrectionLevel;
import org.apache.logging.log4j.LogManager;
import org.apache.logging.log4j.Logger;
import org.springframework.stereotype.Component;

import javax.imageio.ImageIO;
import java.awt.*;
import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.Base64;

/**
 * Utility class for generating QR codes for patient registration.
 *
 * <p>Generates QR codes that encode the registration URL. The static QR code
 * displayed in the clinic always points to the same URL, but each scan
 * generates a unique session token.</p>
 *
 * <p>Part of the Patient Self-Registration Module - a standalone commercial module
 * for OpenO EMR that enables patients to self-register via QR code scanning.</p>
 *
 * @since 2025-12-29
 */
@Component
public class QRCodeGenerator {

    private static final Logger logger = LogManager.getLogger(QRCodeGenerator.class);

    /** Default QR code width in pixels */
    private int defaultWidth = 300;

    /** Default QR code height in pixels */
    private int defaultHeight = 300;

    /** QR code foreground color (dark modules) */
    private Color foregroundColor = Color.BLACK;

    /** QR code background color (light modules) */
    private Color backgroundColor = Color.WHITE;

    /** Error correction level */
    private ErrorCorrectionLevel errorCorrectionLevel = ErrorCorrectionLevel.M;

    // ========================================================================
    // QR Code Generation
    // ========================================================================

    /**
     * Generates a QR code image as a byte array (PNG format).
     *
     * @param content String the content to encode (typically a URL)
     * @return byte[] the PNG image data
     * @throws QRCodeGenerationException if generation fails
     */
    public byte[] generateQRCode(String content) throws QRCodeGenerationException {
        return generateQRCode(content, defaultWidth, defaultHeight);
    }

    /**
     * Generates a QR code image as a byte array (PNG format) with specified dimensions.
     *
     * @param content String the content to encode (typically a URL)
     * @param width int the image width in pixels
     * @param height int the image height in pixels
     * @return byte[] the PNG image data
     * @throws QRCodeGenerationException if generation fails
     */
    public byte[] generateQRCode(String content, int width, int height) throws QRCodeGenerationException {
        try {
            BufferedImage image = generateQRCodeImage(content, width, height);
            return imageToBytes(image, "PNG");
        } catch (IOException e) {
            logger.error("Failed to generate QR code", e);
            throw new QRCodeGenerationException("Failed to generate QR code image", e);
        }
    }

    /**
     * Generates a QR code as a Base64-encoded string for inline display.
     *
     * @param content String the content to encode
     * @return String the Base64-encoded PNG image (suitable for data: URLs)
     * @throws QRCodeGenerationException if generation fails
     */
    public String generateQRCodeBase64(String content) throws QRCodeGenerationException {
        return generateQRCodeBase64(content, defaultWidth, defaultHeight);
    }

    /**
     * Generates a QR code as a Base64-encoded string with specified dimensions.
     *
     * @param content String the content to encode
     * @param width int the image width in pixels
     * @param height int the image height in pixels
     * @return String the Base64-encoded PNG image
     * @throws QRCodeGenerationException if generation fails
     */
    public String generateQRCodeBase64(String content, int width, int height) throws QRCodeGenerationException {
        byte[] imageData = generateQRCode(content, width, height);
        return Base64.getEncoder().encodeToString(imageData);
    }

    /**
     * Generates a complete data URL for inline image display.
     *
     * @param content String the content to encode
     * @return String the complete data URL (data:image/png;base64,...)
     * @throws QRCodeGenerationException if generation fails
     */
    public String generateQRCodeDataUrl(String content) throws QRCodeGenerationException {
        return generateQRCodeDataUrl(content, defaultWidth, defaultHeight);
    }

    /**
     * Generates a complete data URL for inline image display with specified dimensions.
     *
     * @param content String the content to encode
     * @param width int the image width in pixels
     * @param height int the image height in pixels
     * @return String the complete data URL
     * @throws QRCodeGenerationException if generation fails
     */
    public String generateQRCodeDataUrl(String content, int width, int height) throws QRCodeGenerationException {
        String base64 = generateQRCodeBase64(content, width, height);
        return "data:image/png;base64," + base64;
    }

    /**
     * Generates a BufferedImage of the QR code.
     *
     * @param content String the content to encode
     * @param width int the image width in pixels
     * @param height int the image height in pixels
     * @return BufferedImage the QR code image
     * @throws QRCodeGenerationException if generation fails
     */
    public BufferedImage generateQRCodeImage(String content, int width, int height) throws QRCodeGenerationException {
        try {
            // Configure encoding hints (using Hashtable for ZXing 1.5 compatibility)
            java.util.Hashtable<EncodeHintType, Object> hints = new java.util.Hashtable<>();
            hints.put(EncodeHintType.ERROR_CORRECTION, errorCorrectionLevel);
            hints.put(EncodeHintType.CHARACTER_SET, "UTF-8");

            // Generate QR code byte matrix (ZXing 1.5 uses ByteMatrix)
            QRCodeWriter writer = new QRCodeWriter();
            ByteMatrix byteMatrix = writer.encode(content, BarcodeFormat.QR_CODE, width, height, hints);

            // Convert to BufferedImage
            int matrixWidth = byteMatrix.getWidth();
            int matrixHeight = byteMatrix.getHeight();
            BufferedImage image = new BufferedImage(width, height, BufferedImage.TYPE_INT_RGB);
            int fgColor = foregroundColor.getRGB();
            int bgColor = backgroundColor.getRGB();

            // Scale matrix to requested image size
            double scaleX = (double) width / matrixWidth;
            double scaleY = (double) height / matrixHeight;

            for (int x = 0; x < width; x++) {
                for (int y = 0; y < height; y++) {
                    int matrixX = Math.min((int) (x / scaleX), matrixWidth - 1);
                    int matrixY = Math.min((int) (y / scaleY), matrixHeight - 1);
                    // ByteMatrix.get() returns 0 for black (on), non-zero for white (off) in ZXing 1.5
                    image.setRGB(x, y, byteMatrix.get(matrixX, matrixY) == 0 ? fgColor : bgColor);
                }
            }

            return image;

        } catch (WriterException e) {
            logger.error("Failed to encode QR code: {}", content, e);
            throw new QRCodeGenerationException("Failed to encode QR code", e);
        }
    }

    // ========================================================================
    // Registration URL Building
    // ========================================================================

    /**
     * Builds the registration start URL for QR code encoding.
     *
     * @param baseUrl String the base URL of the application (e.g., https://clinic.com/oscar)
     * @return String the complete registration URL
     */
    public String buildRegistrationUrl(String baseUrl) {
        if (baseUrl == null) {
            throw new IllegalArgumentException("Base URL cannot be null");
        }

        // Remove trailing slash if present
        String cleanBaseUrl = baseUrl.endsWith("/") ? baseUrl.substring(0, baseUrl.length() - 1) : baseUrl;

        return cleanBaseUrl + "/registration/start";
    }

    /**
     * Generates a complete QR code for the registration URL.
     *
     * @param baseUrl String the base URL of the application
     * @return byte[] the PNG image data
     * @throws QRCodeGenerationException if generation fails
     */
    public byte[] generateRegistrationQRCode(String baseUrl) throws QRCodeGenerationException {
        String url = buildRegistrationUrl(baseUrl);
        logger.debug("Generating QR code for registration URL: {}", url);
        return generateQRCode(url);
    }

    /**
     * Generates a complete QR code data URL for the registration URL.
     *
     * @param baseUrl String the base URL of the application
     * @return String the complete data URL
     * @throws QRCodeGenerationException if generation fails
     */
    public String generateRegistrationQRCodeDataUrl(String baseUrl) throws QRCodeGenerationException {
        String url = buildRegistrationUrl(baseUrl);
        return generateQRCodeDataUrl(url);
    }

    // ========================================================================
    // Helper Methods
    // ========================================================================

    /**
     * Converts a BufferedImage to a byte array.
     */
    private byte[] imageToBytes(BufferedImage image, String format) throws IOException {
        ByteArrayOutputStream baos = new ByteArrayOutputStream();
        ImageIO.write(image, format, baos);
        return baos.toByteArray();
    }

    // ========================================================================
    // Configuration Setters
    // ========================================================================

    public void setDefaultWidth(int defaultWidth) {
        this.defaultWidth = defaultWidth;
    }

    public void setDefaultHeight(int defaultHeight) {
        this.defaultHeight = defaultHeight;
    }

    public void setForegroundColor(Color foregroundColor) {
        this.foregroundColor = foregroundColor;
    }

    public void setBackgroundColor(Color backgroundColor) {
        this.backgroundColor = backgroundColor;
    }

    public void setErrorCorrectionLevel(ErrorCorrectionLevel errorCorrectionLevel) {
        this.errorCorrectionLevel = errorCorrectionLevel;
    }

    public int getDefaultWidth() {
        return defaultWidth;
    }

    public int getDefaultHeight() {
        return defaultHeight;
    }

    // ========================================================================
    // Exception Class
    // ========================================================================

    /**
     * Exception thrown when QR code generation fails.
     */
    public static class QRCodeGenerationException extends Exception {
        public QRCodeGenerationException(String message) {
            super(message);
        }

        public QRCodeGenerationException(String message, Throwable cause) {
            super(message, cause);
        }
    }
}
