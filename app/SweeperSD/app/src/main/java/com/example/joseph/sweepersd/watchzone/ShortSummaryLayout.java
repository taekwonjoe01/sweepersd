package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.content.Intent;
import android.os.Bundle;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

public class ShortSummaryLayout extends RelativeLayout {

    private WatchZoneModel mWatchZoneModel = null;
    private SummaryAction mSummaryAction = SummaryAction.None;
    private int mUpdatingProgress = -1;

    public enum SummaryAction {
        None,
        Save,
        Customize
    }

    public interface SummaryLayoutCallback {
        void onSummaryActionClicked();
        void onLayoutClicked();
        void onMoreInfoClicked();
    }

    private TextView mLabel;
    private ProgressBar mProgressBar;
    private ImageView mStatusIcon;
    private TextView mStatusText;
    private LinearLayout mSummaryDetails;
    private TextView mSummaryPostedLimits;
    private TextView mSummaryStreets;
    private RelativeLayout mLayoutMoreInfo;
    private Button mActionButton;

    private SummaryLayoutCallback mCallback;

    private Observer<WatchZoneModel> mObserver;

    public ShortSummaryLayout(Context context) {
        super(context);
        init();
    }

    public ShortSummaryLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShortSummaryLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_watchzone_summary_short, this);

        mLabel = findViewById(R.id.text_watch_zone_label);
        mProgressBar = findViewById(R.id.progressbar_updating_watchzone);
        mStatusIcon = findViewById(R.id.imageview_status_icon);
        mStatusText = findViewById(R.id.textview_status);
        mSummaryDetails = findViewById(R.id.layout_summary_details);
        mSummaryPostedLimits = findViewById(R.id.textview_number_posted_limits);
        mSummaryStreets = findViewById(R.id.textview_number_streets);
        mLayoutMoreInfo = findViewById(R.id.layout_more_info_group);
        mActionButton = findViewById(R.id.button_summary_action);
    }

    public void setWatchZoneModel(WatchZoneModel watchZoneModel) {
        mWatchZoneModel = watchZoneModel;
        setPresentation();
    }

    public void setSummaryAction(SummaryAction summaryAction) {
        mSummaryAction = summaryAction;
        setPresentation();
    }

    public void setUpdatingProgress(int progress) {
        mUpdatingProgress = progress;
        setPresentation();
    }

    public void set(WatchZoneModel watchZoneModel, SummaryAction summaryAction, int progress) {
        mWatchZoneModel = watchZoneModel;
        mSummaryAction = summaryAction;
        mUpdatingProgress = progress;
        setPresentation();
    }

    public void setCallback(SummaryLayoutCallback callback) {
        mCallback = callback;
    }

    private void setPresentation() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        mLabel.setText(WordUtils.capitalize(label));

        mLayoutMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onMoreInfoClicked();
                }
            }
        });
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onLayoutClicked();
                }
            }
        });
        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onSummaryActionClicked();
                }
            }
        });
        // Any status could be being updated by the WatchZoneModelUpdater...
        Integer progress = null;
        if (mUpdatingProgress != -1) {
            progress = mUpdatingProgress;
        }

        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            // Get all LimitSchedules to determine sweeping dates
            List<LimitSchedule> allLimitSchedules = new ArrayList<>();
            for (Long uniqueLimitUid : mWatchZoneModel.getUniqueLimitModels().keySet()) {
                allLimitSchedules.addAll(new ArrayList<>(
                        mWatchZoneModel.getUniqueLimitModels().get(uniqueLimitUid).schedules));
            }

            String dateString = getResources().getString(R.string.watch_zone_no_sweeping);
            List<LimitScheduleDate> sweepingDates =
                    WatchZoneUtils.getStartTimeOrderedDatesForWatchZone(mWatchZoneModel);
            if (sweepingDates != null) {
                List<LimitScheduleDate> currentSweeping = new ArrayList<>();
                List<LimitScheduleDate> upcomingSweeping = new ArrayList<>();
                long now = new GregorianCalendar(
                        TimeZone.getTimeZone("America/Los_Angeles"), Locale.US).getTime().getTime();
                long startOffset = WatchZoneUtils.getStartHourOffset(watchZone);
                for (LimitScheduleDate date : sweepingDates) {
                    long warningTime = date.getStartCalendar().getTime().getTime() - startOffset;
                    long startTime = date.getStartCalendar().getTime().getTime();
                    long endTime = date.getEndCalendar().getTime().getTime();
                    if (startTime <= now && endTime >= now) {
                        currentSweeping.add(date);
                    } else if (warningTime <= now && endTime >= now) {
                        upcomingSweeping.add(date);
                    }
                }

                if (!currentSweeping.isEmpty()) {
                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_red_24dp));
                    dateString = "Street sweeping is happening now.";
                } else if (!upcomingSweeping.isEmpty()) {
                    long nextSweepingTime = upcomingSweeping.get(0).getStartCalendar().getTime().getTime();

                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_yellow_24dp));
                    Calendar sweeping = Calendar.getInstance();
                    sweeping.setTime(new Date(nextSweepingTime));
                    Calendar today = Calendar.getInstance();
                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DATE, 1);
                    if (sweeping.get(Calendar.DATE) == today.get(Calendar.DATE)) {
                        dateString = "Next sweeping will occur today at " +
                                new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                    } else if (sweeping.get(Calendar.DATE) == tomorrow.get(Calendar.DATE)) {
                        dateString = "Next sweeping will occur tomorrow at " +
                                new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                    } else {
                        dateString = "Next sweeping will occur on " +
                                new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(nextSweepingTime));
                    }
                } else if (!sweepingDates.isEmpty()) {
                    long nextSweepingTime = sweepingDates.get(0).getStartCalendar().getTime().getTime();

                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_green_24dp));
                    Calendar sweeping = Calendar.getInstance();
                    sweeping.setTime(new Date(nextSweepingTime));
                    Calendar today = Calendar.getInstance();
                    Calendar tomorrow = Calendar.getInstance();
                    tomorrow.add(Calendar.DATE, 1);
                    if (sweeping.get(Calendar.DATE) == today.get(Calendar.DATE)) {
                        dateString = "Next sweeping will occur today at " +
                                new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                    } else if (sweeping.get(Calendar.DATE) == tomorrow.get(Calendar.DATE)) {
                        dateString = "Next sweeping will occur tomorrow at " +
                                new SimpleDateFormat("K:mma").format(new Date(nextSweepingTime));
                    } else {
                        dateString = "Next sweeping will occur on " +
                                new SimpleDateFormat("EEE, MMM dd 'at' K:mma").format(new Date(nextSweepingTime));
                    }
                } else {
                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_black_24dp));
                }
            }
            mStatusText.setText(dateString);
        }

        if (mSummaryAction == SummaryAction.None) {
            mActionButton.setVisibility(View.INVISIBLE);
        } else if (mSummaryAction == SummaryAction.Customize) {
            mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_customize_black_18dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_customize);
            mActionButton.setVisibility(View.VISIBLE);
        } else if (mSummaryAction == SummaryAction.Save) {
            mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_favorite_black_18dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_save);
            mActionButton.setVisibility(View.VISIBLE);
        }
    }
}
