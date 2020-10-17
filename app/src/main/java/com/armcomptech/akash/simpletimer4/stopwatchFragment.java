package com.armcomptech.akash.simpletimer4;

import android.annotation.SuppressLint;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.inputmethod.EditorInfo;
import android.view.inputmethod.InputMethodManager;
import android.widget.ArrayAdapter;
import android.widget.AutoCompleteTextView;
import android.widget.Chronometer;
import android.widget.LinearLayout;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.fragment.app.Fragment;

import com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;

import java.util.ArrayList;
import java.util.Locale;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.view.ViewGroup.LayoutParams.MATCH_PARENT;
import static com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity.logFirebaseAnalyticsEvents;

public class stopwatchFragment extends Fragment {

    private FloatingActionButton mButtonStart;
    private FloatingActionButton mButtonPause;
    private FloatingActionButton mButtonReset;
    private FloatingActionButton mButtonLap;
    private ProgressBar mProgressBar;
    private TextView mMillis;
    private AutoCompleteTextView mTimerNameAutoComplete;
    private TextView mTimerNameTextView;
    private TextView mTextViewCountDown;
    private ListView lapListView;
    ConstraintLayout lapListViewConstraintLayout;
    ConstraintLayout buttonConstraintLayout;

    Chronometer chronometer;
    CountDownTimer countDownTimer;

    boolean stopWatchRunning = false;

    int milliseconds = 0;
    long pauseOffset = 0;
    ArrayList<String> lapTimeInfo = new ArrayList<>();
    ArrayAdapter<String> lapListAdapter;

    @SuppressLint("StaticFieldLeak")
    private static TabbedActivity instance;

    public static stopwatchFragment newInstance() {
        return new stopwatchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        instance = (TabbedActivity) requireContext();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        View root = inflater.inflate(R.layout.fragment_stopwatch, container, false);
        lapListAdapter = new ArrayAdapter<>(getContext(), R.layout.listview_adapter, lapTimeInfo);

        lapListViewConstraintLayout = root.findViewById(R.id.lapListParentView);
        lapListViewConstraintLayout.setVisibility(View.GONE);
        buttonConstraintLayout = root.findViewById(R.id.stopwatchButtonView);

        lapListView = root.findViewById(R.id.lapListView);
        lapListView.setAdapter(lapListAdapter);
        chronometer = root.findViewById(R.id.stopWatchText_view_countdown);
        mProgressBar = root.findViewById(R.id.stopWatchProgressBar);
        mProgressBar.setVisibility(View.INVISIBLE);
        mButtonStart = root.findViewById(R.id.stopWatchButton_start);
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonPause = root.findViewById(R.id.stopWatchButton_pause);
        mButtonPause.setVisibility(View.INVISIBLE);
        mButtonReset = root.findViewById(R.id.stopWatchButton_reset);
        mButtonReset.setVisibility(View.INVISIBLE);
        mButtonLap = root.findViewById(R.id.stopWatchLapFloatingActionButton);
        mButtonLap.setVisibility(View.INVISIBLE);
        mTimerNameAutoComplete = root.findViewById(R.id.stopWatchTimerNameAutoComplete);
//        mTimerNameAutoComplete.setAdapter(new ArrayAdapter<>(
//                this, R.layout.timername_autocomplete_textview, timerName));
        mTimerNameAutoComplete.setThreshold(0);
        mTimerNameAutoComplete.setOnEditorActionListener((v, actionId, event) -> {
            if (actionId == EditorInfo.IME_ACTION_GO
                    || actionId == EditorInfo.IME_ACTION_DONE) {
                closeKeyboard();
                return true;
            }
            return false;
        });

        mMillis = root.findViewById(R.id.stopWatchMillis);
        mTimerNameTextView = root.findViewById(R.id.stopWatchTimerNameTextView);
        mTimerNameTextView.setVisibility(View.INVISIBLE);
        setWithoutLapView();

        mButtonStart.setOnClickListener(v -> {
            startWatch();
            mButtonStart.setVisibility(View.INVISIBLE);
            mButtonPause.setVisibility(View.VISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.VISIBLE);
            if (!getTimerName().equals("")) {
                mTimerNameTextView.setVisibility(View.VISIBLE);
                mTimerNameTextView.setText(getTimerName());
            } else {
                mTimerNameTextView.setVisibility(View.GONE);
            }
            mTimerNameAutoComplete.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.VISIBLE);
        });

