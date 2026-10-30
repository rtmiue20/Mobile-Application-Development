package vn.edu.ueh.rtmiue20.project.ui.request;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import com.google.firebase.firestore.FirebaseFirestore;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;
import vn.edu.ueh.rtmiue20.project.util.FCMSender;

public class RequestViewModel extends AndroidViewModel {

    private final RequestRepository repository;

    public RequestViewModel(@NonNull Application application) {
        super(application);
        // Truyền Application context để SharedPreferences (offline cache) hoạt động đúng
        repository = RequestRepository.getInstance(application);
    }

    public LiveData<List<Request>> getReceivedRequests(String ownerId) {
        return repository.getReceivedRequests(ownerId);
    }

    public LiveData<List<Request>> getSentRequests(String requesterId) {
        return repository.getSentRequests(requesterId);
    }

    /** TC-ADMIN-07: Admin Dashboard quan sát và kiểm duyệt tất cả yêu cầu */
    public LiveData<List<Request>> getAllRequests() {
        return repository.getAllRequests();
    }

    public void sendRequest(Request request) {
        repository.sendRequest(request);
    }

    public void cancelRequest(String requestId) {
        repository.cancelRequest(requestId);
    }

    /**
     * Cập nhật trạng thái yêu cầu lên Firestore và bắn FCM thông báo cho người xin
     * khi chủ sách Accept hoặc Reject.
     */
    public void updateRequestStatus(String requestId, String newStatus) {
        repository.updateRequestStatus(requestId, newStatus);

        // Tìm request trong cache để lấy requesterId → bắn FCM
        for (Request r : repository.getAllLocalRequests()) {
            if (requestId != null && requestId.equals(r.getRequestId())) {
                notifyRequesterOfStatusChange(r, newStatus);
                break;
            }
        }
    }

    /**
     * Bắn FCM thông báo đến người xin sách khi chủ sách chấp nhận hoặc từ chối.
     */
    private void notifyRequesterOfStatusChange(Request request, String newStatus) {
        if (request.getRequesterId() == null || request.getRequesterId().isEmpty()) return;

        FirebaseFirestore.getInstance()
                .collection("users")
                .document(request.getRequesterId())
                .get()
                .addOnSuccessListener(doc -> {
                    if (doc == null || !doc.exists()) return;
                    String token = doc.getString("fcmToken");
                    if (token == null || token.isEmpty()) return;

                    String bookTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
                    String ownerName = request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách";

                    if (Request.STATUS_ACCEPTED.equalsIgnoreCase(newStatus)) {
                        FCMSender.pushNotification(
                                token,
                                "Yêu cầu của bạn được chấp nhận! 🎉",
                                ownerName + " đã đồng ý cho bạn nhận cuốn \"" + bookTitle + "\". Hẹn gặp tại UEH Cơ sở B!"
                        );
                    } else if (Request.STATUS_REJECTED.equalsIgnoreCase(newStatus)) {
                        FCMSender.pushNotification(
                                token,
                                "Thông báo yêu cầu nhận sách",
                                ownerName + " đã từ chối yêu cầu của bạn về cuốn \"" + bookTitle + "\". Hãy tìm sách khác nhé!"
                        );
                    }
                });
    }

    public void confirmReceivedBook(Request request, RequestRepository.RequestCallback callback) {
        repository.confirmReceivedBook(request, callback);
    }

    public void reportNotReceivedBook(Request request, String note, RequestRepository.RequestCallback callback) {
        repository.reportNotReceivedBook(request, note, callback);
    }

    public void completeAndRateRequest(Request request, float ratingValue, String comment) {
        repository.completeAndRateRequest(request, ratingValue, comment);
    }

    public void ownerRateRequester(Request request, float ratingValue, String comment, RequestRepository.RequestCallback callback) {
        repository.ownerRateRequester(request, ratingValue, comment, callback);
    }

    public LiveData<Boolean> getActionSuccess() {
        return repository.getActionSuccess();
    }

    public LiveData<String> getError() {
        return repository.getActionError();
    }

    public void resetStatus() {
        repository.clearActionStatus();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }
}
