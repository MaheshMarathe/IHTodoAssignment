package com.example.maheshmarathe.ihtodos.view;

import android.app.AlarmManager;
import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.arch.lifecycle.ViewModelProviders;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.StringRes;
import android.support.design.widget.Snackbar;
import android.support.v4.app.NotificationCompat;
import android.support.v4.app.TaskStackBuilder;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.InputMethodManager;
import android.widget.ProgressBar;
import android.widget.Toast;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.adapter.TodoAdapter;
import com.example.maheshmarathe.ihtodos.model.Comment;
import com.example.maheshmarathe.ihtodos.model.Todo;
import com.example.maheshmarathe.ihtodos.notification.NotificationPublisher;
import com.example.maheshmarathe.ihtodos.viewmodel.MainActivityViewModel;
import com.firebase.ui.auth.AuthUI;
import com.firebase.ui.auth.ErrorCodes;
import com.firebase.ui.auth.IdpResponse;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.auth.FirebaseAuth;
import com.google.firebase.firestore.DocumentReference;
import com.google.firebase.firestore.DocumentSnapshot;
import com.google.firebase.firestore.FirebaseFirestore;
import com.google.firebase.firestore.FirebaseFirestoreException;
import com.google.firebase.firestore.Query;
import com.google.firebase.firestore.WriteBatch;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Collections;
import java.util.List;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;

import static com.example.maheshmarathe.ihtodos.AppConstants.LIMIT;
import static com.example.maheshmarathe.ihtodos.AppConstants.RC_SIGN_IN;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODOS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_COMMENTS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_ID;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_PROGRESS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_TITLE;
import static com.example.maheshmarathe.ihtodos.AppConstants.USER_ID;

public class MainActivity extends AppCompatActivity implements TodoAdapter.OnTodoSelectedListener, AddTodoDialogFragment.TodoListener {

    private static final String TAG = MainActivity.class.getSimpleName();

    @BindView(R.id.recyclerTodos)
    RecyclerView mTodosRecycler;

    @BindView(R.id.viewEmpty)
    ViewGroup mEmptyView;

    @BindView(R.id.progressLoading)
    ProgressBar mLoading;

    private FirebaseFirestore mFirestore;
    private Query mQuery;

    private TodoAdapter mAdapter;

    private MainActivityViewModel mViewModel;

    private AddTodoDialogFragment mAddTodoDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        ButterKnife.bind(this);

        // View model
        mViewModel = ViewModelProviders.of(this).get(MainActivityViewModel.class);

        // Enable Firestore logging
        FirebaseFirestore.setLoggingEnabled(true);

