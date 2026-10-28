package vn.edu.ueh.rtmiue20.project.util;

import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.util.Base64;

import java.io.ByteArrayOutputStream;
import java.io.InputStream;

public class ImageHelper {

    /**
     * Đọc ảnh từ Uri, thu nhỏ kích thước tối đa maxDimension (px) và nén sang JPEG byte[].
     * Giúp tải lên Firebase Storage nhanh hoặc chuyển sang Base64 Data URI gọn nhẹ (< 45KB).
     */
    public static byte[] compressImageUriToJpegBytes(Context context, Uri imageUri, int maxDimension, int quality) {
        if (context == null || imageUri == null) return null;
        try {
            // Bước 1: Đọc kích thước ảnh gốc
            BitmapFactory.Options boundsOptions = new BitmapFactory.Options();
            boundsOptions.inJustDecodeBounds = true;
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                if (is == null) return null;
                BitmapFactory.decodeStream(is, null, boundsOptions);
            }

            int srcWidth = boundsOptions.outWidth;
            int srcHeight = boundsOptions.outHeight;
            if (srcWidth <= 0 || srcHeight <= 0) return null;

            int inSampleSize = 1;
            while ((srcWidth / inSampleSize) > maxDimension * 2 || (srcHeight / inSampleSize) > maxDimension * 2) {
                inSampleSize *= 2;
            }

            // Bước 2: Decode Bitmap với inSampleSize
            BitmapFactory.Options decodeOptions = new BitmapFactory.Options();
            decodeOptions.inSampleSize = inSampleSize;
            Bitmap decodedBitmap;
            try (InputStream is = context.getContentResolver().openInputStream(imageUri)) {
                if (is == null) return null;
                decodedBitmap = BitmapFactory.decodeStream(is, null, decodeOptions);
            }
            if (decodedBitmap == null) return null;

            // Bước 3: Scale chính xác về maxDimension giữ nguyên tỷ lệ khung hình
            int width = decodedBitmap.getWidth();
            int height = decodedBitmap.getHeight();
            Bitmap finalBitmap = decodedBitmap;
            if (width > maxDimension || height > maxDimension) {
                float ratio = Math.min((float) maxDimension / width, (float) maxDimension / height);
                int targetWidth = Math.max(1, Math.round(width * ratio));
                int targetHeight = Math.max(1, Math.round(height * ratio));
                finalBitmap = Bitmap.createScaledBitmap(decodedBitmap, targetWidth, targetHeight, true);
                if (finalBitmap != decodedBitmap) {
                    decodedBitmap.recycle();
                }
            }

            // Bước 4: Nén thành JPEG byte[]
            ByteArrayOutputStream baos = new ByteArrayOutputStream();
            int currentQuality = quality;
            finalBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos);

            // Đảm bảo dung lượng nhỏ gọn (< 60KB) để lưu mượt trên cả Firestore và Room SQLite
            while (baos.toByteArray().length > 60 * 1024 && currentQuality > 35) {
                baos.reset();
                currentQuality -= 10;
                finalBitmap.compress(Bitmap.CompressFormat.JPEG, currentQuality, baos);
            }

            finalBitmap.recycle();
            return baos.toByteArray();
        } catch (Exception e) {
            return null;
        }
    }

    /**
     * Chuyển mảng byte JPEG sang chuỗi Data URI (Glide hỗ trợ hiển thị trực tiếp).
     */
    public static String toBase64DataUri(byte[] jpegBytes) {
        if (jpegBytes == null || jpegBytes.length == 0) return null;
        return "data:image/jpeg;base64," + Base64.encodeToString(jpegBytes, Base64.NO_WRAP);
    }

    /**
     * Đồng bộ hiển thị ảnh đại diện (Avatar) trên mọi màn hình.
     * Khi avatarUrl rỗng/đã xóa, tự động khôi phục về icon mặc định ban đầu.
     */
    public static void bindAvatar(Context context, android.widget.ImageView imageView, String avatarUrl, boolean isGreenCircleIcon) {
        if (context == null || imageView == null) return;
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);
            imageView.clearColorFilter();
            imageView.setPadding(0, 0, 0, 0);
            if (isGreenCircleIcon) {
                com.bumptech.glide.Glide.with(context)
                        .load(avatarUrl)
                        .circleCrop()
                        .placeholder(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                        .error(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                        .into(imageView);
            } else {
                com.bumptech.glide.Glide.with(context)
                        .load(avatarUrl)
                        .centerCrop()
                        .placeholder(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                        .error(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                        .into(imageView);
            }
        } else {
            try {
                com.bumptech.glide.Glide.with(context.getApplicationContext()).clear(imageView);
            } catch (Exception ignored) {}
            imageView.setImageResource(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person);
            if (isGreenCircleIcon) {
                int padPx = Math.round(6f * context.getResources().getDisplayMetrics().density);
                imageView.setPadding(padPx, padPx, padPx, padPx);
                androidx.core.widget.ImageViewCompat.setImageTintList(
                        imageView,
                        android.content.res.ColorStateList.valueOf(android.graphics.Color.WHITE)
                );
            } else {
                imageView.setScaleType(android.widget.ImageView.ScaleType.CENTER_CROP);
            }
        }
    }

    /**
     * Đồng bộ hiển thị ảnh đại diện Admin ở góc trên bên phải (ivAdminAvatar).
     * Khi có ảnh: hiển thị ảnh tròn rõ nét. Khi chưa có ảnh: hiển thị icon mặc định với màu secondary.
     */
    public static void bindAdminHeaderAvatar(Context context, android.widget.ImageView imageView, String avatarUrl) {
        if (context == null || imageView == null) return;
        if (avatarUrl != null && !avatarUrl.trim().isEmpty()) {
            androidx.core.widget.ImageViewCompat.setImageTintList(imageView, null);
            imageView.clearColorFilter();
            imageView.setPadding(0, 0, 0, 0);
            com.bumptech.glide.Glide.with(context)
                    .load(avatarUrl)
                    .circleCrop()
                    .placeholder(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                    .error(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person)
                    .into(imageView);
        } else {
            try {
                com.bumptech.glide.Glide.with(context.getApplicationContext()).clear(imageView);
            } catch (Exception ignored) {}
            imageView.setImageResource(vn.edu.ueh.rtmiue20.project.R.drawable.ic_person);
            int padPx = Math.round(2f * context.getResources().getDisplayMetrics().density);
            imageView.setPadding(padPx, padPx, padPx, padPx);
            int tintColor = androidx.core.content.ContextCompat.getColor(
                    context, vn.edu.ueh.rtmiue20.project.R.color.secondary);
            androidx.core.widget.ImageViewCompat.setImageTintList(
                    imageView,
                    android.content.res.ColorStateList.valueOf(tintColor)
            );
        }
    }
}
