package vn.edu.ueh.rtmiue20.project;

import android.app.Application;

import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;

public class BookShareApp extends Application {
    @Override
    public void onCreate() {
        super.onCreate();
        // Khởi tạo RequestRepository với Application Context ngay khi app bật
        RequestRepository.getInstance(this);
    }
}
