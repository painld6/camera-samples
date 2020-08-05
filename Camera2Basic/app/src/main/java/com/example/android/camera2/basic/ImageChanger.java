package com.example.android.camera2.basic;

import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.graphics.Color;
import android.graphics.ImageFormat;
import android.media.Image;

import java.io.IOException;
import java.io.OutputStream;
import java.nio.ShortBuffer;

public class ImageChanger {

    public static void writeDepth16Image(Image img, OutputStream out) throws IOException {
        if (img.getFormat() != ImageFormat.DEPTH16) {
            throw new IOException(
                    String.format("Unexpected Image format: %d, expected ImageFormat.DEPTH16",
                            img.getFormat()));
        }
        int w = img.getWidth();
        int h = img.getHeight();
        int rowStride = img.getPlanes()[0].getRowStride() / 2; // in shorts
        ShortBuffer y16Data = img.getPlanes()[0].getBuffer().asShortBuffer();

        Bitmap rgbImage = convertDepthToFalseColor(y16Data, w, h, rowStride, /*scale*/ 1);
        rgbImage.compress(Bitmap.CompressFormat.JPEG, 100, out);
        rgbImage.recycle();
    }

    /**
     * Convert depth16 buffer into a false-color RGBA Bitmap, scaling down
     * by factor of scale
     */
    private static Bitmap convertDepthToFalseColor(ShortBuffer depthBuffer, int w, int h,
                                            int stride, int scale) {
        short[] yRow = new short[w];
        int[] imgArray = new int[w * h];
        w = w / scale;
        h = h / scale;
        stride = stride * scale;
        short[] imgBuffer = new short[depthBuffer.remaining()];
        int[] imgData = new int[depthBuffer.remaining()];
        depthBuffer.position(0);
        depthBuffer.get(imgBuffer);
        for (int y = 0; y < imgData.length; y++) {
            short y16 = imgBuffer[y];
            int g = (y16 >> 8) & 0xff;
            imgData[y] = Color.argb(255, g, g, g);
        }
//        for (int y = 0, j = 0, rowStart = 0; y < h; y++, rowStart += stride) {
//            // Align to start of nearest-neighbor row
//            depthBuffer.position(rowStart);
//            depthBuffer.get(yRow);
//            for (int x = 0, i = 0; x < w; x++, i += scale, j++) {
//                short y16 = yRow[i];
//                int g = (y16 >> 8) & 0x00FF;
//                int color = Color.rgb(g, g, g);
//                imgArray[j] = color;
//            }
//        }
        return Bitmap.createBitmap(imgData, w, h, Bitmap.Config.ARGB_8888);
    }
}
