package com.example.maheshmarathe.ihtodos.adapter;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.model.Todo;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.Query;

import butterknife.BindView;
import butterknife.ButterKnife;

/**
 * RecyclerView adapter for a list of todos.
 */
public class TodoAdapter extends FirestoreAdapter<TodoAdapter.ViewHolder> {

    /**
     * Holds context.
     */
    private final Context mContext;

    public interface OnTodoSelectedListener {

        void onTodoSelected(DocumentSnapshot todo);

        void onTodoUpdate(Todo todo, String id);
    }

    private OnTodoSelectedListener mListener;

    public TodoAdapter(Query query, OnTodoSelectedListener listener, Context context) {
        super(query);
        mContext = context;
        mListener = listener;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(parent.getContext());
        return new ViewHolder(inflater.inflate(R.layout.item_todo, parent, false));
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.bind(getSnapshot(position), mListener);
    }

    class ViewHolder extends RecyclerView.ViewHolder {
        @BindView(R.id.todoTitle)
        TextView mTodoTitle;

        @BindView(R.id.todo_progress)
        TextView mTodoProgress;

        public ViewHolder(View itemView) {
            super(itemView);
            ButterKnife.bind(this, itemView);
        }

        public void bind(final DocumentSnapshot snapshot,
                         final OnTodoSelectedListener listener) {

            final Todo todo = snapshot.toObject(Todo.class);

            mTodoTitle.setText(todo.getTitle());
            mTodoProgress.setText(todo.getProgress());
            if (todo.getProgress().equalsIgnoreCase(mContext.getResources().getString(R.string.todo_todo))) {
                mTodoProgress.setBackground(mContext.getResources().getDrawable(R.drawable.bg_todo));
            } else if (todo.getProgress().equalsIgnoreCase(mContext.getResources().getString(R.string.todo_done))) {
                mTodoProgress.setBackground(mContext.getResources().getDrawable(R.drawable.bg_done));
            } else if (todo.getProgress().equalsIgnoreCase(mContext.getResources().getString(R.string.todo_inprogress))) {
                mTodoProgress.setBackground(mContext.getResources().getDrawable(R.drawable.bg_inprogress));
            }
            mTodoProgress.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    listener.onTodoUpdate(todo, snapshot.getId());
                }
            });

            itemView.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    if (listener != null) {
                        listener.onTodoSelected(snapshot);
                    }
                }
            });
        }

    }
}
