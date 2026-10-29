package vn.edu.ueh.rtmiue20.project.ui.request;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;

public class RequestViewModel extends ViewModel {

    private final RequestRepository repository = RequestRepository.getInstance();

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

    // Đã đổi tên hàm thành updateRequestStatus
    public void updateRequestStatus(String requestId, String newStatus) {
        repository.updateRequestStatus(requestId, newStatus);
    }

    public void completeAndRateRequest(Request request, float ratingValue, String comment) {
        repository.completeAndRateRequest(request, ratingValue, comment);
    }

    public LiveData<Boolean> getActionSuccess() {
        return repository.getActionSuccess();
    }

    // Đã đổi tên hàm thành getError()
    public LiveData<String> getError() {
        return repository.getActionError();
    }

    // Đã đổi tên hàm thành resetStatus()
    public void resetStatus() {
        repository.clearActionStatus();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }
}
