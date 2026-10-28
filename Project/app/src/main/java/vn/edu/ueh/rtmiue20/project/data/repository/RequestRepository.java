package vn.edu.ueh.rtmiue20.project.data.repository;

import android.content.Context;
import android.content.SharedPreferences;
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

import org.json.JSONArray;
import org.json.JSONObject;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Request;

public class RequestRepository {

    public static final String COLLECTION_REQUESTS = "requests";
    private static final String PREF_NAME = "BookSharePrefs";
    private static final String PREF_KEY_CACHED_REQUESTS = "cached_requests_json";

    private static volatile RequestRepository instance;
    private Context appContext;

    private final FirebaseFirestore firestore = FirebaseFirestore.getInstance();

    private final MutableLiveData<List<Request>> receivedLiveData = new MutableLiveData<List<Request>>();
    private final MutableLiveData<List<Request>> sentLiveData = new MutableLiveData<List<Request>>();
    private final MutableLiveData<List<Request>> allRequestsLiveData = new MutableLiveData<List<Request>>();

    private final MutableLiveData<Boolean> actionSuccess = new MutableLiveData<Boolean>();
    private final MutableLiveData<String> actionError = new MutableLiveData<String>();

    private final List<Request> localCache = new ArrayList<>();

    private ListenerRegistration receivedListener;
    private ListenerRegistration sentListener;
    private ListenerRegistration allRequestsListener;
    private String currentOwnerListening = null;
    private String currentRequesterListening = null;

    public static RequestRepository getInstance(Context context) {
        if (instance == null) {
            synchronized (RequestRepository.class) {
                if (instance == null) {
                    instance = new RequestRepository(context != null ? context.getApplicationContext() : null);
                }
            }
        }
        if (instance.appContext == null && context != null) {
            instance.appContext = context.getApplicationContext();
            instance.loadLocalCacheFromPrefs();
        }
        return instance;
    }

    public static RequestRepository getInstance() {
        return getInstance(null);
    }

    public RequestRepository() {
        this(null);
    }

    public RequestRepository(Context context) {
        if (context != null) {
            this.appContext = context.getApplicationContext();
        }
        loadLocalCacheFromPrefs();
    }

    public void setContext(Context context) {
        if (context != null && this.appContext == null) {
            this.appContext = context.getApplicationContext();
            loadLocalCacheFromPrefs();
        }
    }

