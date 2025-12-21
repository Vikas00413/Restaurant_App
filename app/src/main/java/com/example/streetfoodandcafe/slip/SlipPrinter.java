package com.example.streetfoodandcafe.slip;


import android.content.ContentValues;
import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;

import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.net.Uri;
import android.os.Build;
import android.provider.MediaStore;
import android.util.Log;

import com.example.streetfoodandcafe.R;
import com.example.streetfoodandcafe.ui.module.data.CartItem;

import java.io.File;
import java.io.IOException;
import java.io.OutputStream;
import java.util.List;
import android.os.Environment;

import android.widget.Toast;
public class SlipPrinter extends Thread {
    private Context context;

    public String tag = "PrintActivity-Cagri";

    int ret = -1;

    private boolean m_bThreadFinished = true;

    POSAPIHelper PosAPI = POSAPIHelper.getInstance();

    SlipPrinter printThread = null;

    public SlipPrinter(Context context) {
        this.context = context;
    }

    SlipPrinterBitmap slip = new SlipPrinterBitmap();

    private Bitmap makeSmallerBitmap(Bitmap myBitmap, Integer maxSize) {
        if (myBitmap != null) {
            Integer width = myBitmap.getWidth();
            Integer height = myBitmap.getHeight();
            Double bitmapRatio = (width.doubleValue() / height.doubleValue());

            if (bitmapRatio > 1) {
                width = maxSize;
                Double scaledHeight = width / bitmapRatio;
                height = scaledHeight.intValue();
            } else {
                height = maxSize;
                Double scaledWidth = height * bitmapRatio;
                width = scaledWidth.intValue();
            }

            return Bitmap.createScaledBitmap(myBitmap, width, height, true);
        } else {
            Log.e("SlipPrinter", "Error: Bitmap is null. Cannot resize.");
            // Handle the error (show a toast, use a placeholder image, etc.)
            return null;
        }

    }

    public void clickAndPrint(Context context, String customerName, String mobileNo, List<CartItem> cartItems, double totalAmount, long orderId) {
        slip.generateSlip(context,customerName,mobileNo,cartItems,totalAmount,orderId);
        if (printThread != null && !printThread.isThreadFinished()) {
            Log.e(tag, "Thread is still running...");
            return;
        }

        synchronized (this) {

            Resources resources = context.getResources();
            ret = PosAPI.PrintInit(2, 24, 24, 0);
            PosAPI.PrintSetGray(5);

            Print.PrintSetFont((byte) 24, (byte) 24, (byte) 2);

            Bitmap bit = SingletonBitmap.getInstance().getBitmap();
            PosAPI.PrintBitmap(bit);

            // --- FIX FOR ADAPTIVE ICON ---
            Drawable drawable = resources.getDrawable(R.drawable.ic_launcher_foreground, null);
            Bitmap bmp1 = null;
            if (drawable != null) {
                // Create a mutable bitmap to draw on
                bmp1 = Bitmap.createBitmap(drawable.getIntrinsicWidth(), drawable.getIntrinsicHeight(), Bitmap.Config.ARGB_8888);
                Canvas canvas = new Canvas(bmp1);
                drawable.setBounds(0, 0, canvas.getWidth(), canvas.getHeight());
                drawable.draw(canvas);
            }
            // --- END OF FIX ---
            Bitmap bmp2 = makeSmallerBitmap(bmp1, 384);
            if(bmp2 != null){
                String fileName = "order_logo_" + orderId + ".png";
                saveBitmapToGallery(context, bmp2, fileName);
                File slipFile = new File(context.getExternalFilesDir(Environment.DIRECTORY_PICTURES), fileName);
                Log.d(tag, "Slip image saved to: " + slipFile.getAbsolutePath());
                ret = PosAPI.PrintBitmap(bmp2);
                ret = PosAPI.PrintStart();
            }


        }
    }

    public boolean isThreadFinished() {
        return m_bThreadFinished;
    }


    // ... existing code in SlipPrinter.java ...

    // --- PASTE THIS METHOD HERE ---
    private void saveBitmapToGallery(Context context, Bitmap bitmap, String displayName) {
        if (bitmap == null) {
            android.util.Log.e("SaveBitmap", "Bitmap is null, cannot save.");
            return;
        }

        java.io.OutputStream fos = null;
        try {
            android.content.ContentValues values = new android.content.ContentValues();
            values.put(android.provider.MediaStore.Images.Media.DISPLAY_NAME, displayName);
            values.put(android.provider.MediaStore.Images.Media.MIME_TYPE, "image/png");

            // Set the location to the public Pictures directory for Android 10+
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.Q) {
                values.put(android.provider.MediaStore.Images.Media.RELATIVE_PATH, android.os.Environment.DIRECTORY_PICTURES);
            }

            android.net.Uri imageUri = context.getContentResolver().insert(android.provider.MediaStore.Images.Media.EXTERNAL_CONTENT_URI, values);

            if (imageUri != null) {
                fos = context.getContentResolver().openOutputStream(imageUri);
                if (fos != null) {
                    // Compress and save the bitmap
                    bitmap.compress(Bitmap.CompressFormat.PNG, 100, fos);
                    // Show confirmation message
                    android.widget.Toast.makeText(context, "Image saved to Gallery", android.widget.Toast.LENGTH_SHORT).show();
                }
            } else {
                throw new java.io.IOException("Failed to create new MediaStore record.");
            }

        } catch (Exception e) {
            android.util.Log.e("SaveBitmap", "Error saving bitmap to gallery", e);
            android.widget.Toast.makeText(context, "Failed to save image", android.widget.Toast.LENGTH_SHORT).show();
        } finally {
            try {
                if (fos != null) {
                    fos.close();
                }
            } catch (java.io.IOException e) {
                e.printStackTrace();
            }
        }
    }
} // End of SlipPrinter class





