package com.example.maheshmarathe.ihtodos.view;

import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.model.Comment;
import com.google.firebase.auth.FirebaseAuth;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

/**
 * Dialog Fragment containing comment form.
 */
public class CommentDialogFragment extends DialogFragment {

    public static final String TAG = CommentDialogFragment.class.getSimpleName();

    @BindView(R.id.comment_text)
    EditText mCommentText;

    interface CommentListener {

        void onComment(Comment comment);

    }

    private CommentListener mCommentListener;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_todo_comment, container, false);
        unbinder = ButterKnife.bind(this, v);
        return v;
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof CommentListener) {
            mCommentListener = (CommentListener) context;
        }
    }

    @Override
    public void onResume() {
        super.onResume();
        getDialog().getWindow().setLayout(
                ViewGroup.LayoutParams.MATCH_PARENT,
                ViewGroup.LayoutParams.WRAP_CONTENT);

    }

    @Override
    public void onDestroyView() {
        super.onDestroyView();

        if (unbinder != null) {
            unbinder.unbind();
        }
    }

    @OnClick(R.id.commentFormSubmit)
    public void onSubmitClicked(View view) {
        Comment comment = new Comment(
                FirebaseAuth.getInstance().getCurrentUser(),
                mCommentText.getText().toString());

        if (mCommentListener != null) {
            mCommentListener.onComment(comment);
        }

        dismiss();
    }

    @OnClick(R.id.commentFormCancel)
    public void onCancelClicked(View view) {
        dismiss();
    }
}
