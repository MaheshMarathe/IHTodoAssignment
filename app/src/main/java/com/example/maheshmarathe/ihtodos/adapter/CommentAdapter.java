package com.example.maheshmarathe.ihtodos.adapter;

import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.model.Comment;
import com.google.firebase.firestore.Query;

import java.text.SimpleDateFormat;
import java.util.Locale;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView adapter for a list of {@link Comment}.
 */
public class CommentAdapter extends FirestoreAdapter<CommentAdapter.ViewHolder> {

    public CommentAdapter(Query query) {
        super(query);
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        return new ViewHolder(LayoutInflater.from(parent.getContext())
                .inflate(R.layout.item_todo_comment, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(getSnapshot(position).toObject(Comment.class));
    }

    static class ViewHolder extends RecyclerView.ViewHolder {

        private static final SimpleDateFormat FORMAT = new SimpleDateFormat(
                "MM/dd/yyyy", Locale.US);

        @BindView(R.id.comment_name)
        TextView nameView;

        @BindView(R.id.comment_text)
        TextView commentView;

        @BindView(R.id.comment_date)
        TextView dateView;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(Comment comment) {
            nameView.setText(comment.getUserName());
            commentView.setText(comment.getText());

            if (comment.getTimestamp() != null) {
                dateView.setText(FORMAT.format(comment.getTimestamp()));
            }
        }
    }

}
