package vn.edu.ueh.rtmiue20.project.util;

import android.content.Context;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.content.pm.Signature;
import android.os.Build;

import java.security.MessageDigest;

/**
 * Tiện ích đọc dấu vân tay chứng chỉ (SHA-1 / SHA-256) của chính APK đang chạy.
 * Dùng để chẩn đoán lỗi Google Sign-In DEVELOPER_ERROR (mã 10): so sánh với SHA đã đăng ký trên Firebase.
 */
public final class SigningInfoHelper {

    private SigningInfoHelper() {
    }

    public static String getFingerprint(Context context, String algorithm) {
        try {
            Signature[] signatures;
            PackageManager pm = context.getPackageManager();
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.P) {
                PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNING_CERTIFICATES);
                signatures = info.signingInfo.getApkContentsSigners();
            } else {
                PackageInfo info = pm.getPackageInfo(context.getPackageName(), PackageManager.GET_SIGNATURES);
                signatures = info.signatures;
            }
            if (signatures == null || signatures.length == 0) {
                return "Không đọc được";
            }
            MessageDigest md = MessageDigest.getInstance(algorithm);
            byte[] digest = md.digest(signatures[0].toByteArray());
            StringBuilder sb = new StringBuilder();
            for (int i = 0; i < digest.length; i++) {
                if (i > 0) sb.append(':');
                sb.append(String.format("%02X", digest[i]));
            }
            return sb.toString();
        } catch (Exception e) {
            return "Lỗi: " + e.getMessage();
        }
    }
}
