package com.example.maheshmarathe.ihtodos.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.Snackbar;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.adapter.CommentAdapter;
import com.example.maheshmarathe.ihtodos.model.Comment;
import com.example.maheshmarathe.ihtodos.model.Todo;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.EventListener;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.ListenerRegistration;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.Transaction;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.maheshmarathe.ihtodos.AppConstants.LIMIT;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODOS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_COMMENTS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_COMMENTS_TIMESTAMP;

public class TodoDetailActivity extends AppCompatActivity
        implements EventListener<DocumentSnapshot>, CommentDialogFragment.CommentListener {
    /**
     * Tag for logging purpose.
     */
    private static final String TAG = TodoDetailActivity.class.getSimpleName();
    /**
     * Holds key for todo id.
     */
    public static final String KEY_TODO_ID = "key_todo_id";

    @BindView(R.id.todo_title)
    TextView mTodoTitle;

    @BindView(R.id.todo_progress)
    TextView mTodoProgress;

    @BindView(R.id.empty_comments)
    ViewGroup mEmptyView;

    @BindView(R.id.recycler_comments)
    RecyclerView mCommentsRecycler;

    @BindView(R.id.progressLoading)
    ProgressBar mLoading;
    /**
     * Holds add comment dialog.
     */
    private CommentDialogFragment mCommentDialog;
    /**
     * Holds fire store instance.
     */
    private FirebaseFirestore mFirestore;
    /**
     * Holds todos document reference.
     */
    private DocumentReference mTodoRef;
    /**
     * Holds todos registration listener
     */
    private ListenerRegistration mTodoRegistration;
    /**
     * Holds comments adapter.
     */
    private CommentAdapter mCommentAdapter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_todo_detail);
        ButterKnife.bind(this);

        // Get todos ID from extras
        String todoId = getIntent().getExtras().getString(KEY_TODO_ID);
        if (todoId == null) {
            throw new IllegalArgumentException("Must pass extra " + KEY_TODO_ID);
        }

        // Initialize Firestore
        mFirestore = FirebaseFirestore.getInstance();

        // Get reference to the todos
        mTodoRef = mFirestore.collection(TODOS).document(todoId);

        mLoading.setVisibility(View.VISIBLE);
        // Get Comments
        Query commentsQuery = mTodoRef
                .collection(TODO_COMMENTS)
                .orderBy(TODO_COMMENTS_TIMESTAMP, Query.Direction.DESCENDING)
                .limit(LIMIT);

        // RecyclerView
        mCommentAdapter = new CommentAdapter(commentsQuery) {
            @Override
            protected void onDataChanged() {
                if (getItemCount() == 0) {
                    mCommentsRecycler.setVisibility(View.GONE);
                    mEmptyView.setVisibility(View.VISIBLE);
                } else {
                    mCommentsRecycler.setVisibility(View.VISIBLE);
                    mEmptyView.setVisibility(View.GONE);
                }
            }
        };
        mCommentsRecycler.setLayoutManager(new LinearLayoutManager(this));
        mCommentsRecycler.setAdapter(mCommentAdapter);

        mCommentDialog = new CommentDialogFragment();
    }

    @Override
    public void onStart() {
        super.onStart();

        mCommentAdapter.startListening();
        mTodoRegistration = mTodoRef.addSnapshotListener(this);
    }

    @Override
    public void onStop() {
        super.onStop();

        mCommentAdapter.stopListening();

        if (mTodoRegistration != null) {
            mTodoRegistration.remove();
            mTodoRegistration = null;
        }
    }

    @Override
    public void finish() {
        super.finish();
    }

    /**
     * Listener for the Todo document ({@link #mTodoRef}).
     */
    @Override
    public void onEvent(DocumentSnapshot snapshot, FirebaseFirestoreException e) {
        if (e != null) {
            Log.w(TAG, "todo:onEvent", e);
            return;
        }

        onTodosLoaded(snapshot.toObject(Todo.class));
    }

    private void onTodosLoaded(Todo todo) {
        mLoading.setVisibility(View.GONE);
        mTodoTitle.setText(todo.getTitle());
        if(todo.getProgress().equalsIgnoreCase(getString(R.string.todo_todo))){
            mTodoProgress.setBackground(getResources().getDrawable(R.drawable.bg_todo));
        }else if(todo.getProgress().equalsIgnoreCase(getString(R.string.todo_done))){
            mTodoProgress.setBackground(getResources().getDrawable(R.drawable.bg_done));
        }else if(todo.getProgress().equalsIgnoreCase(getString(R.string.todo_inprogress))) {
            mTodoProgress.setBackground(getResources().getDrawable(R.drawable.bg_inprogress));
        }
        mTodoProgress.setText(todo.getProgress());
    }

    @OnClick(R.id.todo_details_back)
    public void onBackArrowClicked(View view) {
        onBackPressed();
    }

    @OnClick(R.id.add_comment)
    public void onAddCommentClicked(View view) {
        mCommentDialog.show(getSupportFragmentManager(), CommentDialogFragment.TAG);
    }

    @Override
    public void onComment(Comment comment) {
        // In a transaction, add the new comment and update the aggregate totals
        addComment(mTodoRef, comment)
                .addOnSuccessListener(this, new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Log.d(TAG, "Comment added");

                        // Hide keyboard and scroll to top
                        hideKeyboard();
                        mCommentsRecycler.smoothScrollToPosition(0);
                    }
                })
                .addOnFailureListener(this, new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Log.w(TAG, "Add comment failed", e);

                        // Show failure message and hide keyboard
                        hideKeyboard();
                        Snackbar.make(findViewById(android.R.id.content), "Failed to add comment",
                                Snackbar.LENGTH_SHORT).show();
                    }
                });
    }

    private Task<Void> addComment(final DocumentReference todoReference, final Comment comment) {
        // Create reference for new comment, for use inside the transaction
        final DocumentReference commentReference = todoReference.collection(TODO_COMMENTS).document();

        // In a transaction, add the new comment and update the aggregate totals
        return mFirestore.runTransaction(new Transaction.Function<Void>() {
            @Override
            public Void apply(Transaction transaction) throws FirebaseFirestoreException {
                Todo todo = transaction.get(todoReference).toObject(Todo.class);
                // Commit to Firestore
                transaction.set(todoReference, todo);
                transaction.set(commentReference, comment);

                return null;
            }
        });
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
