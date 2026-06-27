package com.bd.erecruitment.util;

import java.io.ByteArrayOutputStream;
import java.util.zip.Deflater;
import java.util.zip.Inflater;

public class ImageUtils {

    public static final String DEFAULT_AVATAR_BASE64 = "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";

    public static byte[] compressImage(byte[] data) {
        Deflater deflater = new Deflater();
        deflater.setLevel(Deflater.BEST_COMPRESSION);
        deflater.setInput(data);
        deflater.finish();

        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] tmp = new byte[4*1024];
        while (!deflater.finished()) {
            int size = deflater.deflate(tmp);
            outputStream.write(tmp, 0, size);
        }
        try {
            outputStream.close();
        } catch (Exception ignored) {
        }
        return outputStream.toByteArray();
    }

    public static byte[] decompressImage(byte[] data) {
        Inflater inflater = new Inflater();
        inflater.setInput(data);
        ByteArrayOutputStream outputStream = new ByteArrayOutputStream(data.length);
        byte[] tmp = new byte[4*1024];
        try {
            while (!inflater.finished()) {
                int count = inflater.inflate(tmp);
                outputStream.write(tmp, 0, count);
            }
            outputStream.close();
        } catch (Exception ignored) {
        }
        return outputStream.toByteArray();
    }

}
