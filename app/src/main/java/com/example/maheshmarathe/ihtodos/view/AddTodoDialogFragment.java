package com.example.maheshmarathe.ihtodos.view;

import android.app.TimePickerDialog;
import android.content.Context;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.DialogFragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.CompoundButton;
import android.widget.EditText;
import android.widget.Spinner;
import android.widget.Switch;
import android.widget.TimePicker;
import android.widget.Toast;

import com.example.maheshmarathe.ihtodos.R;
import com.example.maheshmarathe.ihtodos.model.Todo;
import com.google.firebase.auth.FirebaseAuth;

import java.util.Calendar;

import butterknife.BindView;
import butterknife.ButterKnife;
import butterknife.OnClick;
import butterknife.Unbinder;

import static com.example.maheshmarathe.ihtodos.AppConstants.TODOS;
import static com.example.maheshmarathe.ihtodos.AppConstants.TODO_ID;

/**
 * Dialog Fragment containing add todos form.
 */
public class AddTodoDialogFragment extends DialogFragment {

    public static final String TAG = AddTodoDialogFragment.class.getSimpleName();

    @BindView(R.id.todo_title)
    EditText mTodoTitle;

    @BindView(R.id.todo_progress)
    Spinner mProgress;

    @BindView(R.id.todo_form_add)
    Button mAdd;

    @BindView(R.id.todo_form_update)
    Button mUpdate;

    @BindView(R.id.todo_enable_daily_alarm)
    Switch mDailyAlarm;

    @BindView(R.id.select_time)
    EditText mSelectTime;
    /**
     * Holds todo instance.
     */
    private Todo mTodo;
    /**
     * Holds todo id.
     */
    private String id;
    /**
     * Holds alarm hours.
     */
    private int alarmHours = 0;
    /**
     * Holds alarm minutes.
     */
    private int alarmMinutes = 0;

    interface TodoListener {

        void onTodo(Todo todo);

        void onTodoUpdated(Todo todo, String id);

    }

    private TodoListener mTodoListener;

    private Unbinder unbinder;

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater,
                             @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.dialog_add_todo, container, false);
        unbinder = ButterKnife.bind(this, v);
        Bundle bundle = getArguments();
        if (bundle != null) {
            mTodo = (Todo) bundle.get(TODOS);
            id = bundle.getString(TODO_ID);
            mTodoTitle.setText(mTodo.getTitle());
            String[] progress = getResources().getStringArray(R.array.progress);
            int id = 0;
            for (int i = 0; i < progress.length; i++) {
                if (progress[i].equals(mTodo.getProgress())) {
                    id = i;
                    break;
                }
            }
            mProgress.setSelection(id);
            mUpdate.setVisibility(View.VISIBLE);
            mAdd.setVisibility(View.GONE);
            mDailyAlarm.setVisibility(View.GONE);
        } else {
            mUpdate.setVisibility(View.GONE);
            mAdd.setVisibility(View.VISIBLE);
            mDailyAlarm.setVisibility(View.VISIBLE);
        }
        mDailyAlarm.setOnCheckedChangeListener(new CompoundButton.OnCheckedChangeListener() {
            @Override
            public void onCheckedChanged(CompoundButton buttonView, boolean isChecked) {
                if (isChecked) {
                    mSelectTime.setVisibility(View.VISIBLE);
                } else {
                    mSelectTime.setVisibility(View.GONE);
                }
            }
        });

        mSelectTime.setFocusable(false);
        mSelectTime.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                showTimePicker();
            }
        });
        return v;
    }

    private void showTimePicker() {
        final Calendar c = Calendar.getInstance();
        int hour = c.get(Calendar.HOUR_OF_DAY);
        int minute = c.get(Calendar.MINUTE);

        // Launch Time Picker Dialog
        TimePickerDialog timePickerDialog = new TimePickerDialog(getActivity(),
                new TimePickerDialog.OnTimeSetListener() {
                    @Override
                    public void onTimeSet(TimePicker view, int hourOfDay, int minute) {
                        alarmHours = hourOfDay;
                        alarmMinutes = minute;
                        mSelectTime.setText(hourOfDay + ":" + minute);
                    }
                }, hour, minute, false);
        timePickerDialog.show();
    }

    @Override
    public void onAttach(Context context) {
        super.onAttach(context);

        if (context instanceof TodoListener) {
            mTodoListener = (TodoListener) context;
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

    @OnClick(R.id.todo_form_add)
    public void onSubmitClicked(View view) {
        if (!mTodoTitle.getText().toString().trim().equals("")) {
            if (!mDailyAlarm.isChecked() || !mSelectTime.getText().toString().equals("")) {
                Todo todo = new Todo(FirebaseAuth.getInstance().getCurrentUser().getUid(), mTodoTitle.getText().toString().trim(), mProgress.getSelectedItem().toString());
                todo.setAlarmMinute(alarmMinutes);
                todo.setAlarmHour(alarmHours);
                if (mTodoListener != null) {
                    mTodoListener.onTodo(todo);
                }
                dismiss();
            }else{
                Toast.makeText(getActivity(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show();
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show();
        }
    }

    @OnClick(R.id.todo_form_cancel)
    public void onCancelClicked(View view) {
        dismiss();
    }

    @OnClick(R.id.todo_form_update)
    public void onUpdateClicked(View view) {
        if (!mTodoTitle.getText().toString().trim().equals("")) {
            dismiss();
            if (mTodoListener != null) {
                mTodo.setTitle(mTodoTitle.getText().toString().trim());
                mTodo.setProgress(mProgress.getSelectedItem().toString().trim());
                mTodoListener.onTodoUpdated(mTodo, id);
            }
        } else {
            Toast.makeText(getActivity(), getString(R.string.enter_title), Toast.LENGTH_SHORT).show();
        }

    }
}
