package com.armcomptech.akash.simpletimer4.stopwatch;

import android.annotation.SuppressLint;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.os.CountDownTimer;
import android.os.SystemClock;
import android.util.Log;
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
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.annotation.Nullable;
import androidx.constraintlayout.widget.ConstraintLayout;
import androidx.core.app.NotificationManagerCompat;
import androidx.fragment.app.Fragment;

import com.armcomptech.akash.simpletimer4.R;
import com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.gms.ads.MobileAds;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;
import java.util.TimerTask;

import static android.content.Context.INPUT_METHOD_SERVICE;
import static android.content.Context.MODE_PRIVATE;
import static com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity.disableFirebaseLogging;

public class stopwatchFragment extends Fragment {

    private static final int notification_id = 2;
    private FloatingActionButton mButtonStart;
    private FloatingActionButton mButtonPause;
    private FloatingActionButton mButtonReset;
    private FloatingActionButton mButtonLap;
    private TextView mMillis;
    private AutoCompleteTextView mTimerNameAutoComplete;
    private TextView mTimerNameTextView;
    private TextView mTextViewCountDown;
    private ListView lapListView;
    ConstraintLayout lapListViewConstraintLayout;
    LinearLayout buttonLinearLayout;

    boolean showNotification;
    private NotificationManagerCompat notificationManager;

    Chronometer chronometer;
    CountDownTimer countDownTimer;

    boolean stopWatchRunning = false;

    long pauseOffset = 0;
    ArrayList<String> lapTimeInfo = new ArrayList<>();
    ArrayList<Integer> lapTimeStamp = new ArrayList<>();
    ArrayList<String> timerName = new ArrayList<>();
    ArrayAdapter<String> lapListAdapter;

    @SuppressLint("StaticFieldLeak")
    private static TabbedActivity instance;

    java.util.Timer tempTimer;
    TimerTask tempTimerTask;
    private boolean watchIsReset = true;
    private boolean fragmentAttached;
    private FirebaseAnalytics mFirebaseAnalytics;

    private AdView banner_adView;
    AdRequest banner_adRequest;
    InterstitialAd mResetButtonInterstitialAd;

