package com.armcomptech.akash.simpletimer4.multiTimer;

import android.annotation.SuppressLint;
import android.app.Notification;
import android.app.PendingIntent;
import android.content.ComponentName;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.graphics.Canvas;
import android.graphics.drawable.Drawable;
import android.os.Build;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.View;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.NotificationCompat;
import androidx.core.app.NotificationManagerCompat;
import androidx.core.content.ContextCompat;
import androidx.recyclerview.widget.DefaultItemAnimator;
import androidx.recyclerview.widget.DividerItemDecoration;
import androidx.recyclerview.widget.ItemTouchHelper;
import androidx.recyclerview.widget.LinearLayoutManager;
import androidx.recyclerview.widget.RecyclerView;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.armcomptech.akash.simpletimer4.EmailLogic.SendMailTask;
import com.armcomptech.akash.simpletimer4.R;
import com.armcomptech.akash.simpletimer4.Settings.SettingsActivity;
import com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity;
import com.armcomptech.akash.simpletimer4.Timer;
import com.armcomptech.akash.simpletimer4.buildTimer.buildTimer_Activity;
import com.armcomptech.akash.simpletimer4.statistics.StatisticsActivity;
import com.google.android.gms.ads.AdListener;
import com.google.android.gms.ads.AdRequest;
import com.google.android.gms.ads.AdView;
import com.google.android.gms.ads.LoadAdError;
import com.google.android.material.floatingactionbutton.ExtendedFloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Objects;

import it.xabaras.android.recyclerview.swipedecorator.RecyclerViewSwipeDecorator;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.App.MAIN_CHANNEL_ID;

public class MultiTimerActivity extends AppCompatActivity implements setNameAndTimerDialog.setTimerDialogListener, BillingProcessor.IBillingHandler {

    BillingProcessor bp;
    RecyclerView recyclerView;
    ExtendedFloatingActionButton addTimerFab;
    private final ArrayList<Timer> timers = new ArrayList<>();
    private final ArrayList<RecyclerView.ViewHolder> holders = new ArrayList<>();
    final static int GROUP_NOTIFICATION_ID = 1000;
    private FirebaseAnalytics mFirebaseAnalytics;

