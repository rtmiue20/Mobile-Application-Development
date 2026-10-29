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

    public interface RequestCallback {
        void onSuccess();
        void onError(String errorMessage);
    }

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

        docRef.set(request)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        // Chỉ lưu cache khi Firestore ghi thành công
                        addOrUpdateLocalRequest(request);
                        if (request.getRequesterId() != null) {
                            sentLiveData.setValue(getLocalSent(request.getRequesterId()));
                        }
                        if (request.getOwnerId() != null) {
                            receivedLiveData.setValue(getLocalReceived(request.getOwnerId()));
                        }
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
                            android.util.Log.e("RequestRepository", "Firestore PERMISSION_DENIED cho 'requests': Rules chưa deploy hoặc không có quyền ghi.");
                            actionError.setValue("Lỗi quyền Firestore (PERMISSION_DENIED): Vui lòng kiểm tra và cập nhật Rules trên Firebase Console!");
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
        firestore.collection(COLLECTION_REQUESTS)
                .document(requestId)
                .update("status", newStatus)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void unused) {
                        updateLocalRequestStatus(requestId, newStatus);
                        actionSuccess.setValue(true);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        String msg = e.getMessage() != null ? e.getMessage() : "";
                        if (msg.contains("PERMISSION_DENIED")) {
                            android.util.Log.e("RequestRepository", "PERMISSION_DENIED khi cập nhật trạng thái Firestore: " + msg);
                            actionError.setValue("Lỗi quyền Firestore (PERMISSION_DENIED): Không thể cập nhật trạng thái.");
                        } else {
                            actionError.setValue("Cập nhật trạng thái thất bại: " + msg);
                        }
                    }
                });
    }

    /**
     * Xác nhận người nhận ĐÃ NHẬN ĐƯỢC SÁCH:
     * Cập nhật status thành completed, lưu receivedAt và completedTimestamp, xóa cờ chưa nhận.
     */
    public void confirmReceivedBook(final Request request, final RequestCallback callback) {
        if (request == null || request.getRequestId() == null) {
            if (callback != null) callback.onError("Yêu cầu không hợp lệ.");
            return;
        }

        long now = System.currentTimeMillis();
        request.setStatus(Request.STATUS_COMPLETED);
        request.setReceivedAt(now);
        request.setCompletedTimestamp(now);
        request.setReceiveReportedNotYet(false);
        request.setNotReceivedNote(null);

        addOrUpdateLocalRequest(request);
        actionSuccess.setValue(true);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", Request.STATUS_COMPLETED);
        updates.put("receivedAt", now);
        updates.put("completedTimestamp", now);
        updates.put("receiveReportedNotYet", false);
        updates.put("notReceivedNote", null);

        firestore.collection(COLLECTION_REQUESTS)
                .document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    actionSuccess.setValue(true);
                    if (callback != null) callback.onSuccess();

                    // Bắn FCM thông báo cho chủ sách biết người xin đã nhận sách
                    if (request.getOwnerId() != null && !request.getOwnerId().isEmpty()) {
                        firestore.collection("users").document(request.getOwnerId()).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc != null && doc.contains("fcmToken")) {
                                        String token = doc.getString("fcmToken");
                                        if (token != null && !token.isEmpty()) {
                                            String rName = request.getRequesterName() != null ? request.getRequesterName() : "Người nhận";
                                            String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
                                            vn.edu.ueh.rtmiue20.project.util.FCMSender.pushNotification(
                                                    token,
                                                    "Sách đã được nhận thành công!",
                                                    rName + " đã xác nhận nhận cuốn \"" + bTitle + "\" từ bạn."
                                            );
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("PERMISSION_DENIED")) {
                        String err = "Lỗi quyền Firestore (PERMISSION_DENIED): Không thể xác nhận nhận sách.";
                        android.util.Log.e("RequestRepository", err);
                        actionError.setValue(err);
                        if (callback != null) callback.onError(err);
                    } else {
                        actionError.setValue("Xác nhận nhận sách thất bại: " + msg);
                        if (callback != null) callback.onError(msg);
                    }
                });
    }

    /**
     * Báo cáo CHƯA NHẬN ĐƯỢC SÁCH:
     * Giữ nguyên status = accepted, lưu cờ receiveReportedNotYet = true và ghi chú tùy chọn.
     */
    public void reportNotReceivedBook(final Request request, final String note, final RequestCallback callback) {
        if (request == null || request.getRequestId() == null) {
            if (callback != null) callback.onError("Yêu cầu không hợp lệ.");
            return;
        }

        long now = System.currentTimeMillis();
        request.setReceiveReportedNotYet(true);
        request.setNotReceivedNote(note);
        request.setNotReceivedTimestamp(now);

        addOrUpdateLocalRequest(request);
        actionSuccess.setValue(true);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("receiveReportedNotYet", true);
        updates.put("notReceivedNote", note != null ? note : "");
        updates.put("notReceivedTimestamp", now);

        firestore.collection(COLLECTION_REQUESTS)
                .document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(unused -> {
                    actionSuccess.setValue(true);
                    if (callback != null) callback.onSuccess();

                    // Bắn FCM thông báo cho chủ sách biết
                    if (request.getOwnerId() != null && !request.getOwnerId().isEmpty()) {
                        firestore.collection("users").document(request.getOwnerId()).get()
                                .addOnSuccessListener(doc -> {
                                    if (doc != null && doc.contains("fcmToken")) {
                                        String token = doc.getString("fcmToken");
                                        if (token != null && !token.isEmpty()) {
                                            String rName = request.getRequesterName() != null ? request.getRequesterName() : "Người nhận";
                                            String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
                                            String detail = (note != null && !note.trim().isEmpty()) ? " (" + note.trim() + ")" : "";
                                            vn.edu.ueh.rtmiue20.project.util.FCMSender.pushNotification(
                                                    token,
                                                    "Thông báo giao nhận sách",
                                                    rName + " báo chưa nhận được cuốn \"" + bTitle + "\"" + detail + ". Vui lòng kiểm tra lại điểm hẹn."
                                            );
                                        }
                                    }
                                });
                    }
                })
                .addOnFailureListener(e -> {
                    String msg = e.getMessage() != null ? e.getMessage() : "";
                    if (msg.contains("PERMISSION_DENIED")) {
                        String err = "Lỗi quyền Firestore (PERMISSION_DENIED): Không thể báo cáo chưa nhận sách.";
                        android.util.Log.e("RequestRepository", err);
                        actionError.setValue(err);
                        if (callback != null) callback.onError(err);
                    } else {
                        actionError.setValue("Gửi phản hồi thất bại: " + msg);
                        if (callback != null) callback.onError(msg);
                    }
                });
    }

    /**
     * TC-RATING: Người nhận xác nhận đã nhận sách, đánh giá số sao (1-5★) và hoàn tất giao dịch.
     */
    public void completeAndRateRequest(final Request request, final float ratingValue, final String comment) {
        if (request == null || request.getRequestId() == null) return;

        long now = System.currentTimeMillis();
        request.setStatus(Request.STATUS_COMPLETED);
        request.setRated(true);
        request.setRatingValue(ratingValue);
        request.setRatingComment(comment);
        request.setCompletedTimestamp(now);
        if (request.getReceivedAt() <= 0) {
            request.setReceivedAt(now);
        }
        request.setReceiveReportedNotYet(false);
        request.setNotReceivedNote(null);

        addOrUpdateLocalRequest(request);
        actionSuccess.setValue(true);

        java.util.Map<String, Object> updates = new java.util.HashMap<>();
        updates.put("status", Request.STATUS_COMPLETED);
        updates.put("rated", true);
        updates.put("ratingValue", ratingValue);
        updates.put("ratingComment", comment != null ? comment : "");
        updates.put("completedTimestamp", now);
        updates.put("receivedAt", request.getReceivedAt());
        updates.put("receiveReportedNotYet", false);
        updates.put("notReceivedNote", null);

        firestore.collection(COLLECTION_REQUESTS)
                .document(request.getRequestId())
                .update(updates)
                .addOnSuccessListener(unused -> actionSuccess.setValue(true))
                .addOnFailureListener(e -> {
                    android.util.Log.w("RequestRepository", "Cập nhật Firestore completed thất bại: " + e.getMessage());
                });

        // Cập nhật điểm uy tín cho chủ sách (owner)
        if (request.getOwnerId() != null && !request.getOwnerId().isEmpty()) {
            UserRepository.getInstance().submitUserRating(request.getOwnerId(), ratingValue, comment, null);

            // Bắn FCM thông báo cho chủ sách
            firestore.collection("users").document(request.getOwnerId()).get()
                    .addOnSuccessListener(doc -> {
                        if (doc != null && doc.contains("fcmToken")) {
                            String token = doc.getString("fcmToken");
                            if (token != null && !token.isEmpty()) {
                                String rName = request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc";
                                String bTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
                                vn.edu.ueh.rtmiue20.project.util.FCMSender.pushNotification(
                                        token,
                                        "Giao dịch sách hoàn tất!",
                                        rName + " đã nhận cuốn \"" + bTitle + "\" và gửi tặng bạn " + ratingValue + "★ uy tín!"
                                );
                            }
                        }
                    });
        }
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
                    r.setRated(obj.optBoolean("rated", false));
                    r.setRatingValue((float) obj.optDouble("ratingValue", 5.0));
                    r.setRatingComment(obj.optString("ratingComment", ""));
                    r.setCompletedTimestamp(obj.optLong("completedTimestamp", 0));
                    r.setReceivedAt(obj.optLong("receivedAt", 0));
                    r.setReceiveReportedNotYet(obj.optBoolean("receiveReportedNotYet", false));
                    r.setNotReceivedNote(obj.optString("notReceivedNote", null));
                    r.setNotReceivedTimestamp(obj.optLong("notReceivedTimestamp", 0));
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
                obj.put("rated", r.isRated());
                obj.put("ratingValue", r.getRatingValue());
                obj.put("ratingComment", r.getRatingComment());
                obj.put("completedTimestamp", r.getCompletedTimestamp());
                obj.put("receivedAt", r.getReceivedAt());
                obj.put("receiveReportedNotYet", r.isReceiveReportedNotYet());
                if (r.getNotReceivedNote() != null) {
                    obj.put("notReceivedNote", r.getNotReceivedNote());
                }
                obj.put("notReceivedTimestamp", r.getNotReceivedTimestamp());
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