    public void sendRequest(Request request) {
        DocumentReference docRef = firestore.collection(COLLECTION_REQUESTS).document();
        if (request.getRequestId() == null || request.getRequestId().trim().isEmpty()) {
            request.setRequestId(docRef.getId());
        }
        request.setStatus(Request.STATUS_PENDING);
        request.setTimestamp(System.currentTimeMillis());

        // Lưu vào bộ nhớ cục bộ / SharedPreferences trước để đảm bảo mượt mà
        addOrUpdateLocalRequest(request);
        if (request.getRequesterId() != null) {
            sentLiveData.setValue(getLocalSent(request.getRequesterId()));
        }
        if (request.getOwnerId() != null) {
            receivedLiveData.setValue(getLocalReceived(request.getOwnerId()));
        }

        docRef.set(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        actionSuccess.setValue(true);

                        // --- GỌI FCM: TÌM TOKEN CHỦ SÁCH VÀ BẮN THÔNG BÁO ---
                        if (request.getOwnerId() != null && !request.getOwnerId().isEmpty()) {
                            firestore.collection("users").document(request.getOwnerId()).get()
                                     .addOnSuccessListener(new OnSuccessListener<DocumentSnapshot>() {
                                         @Override
                                         public void onSuccess(DocumentSnapshot documentSnapshot) {
                                             if (documentSnapshot.exists() && documentSnapshot.contains("fcmToken")) {
                                                 String token = documentSnapshot.getString("fcmToken");
                                                 if (token != null && !token.isEmpty()) {
                                                     vn.edu.ueh.rtmiue20.project.util.FCMSender.pushNotification(
                                                             token,
                                                             "Yêu cầu nhận sách mới!",
                                                             request.getRequesterName() + " muốn nhận cuốn " + request.getBookTitle() + " của bạn."
                                                     );
                                                 }
                                             }
                                         }
                                     });
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("PERMISSION_DENIED")) {
                            android.util.Log.w("RequestRepository", "Firestore PERMISSION_DENIED cho 'requests', đang dùng dữ liệu cục bộ.");
                            actionSuccess.setValue(true);
                        } else {
                            actionError.setValue("Gửi yêu cầu thất bại: " + msg);
                        }
                    }
                });
    }

    public LiveData<List<Request>> getReceivedRequests(final String ownerId) {
        currentOwnerListening = ownerId;
        receivedLiveData.setValue(getLocalReceived(ownerId));

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
                            if (error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                android.util.Log.w("RequestRepository", "Chưa mở quyền Firestore cho bảng 'requests'. Đang hiển thị dữ liệu cục bộ.");
                            } else {
                                actionError.setValue("Không thể tải yêu cầu đến: " + error.getMessage());
                            }
                            receivedLiveData.setValue(getLocalReceived(ownerId));
                            return;
                        }
                        if (snapshots != null) {
                            List<Request> remoteList = toRequestList(snapshots);
                            mergeRemoteRequests(remoteList);
                            receivedLiveData.setValue(getLocalReceived(ownerId));
                        }
                    }
                });
        return receivedLiveData;
    }

    public LiveData<List<Request>> getSentRequests(final String requesterId) {
        currentRequesterListening = requesterId;
        sentLiveData.setValue(getLocalSent(requesterId));

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
                            if (error.getCode() == FirebaseFirestoreException.Code.PERMISSION_DENIED) {
                                android.util.Log.w("RequestRepository", "Chưa mở quyền Firestore cho bảng 'requests'. Đang hiển thị dữ liệu cục bộ.");
                            } else {
                                actionError.setValue("Không thể tải yêu cầu đã gửi: " + error.getMessage());
                            }
                            sentLiveData.setValue(getLocalSent(requesterId));
                            return;
                        }
                        if (snapshots != null) {
                            List<Request> remoteList = toRequestList(snapshots);
                            mergeRemoteRequests(remoteList);
                            sentLiveData.setValue(getLocalSent(requesterId));
                        }
                    }
                });
        return sentLiveData;
    }

    /** TC-ADMIN-07: Admin Dashboard quan sát tất cả yêu cầu (Realtime Firestore + Offline Room/Prefs) */
    public LiveData<List<Request>> getAllRequests() {
        loadLocalCacheFromPrefs();
        allRequestsLiveData.setValue(getAllLocalRequests());

        if (allRequestsListener != null) {
            allRequestsListener.remove();
        }
        allRequestsListener = firestore.collection(COLLECTION_REQUESTS)
                .addSnapshotListener(new EventListener<QuerySnapshot>() {
                    @Override
                    public void onEvent(@Nullable QuerySnapshot snapshots,
                                        @Nullable FirebaseFirestoreException error) {
                        if (error != null) {
                            android.util.Log.w("RequestRepository", "Firestore getAllRequests: " + error.getMessage() + ". Dùng dữ liệu cục bộ.");
                            allRequestsLiveData.setValue(getAllLocalRequests());
                            return;
                        }
                        if (snapshots != null) {
                            List<Request> remoteList = toRequestList(snapshots);
                            mergeRemoteRequests(remoteList);
                            allRequestsLiveData.setValue(getAllLocalRequests());
                        }
                    }
                });
        return allRequestsLiveData;
    }

    public synchronized List<Request> getAllLocalRequests() {
        loadLocalCacheFromPrefs();
        List<Request> list = new ArrayList<>(localCache);
        Collections.sort(list, new Comparator<Request>() {
            @Override
            public int compare(Request a, Request b) {
                boolean pa = Request.STATUS_PENDING.equalsIgnoreCase(a.getStatus());
                boolean pb = Request.STATUS_PENDING.equalsIgnoreCase(b.getStatus());
                if (pa != pb) return pa ? -1 : 1;
                return Long.compare(b.getTimestamp(), a.getTimestamp());
            }
        });
        return list;
    }

    public void updateRequestStatus(final String requestId, final String newStatus) {
        updateLocalRequestStatus(requestId, newStatus);
        actionSuccess.setValue(true);

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
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("PERMISSION_DENIED")) {
                            android.util.Log.w("RequestRepository", "Đã cập nhật trạng thái cục bộ cho request: " + requestId);
                        } else {
                            actionError.setValue("Cập nhật trạng thái thất bại: " + msg);
                        }
                    }
                });
    }

    public void cancelRequest(final String requestId) {
        removeLocalRequest(requestId);
        actionSuccess.setValue(true);

        firestore.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .delete()
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        // Bỏ qua lỗi xóa trên Firestore nếu không có quyền
                    }
                });
    }

    // --- Quản lý bộ nhớ cục bộ (Local Cache & SharedPreferences) ---

    private synchronized void addOrUpdateLocalRequest(Request request) {
        boolean found = false;
        for (int i = 0; i < localCache.size(); i++) {
            if (localCache.get(i).getRequestId() != null &&
                    localCache.get(i).getRequestId().equals(request.getRequestId())) {
                localCache.set(i, request);
                found = true;
                break;
            }
        }
        if (!found) {
            localCache.add(0, request);
        }
        saveLocalCacheToPrefs();
        allRequestsLiveData.setValue(getAllLocalRequests());
    }

    private synchronized void updateLocalRequestStatus(String requestId, String status) {
        for (Request r : localCache) {
            if (requestId != null && requestId.equals(r.getRequestId())) {
                r.setStatus(status);
                break;
            }
        }
        saveLocalCacheToPrefs();
        allRequestsLiveData.setValue(getAllLocalRequests());
        if (currentOwnerListening != null) {
            receivedLiveData.setValue(getLocalReceived(currentOwnerListening));
        }
        if (currentRequesterListening != null) {
            sentLiveData.setValue(getLocalSent(currentRequesterListening));
        }
    }

    private synchronized void removeLocalRequest(String requestId) {
        for (int i = 0; i < localCache.size(); i++) {
            if (requestId != null && requestId.equals(localCache.get(i).getRequestId())) {
                localCache.remove(i);
                break;
            }
        }
        saveLocalCacheToPrefs();
        allRequestsLiveData.setValue(getAllLocalRequests());
        if (currentOwnerListening != null) {
            receivedLiveData.setValue(getLocalReceived(currentOwnerListening));
        }
        if (currentRequesterListening != null) {
            sentLiveData.setValue(getLocalSent(currentRequesterListening));
        }
    }

    private synchronized void mergeRemoteRequests(List<Request> remoteList) {
        if (remoteList == null) return;
        for (Request remote : remoteList) {
            boolean found = false;
            for (int i = 0; i < localCache.size(); i++) {
                if (localCache.get(i).getRequestId() != null &&
                        localCache.get(i).getRequestId().equals(remote.getRequestId())) {
                    localCache.set(i, remote);
                    found = true;
                    break;
                }
            }
            if (!found) {
                localCache.add(remote);
            }
        }
        saveLocalCacheToPrefs();
        allRequestsLiveData.setValue(getAllLocalRequests());
    }

    private synchronized List<Request> getLocalReceived(String ownerId) {
        List<Request> list = new ArrayList<>();
        if (ownerId == null) return list;
        for (Request r : localCache) {
            if (ownerId.equals(r.getOwnerId())) {
                list.add(r);
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

    private synchronized List<Request> getLocalSent(String requesterId) {
        List<Request> list = new ArrayList<>();
        if (requesterId == null) return list;
        for (Request r : localCache) {
            if (requesterId.equals(r.getRequesterId())) {
                list.add(r);
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

    private synchronized void loadLocalCacheFromPrefs() {
        if (appContext == null) return;
        try {
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            String json = prefs.getString(PREF_KEY_CACHED_REQUESTS, null);
            if (json != null && !json.trim().isEmpty()) {
                JSONArray arr = new JSONArray(json);
                localCache.clear();
                for (int i = 0; i < arr.length(); i++) {
                    JSONObject obj = arr.getJSONObject(i);
                    Request r = new Request();
                    r.setRequestId(obj.optString("requestId"));
                    r.setBookId(obj.optString("bookId"));
                    r.setBookTitle(obj.optString("bookTitle"));
                    r.setBookImageUrl(obj.optString("bookImageUrl"));
                    r.setBookAuthor(obj.optString("bookAuthor"));
                    r.setRequesterId(obj.optString("requesterId"));
                    r.setRequesterName(obj.optString("requesterName"));
                    r.setRequesterAvatar(obj.optString("requesterAvatar"));
                    r.setOwnerId(obj.optString("ownerId"));
                    r.setOwnerName(obj.optString("ownerName"));
                    r.setMessage(obj.optString("message"));
                    r.setStatus(obj.optString("status", Request.STATUS_PENDING));
                    r.setTimestamp(obj.optLong("timestamp", System.currentTimeMillis()));
                    localCache.add(r);
                }
            }
        } catch (Exception ignored) {}
    }

    private synchronized void saveLocalCacheToPrefs() {
        if (appContext == null) return;
        try {
            JSONArray arr = new JSONArray();
            for (Request r : localCache) {
                JSONObject obj = new JSONObject();
                obj.put("requestId", r.getRequestId());
                obj.put("bookId", r.getBookId());
                obj.put("bookTitle", r.getBookTitle());
                obj.put("bookImageUrl", r.getBookImageUrl());
                obj.put("bookAuthor", r.getBookAuthor());
                obj.put("requesterId", r.getRequesterId());
                obj.put("requesterName", r.getRequesterName());
                obj.put("requesterAvatar", r.getRequesterAvatar());
                obj.put("ownerId", r.getOwnerId());
                obj.put("ownerName", r.getOwnerName());
                obj.put("message", r.getMessage());
                obj.put("status", r.getStatus());
                obj.put("timestamp", r.getTimestamp());
                arr.put(obj);
            }
            SharedPreferences prefs = appContext.getSharedPreferences(PREF_NAME, Context.MODE_PRIVATE);
            prefs.edit().putString(PREF_KEY_CACHED_REQUESTS, arr.toString()).apply();
        } catch (Exception ignored) {}
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
        if (allRequestsListener != null) {
            allRequestsListener.remove();
            allRequestsListener = null;
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
