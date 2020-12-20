package com.armcomptech.akash.simpletimer4.buildTimer;

import android.annotation.SuppressLint;
import android.content.Intent;
import android.content.SharedPreferences;
import android.graphics.drawable.Drawable;
import android.os.Bundle;
import android.text.Spannable;
import android.text.SpannableString;
import android.text.style.ImageSpan;
import android.util.Log;
import android.view.Menu;
import android.view.MenuInflater;
import android.view.MenuItem;
import android.view.WindowManager;
import android.widget.EditText;
import android.widget.Toast;

import androidx.appcompat.app.ActionBar;
import androidx.appcompat.app.AlertDialog;
import androidx.appcompat.app.AppCompatActivity;

import com.anjlab.android.iab.v3.BillingProcessor;
import com.anjlab.android.iab.v3.TransactionDetails;
import com.armcomptech.akash.simpletimer4.EmailLogic.SendMailTask;
import com.armcomptech.akash.simpletimer4.R;
import com.armcomptech.akash.simpletimer4.Settings.SettingsActivity;
import com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity;
import com.armcomptech.akash.simpletimer4.multiTimer.MultiTimerActivity;
import com.armcomptech.akash.simpletimer4.statistics.StatisticsActivity;

import java.io.File;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TASK;
import static android.content.Intent.FLAG_ACTIVITY_CLEAR_TOP;
import static android.content.Intent.FLAG_ACTIVITY_NEW_TASK;
import static com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity.logFirebaseAnalyticsEvents;

public class buildTimer_Activity extends AppCompatActivity implements BillingProcessor.IBillingHandler{

    BillingProcessor bp;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_build_timer_);

        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        setTitle("   Build your timer");
        ActionBar actionBar = getSupportActionBar();
        if (actionBar != null) {
            actionBar.setDisplayShowHomeEnabled(true);
            actionBar.setIcon(R.drawable.ic_build_white);
        }

        bp = new BillingProcessor(this, getString(R.string.licence_key), this);
        bp.initialize();
    }

    @SuppressLint("UseCompatLoadingForDrawables")
    @SuppressWarnings("deprecation")
    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        MenuInflater inflater = getMenuInflater();
        inflater.inflate(R.menu.option_menu, menu);

        menu.findItem(R.id.check_sound).setVisible(false);
        menu.findItem(R.id.build_Timer_Mode).setVisible(false);
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

    public boolean isRemovedAds() {
        SharedPreferences sharedPreferences = getSharedPreferences("shared preferences", MODE_PRIVATE);
        return sharedPreferences.getBoolean("removed_Ads", false);
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

            case R.id.multi_Timer_Mode:
                Intent intent3 = new Intent(this, MultiTimerActivity.class);
                intent3.addFlags(FLAG_ACTIVITY_CLEAR_TOP | FLAG_ACTIVITY_CLEAR_TASK | FLAG_ACTIVITY_NEW_TASK);
                startActivity(intent3);
                break;

            case R.id.setting_activity:
                startActivity(new Intent(this, SettingsActivity.class));
                break;

            case R.id.remove_Ads:
                bp.purchase(this, "remove_ads");
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

    @Override
    public void onProductPurchased(String productId, TransactionDetails details) {
        if (productId.equals("remove_ads")) {
            removeAds();

            Toast.makeText(this, "Removed Ads", Toast.LENGTH_SHORT).show();
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
    }

    @Override
    public void onBillingError(int errorCode, Throwable error) {
        Log.d("Billing", "Something went wrong in billing. Errorcode : " + errorCode);
//        Toast.makeText(this, "Something went wrong", Toast.LENGTH_SHORT).show();
    }

    @Override
    public void onBillingInitialized() {

    }
}