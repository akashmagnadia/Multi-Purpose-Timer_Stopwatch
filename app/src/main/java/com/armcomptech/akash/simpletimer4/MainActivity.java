package com.armcomptech.akash.simpletimer4;

import android.content.Intent;
import android.graphics.Color;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Build;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.content.SharedPreferences;
import android.os.CountDownTimer;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.view.inputmethod.InputMethodManager;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ProgressBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.InterstitialAd;
import com.google.android.gms.ads.MobileAds;
import com.google.gson.Gson;
import com.google.gson.reflect.TypeToken;

import java.lang.reflect.Type;
import java.util.ArrayList;
import java.util.Locale;

import static java.util.logging.Level.parse;

public class MainActivity extends AppCompatActivity implements  ExampleDialog.ExmapleDialogListner{

    private TextView mTextViewCountDown;
    private Button mButtonStartPause;
    private Button mButtonReset;
    private ProgressBar mProgressBar;
    private Button mButtonSetTimer;
    private CountDownTimer mCountDownTimer;
    private TextView mMillis;
    private EditText mTimerNameEditText;
    private TextView mTimerNameTextView;

    private boolean mTimerRunning;
    private boolean BlinkTimerStopRequest;
    private boolean heartbeatChecked;
    private boolean soundChecked;

    private long mStartTimeInMillis;
    private long mTimeLeftInMillis;
    private long mEndTime;
    private int alternate;

    MediaPlayer player;
    private InterstitialAd mInterstitialAd;

    ArrayList<String> timerNames = new ArrayList<String>();
    ArrayList<Integer> count = new ArrayList<Integer>();
    ArrayList<Integer> timeInSeconds = new ArrayList<Integer>();
//    String[] timerName = {"timerName", "timerName2"};


    public String currentTimerName;
    public int currentTimerNamePosition;
    public int ticksToPass;
    public int counter;


    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        //ad stuff
        MobileAds.initialize(this,getString(R.string.admob_app_id));

        mInterstitialAd = new InterstitialAd(this);
        mInterstitialAd.setAdUnitId(getString(R.string.interstital_ad_id));
        mInterstitialAd.loadAd(new AdRequest.Builder().build());

        mButtonSetTimer = findViewById(R.id.setTimer);
        mProgressBar = findViewById(R.id.progressBar);
        mTextViewCountDown = findViewById(R.id.text_view_countdown);
        mButtonStartPause = findViewById(R.id.button_start_pause);
        mButtonReset = findViewById(R.id.button_reset);
        mTimerNameEditText = findViewById(R.id.timerNameEditText);

        mTimerNameTextView = findViewById(R.id.timerNameTextView);
        mTimerNameTextView.setVisibility(View.INVISIBLE );

        mButtonSetTimer.setBackgroundColor(Color.TRANSPARENT);
        mMillis = findViewById(R.id.millis);

