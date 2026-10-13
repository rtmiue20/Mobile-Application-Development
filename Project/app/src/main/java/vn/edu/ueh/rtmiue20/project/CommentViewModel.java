package vn.edu.ueh.rtmiue20.project;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import java.util.List;

public class CommentViewModel extends ViewModel {

    private final CommentRepository repository;
    private LiveData<List<Comment>> commentsLiveData;
    private String currentBookId;

    // Constructor khong tham so de dung duoc ViewModelProvider mac dinh
    public CommentViewModel() {
        repository = new CommentRepository();
    }

    public LiveData<List<Comment>> getComments(String bookId) {
        if (commentsLiveData == null || currentBookId == null || !currentBookId.equals(bookId)) {
            currentBookId = bookId;
            commentsLiveData = repository.getComments(bookId);
        }
        return commentsLiveData;
    }

    public void addComment(String bookId, Comment comment) {
        repository.addComment(bookId, comment);
    }

    public LiveData<Boolean> getAddSuccess() {
        return repository.getAddSuccess();
    }

    public LiveData<String> getError() {
        return repository.getError();
    }

    public void resetAddSuccess() {
        repository.resetAddSuccess();
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