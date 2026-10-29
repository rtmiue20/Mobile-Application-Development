package vn.edu.ueh.rtmiue20.project.ui.request;

import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.Color;
import android.text.format.DateUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AlertDialog;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;
import com.google.firebase.firestore.FirebaseFirestore;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Locale;

import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Request;
import vn.edu.ueh.rtmiue20.project.ui.book.BookDetailActivity;
import vn.edu.ueh.rtmiue20.project.util.FCMSender;
import vn.edu.ueh.rtmiue20.project.util.LocationHelper;
import vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper;

public class RequestAdapter extends RecyclerView.Adapter<RequestAdapter.RequestViewHolder> {

    public interface OnRequestActionListener {
        void onAccept(Request request);
        void onReject(Request request);
        void onCancel(Request request);
        void onCompleteAndRate(Request request);
        void onOwnerComplete(Request request);
        void onConfirmReceive(Request request);
    }

    private final List<Request> requests = new ArrayList<>();
    private final OnRequestActionListener listener;
    private boolean isSentTab;
    private boolean isAdminMode = false;
    private final SimpleDateFormat dateFormat = new SimpleDateFormat("HH:mm dd/MM/yyyy", Locale.getDefault());

    public RequestAdapter(boolean isSentTab, OnRequestActionListener listener) {
        this.isSentTab = isSentTab;
        this.listener = listener;
    }

    public void setAdminMode(boolean isAdminMode) {
        this.isAdminMode = isAdminMode;
    }

    public void setSentTab(boolean isSentTab) {
        this.isSentTab = isSentTab;
        this.requests.clear();
        notifyDataSetChanged();
    }

    public boolean isSentTabActive() {
        return isSentTab;
    }

