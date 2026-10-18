package vn.edu.ueh.rtmiue20.project.ui.book;

import androidx.lifecycle.LiveData;
import androidx.lifecycle.ViewModel;

import vn.edu.ueh.rtmiue20.project.data.model.Comment;
import vn.edu.ueh.rtmiue20.project.data.repository.CommentRepository;

import java.util.List;

public class CommentViewModel extends ViewModel {

    private final CommentRepository repository;
    private LiveData<List<Comment>> commentsLiveData;
    private String currentBookId;

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

    // U: Cập nhật bình luận
    public void updateComment(String bookId, String commentId, String newContent) {
        repository.updateComment(bookId, commentId, newContent);
    }

    // D: Xóa bình luận
    public void deleteComment(String bookId, String commentId) {
        repository.deleteComment(bookId, commentId);
    }

    public LiveData<Boolean> getAddSuccess() { return repository.getAddSuccess(); }
    public LiveData<Boolean> getUpdateSuccess() { return repository.getUpdateSuccess(); }
    public LiveData<Boolean> getDeleteSuccess() { return repository.getDeleteSuccess(); }
    public LiveData<String> getError() { return repository.getError(); }

    public void resetStatus() {
        repository.resetStatus();
    }

    @Override
    protected void onCleared() {
        super.onCleared();
        repository.removeListener();
    }
}
