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

    public void acceptRequest(String requestId) {
        repository.updateRequestStatus(requestId, Request.STATUS_ACCEPTED);
    }

    public void rejectRequest(String requestId) {
        repository.updateRequestStatus(requestId, Request.STATUS_REJECTED);
    }

    public LiveData<Boolean> getActionSuccess() {
        return repository.getActionSuccess();
    }

    public LiveData<String> getActionError() {
        return repository.getActionError();
    }

    public void clearActionStatus() {
        repository.clearActionStatus();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListeners();
    }
}