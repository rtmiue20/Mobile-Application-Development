package vn.edu.ueh.rtmiue20.project.ui.home;

import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ImageView;
import android.widget.TextView;

import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.RecyclerView;

import com.bumptech.glide.Glide;

import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.auth.FirebaseUser;
import vn.edu.ueh.rtmiue20.project.R;
import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.model.User;
import vn.edu.ueh.rtmiue20.project.data.repository.FavoriteRepository;
import vn.edu.ueh.rtmiue20.project.data.repository.UserRepository;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

public class BookAdapter extends RecyclerView.Adapter<BookAdapter.BookHolder> {
    public interface OnBookClick { void onClick(Book book); }

    private static final Map<String, String> userNamesCache = new ConcurrentHashMap<>();
    private List<Book> books = new ArrayList<>();
    private Set<String> favoritedBookIds = new HashSet<>();
    private String userDistanceToExchange = null;
    private final OnBookClick listener;

    public BookAdapter(OnBookClick listener) { this.listener = listener; }

    public void update(List<Book> list) {
        books = list;
        notifyDataSetChanged();
    }

    public void setFavoritedBookIds(Collection<String> ids) {
        this.favoritedBookIds = ids != null ? new HashSet<>(ids) : new HashSet<>();
        notifyDataSetChanged();
    }

    public void setUserDistanceToExchange(String distance) {
        this.userDistanceToExchange = distance;
        notifyDataSetChanged();
    }

