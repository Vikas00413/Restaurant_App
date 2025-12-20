package com.example.streetfoodandcafe.slip;


import android.content.Context;
import android.content.res.Resources;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.util.Log;

import com.example.streetfoodandcafe.R;
import com.example.streetfoodandcafe.ui.module.data.CartItem;

import java.util.List;


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
            int width = myBitmap.getWidth();
            // ... rest of your resizing logic
        } else {
            Log.e("SlipPrinter", "Error: Bitmap is null. Cannot resize.");
            // Handle the error (show a toast, use a placeholder image, etc.)
        }
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

            Bitmap bmp1 = BitmapFactory.decodeResource(resources, R.drawable.ic_launcher_foreground);
            Bitmap bmp2 = makeSmallerBitmap(bmp1, 384);
            ret = PosAPI.PrintBitmap(bmp2);
            ret = PosAPI.PrintStart();

        }
    }

    public boolean isThreadFinished() {
        return m_bThreadFinished;
    }
}



