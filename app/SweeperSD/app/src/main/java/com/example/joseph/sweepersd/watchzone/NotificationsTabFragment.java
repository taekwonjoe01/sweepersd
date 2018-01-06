package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.RadioButton;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabFragment;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;

public class NotificationsTabFragment extends TabFragment {

    private String mTabTitle;

    private RadioButton mRemind48Hour;
    private RadioButton mRemind24Hour;
    private RadioButton mRemind12Hour;

    private RadioButton mPolicyAlways;
    private RadioButton mPolicyNearby;

    private Long mWatchZoneUid = 0L;

    private WatchZoneModelObserver mWatchZoneObserver;

    public NotificationsTabFragment() {
    }

    @Override
    public void setTabTitle(String tabTitle) {
        mTabTitle = tabTitle;
    }

    @Override
    public String getTabTitle() {
        return mTabTitle;
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        View view = inflater.inflate(R.layout.fragment_watchzone_notif_settings,
                container, false);

        mRemind48Hour = view.findViewById(R.id.radiobutton_remind_range_48);
        mRemind24Hour = view.findViewById(R.id.radiobutton_remind_range_24);
        mRemind12Hour = view.findViewById(R.id.radiobutton_remind_range_12);

        mPolicyAlways = view.findViewById(R.id.radiobutton_remind_policy_always);
        mPolicyNearby = view.findViewById(R.id.radiobutton_remind_policy_nearby);

        mRemind48Hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WatchZone watchZone = mWatchZoneObserver.getWatchZone();
                WatchZoneModelRepository.getInstance(getActivity()).updateWatchZone(mWatchZoneUid, watchZone.getLabel(),
                        watchZone.getCenterLatitude(), watchZone.getCenterLongitude(),
                        watchZone.getRadius(), WatchZone.REMIND_RANGE_48_HOURS, watchZone.getRemindPolicy());
            }
        });
        mRemind24Hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WatchZone watchZone = mWatchZoneObserver.getWatchZone();
                WatchZoneModelRepository.getInstance(getActivity()).updateWatchZone(mWatchZoneUid, watchZone.getLabel(),
                        watchZone.getCenterLatitude(), watchZone.getCenterLongitude(),
                        watchZone.getRadius(), WatchZone.REMIND_RANGE_24_HOURS, watchZone.getRemindPolicy());
            }
        });
        mRemind12Hour.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WatchZone watchZone = mWatchZoneObserver.getWatchZone();
                WatchZoneModelRepository.getInstance(getActivity()).updateWatchZone(mWatchZoneUid, watchZone.getLabel(),
                        watchZone.getCenterLatitude(), watchZone.getCenterLongitude(),
                        watchZone.getRadius(), WatchZone.REMIND_RANGE_12_HOURS, watchZone.getRemindPolicy());
            }
        });
        mPolicyAlways.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WatchZone watchZone = mWatchZoneObserver.getWatchZone();
                WatchZoneModelRepository.getInstance(getActivity()).updateWatchZone(mWatchZoneUid, watchZone.getLabel(),
                        watchZone.getCenterLatitude(), watchZone.getCenterLongitude(),
                        watchZone.getRadius(), watchZone.getRemindRange(), WatchZone.REMIND_POLICY_ANYWHERE);
            }
        });
        mPolicyNearby.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                WatchZone watchZone = mWatchZoneObserver.getWatchZone();
                WatchZoneModelRepository.getInstance(getActivity()).updateWatchZone(mWatchZoneUid, watchZone.getLabel(),
                        watchZone.getCenterLatitude(), watchZone.getCenterLongitude(),
                        watchZone.getRadius(), watchZone.getRemindRange(), WatchZone.REMIND_POLICY_NEARBY);
            }
        });

        if (mWatchZoneUid != 0L) {
            setPresentation();
        }
        return view;
    }

    public void setWatchZoneUid(Long watchZoneUid) {
        mWatchZoneUid = watchZoneUid;
        if (mRemind48Hour != null) {
            setPresentation();
        }
    }

    private void setPresentation() {
        if (mWatchZoneObserver != null) {
            WatchZoneModelRepository.getInstance(getActivity()).getZoneModelForUid(mWatchZoneUid).removeObserver(mWatchZoneObserver);
        }

        setEnabled(false);

        mWatchZoneObserver = new WatchZoneModelObserver(mWatchZoneUid,
                new WatchZoneModelObserver.WatchZoneModelChangedCallback() {
            @Override
            public void onWatchZoneModelChanged(WatchZone watchZone) {
                setPresentation(watchZone);
            }

            @Override
            public void onDataLoaded(WatchZone watchZone) {
                setEnabled(true);
                setPresentation(watchZone);
            }

            @Override
            public void onDataInvalid() {
                setEnabled(false);
            }
        });
        WatchZoneModelRepository.getInstance(getActivity()).getZoneModelForUid(mWatchZoneUid).observe(getActivity(), mWatchZoneObserver);
    }

    private void setEnabled(boolean enabled) {
        mRemind48Hour.setEnabled(enabled);
        mRemind24Hour.setEnabled(enabled);
        mRemind12Hour.setEnabled(enabled);

        mPolicyAlways.setEnabled(enabled);
        mPolicyNearby.setEnabled(enabled);
    }

    private void setPresentation(WatchZone watchZone) {
        switch (watchZone.getRemindRange()) {
            case WatchZone.REMIND_RANGE_48_HOURS:
                mRemind48Hour.setChecked(true);
                break;
            case WatchZone.REMIND_RANGE_24_HOURS:
                mRemind24Hour.setChecked(true);
                break;
            case WatchZone.REMIND_RANGE_12_HOURS:
                mRemind12Hour.setChecked(true);
                break;
        }
        switch (watchZone.getRemindPolicy()) {
            case WatchZone.REMIND_POLICY_NEARBY:
                mPolicyNearby.setChecked(true);
                break;
            case WatchZone.REMIND_POLICY_ANYWHERE:
                mPolicyAlways.setChecked(true);
                break;
        }
    }
}