    public static stopwatchFragment newInstance() {
        return new stopwatchFragment();
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        instance = (TabbedActivity) requireContext();
        notificationManager = NotificationManagerCompat.from(requireContext());
        loadData();

        if (!disableFirebaseLogging) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(instance);
        }
    }

    @SuppressLint("SetTextI18n")
    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        setRetainInstance(true);
        View root = inflater.inflate(R.layout.fragment_stopwatch, container, false);

        banner_adView = (AdView) root.findViewById(R.id.banner_ad);
        banner_adView.setVisibility(View.GONE);
        if (!isRemovedAds()) {
            banner_adRequest = new AdRequest.Builder().build();
            banner_adView.loadAd(banner_adRequest);
            banner_adView.setAdListener(new AdListener() {
                @Override
                public void onAdLoaded() {
                    banner_adView.setVisibility(View.VISIBLE);
                    logFirebaseAnalyticsEvents("Loaded banner ad");
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    banner_adView.setVisibility(View.GONE);
                    logFirebaseAnalyticsEvents("Failed to load banner ad");
                }
            });
        }

        lapListAdapter = new ArrayAdapter<>(requireContext(), R.layout.listview_adapter, lapTimeInfo);

        lapListViewConstraintLayout = root.findViewById(R.id.lapListParentView);
        lapListViewConstraintLayout.setVisibility(View.GONE);
        buttonLinearLayout = root.findViewById(R.id.stopwatchButtonView);

        lapListView = root.findViewById(R.id.lapListView);
        lapListView.setAdapter(lapListAdapter);
        chronometer = root.findViewById(R.id.stopWatchText_view_countdown);
        mButtonStart = root.findViewById(R.id.stopWatchButton_start);
        mButtonStart.setVisibility(View.VISIBLE);
        mButtonPause = root.findViewById(R.id.stopWatchButton_pause);
        mButtonPause.setVisibility(View.GONE);
        mButtonReset = root.findViewById(R.id.stopWatchButton_reset);
        mButtonReset.setVisibility(View.INVISIBLE);
        mButtonLap = root.findViewById(R.id.stopWatchLapFloatingActionButton);
        mButtonLap.setVisibility(View.INVISIBLE);
        mTimerNameAutoComplete = root.findViewById(R.id.stopWatchAutoComplete);
        mTimerNameAutoComplete.setAdapter(new ArrayAdapter<>(
                requireActivity(), R.layout.timername_autocomplete_textview, timerName));
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
        mTimerNameTextView = root.findViewById(R.id.stopWatchTextView);
        mTimerNameTextView.setVisibility(View.INVISIBLE);
        setWithoutLapView();

        mButtonStart.setOnClickListener(v -> {
            startWatch();
            mButtonStart.setVisibility(View.GONE);
            mButtonPause.setVisibility(View.VISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.VISIBLE);
            mTimerNameTextView.setVisibility(View.VISIBLE);
            mTimerNameTextView.setText(getTimerName());
            mTimerNameAutoComplete.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.VISIBLE);

            logFirebaseAnalyticsEvents("Started Stopwatch");
        });

        mButtonPause.setOnClickListener(v -> {
            pauseWatch();
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.GONE);
            mButtonReset.setVisibility(View.VISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);

            logFirebaseAnalyticsEvents("Paused Stopwatch");
        });

        mButtonReset.setOnClickListener(v -> {
            resetWatch();
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.GONE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);
            mTimerNameTextView.setVisibility(View.INVISIBLE);
            mTimerNameAutoComplete.setVisibility(View.VISIBLE);
            mMillis.setText("000");
            setWithoutLapView();

            logFirebaseAnalyticsEvents("Reset Stopwatch");

            if (!isRemovedAds()) {
                if (mResetButtonInterstitialAd.isLoaded()) {
                    mResetButtonInterstitialAd.show();
                    logFirebaseAnalyticsEvents("Showed Ad");
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                    logFirebaseAnalyticsEvents("Ad not loaded");
                }
            }
        });

        mButtonLap.setOnClickListener(v -> {
            lapWatch();
            mButtonLap.setExpanded(true);
            setWithLapView();
        });

        if (!isRemovedAds()) {
            //noinspection deprecation
            MobileAds.initialize(getContext(),getString(R.string.admob_app_id));

            //reset button ad
            mResetButtonInterstitialAd = new InterstitialAd(requireContext());
            mResetButtonInterstitialAd.setAdUnitId(getString(R.string.resetButton_interstitial_ad_id));
            mResetButtonInterstitialAd.loadAd(new AdRequest.Builder().build());
        }

        return root;
    }

    public boolean isRemovedAds() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("shared preferences", MODE_PRIVATE);
        return sharedPreferences.getBoolean("removed_Ads", false);
    }

    public void startWatch() {
        instance.startService(new Intent(instance, stopwatchWithService.class));

        stopWatchRunning = true;
        watchIsReset = false;
        chronometer.setBase(SystemClock.elapsedRealtime() - pauseOffset);
        chronometer.start();

        tempTimer = new java.util.Timer();
        tempTimerTask = new TimerTask() {
            @Override
            public void run() {
                showNotification();
            }
        };
        tempTimer.scheduleAtFixedRate(tempTimerTask, 0, 1000); //show notificaiton every second

        chronometer.setOnChronometerTickListener(chronometer -> {
            if (countDownTimer != null) {
                countDownTimer.cancel();
            }
            countDownTimer = new CountDownTimer(1000, 1) {
                @Override
                public void onTick(long millisUntilFinished) {
                    if (stopWatchRunning) {
                        mMillis.setText(String.valueOf((SystemClock.elapsedRealtime() - chronometer.getBase()) % 1000));
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
        cancelNotification();
    }

    public void resetWatch() {
        chronometer.setBase(SystemClock.elapsedRealtime());
        pauseOffset = 0;
        watchIsReset = true;
        lapTimeInfo.clear();
        lapTimeStamp.clear();
        lapListViewConstraintLayout.setVisibility(View.GONE);
        cancelNotification();
    }

    public void lapWatch() {
        int lap;
        if (lapTimeInfo.isEmpty()) {
            lap = 1;
            lapListViewConstraintLayout.setVisibility(View.VISIBLE);
        } else {
            lap = lapTimeInfo.size() + 1;
        }

        StringBuilder stringBuilder = new StringBuilder();
        stringBuilder.append("Lap ").append(lap).append(" : ");
        stringBuilder.append(getTimeFormatted(SystemClock.elapsedRealtime() - chronometer.getBase()));
        stringBuilder.append("    ");
        if (lapTimeStamp.isEmpty()) {
            stringBuilder.append(getTimeFormatted(SystemClock.elapsedRealtime() - chronometer.getBase()));
        } else {
            stringBuilder.append(getTimeFormatted(SystemClock.elapsedRealtime() - chronometer.getBase() - lapTimeStamp.get(lapTimeStamp.size() - 1)));
        }
        lapTimeStamp.add((int) (SystemClock.elapsedRealtime() - chronometer.getBase()));
        lapTimeInfo.add(stringBuilder.toString());
        lapListAdapter.notifyDataSetChanged();
        lapListView.smoothScrollToPosition(lapTimeInfo.size());
    }

    private String getTimerName() {
        String timerName = mTimerNameAutoComplete.getText().toString();
        if (timerName.matches("")) {
            timerName = "General";
        }

        logFirebaseAnalyticsEvents("TimerName: " + timerName);
        return timerName;
    }

    public void setWithoutLapView() {
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(150));
////        layoutParams.height = 200;
//        buttonLinearLayout.setLayoutParams(layoutParams);
//        chronometer.setTextSize(dpToPx(30));
        lapListViewConstraintLayout.setVisibility(View.GONE);
    }

    public void setWithLapView() {
//        LinearLayout.LayoutParams layoutParams = new LinearLayout.LayoutParams(MATCH_PARENT, dpToPx(70));
////        layoutParams.height = 70;
//        buttonLinearLayout.setLayoutParams(layoutParams);
//        chronometer.setTextSize(dpToPx(25));
        lapListViewConstraintLayout.setVisibility(View.VISIBLE);
    }

    public int dpToPx(int dp) {
        float density = requireContext().getResources()
                .getDisplayMetrics()
                .density;
        return Math.round((float) dp * density);
    }

    public String getTimeFormatted(long milliseconds) {
        int hours = (int) (milliseconds / 1000) / 3600;
        int minutes = (int) ((milliseconds / 1000) % 3600) / 60;
        int seconds = (int) (milliseconds / 1000) % 60;

        String timeLeftFormatted;

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
            if (imm != null) {
                imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
            }
        }
    }

    private void loadData() {
        SharedPreferences sharedPreferences = requireContext().getSharedPreferences("shared preferences", MODE_PRIVATE);
        Gson gson = new Gson();

        String timerNameJson = sharedPreferences.getString("timerName", null);
        Type timerNameType = new TypeToken<ArrayList<String>>(){}.getType();
        timerName = gson.fromJson(timerNameJson, timerNameType);
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        showNotification = false;
        notificationManager.cancel(notification_id);
        if (tempTimer != null) {
            tempTimer.cancel();
        }
        if (tempTimerTask != null) {
            tempTimerTask.cancel();
        }
    }

    @Override
    public void onStop() {
        super.onStop();
        showNotification = true;
    }

    @Override
    public void onResume() {
        super.onResume();
        showNotification = false;
        notificationManager.cancel(notification_id);
        if (tempTimer != null) {
            tempTimer.cancel();
        }
        if (tempTimerTask != null) {
            tempTimerTask.cancel();
        }
    }

    @Override
    public void onPause() {
        super.onPause();
        showNotification = true;
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        super.onSaveInstanceState(outState);

        outState.putBoolean("watchIsReset", watchIsReset);
        outState.putBoolean("stopWatchRunning", stopWatchRunning);
        outState.putLong("pauseOffset", SystemClock.elapsedRealtime() - chronometer.getBase());
        outState.putStringArrayList("lapTimeInfo", lapTimeInfo);
        outState.putIntegerArrayList("lapTimeStamp", lapTimeStamp);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onViewStateRestored(@Nullable Bundle savedInstanceState) {
        super.onViewStateRestored(savedInstanceState);

        if (savedInstanceState == null) {
            return;
        }

        lapListView.setAdapter(lapListAdapter);

        watchIsReset = savedInstanceState.getBoolean("watchIsReset");
        stopWatchRunning = savedInstanceState.getBoolean("stopWatchRunning");
        lapTimeInfo = savedInstanceState.getStringArrayList("lapTimeInfo");
        lapTimeStamp = savedInstanceState.getIntegerArrayList("lapTimeStamp");

        if (watchIsReset) {
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.GONE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);
            mTimerNameTextView.setVisibility(View.INVISIBLE);
            mTimerNameAutoComplete.setVisibility(View.VISIBLE);
            mMillis.setText("000");
            setWithoutLapView();
        } else if (stopWatchRunning) {
            pauseOffset = savedInstanceState.getLong("pauseOffset");
            startWatch();

            //setup UI
            mButtonStart.setVisibility(View.GONE);
            mButtonPause.setVisibility(View.VISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonLap.setVisibility(View.VISIBLE);
            mTimerNameTextView.setVisibility(View.VISIBLE);
            mTimerNameTextView.setText(getTimerName());
            mTimerNameAutoComplete.setVisibility(View.INVISIBLE);

            if (lapTimeInfo.isEmpty()) {
                setWithoutLapView();
            } else {
                setWithLapView();
            }
        } else {
            mButtonStart.setVisibility(View.VISIBLE);
            mButtonPause.setVisibility(View.GONE);
            mButtonReset.setVisibility(View.VISIBLE);
            mButtonLap.setVisibility(View.INVISIBLE);
            mTimerNameTextView.setVisibility(View.VISIBLE);
            mTimerNameTextView.setText(getTimerName());
            mTimerNameAutoComplete.setVisibility(View.INVISIBLE);
            startWatch();
            pauseWatch();
            cancelNotification();

            if (lapTimeInfo.isEmpty()) {
                setWithoutLapView();
            } else {
                setWithLapView();
            }
        }
    }

    @Override
    public void onAttach(@NonNull Context context) {
        super.onAttach(context);
        fragmentAttached = true;
    }

    @Override
    public void onDetach() {
        super.onDetach();
        fragmentAttached = false;
    }

    public void showNotification() {
        if (!fragmentAttached || isDetached()) {
            return;
        }
        try {
            long elapsedMillis = SystemClock.elapsedRealtime() - chronometer.getBase();
            Intent intent1local = new Intent("stopwatchPlayer");
            intent1local.putExtra("notification", "updateNotification");
            intent1local.putExtra("timeLeft", String.valueOf(elapsedMillis));
            intent1local.putExtra("name", getTimerName());
            requireContext().sendBroadcast(intent1local);
        } catch (IllegalStateException exception) {
            logFirebaseAnalyticsEvents("Stopwatch notification failed");
        }
    }

    public void cancelNotification() {
        if (!fragmentAttached) {
            return;
        }
        synchronized (requireContext()) {
            Intent intent1local = new Intent("stopwatchPlayer");
            intent1local.putExtra("notification", "cancelNotification");
            requireContext().sendBroadcast(intent1local);
        }
    }

    public void logFirebaseAnalyticsEvents(String eventName) {
        if (!disableFirebaseLogging) {
            eventName = eventName.replace(" ", "_");
            eventName = eventName.replace(":", "");

            Bundle bundle = new Bundle();
            bundle.putString("Event", eventName);
            mFirebaseAnalytics.logEvent(eventName, bundle);
        }
    }
}