        mButtonPause.setOnClickListener(v -> {
            pauseWatch();
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.INVISIBLE);
            mButtonReset.setVisibility(View.VISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);
        });

        mButtonReset.setOnClickListener(v -> {
            resetWatch();
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.INVISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);
            mTimerNameTextView.setVisibility(View.INVISIBLE);
            mTimerNameAutoComplete.setVisibility(View.VISIBLE);
            setWithoutLapView();
        });

        mButtonLap.setOnClickListener(v -> {
            lapWatch();
            mButtonLap.setExpanded(true);
            setWithLapView();
        });

        return root;
    }

    public void startWatch() {
        stopWatchRunning = true;
        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        chronometer.start();

        chronometer.setOnChronometerTickListener(chronometer -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            countDownTimer = new CountDownTimer(1000, 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (stopWatchRunning) {
                        mMillis.setText(String.format(Locale.getDefault(), "%02d", 1000 - millisUntilFinished));
                    }
                }

                @Override
                public void onFinish() {
                }
            };
            countDownTimer.start();
        });
    }

    public void pauseWatch() {
        stopWatchRunning = false;
        chronometer.stop();
        pauseOffset = SystemClock.elapsedRealtime() - chronometer.getBase();
    }

    public void resetWatch() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        lapTimeInfo.clear();
        lapListViewConstraintLayout.setVisibility(View.GONE);
    }

    public void lapWatch() {
        int lap = 0;
        if (lapTimeInfo.isEmpty()) {
            lap = 1;
            lapListViewConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            lap = lapTimeInfo.size() + 1;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lap " + lap + " : ");
        stringBuilder.append(getTimeFormatted(SystemClock.elapsedRealtime() - chronometer.getBase()));
        getTimeFormatted(SystemClock.elapsedRealtime() - chronometer.getBase()); // use this to get format of the string
        lapTimeInfo.add(stringBuilder.toString());
        lapListAdapter.notifyDataSetChanged();
        lapListView.smoothScrollToPosition(lapTimeInfo.size());
    }

    private String getTimerName() {
        String timerName = mTimerNameAutoComplete.getText().toString();
        if (timerName.matches("")) {
            timerName = "";
        }

        logFirebaseAnalyticsEvents("TimerName: " + timerName);
        return timerName;
    }

    public void setWithoutLapView() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(150));
//        layoutParams.height = 200;
        buttonConstraintLayout.setLayoutParams(layoutParams);
        chronometer.setTextSize(dpToPx(30));
    }

    public void setWithLapView() {
        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(70));
//        layoutParams.height = 70;
        buttonConstraintLayout.setLayoutParams(layoutParams);
        chronometer.setTextSize(dpToPx(25));
    }

    public int dpToPx(int dp) {
        float density = getContext().getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }

    public String getTimeFormatted(long milliseconds) {
        int hours = (int) (milliseconds / 1000) / 3600;
        int minutes = (int) ((milliseconds / 1000) % 3600) / 60;
        int seconds = (int) (milliseconds / 1000) % 60;

        String timeLeftFormatted = "";

        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%d:%02d:%02d.%03d", hours, minutes, seconds, milliseconds % 1000);
        } else if (minutes > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d:%02d.%03d", minutes, seconds, milliseconds % 1000);
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(), "%02d.%03d", seconds, milliseconds % 1000);
        }
        return timeLeftFormatted;
    }

    private void closeKeyboard() {
        View view = instance.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) instance.getSystemService(INPUT_METHOD_SERVICE);
            assert imm != null;
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }
}