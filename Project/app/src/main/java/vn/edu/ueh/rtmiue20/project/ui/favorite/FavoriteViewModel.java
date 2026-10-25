package vn.edu.ueh.rtmiue20.project.ui.favorite;

import android.app.Application;

import androidx.annotation.NonNull;
import androidx.lifecycle.AndroidViewModel;
import androidx.lifecycle.LiveData;

import java.util.List;

import vn.edu.ueh.rtmiue20.project.data.model.Book;
import vn.edu.ueh.rtmiue20.project.data.repository.FavoriteRepository;

public class FavoriteViewModel extends AndroidViewModel {

    private final FavoriteRepository repository;
    private LiveData<Boolean> isFavorited;
    private String currentBookId;

    public FavoriteViewModel(@NonNull Application application) {
        super(application);
        repository = FavoriteRepository.getInstance(application);
        repository.startSyncFavorites();
    }

    public LiveData<List<Book>> getFavoritedBooksOffline() {
        return repository.getFavoritedBooksOffline();
    }

    public LiveData<List<String>> getFavoriteBookIds() {
        return repository.getFavoriteBookIds();
    }

    public LiveData<Boolean> getIsFavorited(String bookId) {
        if (isFavorited == null || currentBookId == null || !currentBookId.equals(bookId)) {
            currentBookId = bookId;
            isFavorited = repository.checkIsFavorited(bookId);
        }
        return isFavorited;
    }

    public void toggleFavorite(String bookId) {
        repository.toggleFavorite(bookId);
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public void resetError() {
        repository.resetError();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListener();
        repository.stopSyncFavorites();
    }
}