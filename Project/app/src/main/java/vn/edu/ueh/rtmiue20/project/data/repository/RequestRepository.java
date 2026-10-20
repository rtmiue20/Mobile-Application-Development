package vn.edu.ueh.rtmiue20.project.data.repository;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.lifecycle.LiveData;
import androidx.lifecycle.MutableLiveData;

import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.QuerySnapshot;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Request;

public class RequestRepository {

    public static final String COLLECTION_REQUESTS = "requests";

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Request>> receivedLiveData = new MutableLiveData<List<Request>>();
    private final MutableLiveData<List<Request>> sentLiveData = new MutableLiveData<List<Request>>();

    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> actionError = new MutableLiveData<String>();

    private ListenerRegistration receivedListener;
    private ListenerRegistration sentListener;

    public void sendRequest(Request request) {
        DocumentReference docRef = firestore.collection(COLLECTION_REQUESTS).document();
        request.setRequestId(docRef.getId());
        request.setStatus(Request.STATUS_PENDING);
        request.setTimestamp(System.currentTimeMillis());

        docRef.set(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        actionSuccess.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        actionError.setValue("Gửi yêu cầu thất bại: " + e.getMessage());
                    }
                });
    }

    public LiveData<List<Request>> getReceivedRequests(String ownerId) {
        if (receivedListener != null) {
            receivedListener.remove();
        }
        receivedListener = firestore.collection(COLLECTION_REQUESTS)
                .whereEqualTo("ownerId", ownerId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            actionError.setValue("Không thể tải yêu cầu đến: " + error.getMessage());
                            return;
                        }
                        if (snapshots != null) {
                            receivedLiveData.setValue(toRequestList(snapshots));
                        }
                    }
                });
        return receivedLiveData;
    }

    public LiveData<List<Request>> getSentRequests(String requesterId) {
        if (sentListener != null) {
            sentListener.remove();
        }
        sentListener = firestore.collection(COLLECTION_REQUESTS)
                .whereEqualTo("requesterId", requesterId)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            actionError.setValue("Không thể tải yêu cầu đã gửi: " + error.getMessage());
                            return;
                        }
                        if (snapshots != null) {
                            sentLiveData.setValue(toRequestList(snapshots));
                        }
                    }
                });
        return sentLiveData;
    }

    public void updateRequestStatus(String requestId, final String newStatus) {
        firestore.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .update("status", newStatus)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        actionSuccess.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        actionError.setValue("Cập nhật trạng thái thất bại: " + e.getMessage());
                    }
                });
    }

    public LiveData<Boolean> getActionSuccess() {
        return actionSuccess;
    }

    public LiveData<String> getActionError() {
        return actionError;
    }

    public void clearActionStatus() {
        actionSuccess.setValue(null);
        actionError.setValue(null);
    }

    public void removeListeners() {
        if (receivedListener != null) {
            receivedListener.remove();
            receivedListener = null;
        }
        if (sentListener != null) {
            sentListener.remove();
            sentListener = null;
        }
    }

    private List<Request> toRequestList(QuerySnapshot snapshots) {
        List<Request> list = new ArrayList<Request>();
        for (DocumentSnapshot doc : snapshots.getDocuments()) {
            Request request = doc.toObject(Request.class);
            if (request != null) {
                request.setRequestId(doc.getId());
                list.add(request);
            }
        }
        Collections.sort(list, new Comparator<Request>() {
            @Override
            public int compare(Request a, Request b) {
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });
        return list;
    }
}