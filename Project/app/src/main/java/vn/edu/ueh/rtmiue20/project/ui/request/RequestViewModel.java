package vn.edu.ueh.rtmiue20.project.ui.request;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.data.repository.RequestRepository;

public class RequestViewModel extends ViewModel {

    private final RequestRepository repository = new RequestRepository();

    public LiveData<List<Request>> getReceivedRequests(String ownerId) {
        return repository.getReceivedRequests(ownerId);
    }

    public LiveData<List<Request>> getSentRequests(String requesterId) {
        return repository.getSentRequests(requesterId);
    }

    public void sendRequest(Request request) {
        repository.sendRequest(request);
    }

    // Đã đổi tên hàm thành updateRequestStatus
    public void updateRequestStatus(String requestId, String newStatus) {
        repository.updateRequestStatus(requestId, newStatus);
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