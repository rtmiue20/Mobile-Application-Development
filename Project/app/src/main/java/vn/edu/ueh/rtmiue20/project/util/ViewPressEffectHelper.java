package vn.edu.ueh.rtmiue20.project.util;

import android.view.MotionEvent;
import android.view.View;

public class ViewPressEffectHelper {

    /**
     * Áp dụng hiệu ứng ấn xuống (scale down khi chạm và đàn hồi về bình thường khi nhấc tay).
     * Trả về false để OnClickListener vẫn hoạt động mượt mà.
     */
    public static void applyPressEffect(View... views) {
        if (views == null) return;
        for (View view : views) {
            if (view == null) continue;
            view.setOnTouchListener((v, event) -> {
                switch (event.getAction()) {
                    case MotionEvent.ACTION_DOWN:
                        v.animate().scaleX(0.92f).scaleY(0.92f).setDuration(100).start();
                        break;
                    case MotionEvent.ACTION_UP:
                    case MotionEvent.ACTION_CANCEL:
                        v.animate().scaleX(1.0f).scaleY(1.0f).setDuration(120).start();
                        break;
                }
                return false;
            });
        }
    }
}
