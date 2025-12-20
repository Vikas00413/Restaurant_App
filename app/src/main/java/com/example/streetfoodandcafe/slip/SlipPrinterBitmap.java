package com.example.streetfoodandcafe.slip;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Color;
import android.graphics.Paint;
import android.graphics.Typeface;


import com.example.streetfoodandcafe.ui.module.data.CartItem;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileOutputStream;
import java.io.FileReader;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class SlipPrinterBitmap {

    // We use app-specific cache/files dir to avoid permission issues on Android 10+
    // We use app-specific cache/files dir to avoid permission issues on Android 10+
    private String getFilePath(Context context, String fileName) {
        // Get the directory handle
        File directory = context.getExternalFilesDir(null);

        // Safety check: if external is not available, fall back to internal cache
        if (directory == null) {
            directory = context.getFilesDir();
        }

        // Ensure directory exists
        if (!directory.exists()) {
            directory.mkdirs();
        }

        // Construct the full file object
        File file = new File(directory, fileName);

        // Return the absolute path string
        return file.getAbsolutePath();
    }


    /**
     * Generates the Slip Bitmap using the data passed from the UI.
     *
     * @param context       Android Context
     * @param customerName  Name of the customer (or Mobile if name is guest)
     * @param mobileNo      Customer mobile number
     * @param cartItems     List of items in the cart (from AddOrderSheet)
     * @param totalAmount   Total order amount
     * @param orderId       The ID of the order saved in DB
     */
    public void generateSlip(Context context, String customerName, String mobileNo, List<CartItem> cartItems, double totalAmount, long orderId) {

        String slipHeaderFilePath = getFilePath(context, "SlipHeader.txt");
        String slipSQLFilePath = getFilePath(context, "SlipSQL.txt");
        String slipTailFilePath = getFilePath(context, "SlipTail.txt");

        // Format Date and Time
        SimpleDateFormat dateFormat = new SimpleDateFormat("dd.MM.yyyy", Locale.getDefault());
        SimpleDateFormat timeFormat = new SimpleDateFormat("HH:mm:ss", Locale.getDefault());
        String date = dateFormat.format(new Date());
        String time = timeFormat.format(new Date());

        // --- 1. WRITE HEADER ---
        try {
            FileOutputStream fos = new FileOutputStream(slipHeaderFilePath);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write(Constants.ORTALA + "STREET FOOD & CAFE"); // Title
            writer.write("\n");
            writer.write(Constants.ORTALA + "Fresh & Tasty");
            writer.write("\n");
            writer.write(Constants.ORTALA + Constants.BORDER);
            writer.write("\n");

            // Customer Info
            writer.write(Constants.SOLA_YASLA + "Name: " + customerName);
            writer.write("\n");
            if (!mobileNo.isEmpty()) {
                writer.write(Constants.SOLA_YASLA + "Mobile: " + mobileNo);
                writer.write("\n");
            }

            writer.write(Constants.ORTALA + " "); // Space
            writer.write("\n");

            // Order Details
            writer.write(Constants.SOLA_YASLA + "Order No: " + orderId);
            writer.write("\n");
            writer.write(Constants.SOLA_YASLA + "Date: " + date + " " + time);
            writer.write("\n");
            writer.write(Constants.ORTALA + Constants.BORDER);
            writer.write("\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- 2. WRITE ITEMS (BODY) ---
        try {
            FileOutputStream fos = new FileOutputStream(slipSQLFilePath);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            // Column Headers: Name | Qty | Price
            writer.write(Constants.SOLA_YASLA + "" + Constants.BOLD + "Item");
            writer.write(Constants.ORTALA + "Qty");
            writer.write(Constants.SAGA_YASLA + "Price");
            writer.write("\n");

            writer.write(Constants.ORTALA + Constants.BORDER);
            writer.write("\n");

            for (CartItem item : cartItems) {
                // Determine Name (add variant if needed)
                String itemName = item.getItem().getFoodName();
                if (!item.getVariant().equals("Standard")) {
                    itemName += " (" + item.getVariant() + ")";
                }

                // Get Quantity (Kotlin MutableIntState access from Java)
                // Assuming CartItem.count is a MutableIntState, we use .getValue().intValue() or similar.
                // If it exposes a getter via data class:
                int quantity = item.getCount().getValue(); // or item.getCount().intValue() depending on Kotlin version

                double itemTotal = item.getUnitPrice() * quantity;
                String priceStr = String.format(Locale.US, "%.2f", itemTotal);

                // Write Line
                // Format: Name (Left) -- Qty (Center) -- Price (Right)
                // Note: The custom format logic handles alignment based on control chars

                // Name (Trimmed if too long)
                writer.write(Constants.SOLA_YASLA + "" + Constants.FONT24 + customFormat(itemName, 18));

                // Qty (Centered manually or via tag if logic supports inline, keeping it simple here)
                // The parser splits lines by tags. To put them on same line requires tricky formatting with this parser.
                // We will try to combine them in the string since standard alignment tags split lines in the parser loop.

                // Alternative approach matching the parser:
                // The parser reads line by line. It cannot do 3 columns easily unless formatted in one string.
                // Let's use the layout from your previous code:
                // Name (Left) | Tax (Center) | Price (Right)
                // We will reuse that structure but put Qty in the middle.

                // Because the parser seems to draw the whole line based on the FIRST char tag,
                // we cannot mix Left/Center/Right tags in one line easily without modifying the parser.
                // However, the previous code loop did: writer.write(...SOLA...); writer.write(...ORTALA...);
                // This implies the parser handles partial writes? No, it writes new lines.

                // Correct approach for this specific parser:
                // It draws text at xPosition based on tag.
                // It seems to expect one tag per line in the text file.
                // BUT the previous loop wrote 3 distinct writes for one loop iteration.
                // Does the parser handle that?
                // Looking at createBitmapFromTextFile: `while ((line = reader1.readLine()) != null...)`
                // It reads LINE BY LINE.
                // So the previous code:
                // writer.write(SOLA...); writer.write(ORTALA...); writer.write(SAGA...); writer.write("\n");
                // produced ONE line in the text file containing all 3 strings?
                // No, string concatenation happens there.

                // Let's construct a single formatted string for the line.

                String lineOutput = String.format("%-14s %3s %9s",
                        (itemName.length() > 14 ? itemName.substring(0, 14) : itemName),
                        "x" + quantity,
                        priceStr);

                // Write standard line
                writer.write(Constants.SOLA_YASLA + lineOutput);
                writer.write("\n");
            }

            writer.write(Constants.ORTALA + Constants.BORDER);
            writer.write("\n");

            // Total
            String totalStr = String.format(Locale.US, "%.2f", totalAmount);
            writer.write(Constants.SAGA_YASLA + "" + Constants.BOLD + "TOTAL: " + totalStr);
            writer.write("\n");
            writer.write(Constants.ORTALA + Constants.BORDER);
            writer.write("\n");

            writer.close();

        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- 3. WRITE TAIL (FOOTER) ---
        try {
            FileOutputStream fos = new FileOutputStream(slipTailFilePath);
            OutputStreamWriter writer = new OutputStreamWriter(fos);

            writer.write(Constants.ORTALA + "Thank You!");
            writer.write("\n");
            writer.write(Constants.ORTALA + "Please Visit Again");
            writer.write("\n");
            writer.write(Constants.ORTALA + " ");
            writer.write("\n");
            writer.write(Constants.ORTALA + "."); // End marker
            writer.write("\n");

            writer.close();
        } catch (IOException e) {
            e.printStackTrace();
        }

        // --- 4. GENERATE BITMAP ---
        // Dynamically calculate height first
        int dynamicHeight = calculateTotalHeight(slipHeaderFilePath, slipSQLFilePath, slipTailFilePath);
        // Ensure minimum height
        if (dynamicHeight < 400) dynamicHeight = 400;

        Bitmap slipBitmap = createBitmapFromTextFile(slipHeaderFilePath, slipSQLFilePath, slipTailFilePath, dynamicHeight);

        // Save to singleton (or return it callback if you refactor further)
        SingletonBitmap.getInstance().setBitmap(slipBitmap);
    }

    private String customFormat(String text, int maxLength) {
        if (text.length() < maxLength) {
            StringBuilder formattedText = new StringBuilder(text);
            for (int i = 0; i < maxLength - text.length(); i++) {
                formattedText.append(" ");
            }
            return formattedText.toString();
        } else if (text.length() == maxLength) {
            return text;
        } else {
            return text.substring(0, maxLength - 1) + ".";
        }
    }

    private int calculateTotalHeight(String filePath1, String filePath2, String filePath3) {
        int totalHeight =100; // Base padding

        try {
            // We replace the missing 'countLines' call with getFileHeight
            // which sums up the height line by line based on font size.

            totalHeight += getFileHeight(filePath1);
            totalHeight += getFileHeight(filePath2);
            totalHeight += getFileHeight(filePath3);

        } catch (Exception e) {
            e.printStackTrace();
            return 1200; // Fallback
        }
        return totalHeight + 100; // Extra padding
    }


    // Helper to calculate height contribution of a file
    private int getFileHeight(String path) throws IOException {
        BufferedReader reader = new BufferedReader(new FileReader(path));
        String line;
        int height = 0;
        Paint p = new Paint();

        while ((line = reader.readLine()) != null) {
            if (line.length() > 1) {
                char tag2 = line.charAt(1);

                if (tag2 == Constants.BOLD || tag2 == Constants.FONT48) {
                    height += 40; // Approx height for large font
                } else if (tag2 == Constants.FONTTOTALANDKDV) {
                    height += 35;
                } else {
                    height += 30; // Approx height for normal font
                }
            } else {
                height += 20; // Empty line
            }
        }
        reader.close();
        return height;
    }


    private int countSpecificLines(String path, char type) {
        // Implementation omitted for brevity, using getFileHeight instead
        return 0;
    }

    private Bitmap createBitmapFromTextFile(String filePath1, String filePath2, String filePath3, int dynamicHeight) {
        int genislik = 384; // Standard Thermal Printer Width

        Bitmap slipBitmap = Bitmap.createBitmap(genislik, dynamicHeight, Bitmap.Config.ARGB_8888);
        Canvas canvas = new Canvas(slipBitmap);
        canvas.drawColor(Color.WHITE); // Ensure white background

        Paint paint = new Paint();
        paint.setColor(Color.BLACK);
        paint.setTextSize(Constants.FONT_SIZE_FOR_24_CHARACTERS);
        Typeface monospaceTypeface = paint.setTypeface(Typeface.create("monospace", Typeface.NORMAL));
        Typeface boldTypeface = paint.setTypeface(Typeface.create("monospace", Typeface.BOLD));
        paint.setTypeface(monospaceTypeface);

        Paint.FontMetrics fm = paint.getFontMetrics();
        int y = 40; // Initial Padding

        y = drawFileContent(canvas, paint, filePath1, y, monospaceTypeface, boldTypeface);
        y = drawFileContent(canvas, paint, filePath2, y, monospaceTypeface, boldTypeface);
        y = drawFileContent(canvas, paint, filePath3, y, monospaceTypeface, boldTypeface);

        return slipBitmap;
    }

    private int drawFileContent(Canvas canvas, Paint paint, String filePath, int startY, Typeface normal, Typeface bold) {
        int y = startY;
        Paint.FontMetrics fm = paint.getFontMetrics();

        try (BufferedReader reader = new BufferedReader(new FileReader(filePath))) {
            String line;
            while ((line = reader.readLine()) != null) {
                if (line.length() < 2) continue;

                char tag = line.charAt(0);
                char tag2 = line.charAt(1);
                String textToDraw;

                // Configure Paint based on Tag2
                if (tag2 == Constants.BOLD) {
                    paint.setTypeface(bold);
                    paint.setTextSize(Constants.FONT_SIZE_FOR_48_CHARACTERS); // Larger
                    textToDraw = line.substring(2);
                } else if (tag2 == Constants.FONT48) {
                    paint.setTypeface(normal);
                    paint.setTextSize(Constants.FONT_SIZE_FOR_48_CHARACTERS);
                    textToDraw = line.substring(2);
                } else if (tag2 == Constants.FONTTOTALANDKDV) {
                    paint.setTypeface(bold);
                    paint.setTextSize(Constants.FONT_SIZE_FOR_TOTAL_AND_KDV);
                    textToDraw = line.substring(2);
                } else {
                    paint.setTypeface(normal);
                    paint.setTextSize(Constants.FONT_SIZE_FOR_24_CHARACTERS);
                    // Check if char 1 is a tag or part of text
                    if (tag2 == Constants.ORTALA || tag2 == Constants.SOLA_YASLA || tag2 == Constants.SAGA_YASLA) {
                        textToDraw = line.substring(1); // Standard tag
                    } else {
                        textToDraw = line.substring(1);
                    }
                }

                // Calculate X Position
                float x = 0;
                float textWidth = paint.measureText(textToDraw);

                switch (tag) {
                    case Constants.ORTALA: // Center
                        x = (canvas.getWidth() - textWidth) / 2;
                        break;
                    case Constants.SAGA_YASLA: // Right
                        x = canvas.getWidth() - textWidth - 10;
                        break;
                    case Constants.SOLA_YASLA: // Left
                    default:
                        x = 10;
                        break;
                }

                canvas.drawText(textToDraw, x, y, paint);
                y += (paint.descent() - paint.ascent());
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return y;
    }


    private static class Constants {
        static final char ORTALA = '0'; // Changed to char '0' for safety if reading file text
        static final char SOLA_YASLA = '1';
        static final char SAGA_YASLA = '2';

        // These act as secondary tags
        static final char FONT24 = '3';
        static final char FONT48 = '4';
        static final char FONTTOTALANDKDV = '5';
        static final char BOLD = '6';

        // Original int constants converted to chars for consistency with string writing
        // If your original system used (char)0, (char)1 etc, revert these values.
        // Assuming Standard ASCII mapping for simplicity in text file viewing:

        public static final String BORDER = "--------------------------";
        static final float FONT_SIZE_FOR_24_CHARACTERS = 24f;
        static final float FONT_SIZE_FOR_48_CHARACTERS = 30f;
        static final float FONT_SIZE_FOR_TOTAL_AND_KDV = 26f;
    }
}
