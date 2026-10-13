package vn.edu.ueh.rtmiue20.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

public class FavoriteViewModel extends ViewModel {

    private final FavoriteRepository repository;
    private LiveData<Boolean> isFavorited;
    private String currentBookId;

    // Constructor khong tham so de dung duoc ViewModelProvider mac dinh
    public FavoriteViewModel() {
        repository = new FavoriteRepository();
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
    }
}