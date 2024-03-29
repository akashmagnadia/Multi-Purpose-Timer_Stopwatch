package com.armcomptech.akash.simpletimer4.buildTimer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.os.Bundle;
import android.os.Handler;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import androidx.annotation.NonNull;
import androidx.appcompat.app.AppCompatActivity;
import androidx.recyclerview.widget.RecyclerView;

import com.armcomptech.akash.simpletimer4.R;
import com.armcomptech.akash.simpletimer4.TabbedView.TabbedActivity;
import com.google.android.material.floatingactionbutton.FloatingActionButton;
import com.google.firebase.analytics.FirebaseAnalytics;

import java.util.ArrayList;

import static com.armcomptech.akash.simpletimer4.buildTimer.buildTimer_Activity.clearFocusBuildTimer1;
import static com.armcomptech.akash.simpletimer4.buildTimer.buildTimer_Activity.isFocusedBuildTimer1;

public class BuildTimerAdapter extends RecyclerView.Adapter {

    private final Context context;
    private final ArrayList<BasicTimerInfo> timers;
    private FirebaseAnalytics mFirebaseAnalytics;

    public BuildTimerAdapter(Context context, ArrayList<BasicTimerInfo> timers) {
        this.context = context;
        this.timers = timers;

        if (TabbedActivity.isInProduction) {
            mFirebaseAnalytics = FirebaseAnalytics.getInstance(context);
        }
    }

    public static class Item extends RecyclerView.ViewHolder {
        FloatingActionButton edit_timer_button;
        FloatingActionButton delete_timer_button;
        TextView timer_info_textView;
        Item(@NonNull View itemView) {
            super(itemView);
            edit_timer_button = itemView.findViewById(R.id.edit_timer);
            delete_timer_button = itemView.findViewById(R.id.delete_timer);
            timer_info_textView = itemView.findViewById(R.id.timer_info_build_timer);
        }
    }

    @NonNull
    @Override
    public RecyclerView.ViewHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
        LayoutInflater inflater = LayoutInflater.from(context);
        View row = inflater.inflate(R.layout.build_timer_recycler_view, parent, false);
        return new BuildTimerAdapter.Item(row);
    }

    @SuppressLint("SetTextI18n")
    @Override
    public void onBindViewHolder(@NonNull RecyclerView.ViewHolder holder, int position) {
        int myPosition = holder.getBindingAdapterPosition();

        ((Item)holder).edit_timer_button.setOnClickListener(v -> {
            if (isFocusedBuildTimer1()) {
                clearFocusBuildTimer1();

                new Handler().postDelayed(() -> editTimer(holder), 1000);
            } else {
                editTimer(holder);
            }
        });

        ((Item)holder).delete_timer_button.setOnClickListener(v -> {
            try {
                timers.remove(myPosition);
                notifyItemRemoved(myPosition);
            } catch (IndexOutOfBoundsException e) {
                Bundle bundle = new Bundle();
                bundle.putString("Event", "Error");
                bundle.putString("Timer_Size", String.valueOf(timers.size()));
                bundle.putString("myPosition", String.valueOf(myPosition));
                bundle.putString("position", String.valueOf(position));
                bundle.putString("Location", "delete_timer_button");
                bundle.putString("Error_Type", "Index out of bound");

                mFirebaseAnalytics.logEvent("Error", bundle);
            }

        });

        String timerName = timers.get(myPosition).timerName;
        String timerTime = timers.get(myPosition).getTimeLeftFormatted();
        ((Item)holder).timer_info_textView.setText(timerName + " - " + timerTime);
    }

    private void editTimer(RecyclerView.ViewHolder holder) {
        setNameAndTimerDialogForBuildTimer setNameAndTimerDialogForBuildTimer = new setNameAndTimerDialogForBuildTimer(
                true,
                false,
                holder.getBindingAdapterPosition(),
                timers);
        setNameAndTimerDialogForBuildTimer.show( ((AppCompatActivity) context).getSupportFragmentManager(), "Set Name and Timer Here");
        notifyDataSetChanged();
    }

    @Override
    public int getItemCount() {
        return timers.size();
    }
}