    public void setRequests(List<Request> newRequests) {
        requests.clear();
        if (newRequests != null) {
            requests.addAll(newRequests);
        }
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public RequestViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View view = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_request, parent, false);
        return new RequestViewHolder(view);
    }

    @Override
    public void onBindViewHolder(@NonNull RequestViewHolder holder, int position) {
        final Request request = requests.get(position);
        final Context context = holder.itemView.getContext();

        holder.tvBookTitle.setText(request.getBookTitle() != null ? request.getBookTitle() : "Sách không tên");
        holder.tvMessage.setText(request.getMessage() != null ? request.getMessage() : "Xin chào, mình rất muốn nhận cuốn sách này!");

        // Cho phép bấm vào ảnh bìa sách hoặc tên sách để xem chi tiết
        View.OnClickListener openBookDetailListener = v -> {
            if (request.getBookId() != null && !request.getBookId().isEmpty()) {
                Intent detailIntent = new Intent(v.getContext(), BookDetailActivity.class);
                detailIntent.putExtra("bookId", request.getBookId());
                v.getContext().startActivity(detailIntent);
            }
        };
        holder.ivBookCover.setOnClickListener(openBookDetailListener);
        holder.tvBookTitle.setOnClickListener(openBookDetailListener);

        // Tác giả sách
        if (request.getBookAuthor() != null && !request.getBookAuthor().trim().isEmpty()) {
            holder.tvAuthorMock.setText(request.getBookAuthor());
            holder.tvAuthorMock.setVisibility(View.VISIBLE);
        } else {
            holder.tvAuthorMock.setVisibility(View.GONE);
        }

        // Tính toán khoảng cách đến điểm hẹn trao đổi: UEH Cơ sở B (279 Nguyễn Tri Phương, Q.10)
        SharedPreferences prefs = context.getSharedPreferences("BookSharePrefs", Context.MODE_PRIVATE);
        float lat = prefs.getFloat("user_lat", 0f);
        float lng = prefs.getFloat("user_lng", 0f);
        if (lat != 0f && lng != 0f) {
            String distStr = LocationHelper.getDistanceToUEHCampusB(lat, lng);
            holder.tvDistanceMock.setText("📍 Điểm hẹn: UEH Cơ sở B (" + distStr + ")");
        } else {
            holder.tvDistanceMock.setText("📍 Điểm hẹn: UEH Cơ sở B (279 Nguyễn Tri Phương, Q.10)");
        }

        // Load Book Cover
        if (request.getBookImageUrl() != null && !request.getBookImageUrl().isEmpty()) {
            Glide.with(context).load(request.getBookImageUrl()).into(holder.ivBookCover);
        } else {
            holder.ivBookCover.setImageResource(R.drawable.ic_bookshare_logo);
        }

        // Format Timestamp
        holder.tvTimestamp.setText(DateUtils.getRelativeTimeSpanString(
                request.getTimestamp(), System.currentTimeMillis(), DateUtils.MINUTE_IN_MILLIS));

        // TẨY TRẮNG GIAO DIỆN TRƯỚC KHI HIỂN THỊ LẠI
        if (holder.layoutPendingOwnerActions != null) holder.layoutPendingOwnerActions.setVisibility(View.GONE);
        if (holder.layoutPendingRequesterActions != null) holder.layoutPendingRequesterActions.setVisibility(View.GONE);
        holder.btnReject.setVisibility(View.GONE);
        holder.btnAccept.setVisibility(View.GONE);
        holder.btnCancelRequest.setVisibility(View.GONE);
        holder.btnRemind.setVisibility(View.GONE);
        holder.btnViewQR.setVisibility(View.GONE);
        if (holder.btnConfirmReceived != null) holder.btnConfirmReceived.setVisibility(View.GONE);
        if (holder.layoutReceivedStatus != null) holder.layoutReceivedStatus.setVisibility(View.GONE);
        if (holder.layoutNotReceivedNotice != null) holder.layoutNotReceivedNotice.setVisibility(View.GONE);
        if (holder.tvOwnerStatusNotice != null) holder.tvOwnerStatusNotice.setVisibility(View.GONE);
        if (holder.btnOwnerComplete != null) holder.btnOwnerComplete.setVisibility(View.GONE);
        if (holder.layoutRatingCompleted != null) holder.layoutRatingCompleted.setVisibility(View.GONE);
        if (holder.tvRatingCompletedComment != null) holder.tvRatingCompletedComment.setVisibility(View.GONE);
        if (holder.btnRateNow != null) holder.btnRateNow.setVisibility(View.GONE);
        holder.layoutAppointment.setVisibility(View.GONE);
        holder.tvBookTag.setVisibility(View.GONE);

        // Chi tiết điểm hẹn trao đổi sách
        if (holder.tvAppointmentDetail != null) {
            holder.tvAppointmentDetail.setText("Địa điểm trao đổi: Đại học Kinh Tế TP. Hồ Chí Minh - Cơ sở B (" + LocationHelper.UEH_CAMPUS_B_ADDRESS + ")");
        }

        // UI Logic dựa vào Admin mode hoặc Tab (isSentTab)
        if (isAdminMode) {
            // CHẾ ĐỘ ADMIN: Hiển thị đầy đủ thông tin giao dịch giữa Người xin và Chủ sách
            String personInfo = "Người xin: " + (request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc");
            if (request.getOwnerName() != null && !request.getOwnerName().isEmpty()) {
                personInfo += "  •  Chủ sách: " + request.getOwnerName();
            }
            holder.tvPersonName.setText(personInfo);

            if (request.getRequesterAvatar() != null && !request.getRequesterAvatar().isEmpty()) {
                Glide.with(context).load(request.getRequesterAvatar()).into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            holder.tvBookTag.setVisibility(View.VISIBLE);

            if (Request.STATUS_PENDING.equalsIgnoreCase(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Chờ kiểm duyệt", "#E65100", "#FFF3E0");
                if (holder.layoutPendingOwnerActions != null) holder.layoutPendingOwnerActions.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnAccept.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_ACCEPTED.equalsIgnoreCase(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Đã phê duyệt", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_COMPLETED.equalsIgnoreCase(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "✓ Đã hoàn thành", "#1B5E20", "#E8F5E9");
                if (request.isRated() && holder.layoutRatingCompleted != null) {
                    holder.layoutRatingCompleted.setVisibility(View.VISIBLE);
                    holder.tvRatingCompletedTitle.setText(String.format(Locale.US, "Đánh giá trao đổi: %.1f ★", request.getRatingValue()));
                    if (request.getRatingComment() != null && !request.getRatingComment().trim().isEmpty()) {
                        holder.tvRatingCompletedComment.setText("Nhận xét: \"" + request.getRatingComment().trim() + "\"");
                        holder.tvRatingCompletedComment.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                setupStatusBadge(holder.tvStatusBadge, "Đã từ chối", "#B71C1C", "#FFEBEE");
            }

        } else if (isSentTab) {
            // TAB "ĐÃ GỬI" (Người xin sách xem)
            holder.tvPersonName.setText("Gửi tới: " + (request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách"));
            holder.ivUserAvatar.setImageResource(R.drawable.ic_person);

            if (Request.STATUS_PENDING.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Chờ phản hồi", "#E65100", "#FFF3E0");
                if (holder.layoutPendingRequesterActions != null) holder.layoutPendingRequesterActions.setVisibility(View.VISIBLE);
                holder.btnCancelRequest.setVisibility(View.VISIBLE);
                holder.btnRemind.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_ACCEPTED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "✓ Đã chấp nhận", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE); // Hiện khối Điểm hẹn
                holder.btnViewQR.setVisibility(View.VISIBLE); // Hiện nút QR

                if (request.getReceivedAt() > 0) {
                    // Đã xác nhận nhận sách
                    if (holder.layoutReceivedStatus != null) {
                        holder.layoutReceivedStatus.setVisibility(View.VISIBLE);
                        holder.tvReceivedStatus.setText(context.getString(R.string.status_received_book_format, dateFormat.format(new Date(request.getReceivedAt()))));
                    }
                } else {
                    // Chưa xác nhận: Hiện nút "Xác nhận đã nhận sách" ngay dưới nút QR
                    if (holder.btnConfirmReceived != null) {
                        holder.btnConfirmReceived.setVisibility(View.VISIBLE);
                    }
                    // Nếu trước đó đã báo chưa nhận được sách
                    if (request.isReceiveReportedNotYet() && holder.layoutNotReceivedNotice != null) {
                        holder.layoutNotReceivedNotice.setVisibility(View.VISIBLE);
                        String note = request.getNotReceivedNote();
                        String noteDetail = (note != null && !note.trim().isEmpty()) ? note : "Chưa gặp được chủ sách tại điểm hẹn";
                        holder.tvNotReceivedNoticeText.setText(context.getString(R.string.status_user_reported_not_received, noteDetail));
                    }
                }
            } else if (Request.STATUS_COMPLETED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "✓ Đã hoàn thành", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE);

                if (holder.layoutReceivedStatus != null) {
                    holder.layoutReceivedStatus.setVisibility(View.VISIBLE);
                    long rTime = request.getReceivedAt() > 0 ? request.getReceivedAt() : request.getCompletedTimestamp();
                    String timeStr = rTime > 0 ? dateFormat.format(new Date(rTime)) : "Mới đây";
                    holder.tvReceivedStatus.setText(context.getString(R.string.status_received_book_format, timeStr));
                }

                if (holder.layoutRatingCompleted != null) {
                    holder.layoutRatingCompleted.setVisibility(View.VISIBLE);
                    if (request.isRated()) {
                        holder.tvRatingCompletedTitle.setText(String.format(Locale.US, "Bạn đã đánh giá: %.1f ★", request.getRatingValue()));
                        if (request.getRatingComment() != null && !request.getRatingComment().trim().isEmpty()) {
                            holder.tvRatingCompletedComment.setText("Nhận xét: \"" + request.getRatingComment().trim() + "\"");
                            holder.tvRatingCompletedComment.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.tvRatingCompletedTitle.setText("Chưa đánh giá chủ sách");
                        if (holder.btnRateNow != null) holder.btnRateNow.setVisibility(View.VISIBLE);
                    }
                }
            } else {
                setupStatusBadge(holder.tvStatusBadge, "✕ Đã từ chối", "#B71C1C", "#FFEBEE");
                holder.tvMessage.setText("Lý do: Sách đã được trao cho độc giả khác trước đó.");
            }

        } else {
            // TAB "YÊU CẦU ĐẾN" (Chủ sách xem)
            holder.tvPersonName.setText(request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc");
            if (request.getRequesterAvatar() != null && !request.getRequesterAvatar().isEmpty()) {
                Glide.with(context).load(request.getRequesterAvatar()).into(holder.ivUserAvatar);
            } else {
                holder.ivUserAvatar.setImageResource(R.drawable.ic_person);
            }

            // Hiện nhãn phụ của sách
            holder.tvBookTag.setVisibility(View.VISIBLE);

            if (Request.STATUS_PENDING.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Chờ phản hồi", "#E65100", "#FFF3E0");
                if (holder.layoutPendingOwnerActions != null) holder.layoutPendingOwnerActions.setVisibility(View.VISIBLE);
                holder.btnReject.setVisibility(View.VISIBLE);
                holder.btnAccept.setVisibility(View.VISIBLE);
            } else if (Request.STATUS_ACCEPTED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "Đã đồng ý", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE);

                // Yêu cầu 4: Hiển thị dòng trạng thái nhỏ phía chủ sách (Không có nút xác nhận ở phía này)
                if (request.isReceiveReportedNotYet() && holder.layoutNotReceivedNotice != null) {
                    holder.layoutNotReceivedNotice.setVisibility(View.VISIBLE);
                    String note = request.getNotReceivedNote();
                    String noteDetail = (note != null && !note.trim().isEmpty()) ? note : "Chưa gặp được tại điểm hẹn";
                    holder.tvNotReceivedNoticeText.setText(context.getString(R.string.status_owner_reported_not_received, noteDetail));
                } else if (request.getReceivedAt() > 0 && holder.tvOwnerStatusNotice != null) {
                    holder.tvOwnerStatusNotice.setVisibility(View.VISIBLE);
                    holder.tvOwnerStatusNotice.setText(context.getString(R.string.status_owner_received_format, dateFormat.format(new Date(request.getReceivedAt()))));
                } else if (holder.tvOwnerStatusNotice != null) {
                    holder.tvOwnerStatusNotice.setVisibility(View.VISIBLE);
                    holder.tvOwnerStatusNotice.setText(context.getString(R.string.status_owner_waiting_confirm));
                }
            } else if (Request.STATUS_COMPLETED.equals(request.getStatus())) {
                setupStatusBadge(holder.tvStatusBadge, "✓ Đã hoàn thành", "#1B5E20", "#E8F5E9");
                holder.layoutAppointment.setVisibility(View.VISIBLE);

                if (holder.tvOwnerStatusNotice != null) {
                    holder.tvOwnerStatusNotice.setVisibility(View.VISIBLE);
                    long rTime = request.getReceivedAt() > 0 ? request.getReceivedAt() : request.getCompletedTimestamp();
                    String timeStr = rTime > 0 ? dateFormat.format(new Date(rTime)) : "Mới đây";
                    holder.tvOwnerStatusNotice.setText(context.getString(R.string.status_owner_received_format, timeStr));
                }

                if (holder.layoutRatingCompleted != null) {
                    holder.layoutRatingCompleted.setVisibility(View.VISIBLE);
                    if (request.isRated()) {
                        holder.tvRatingCompletedTitle.setText(String.format(Locale.US, "Đánh giá từ bạn đọc: %.1f ★", request.getRatingValue()));
                        if (request.getRatingComment() != null && !request.getRatingComment().trim().isEmpty()) {
                            holder.tvRatingCompletedComment.setText("Nhận xét: \"" + request.getRatingComment().trim() + "\"");
                            holder.tvRatingCompletedComment.setVisibility(View.VISIBLE);
                        }
                    } else {
                        holder.tvRatingCompletedTitle.setText("Giao dịch hoàn tất • Chờ bạn đọc đánh giá");
                    }
                }
            } else {
                setupStatusBadge(holder.tvStatusBadge, "Đã từ chối", "#B71C1C", "#FFEBEE");
            }
        }

        // Bắt sự kiện Click
        holder.btnAccept.setOnClickListener(v -> {
            if (listener != null) listener.onAccept(request);
        });

        holder.btnReject.setOnClickListener(v -> {
            if (listener != null) listener.onReject(request);
        });

        holder.btnCancelRequest.setOnClickListener(v -> {
            if (listener != null) listener.onCancel(request);
        });

        holder.btnRemind.setOnClickListener(v -> {
            Context ctx = v.getContext();
            if (request.getOwnerId() != null && !request.getOwnerId().isEmpty()) {
                FirebaseFirestore.getInstance().collection("users").document(request.getOwnerId())
                        .get()
                        .addOnSuccessListener(doc -> {
                            if (doc != null && doc.exists()) {
                                String ownerToken = doc.getString("fcmToken");
                                if (ownerToken != null && !ownerToken.isEmpty()) {
                                    String requesterName = request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc";
                                    String bookTitle = request.getBookTitle() != null ? request.getBookTitle() : "sách";
                                    FCMSender.pushNotification(
                                            ownerToken,
                                            "Lời nhắc yêu cầu nhận sách",
                                            requesterName + " đang chờ phản hồi của bạn về cuốn \"" + bookTitle + "\"."
                                    );
                                }
                            }
                        });
            }
            Toast.makeText(ctx, "Đã gửi thông báo nhắc nhở đến chủ sách!", Toast.LENGTH_SHORT).show();
        });

        holder.btnViewQR.setOnClickListener(v -> showExchangeQrDialog(v.getContext(), request));

        if (holder.btnConfirmReceived != null) {
            holder.btnConfirmReceived.setOnClickListener(v -> {
                if (listener != null) listener.onConfirmReceive(request);
            });
        }

        if (holder.btnRateNow != null) {
            holder.btnRateNow.setOnClickListener(v -> {
                if (listener != null) listener.onCompleteAndRate(request);
            });
        }

        if (holder.btnOwnerComplete != null) {
            holder.btnOwnerComplete.setOnClickListener(v -> {
                if (listener != null) listener.onOwnerComplete(request);
            });
        }

        ViewPressEffectHelper.applyPressEffect(holder.btnViewQR, holder.btnConfirmReceived, holder.btnAccept, holder.btnReject, holder.btnCancelRequest, holder.btnRemind, holder.btnRateNow);
    }

    private void showExchangeQrDialog(Context context, Request request) {
        String reqCode = request.getRequestId() != null && request.getRequestId().length() >= 6
                ? request.getRequestId().substring(0, 6).toUpperCase()
                : "BS-UEH";
        new AlertDialog.Builder(context)
                .setTitle("Mã Xác Nhận Trao Đổi")
                .setMessage("Mã giao dịch: #" + reqCode + "\n\n" +
                        "Cuốn sách: " + (request.getBookTitle() != null ? request.getBookTitle() : "") + "\n" +
                        "Chủ sách: " + (request.getOwnerName() != null ? request.getOwnerName() : "Chủ sách") + "\n" +
                        "Người nhận: " + (request.getRequesterName() != null ? request.getRequesterName() : "Bạn đọc") + "\n\n" +
                        "📍 Điểm hẹn giao nhận:\nĐại học Kinh Tế TP. Hồ Chí Minh (Cơ sở B)\nĐịa chỉ: " + LocationHelper.UEH_CAMPUS_B_ADDRESS + "\n\nVui lòng đưa mã này cho chủ sách khi đến nhận trao đổi.")
                .setPositiveButton("Đã hiểu", null)
                .show();
    }

    private void setupStatusBadge(TextView badge, String text, String textColorHex, String bgColorHex) {
        badge.setText(text);
        badge.setTextColor(Color.parseColor(textColorHex));
        badge.setBackgroundColor(Color.parseColor(bgColorHex));
    }

    @Override
    public int getItemCount() { return requests.size(); }

    static class RequestViewHolder extends RecyclerView.ViewHolder {
        TextView tvTimestamp, tvBookTag, tvStatusBadge, tvBookTitle, tvAuthorMock;
        TextView tvPersonName, tvDistanceMock, tvMessage, tvAppointmentDetail;
        ImageView ivTimeIcon, ivBookCover, ivUserAvatar;
        LinearLayout layoutMessage, layoutAppointment, layoutActions, layoutRatingCompleted;
        View layoutPendingOwnerActions, layoutPendingRequesterActions, layoutReceivedStatus, layoutNotReceivedNotice;
        TextView tvReceivedStatus, tvNotReceivedNoticeText, tvOwnerStatusNotice;
        TextView btnReject, btnAccept, btnCancelRequest, btnRemind, btnViewQR, btnConfirmReceived, btnOwnerComplete, tvRatingCompletedTitle, tvRatingCompletedComment, btnRateNow;

        RequestViewHolder(@NonNull View itemView) {
            super(itemView);
            tvTimestamp = itemView.findViewById(R.id.tvTimestamp);
            tvBookTag = itemView.findViewById(R.id.tvBookTag);
            tvStatusBadge = itemView.findViewById(R.id.tvStatusBadge);
            tvBookTitle = itemView.findViewById(R.id.tvBookTitle);
            tvAuthorMock = itemView.findViewById(R.id.tvAuthorMock);
            tvPersonName = itemView.findViewById(R.id.tvPersonName);
            tvDistanceMock = itemView.findViewById(R.id.tvDistanceMock);
            tvMessage = itemView.findViewById(R.id.tvMessage);
            tvAppointmentDetail = itemView.findViewById(R.id.tvAppointmentDetail);
            ivTimeIcon = itemView.findViewById(R.id.ivTimeIcon);
            ivBookCover = itemView.findViewById(R.id.ivBookCover);
            ivUserAvatar = itemView.findViewById(R.id.ivUserAvatar);
            layoutMessage = itemView.findViewById(R.id.layoutMessage);
            layoutAppointment = itemView.findViewById(R.id.layoutAppointment);
            layoutActions = itemView.findViewById(R.id.layoutActions);
            layoutRatingCompleted = itemView.findViewById(R.id.layoutRatingCompleted);
            layoutPendingOwnerActions = itemView.findViewById(R.id.layoutPendingOwnerActions);
            layoutPendingRequesterActions = itemView.findViewById(R.id.layoutPendingRequesterActions);
            layoutReceivedStatus = itemView.findViewById(R.id.layoutReceivedStatus);
            tvReceivedStatus = itemView.findViewById(R.id.tvReceivedStatus);
            layoutNotReceivedNotice = itemView.findViewById(R.id.layoutNotReceivedNotice);
            tvNotReceivedNoticeText = itemView.findViewById(R.id.tvNotReceivedNoticeText);
            tvOwnerStatusNotice = itemView.findViewById(R.id.tvOwnerStatusNotice);
            btnReject = itemView.findViewById(R.id.btnReject);
            btnAccept = itemView.findViewById(R.id.btnAccept);
            btnCancelRequest = itemView.findViewById(R.id.btnCancelRequest);
            btnRemind = itemView.findViewById(R.id.btnRemind);
            btnViewQR = itemView.findViewById(R.id.btnViewQR);
            btnConfirmReceived = itemView.findViewById(R.id.btnConfirmReceived);
            btnOwnerComplete = itemView.findViewById(R.id.btnOwnerComplete);
            tvRatingCompletedTitle = itemView.findViewById(R.id.tvRatingCompletedTitle);
            tvRatingCompletedComment = itemView.findViewById(R.id.tvRatingCompletedComment);
            btnRateNow = itemView.findViewById(R.id.btnRateNow);
        }
    }
}