        mButtonSetTimer.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                openDialog();
            }
        });

        mButtonStartPause.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mTimerRunning) {
                    pauseTimer();
                } else {
                    startTimer();
                    counter = 0;

                    //only update during the start
                    if (mProgressBar.getProgress() == mStartTimeInMillis) {
                        currentTimerName = getTimerName();

                        //get position of timer name and -1 if it doesn't exist
                        currentTimerNamePosition = timerNamePosition(currentTimerName, timerNames);

                        if (currentTimerNamePosition == -1) {
                            timerNames.add(currentTimerName);
                            count.add(1);
                            timeInSeconds.add(0);
                            currentTimerNamePosition = timeInSeconds.size() - 1; //make a new position since adding new value which is at the end
                        } else {
                            //increment count
                            count.set(currentTimerNamePosition, count.get(currentTimerNamePosition) + 1);
                        }
                        saveData(); //save data

                        //just to be safe because sometimes second is one less in statistics
                        if (mStartTimeInMillis >= 4000) { //when timer is set more than 4 seconds
                            timeInSeconds.set(currentTimerNamePosition, timeInSeconds.get(currentTimerNamePosition) + 1);
                        }

                        //update interface to show timer name
                        mTimerNameTextView.setVisibility(View.VISIBLE);
                        mTimerNameTextView.setText(currentTimerName);
                        mTimerNameEditText.setVisibility(View.INVISIBLE);
                    }
                }
            }
        });

        mButtonReset.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                resetTimer();

                mTimerNameTextView.setVisibility(View.INVISIBLE);
                mTimerNameEditText.setVisibility(View.VISIBLE);

                if (mInterstitialAd.isLoaded()) {
                    mInterstitialAd.show();
                } else {
                    Log.d("TAG", "The interstitial wasn't loaded yet.");
                }
            }
        });
        heartbeatChecked = true;
        soundChecked = true;
    }

    private String getTimerName() {
        String timerName = mTimerNameEditText.getText().toString();
        if (timerName.matches("")) {
            timerName = "General";
        }
        return timerName;
    }

    private int timerNamePosition(String currentTimerName, ArrayList<String> timerNames) {
        if (timerNames == null) {
            return -1;
        }

        for (int i = 0; i < timerNames.size(); i++) {
            if (timerNames.get(i).matches(currentTimerName)) {
                return i;
            }
        }
        return -1;
    }

    private void saveData() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        Gson gson = new Gson();

        String timerNameJson = gson.toJson(timerNames);
        editor.putString("timer name", timerNameJson);

        String countJson = gson.toJson(count);
        editor.putString("count", countJson);

        String timeInSecondsJson = gson.toJson(timeInSeconds);
        editor.putString("timeInSeconds", timeInSecondsJson);

        editor.apply();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        if (item.isChecked()) {
            item.setChecked(false);
        } else {
            item.setChecked(true);
        }

        switch (id) {
            case R.id.check_heartbeat:
                heartbeatChecked = !heartbeatChecked;

                //refresh the heartbeat sound
                mButtonStartPause.performClick();
                mButtonStartPause.performClick();
                break;

            case R.id.check_sound:
                soundChecked = !soundChecked;
                break;

            case R.id.statistics_activity:
                startActivity(new Intent(this, statisticsActiivty.class));
                break;

            case R.id.privacy_policy:
                Intent myWebLink = new Intent(android.content.Intent.ACTION_VIEW);
                myWebLink.setData(Uri.parse("https://docs.google.com/document/d/18fpGAZNNOtF02_4no_UD208NmVh_mk6YPTaQBwilxwM/edit#heading=h.ojpotbumetb"));
                startActivity(myWebLink);
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public void timeUp() {
        if (soundChecked) {
            if (player != null) {
                stopPlayer();
            } else {
                player = MediaPlayer.create(this, R.raw.tune1);
                player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                    @Override
                    public void onCompletion(MediaPlayer mp) {
                        player.seekTo(0);
                        player.start();
                    }
                });
            }
            player.start();
        }

        alternate = 0;
        BlinkTimerStopRequest = false;
        Thread blink = new Thread() {
            @Override
            public void run() {
                while((!isInterrupted()) && (!BlinkTimerStopRequest)) {
                    try {
                        Thread.sleep(400);

                        runOnUiThread(new Runnable() {
                            @Override
                            public void run() {
                                if (alternate % 2 == 0) {
                                    alternate++;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        mProgressBar.setProgress(0, false);
                                    }
                                }
                                else {
                                    alternate++;
                                    if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                                        mProgressBar.setProgress((int)mStartTimeInMillis, false);
                                    }
                                }
                            }
                        });
                    } catch (InterruptedException e) {
                        e.printStackTrace();
                    }
                }
            }
        };
        blink.start();
    }

    public void heartbeat() {
        if (heartbeatChecked) {

            player = MediaPlayer.create(this, R.raw.heartbeatnormal2);
            player.setOnCompletionListener(new MediaPlayer.OnCompletionListener() {
                @Override
                public void onCompletion(MediaPlayer mp) {
                    player.seekTo(0);
                    player.start();
                }
            });
            player.start();
        }
    }

    private void setBlinkTimerStopRequest() {
        BlinkTimerStopRequest = true;
    }

    private void stopPlayer() {
        if (player != null) {
            player.release();
            player = null;
//            Toast.makeText(this, "Song stopped", Toast.LENGTH_SHORT).show();
        }
    }

    private void openDialog() {
        ExampleDialog exampleDialog = new ExampleDialog();
        exampleDialog.show(getSupportFragmentManager(), "Set Timer Here");
    }

    public void applyText(String time){

        long input = Long.parseLong(time);
        long hour = input / 10000;
        long minuteraw = (input - (hour * 10000)) ;
        long minuteone = minuteraw / 1000;
        long minutetwo = (minuteraw % 1000) / 100;
        long minute = (minuteone * 10) + minutetwo;
        long second = input - ((hour * 10000) + (minute * 100));
        long finalsecond = (hour * 3600) + (minute * 60) + second;

        if (time.length() == 0) {
            Toast.makeText(MainActivity.this, "Field can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        //long millisInput = Long.parseLong(time) * 1000;
        long millisInput = finalsecond * 1000;
        if (millisInput == 0) {
            Toast.makeText(MainActivity.this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
            return;
        }

        setTime(millisInput);
    }

    private void setTime(long milliseconds) {
        mStartTimeInMillis = milliseconds;
        resetTimer();
        closeKeyboard();
    }

    private void startTimer() {
        mEndTime = System.currentTimeMillis() + mTimeLeftInMillis;
        heartbeat();
        int countDownInterval = 100;
        if (mStartTimeInMillis <= 30000) {
            countDownInterval = 50;
        }

        ticksToPass = 1000 / countDownInterval;

        mCountDownTimer = new CountDownTimer(mTimeLeftInMillis, countDownInterval) {
            @Override
            public void onTick(long millisUntilFinished) {
                mTimeLeftInMillis = millisUntilFinished;
                updateCountDownText();

                //basically increment by one every second
                counter++;
                if (ticksToPass == counter) {
                    timeInSeconds.set(currentTimerNamePosition, timeInSeconds.get(currentTimerNamePosition) + 1);
                    saveData();
                    counter = 0;
                }
            }

            @Override
            public void onFinish() {
                mTimerRunning = false;
                updateWatchInterface();
                mTimeLeftInMillis = 0;
                mMillis.setText("000");
                if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
                    mProgressBar.setProgress(0, true);
                }
                stopPlayer();
                timeUp();
            }
        }.start();

        mTimerRunning = true;
        updateWatchInterface();
    }

    private void pauseTimer() {
        mCountDownTimer.cancel();
        mTimerRunning = false;
        updateWatchInterface();
        stopPlayer();
    }

    private void resetTimer() {
        mTimeLeftInMillis = mStartTimeInMillis;
        updateCountDownText();
        updateWatchInterface();
        mButtonStartPause.setBackgroundResource(R.drawable.playicon);
        setBlinkTimerStopRequest();
        stopPlayer();
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressBar.setProgress((int)mStartTimeInMillis,true);
        }
    }


    private void updateCountDownText() {
        int hours = (int) (mTimeLeftInMillis / 1000) / 3600;
        int minutes = (int) ((mTimeLeftInMillis / 1000) % 3600) / 60;
        int seconds = (int) (mTimeLeftInMillis / 1000) % 60;

        String timeLeftFormatted;

        if (hours > 0) {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%d:%02d:%02d", hours, minutes, seconds);
            mTextViewCountDown.setTextSize(60);
            mMillis.setTextSize(25);
            if (hours > 9) {
                mTextViewCountDown.setTextSize(54);
                mMillis.setTextSize(30);
            }
        } else {
            timeLeftFormatted = String.format(Locale.getDefault(),
                    "%02d:%02d", minutes, seconds);
            mTextViewCountDown.setTextSize(70);
            mMillis.setTextSize(30);
        }

        String millisFormatted;
        millisFormatted = String.format(Locale.getDefault(), "%02d", (mTimeLeftInMillis % 1000));


        mTextViewCountDown.setText(timeLeftFormatted);
        mMillis.setText(millisFormatted);
        mProgressBar.setMax((int)mStartTimeInMillis);
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.N) {
            mProgressBar.setProgress((int)mTimeLeftInMillis,true);
        }


    }

    private void updateWatchInterface() {
        if (mTimerRunning) {
            mButtonSetTimer.setVisibility(View.INVISIBLE);
            mButtonReset.setVisibility(View.INVISIBLE);
            mButtonStartPause.setBackgroundResource(R.drawable.pauseicon6);
        } else {
            mButtonSetTimer.setVisibility(View.VISIBLE);
            if (mCountDownTimer != null)
            {
                mButtonStartPause.setBackgroundResource(R.drawable.playicon);
            } else {
                mButtonStartPause.setBackgroundResource(R.drawable.playicon);
            }

            if (mTimeLeftInMillis < 100) {
                mButtonStartPause.setVisibility(View.INVISIBLE);
            } else {
                mButtonStartPause.setVisibility(View.VISIBLE);
            }

            if (mTimeLeftInMillis < mStartTimeInMillis) {
                mButtonReset.setVisibility(View.VISIBLE);
            } else {
                mButtonReset.setVisibility(View.INVISIBLE);
            }
        }
    }

    private void closeKeyboard() {
        View view = this.getCurrentFocus();
        if (view != null) {
            InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
            imm.hideSoftInputFromWindow(view.getWindowToken(), 0);
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        stopPlayer();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);
        SharedPreferences.Editor editor = prefs.edit();

        editor.putLong("startTimeInMillis", mStartTimeInMillis);
        editor.putLong("millisLeft", mTimeLeftInMillis);
        editor.putBoolean("timerRunning", mTimerRunning);
        editor.putLong("endTime", mEndTime);

        editor.apply();

        if (mCountDownTimer != null) {
            mCountDownTimer.cancel();
        }
    }

    @Override
    protected void onStart() {
        super.onStart();

        SharedPreferences prefs = getSharedPreferences("prefs", MODE_PRIVATE);

        mStartTimeInMillis = prefs.getLong("startTimeInMillis", 600000);
        mTimeLeftInMillis = prefs.getLong("millisLeft", mStartTimeInMillis);
        mTimerRunning = prefs.getBoolean("timerRunning", false);

        updateCountDownText();
        updateWatchInterface();

        if (mTimerRunning) {
            mEndTime = prefs.getLong("endTime", 0);
            mTimeLeftInMillis = mEndTime - System.currentTimeMillis();

            if (mTimeLeftInMillis < 0) {
                mTimeLeftInMillis = 0;
                mTimerRunning = false;
                updateCountDownText();
                updateWatchInterface();
            } else {
                startTimer();
            }
        }
    }
}