    private AdView banner_adView;
    AdRequest banner_adRequest;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_multi_timer);

        if (!isRemovedAds()) {
            banner_adView = (AdView) findViewById(R.id.banner_ad);
            banner_adRequest = new AdRequest.Builder().build();
            banner_adView.loadAd(banner_adRequest);
            banner_adView.setAdListener(new AdListener(){
                @Override
                public void onAdLoaded() {
                    banner_adView.setVisibility(View.VISIBLE);
                    logFirebaseAnalyticsEvents("Loaded banner ad");
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_USER_PORTRAIT);
                }

                @Override
                public void onAdFailedToLoad(LoadAdError loadAdError) {
                    banner_adView.setVisibility(View.GONE);
                    logFirebaseAnalyticsEvents("Failed to load banner ad");
                    setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_FULL_USER);
                }
            });
        }
        mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTitle("   Multi Timer");
        ActionBar actionBar = getSupportActionBar();
        assert actionBar != null;
        actionBar.setDisplayShowHomeEnabled(true);
        actionBar.setIcon(R.drawable.ic_video_library_white);

        bp = new BillingProcessor(this, getString(R.string.licence_key), this);
        bp.initialize();

        Timer tempTimer = new Timer(60 * 1000 , "one minute");
        timers.add(tempTimer);

        recyclerView = findViewById(R.id.multiTimerRecyclerView);
        recyclerView.setLayoutManager(new LinearLayoutManager(this));
        recyclerView.setItemAnimator(new DefaultItemAnimator());
        recyclerView.setAdapter(new MultiTimerAdapter(this, timers, holders));
        recyclerView.addItemDecoration(new DividerItemDecoration(this, DividerItemDecoration.VERTICAL));

        ItemTouchHelper itemTouchHelper = new ItemTouchHelper(new ItemTouchHelper.SimpleCallback(0, ItemTouchHelper.LEFT | ItemTouchHelper.RIGHT) {
            @Override
            public boolean onMove(@NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, @NonNull RecyclerView.ViewHolder target) {
                return false;
            }

            @Override
            public void onSwiped(@NonNull RecyclerView.ViewHolder viewHolder, int direction) {
                timers.get(viewHolder.getAdapterPosition()).clean();
                timers.remove(viewHolder.getAdapterPosition());
                holders.remove(viewHolder.getAdapterPosition());
                Objects.requireNonNull(recyclerView.getAdapter()).notifyItemRemoved(viewHolder.getAdapterPosition());
                NotificationManagerCompat.from(getApplicationContext()).cancel(viewHolder.getAdapterPosition() + 2); //cancel notification
            }

            @Override
            public void onChildDraw(@NonNull Canvas c, @NonNull RecyclerView recyclerView, @NonNull RecyclerView.ViewHolder viewHolder, float dX, float dY, int actionState, boolean isCurrentlyActive) {

                new RecyclerViewSwipeDecorator.Builder(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive)
                        .addBackgroundColor(ContextCompat.getColor(getApplicationContext(), R.color.deleteRed))
                        .addActionIcon(R.drawable.ic_baseline_delete_24)
                        .create()
                        .decorate();

                super.onChildDraw(c, recyclerView, viewHolder, dX, dY, actionState, isCurrentlyActive);
            }
        });
        itemTouchHelper.attachToRecyclerView(recyclerView);

        addTimerFab = findViewById(R.id.addTimerFloatingActionButton);
        addTimerFab.setOnClickListener(v -> openNameAndTimerDialog());
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
            addTimerFab.setTooltipText("Add Timer");
        }
    }

    public void openNameAndTimerDialog() {
        setNameAndTimerDialog setNameAndTimerDialog = new setNameAndTimerDialog(false, true, null, timers);
        setNameAndTimerDialog.show(getSupportFragmentManager(), "Set Name and Timer Here");
    }

    public void createNewTimerNameAndTime(String time, int hours, int minutes, int seconds, String name, boolean creatingNewTimer, boolean updateExistingTimer, MultiTimerAdapter.Item holder, ArrayList<Timer> timers){
        long millisInput;
        long finalSecond;

        if (time.length() == 0) {
            Toast.makeText(this, "Field can't be empty", Toast.LENGTH_SHORT).show();
            return;
        }

        if (!time.equals("null")) {
            long input = Long.parseLong(time);
            long hour = input / 10000;
            long minuteRaw = (input - (hour * 10000)) ;
            long minuteOne = minuteRaw / 1000;
            long minuteTwo = (minuteRaw % 1000) / 100;
            long minute = (minuteOne * 10) + minuteTwo;
            long second = input - ((hour * 10000) + (minute * 100));
            finalSecond = (hour * 3600) + (minute * 60) + second;

            millisInput = finalSecond * 1000;
            if (millisInput == 0) {
                Toast.makeText(this, "Please enter a positive number", Toast.LENGTH_SHORT).show();
                return;
            }
        } else {
            if (hours == 0 && minutes == 0 && seconds == 0){
                Toast.makeText(this, "Time can't be zero", Toast.LENGTH_SHORT).show();
                return;
            } else {
                millisInput = (hours * 3600000) + (minutes * 60000) + (seconds * 1000);
                finalSecond = millisInput/1000;
            }
        }

        if (creatingNewTimer) {
            timers.add(new Timer(finalSecond * 1000, name));
            Objects.requireNonNull(recyclerView.getAdapter()).notifyItemInserted(recyclerView.getAdapter().getItemCount() + 1);
        }
        if (updateExistingTimer) {
            timers.get(holder.getAdapterPosition()).setStartTimeInMillis(finalSecond * 1000);
            timers.get(holder.getAdapterPosition()).setTimeLeftInMillis(finalSecond * 1000);
            timers.get(holder.getAdapterPosition()).setTimerPlaying(false);
            timers.get(holder.getAdapterPosition()).setTimerPaused(false);
            timers.get(holder.getAdapterPosition()).setTimerIsDone(false);
            if (timers.get(holder.getAdapterPosition()).getCountDownTimer() != null) {
                timers.get(holder.getAdapterPosition()).getCountDownTimer().cancel();
                timers.get(holder.getAdapterPosition()).setCountDownTimer(null);
            }
            Objects.requireNonNull(recyclerView.getAdapter()).notifyDataSetChanged();
        }
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        menu.findItem(R.id.check_sound).setVisible(false);
        menu.findItem(R.id.multi_Timer_Mode).setVisible(false);
        menu.findItem(R.id.privacy_policy).setVisible(false);

        if (!isRemovedAds()) {
            menu.add(0, R.id.remove_Ads, 4, menuIconWithText(getResources().getDrawable(R.drawable.ic_baseline_remove_circle_outline_black), "Remove Ads"));
        }
        return true;
    }

    private CharSequence menuIconWithText(Drawable r, String title) {

        r.setBounds(0, 0, r.getIntrinsicWidth(), r.getIntrinsicHeight());
        SpannableString sb = new SpannableString("    " + title);
        ImageSpan imageSpan = new ImageSpan(r, ImageSpan.ALIGN_BOTTOM);
        sb.setSpan(imageSpan, 0, 1, Spannable.SPAN_EXCLUSIVE_EXCLUSIVE);

        return sb;
    }

    @SuppressLint("NonConstantResourceId")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();

        item.setChecked(!item.isChecked());

        switch (id) {
            case R.id.statistics_activity:
                logFirebaseAnalyticsEvents("Opened Statistics");
                startActivity(new Intent(this, StatisticsActivity.class));
                break;

            case R.id.timer_and_stopwatch:
                Intent intent = new Intent(this, TabbedActivity.class);
                intent.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                intent.putExtra("overrideActivityToOpen", true);
                startActivity(intent);
                break;

            case R.id.build_Timer_Mode:

                Intent intent2 = new Intent(this, buildTimer_Activity.class);
                intent2.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent2);
                break;

            case R.id.setting_activity:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.remove_Ads:
                bp.purchase(this, "remove_ads");
                if (bp.isPurchased("remove_ads")) {
                    removeAds();
                    Toast.makeText(this, "All advertisements removed!", Toast.LENGTH_SHORT).show();
                }
                break;

            case R.id.send_feedback:
                final EditText edittext = new EditText(this);
                AlertDialog.Builder alert = new AlertDialog.Builder(this);
                alert.setTitle("Send Feedback");
                alert.setMessage("How can this app be improved?");

                alert.setView(edittext);

                alert.setPositiveButton("Send", (dialog, whichButton) -> {
                    String feedback = String.valueOf(edittext.getText());
                    String subject = "Feedback for Timer Application";
                    List<String> toEmail = Collections.singletonList(getString(R.string.toEmail));
                    new SendMailTask(this).execute(getString(R.string.fromEmail), getString(R.string.fromPassword), toEmail, subject, feedback, new ArrayList<File>());
                    Toast.makeText(getApplicationContext(), "Feedback sent successfully", Toast.LENGTH_SHORT).show();
                });

                alert.setNegativeButton("Cancel", (dialog, whichButton) ->
                        Toast.makeText(getApplicationContext(), "Feedback was not sent", Toast.LENGTH_SHORT).show());

                alert.show();
                break;

            default:
                break;
        }
        return super.onOptionsItemSelected(item);
    }

    public boolean isRemovedAds() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        return sharedPreferences.getBoolean("removed_Ads", false);
    }

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId.equals("remove_ads")) {
            if (!isRemovedAds()) {
                Toast.makeText(this, "Removed Ads", Toast.LENGTH_SHORT).show();
            }
            removeAds();
        }
    }

    private void removeAds() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        SharedPreferences.Editor editor = sharedPreferences.edit();
        editor.putBoolean("removed_Ads", true);
        editor.apply();
    }

    @Override
    public void onPurchaseHistoryRestored() {
        bp.loadOwnedPurchasesFromGoogle();
        if (bp.isPurchased("remove_ads")) {
            removeAds();
            Toast.makeText(this, "All advertisements removed!", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        destroyAllTimerAndNotification();
        NotificationManagerCompat.from(this).cancel(GROUP_NOTIFICATION_ID); //cancel group notification
    }

    private void destroyAllTimerAndNotification() {
        int count = 0;
        for (Timer timer: timers) {
            timer.setShowNotification(false);
            NotificationManagerCompat.from(this).cancel(count + 2); //cancel notification
            if (timer.getCountDownTimer() != null) {
                timer.getCountDownTimer().cancel();
            }
            count++;
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        for (Timer timer: timers) {
            timer.setShowNotification(true);
        }
        showGroupNotification();
    }

    private void showGroupNotification() {
        int count = 0;
        for (Timer timer: timers) {
            if (timer.getStartTimeInMillis() != timer.getTimeLeftInMillis()) {
                if (!timer.getTimerPaused()) {
                    count += 1;
                }
            }
        }
        if (count < 2) {
            return;
        }

        Intent notificationIntent = new Intent(MultiTimerActivity.class.getName());
        notificationIntent.setComponent(new ComponentName("com.armcomptech.akash.simpletimer4", "com.armcomptech.akash.simpletimer4.multiTimer.MultiTimerActivity"));

        final PendingIntent pendingIntent = PendingIntent.getActivity(this, 0,
                notificationIntent, PendingIntent.FLAG_UPDATE_CURRENT);

        Notification summaryNotification =
                new NotificationCompat.Builder(this, MAIN_CHANNEL_ID)
                        .setSmallIcon(R.drawable.ic_video_library_black)
                        .setGroup("multiTimer")
                        .setGroupSummary(true)
                        .setAutoCancel(true)
                        .setFullScreenIntent(pendingIntent, true)
                        .setContentIntent(pendingIntent)
                        .build();
        NotificationManagerCompat.from(this).notify(GROUP_NOTIFICATION_ID, summaryNotification);
    }

    @Override
    protected void onResume() {
        super.onResume();
        for (Timer timer: timers) {
            timer.setShowNotification(false);
        }
        NotificationManagerCompat.from(this).cancel(GROUP_NOTIFICATION_ID); //cancel group notification
    }

    @Override
    protected void onPause() {
        super.onPause();
        for (Timer timer: timers) {
            timer.setShowNotification(true);
        }
        showGroupNotification();
    }

    public void logFirebaseAnalyticsEvents(String eventName) {
        if (!TabbedActivity.disableFirebaseLogging) {
            eventName = eventName.replace(" ", "_");
            eventName = eventName.replace(":", "");

            Bundle bundle = new Bundle();
            bundle.putString("Event", eventName);
            mFirebaseAnalytics.logEvent(eventName, bundle);
        }
    }
}