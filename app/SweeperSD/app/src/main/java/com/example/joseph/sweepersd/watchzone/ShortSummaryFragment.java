package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
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
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
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

public class ShortSummaryFragment extends Fragment {

    private WatchZoneModel mWatchZoneModel = null;
    private SummaryAction mSummaryAction = SummaryAction.None;
    private int mUpdatingProgress = -1;

    public enum SummaryAction {
        None,
        Save,
        Customize
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

    private Observer<WatchZoneModel> mObserver;

    public void setWatchZone(WatchZoneModel watchZoneModel) {
        mWatchZoneModel = watchZoneModel;
        if (mLabel != null) {
            setPresentation();
        }
    }

    public void setSummaryAction(SummaryAction summaryAction) {
        mSummaryAction = summaryAction;
        if (mLabel != null) {
            setPresentation();
        }
    }

    public void setUpdatingProgress(int progress) {
        mUpdatingProgress = progress;
        if (mLabel != null) {
            setPresentation();
        }
    }

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.fragment_watchzone_summary_short, container, false);

        mLabel = v.findViewById(R.id.text_watch_zone_label);
        mProgressBar = v.findViewById(R.id.progressbar_updating_watchzone);
        mStatusIcon = v.findViewById(R.id.imageview_status_icon);
        mStatusText = v.findViewById(R.id.textview_status);
        mSummaryDetails = v.findViewById(R.id.layout_summary_details);
        mSummaryPostedLimits = v.findViewById(R.id.textview_number_posted_limits);
        mSummaryStreets = v.findViewById(R.id.textview_number_streets);
        mLayoutMoreInfo = v.findViewById(R.id.layout_more_info_group);
        mActionButton = v.findViewById(R.id.button_summary_action);

        if (mWatchZoneModel != null) {
            setPresentation();
        }

        return v;
    }

    private void setPresentation() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        mLabel.setText(WordUtils.capitalize(label));

        mLayoutMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Intent intent = new Intent(getActivity(), WatchZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, watchZone.getUid());
                intent.putExtras(b);
                getActivity().startActivity(intent);
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

            String dateString = getActivity().getResources().getString(R.string.watch_zone_no_sweeping);
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
                            getActivity().getResources().getDrawable(R.drawable.ic_local_parking_red_24dp));
                    dateString = "Street sweeping is happening now.";
                } else if (!upcomingSweeping.isEmpty()) {
                    long nextSweepingTime = upcomingSweeping.get(0).getStartCalendar().getTime().getTime();

                    mStatusIcon.setBackground(
                            getActivity().getResources().getDrawable(R.drawable.ic_local_parking_yellow_24dp));
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
                            getActivity().getResources().getDrawable(R.drawable.ic_local_parking_green_24dp));
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
                            getActivity().getResources().getDrawable(R.drawable.ic_local_parking_black_24dp));
                }
            }
            mStatusText.setText(dateString);
        }

        if (mSummaryAction == SummaryAction.None) {
            mActionButton.setVisibility(View.GONE);
        } else if (mSummaryAction == SummaryAction.Customize) {
            mActionButton.setCompoundDrawables(getActivity().getResources().getDrawable(R.drawable.ic_customize_black_24dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_customize);
            mActionButton.setVisibility(View.VISIBLE);
        } else if (mSummaryAction == SummaryAction.Save) {
            mActionButton.setCompoundDrawables(getActivity().getResources().getDrawable(R.drawable.ic_favorite_black_24dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_save);
            mActionButton.setVisibility(View.VISIBLE);
        }
    }
}
