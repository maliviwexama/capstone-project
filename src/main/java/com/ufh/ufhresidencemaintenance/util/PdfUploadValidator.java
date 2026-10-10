package com.ufh.ufhresidencemaintenance.util;

import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.charset.StandardCharsets;

/** Validates proof-of-residence uploads as real PDF files. */
public final class PdfUploadValidator {
    public static final long MAX_SIZE_BYTES = 5L * 1024L * 1024L;

    private PdfUploadValidator() { }

    public static boolean isValid(MultipartFile file) throws IOException {
        if (file == null || file.isEmpty() || file.getSize() > MAX_SIZE_BYTES) return false;
        String name = file.getOriginalFilename() == null ? "" : file.getOriginalFilename().toLowerCase();
        if (!name.endsWith(".pdf")) return false;
        byte[] bytes = file.getBytes();
        if (bytes.length < 5) return false;
        String header = new String(bytes, 0, 5, StandardCharsets.US_ASCII);
        return "%PDF-".equals(header);
    }
}
