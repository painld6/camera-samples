package com.example.android.camera2.basic;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;
import android.util.Log;

import java.io.BufferedOutputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.nio.ShortBuffer;

public class ImageChanger {

    public static void writeDepth16Image(Context context, Image img, OutputStream out) throws Exception {
        if (img.getFormat() != ImageFormat.DEPTH16) {
            throw new IOException(
                    String.format("Unexpected Image format: %d, expected ImageFormat.DEPTH16",
                            img.getFormat()));
        }
        int w = img.getWidth();
        int h = img.getHeight();
        Image.Plane plane = img.getPlanes()[0];
        int pixel = plane.getPixelStride();
        int rowStride = plane.getRowStride() / 2; // in shorts
        ShortBuffer y16Data = img.getPlanes()[0].getBuffer().asShortBuffer();

        Log.e("cyw", "w:" + w + ",h:" + h + ",rowStride:" + rowStride + ",remainSize:" + y16Data.remaining());
        Bitmap rgbImage = convertDepthToFalseColor(context, y16Data, w / pixel, h / pixel, rowStride, /*scale*/ 1);
        rgbImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
        rgbImage.recycle();
    }

    /**
     * Convert depth16 buffer into a false-color RGBA Bitmap, scaling down
     * by factor of scale
     */
    private static Bitmap convertDepthToFalseColor(Context context, ShortBuffer depthBuffer,
                                                   int w, int h,
                                                   int stride, int scale) throws Exception {
        File file = new File(context.getFilesDir(), "img.txt");
        if (file.exists()) {
            file.delete();
        }
        FileOutputStream fileOutputStream = new FileOutputStream(file);
        BufferedOutputStream bufferOutput = new BufferedOutputStream(fileOutputStream);
        short[] yRow = new short[w];
        int[] imgArray = new int[w * h];
        w = w / scale;
        h = h / scale;

        stride = stride * scale;
        for (int y = 0, j = 0, rowStart = 0; y < h; y++, rowStart += stride) {
            StringBuilder builder = new StringBuilder();
            // Align to start of nearest-neighbor row
            depthBuffer.position(rowStart);
            depthBuffer.get(yRow);
            for (int x = 0, i = 0; x < w; x++, i += scale, j++) {
                short y16 = yRow[i];
                short depthRange = (short) (y16 & 0x1fff);
                short depthConfidence = (short) ((y16 >> 13) & 0x7);
                float depthPercentage = depthConfidence == 0 ? 1.f : (depthConfidence - 1) / 7.f;
                int g = (y16 >> 8) & 0x00FF;
                builder.append(depthRange + ":" + depthPercentage + ",");
                int color = Color.rgb(g, g, g);
                imgArray[j] = color;
            }
            byte[] bytes = builder.toString().getBytes();
            bufferOutput.write(bytes, 0, bytes.length);
        }
        bufferOutput.flush();
        fileOutputStream.flush();
        bufferOutput.close();
        return Bitmap.createBitmap(imgArray, w, h, Bitmap.Config.ARGB_8888);
    }
}