        // Firestore
        mFirestore = FirebaseFirestore.getInstance();
    }


    @OnClick(R.id.add_todo)
    public void onAddTodoClicked(View view) {
        mAddTodoDialog = new AddTodoDialogFragment();
        mAddTodoDialog.show(getSupportFragmentManager(), AddTodoDialogFragment.TAG);
    }

    @Override
    public void onStart() {
        super.onStart();

        // Start sign in if necessary
        if (shouldStartSignIn()) {
            startSignIn();
            return;
        } else {
            if (FirebaseAuth.getInstance().getCurrentUser() != null) {
                mLoading.setVisibility(View.VISIBLE);
                String uID = FirebaseAuth.getInstance().getCurrentUser().getUid();
                Log.d(TAG, uID);
                mQuery = mFirestore.collection(TODOS)
                        .whereEqualTo(USER_ID, uID)
                        .limit(LIMIT);
            }

            // RecyclerView
            mAdapter = new TodoAdapter(mQuery, this, MainActivity.this) {
                @Override
                protected void onDataChanged() {
                    // Show/hide content if the query returns empty.
                    mLoading.setVisibility(View.GONE);
                    if (getItemCount() == 0) {
                        mTodosRecycler.setVisibility(View.GONE);
                        mEmptyView.setVisibility(View.VISIBLE);
                    } else {
                        mTodosRecycler.setVisibility(View.VISIBLE);
                        mEmptyView.setVisibility(View.GONE);
                    }
                }

                @Override
                protected void onError(FirebaseFirestoreException e) {
                    // Show a snackbar on errors
                    Snackbar.make(findViewById(android.R.id.content),
                            "Error: check logs for info.", Snackbar.LENGTH_LONG).show();
                }
            };

            mTodosRecycler.setLayoutManager(new LinearLayoutManager(this));
            mTodosRecycler.setAdapter(mAdapter);
        }

        // Start listening for Firestore updates
        if (mAdapter != null) {
            mAdapter.startListening();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        if (mAdapter != null) {
            mAdapter.stopListening();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return super.onCreateOptionsMenu(menu);
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.menu_sign_out:
                FirebaseAuth.getInstance().signOut();
                startSignIn();
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (requestCode == RC_SIGN_IN) {
            IdpResponse response = IdpResponse.fromResultIntent(data);
            mViewModel.setIsSigningIn(false);

            if (resultCode != RESULT_OK) {
                if (response == null) {
                    // User pressed the back button.
                    finish();
                } else if (response.getError() != null
                        && response.getError().getErrorCode() == ErrorCodes.NO_NETWORK) {
                    showSignInErrorDialog(R.string.message_no_network);
                } else {
                    showSignInErrorDialog(R.string.message_unknown);
                }
            }
        }
    }

    @Override
    public void onTodoSelected(DocumentSnapshot todo) {
        // Go to the details page for the selected todo
        Intent intent = new Intent(this, TodoDetailActivity.class);
        intent.putExtra(TodoDetailActivity.KEY_TODO_ID, todo.getId());
        startActivity(intent);
    }

    @Override
    public void onTodoUpdate(Todo todo, String id) {
        mAddTodoDialog = new AddTodoDialogFragment();
        Bundle bundle = new Bundle();
        bundle.putParcelable(TODOS, todo);
        bundle.putString(TODO_ID, id);
        mAddTodoDialog.setArguments(bundle);
        mAddTodoDialog.show(getSupportFragmentManager(), AddTodoDialogFragment.TAG);
    }

    private boolean shouldStartSignIn() {
        return (!mViewModel.getIsSigningIn() && FirebaseAuth.getInstance().getCurrentUser() == null);
    }

    private void startSignIn() {
        // Sign in with FirebaseUI
        Intent intent = AuthUI.getInstance().createSignInIntentBuilder()
                .setAvailableProviders(Collections.singletonList(
                        new AuthUI.IdpConfig.EmailBuilder().build()))
                .setIsSmartLockEnabled(false)
                .build();

        startActivityForResult(intent, RC_SIGN_IN);
        mViewModel.setIsSigningIn(true);
    }


    private void showSignInErrorDialog(@StringRes int message) {
        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle(R.string.title_sign_in_error)
                .setMessage(message)
                .setCancelable(false)
                .setPositiveButton(R.string.option_retry, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        startSignIn();
                    }
                })
                .setNegativeButton(R.string.option_exit, new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialogInterface, int i) {
                        finish();
                    }
                }).create();

        dialog.show();
    }


    @Override
    public void onTodo(Todo todo) {
        // In a transaction, add the new todo and update the aggregate totals
        addTodo(todo);
    }

    @Override
    public void onTodoUpdated(Todo todo, String id) {
        updateTodo(todo, id);
    }

    private void updateTodo(final Todo todo, final String id) {
        DocumentReference contact = mFirestore.collection(TODOS).document(id);
        contact.update(TODO_TITLE, todo.getTitle());
        contact.update(TODO_PROGRESS, todo.getProgress())
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        Toast.makeText(MainActivity.this, "Updated Successfully",
                                Toast.LENGTH_SHORT).show();
                    }
                });
    }

    private void addTodo(final Todo todo) {
        WriteBatch batch = mFirestore.batch();
        DocumentReference todosRef = mFirestore.collection(TODOS).document();
        List<Comment> randomComments = new ArrayList<Comment>();

        // Add todo
        batch.set(todosRef, todo);

        // Add comments to sub collection
        for (Comment comment : randomComments) {
            batch.set(todosRef.collection(TODO_COMMENTS).document(), comment);
        }

        batch.commit().addOnCompleteListener(new OnCompleteListener<Void>() {
            @Override
            public void onComplete(@NonNull Task<Void> task) {
                if (task.isSuccessful()) {
                    Log.d(TAG, "todo added");

                    // Hide keyboard and scroll to top
                    hideKeyboard();
                    if (todo.getAlarmHour() != 0 && todo.getAlarmMinute() != 0) {
                        scheduleNotification(getNotification(todo), todo);
                    }
                    mTodosRecycler.smoothScrollToPosition(0);

                } else {
                    Log.w(TAG, "Add todo failed.", task.getException());

                    // Show failure message and hide keyboard
                    hideKeyboard();
                    Snackbar.make(findViewById(android.R.id.content), "Failed to add todo",
                            Snackbar.LENGTH_SHORT).show();
                }
            }
        });
    }

    private void scheduleNotification(Notification notification, Todo todo) {

        Intent notificationIntent = new Intent(this, NotificationPublisher.class);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION_ID, 1);
        notificationIntent.putExtra(NotificationPublisher.NOTIFICATION, notification);
        PendingIntent pendingIntent = PendingIntent.getBroadcast(this, 0, notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Calendar calendar = Calendar.getInstance();
        calendar.set(Calendar.HOUR_OF_DAY, todo.getAlarmHour());
        calendar.set(Calendar.MINUTE, todo.getAlarmMinute());
        calendar.set(Calendar.SECOND, 0);
        AlarmManager alarmManager = (AlarmManager) getSystemService(Context.ALARM_SERVICE);
        alarmManager.setRepeating(AlarmManager.RTC_WAKEUP, calendar.getTimeInMillis(), AlarmManager.INTERVAL_DAY, pendingIntent);
    }

    private Notification getNotification(Todo todo) {

        NotificationManager notificationManager = (NotificationManager) this.getSystemService(Context.NOTIFICATION_SERVICE);

        String CHANNEL_ID = "channel_01";
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.O) {


            CharSequence name = "channel";
            String Description = "This is channel";
            int importance = NotificationManager.IMPORTANCE_HIGH;
            NotificationChannel mChannel = new NotificationChannel(CHANNEL_ID, name, importance);
            mChannel.setDescription(Description);
            mChannel.enableLights(true);
            mChannel.setLightColor(Color.RED);
            mChannel.enableVibration(true);
            mChannel.setVibrationPattern(new long[]{100, 200, 300, 400, 500, 400, 300, 200, 400});
            mChannel.setShowBadge(false);
            notificationManager.createNotificationChannel(mChannel);
        }

        NotificationCompat.Builder builder = new NotificationCompat.Builder(this, CHANNEL_ID)
                .setSmallIcon(R.drawable.todo_list)
                .setContentTitle(todo.getTitle())
                .setContentText(todo.getProgress());

        Intent resultIntent = new Intent(this, MainActivity.class);
        TaskStackBuilder stackBuilder = TaskStackBuilder.create(this);
        stackBuilder.addParentStack(MainActivity.class);
        stackBuilder.addNextIntent(resultIntent);
        PendingIntent resultPendingIntent = stackBuilder.getPendingIntent(0, PendingIntent.FLAG_UPDATE_CURRENT);

        builder.setContentIntent(resultPendingIntent);
        return builder.build();
    }

    private void hideKeyboard() {
        View view = getCurrentFocus();
        if (view != null) {
            ((InputMethodManager) getSystemService(Context.INPUT_METHOD_SERVICE))
                    .hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}