    @NonNull
    @Override
    public BookHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_book, parent, false);
        return new BookHolder(v);
    }

    @Override
    public void onBindViewHolder(@NonNull BookHolder h, int position) {
        Book b = books.get(position);
        
        h.tvBookTitle.setText(b.getTitle() != null ? b.getTitle() : "");
        h.tvBookAuthor.setText(b.getAuthor() != null ? b.getAuthor() : "");
        
        String condition = b.getCondition();
        if (condition == null || condition.isEmpty()) {
            condition = "Mới";
        }
        h.tvCondition.setText(condition);
        
        String status = b.getStatus();
        if ("available".equals(status)) {
            h.tvExchangeType.setText("Miễn phí");
        } else if ("exchanging".equals(status)) {
            h.tvExchangeType.setText("Đang trao đổi");
        } else if (status != null && !status.isEmpty()) {
            h.tvExchangeType.setText(status);
        } else {
            h.tvExchangeType.setText("Miễn phí");
        }
        
        // Hiển thị tên người dùng đăng theo dữ liệu từ Firebase
        String ownerId = b.getOwnerId();
        if (ownerId != null && !ownerId.trim().isEmpty()) {
            // 1. Kiểm tra tài khoản đang đăng nhập
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser != null && currentUser.getUid().equals(ownerId) && currentUser.getDisplayName() != null && !currentUser.getDisplayName().trim().isEmpty()) {
                userNamesCache.put(ownerId, currentUser.getDisplayName());
            }

            // 2. Nếu đã có trong Cache thì hiển thị ngay
            if (userNamesCache.containsKey(ownerId)) {
                bindUserData(h, userNamesCache.get(ownerId));
            } else {
                // Đặt tag để bảo đảm an toàn khi RecyclerView tái sử dụng ViewHolder
                h.tvUserName.setTag(ownerId);
                bindUserData(h, "Đang tải...");

                // 3. Tải tên người dùng từ Firestore
                UserRepository.getInstance().getUserProfile(ownerId, new UserRepository.UserCallback() {
                    @Override
                    public void onSuccess(User user) {
                        String name = user.getFullName();
                        if (name == null || name.trim().isEmpty()) {
                            name = user.getNickname();
                        }
                        if (name == null || name.trim().isEmpty()) {
                            name = user.getEmail();
                        }
                        if (name == null || name.trim().isEmpty()) {
                            name = "Thành viên BookShare";
                        }

                        userNamesCache.put(ownerId, name);
                        if (ownerId.equals(h.tvUserName.getTag())) {
                            bindUserData(h, name);
                        }
                    }

                    @Override
                    public void onError(String errorMessage) {
                        String fallback = "Thành viên BookShare";
                        userNamesCache.put(ownerId, fallback);
                        if (ownerId.equals(h.tvUserName.getTag())) {
                            bindUserData(h, fallback);
                        }
                    }
                });
            }
        } else {
            bindUserData(h, "Ẩn danh");
        }

        if (b.getImageUrl() == null || b.getImageUrl().isEmpty()) {
            Glide.with(h.itemView.getContext())
                 .load(R.drawable.ic_bookshare_logo)
                 .into(h.ivBookCover);
        } else {
            Glide.with(h.itemView.getContext())
                 .load(b.getImageUrl())
                 .placeholder(R.drawable.ic_bookshare_logo)
                 .error(R.drawable.ic_bookshare_logo)
                 .into(h.ivBookCover);
        }

        h.itemView.setOnClickListener(v -> {
            // Tăng lượt xem (View) lên 1 khi người dùng bấm vào xem
            int newViews = b.getViewCount() + 1;
            b.setViewCount(newViews);
            if (h.tvViewCount != null) {
                h.tvViewCount.setText(String.valueOf(newViews));
            }
            android.app.Application app = (android.app.Application) v.getContext().getApplicationContext();
            new vn.edu.ueh.rtmiue20.project.data.repository.BookRepository(app).incrementViewCount(b.getId());

            listener.onClick(b);
        });

        View favPressTarget = (h.btnFavoriteContainer != null) ? h.btnFavoriteContainer : h.btnFavorite;
        vn.edu.ueh.rtmiue20.project.util.ViewPressEffectHelper.applyPressEffect(h.itemView, favPressTarget);
        // Trạng thái sách yêu thích (Favorite)
        boolean isFav = b.getId() != null && favoritedBookIds.contains(b.getId());
        int favCount = b.getFavoriteCount();
        if (isFav && favCount < 1) {
            favCount = 1;
            b.setFavoriteCount(1);
        }

        // Hiển thị số lượt xem (View)
        if (h.tvViewCount != null) {
            h.tvViewCount.setText(String.valueOf(b.getViewCount()));
        }

        // Hiển thị số lượt tim (Favorite)
        if (h.tvFavoriteCount != null) {
            h.tvFavoriteCount.setText(String.valueOf(favCount));
        }

        if (isFav) {
            h.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
            h.btnFavorite.setColorFilter(ContextCompat.getColor(h.itemView.getContext(), R.color.red_danger));
            h.btnFavorite.setTag(true);
            if (h.tvFavoriteCount != null) {
                h.tvFavoriteCount.setTextColor(ContextCompat.getColor(h.itemView.getContext(), R.color.red_danger));
            }
        } else {
            h.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
            h.btnFavorite.setColorFilter(ContextCompat.getColor(h.itemView.getContext(), R.color.outline));
            h.btnFavorite.setTag(false);
            if (h.tvFavoriteCount != null) {
                h.tvFavoriteCount.setTextColor(android.graphics.Color.parseColor("#4B5563"));
            }
        }

        View.OnClickListener favClickListener = v -> {
            FirebaseUser currentUser = FirebaseAuth.getInstance().getCurrentUser();
            if (currentUser == null) {
                Toast.makeText(v.getContext(), "Vui lòng đăng nhập để lưu bài yêu thích", Toast.LENGTH_SHORT).show();
                return;
            }
            if (b.getId() == null || b.getId().trim().isEmpty()) return;

            boolean currentlyFav = favoritedBookIds.contains(b.getId());
            if (currentlyFav) {
                favoritedBookIds.remove(b.getId());
                h.btnFavorite.setImageResource(R.drawable.ic_heart_outline);
                h.btnFavorite.setColorFilter(ContextCompat.getColor(v.getContext(), R.color.outline));
                h.btnFavorite.setTag(false);
                int newFavs = Math.max(0, b.getFavoriteCount() - 1);
                b.setFavoriteCount(newFavs);
                if (h.tvFavoriteCount != null) {
                    h.tvFavoriteCount.setText(String.valueOf(newFavs));
                    h.tvFavoriteCount.setTextColor(android.graphics.Color.parseColor("#4B5563"));
                }
                Toast.makeText(v.getContext(), "Đã xóa khỏi mục Yêu thích", Toast.LENGTH_SHORT).show();
            } else {
                favoritedBookIds.add(b.getId());
                h.btnFavorite.setImageResource(R.drawable.ic_heart_filled);
                h.btnFavorite.setColorFilter(ContextCompat.getColor(v.getContext(), R.color.red_danger));
                h.btnFavorite.setTag(true);
                int newFavs = Math.max(1, b.getFavoriteCount() + 1);
                b.setFavoriteCount(newFavs);
                if (h.tvFavoriteCount != null) {
                    h.tvFavoriteCount.setText(String.valueOf(newFavs));
                    h.tvFavoriteCount.setTextColor(ContextCompat.getColor(v.getContext(), R.color.red_danger));
                }
                Toast.makeText(v.getContext(), "Đã thêm vào mục Yêu thích", Toast.LENGTH_SHORT).show();
            }

            android.app.Application app = (android.app.Application) v.getContext().getApplicationContext();
            FavoriteRepository.getInstance(app).toggleFavorite(b.getId());
        };

        h.btnFavorite.setOnClickListener(favClickListener);
        if (h.btnFavoriteContainer != null) {
            h.btnFavoriteContainer.setOnClickListener(favClickListener);
        }
        if (h.layoutArticleStats != null) {
            h.layoutArticleStats.setOnClickListener(v -> {
                // Tiêu thụ sự kiện click trên thanh pill để không bị bấm nhầm vào card sách
            });
        }

        // Hiển thị khoảng cách từ người dùng đến UEH cơ sở B (279 Nguyễn Tri Phương)
        if (h.tvDistance != null) {
            String distanceStr = userDistanceToExchange;
            if (distanceStr == null || distanceStr.isEmpty()) {
                android.content.SharedPreferences prefs = h.itemView.getContext().getSharedPreferences("BookSharePrefs", android.content.Context.MODE_PRIVATE);
                float lat = prefs.getFloat("user_lat", 0f);
                float lng = prefs.getFloat("user_lng", 0f);
                if (lat != 0f && lng != 0f) {
                    distanceStr = vn.edu.ueh.rtmiue20.project.util.LocationHelper.getDistanceToUEHCampusB(lat, lng);
                    userDistanceToExchange = distanceStr;
                }
            }
            if (distanceStr != null && !distanceStr.isEmpty()) {
                h.tvDistance.setText(distanceStr);
            } else {
                h.tvDistance.setText("800m");
            }
        }
    }

    private void bindUserData(BookHolder h, String name) {
        if (name == null || name.trim().isEmpty()) {
            name = "Người dùng";
        }
        h.tvUserName.setText(name);
        String initial = String.valueOf(name.trim().charAt(0)).toUpperCase();
        h.tvUserAvatar.setText(initial);
    }

    @Override public int getItemCount() { return books.size(); }

    static class BookHolder extends RecyclerView.ViewHolder {
        ImageView ivBookCover;
        ImageView btnFavorite;
        View btnFavoriteContainer, layoutArticleStats;
        TextView tvBookTitle, tvBookAuthor, tvCondition;
        TextView tvUserAvatar, tvUserName, tvExchangeType;
        TextView tvDistance;
        TextView tvViewCount, tvFavoriteCount;
        
        BookHolder(View v) {
            super(v);
            ivBookCover = v.findViewById(R.id.ivBookCover);
            btnFavorite = v.findViewById(R.id.btnFavorite);
            btnFavoriteContainer = v.findViewById(R.id.btnFavoriteContainer);
            layoutArticleStats = v.findViewById(R.id.layoutArticleStats);
            tvBookTitle = v.findViewById(R.id.tvBookTitle);
            tvBookAuthor = v.findViewById(R.id.tvBookAuthor);
            tvCondition = v.findViewById(R.id.tvCondition);
            tvUserAvatar = v.findViewById(R.id.tvUserAvatar);
            tvUserName = v.findViewById(R.id.tvUserName);
            tvExchangeType = v.findViewById(R.id.tvExchangeType);
            tvDistance = v.findViewById(R.id.tvDistance);
            tvViewCount = v.findViewById(R.id.tvViewCount);
            tvFavoriteCount = v.findViewById(R.id.tvFavoriteCount);
        }
    }
}
