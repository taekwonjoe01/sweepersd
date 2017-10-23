package com.example.joseph.sweepersd.watchzone;

import android.graphics.Color;
import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.FragmentTransaction;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabFragment;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;
import com.roomorama.caldroid.CaldroidFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class CalendarTabFragment extends TabFragment {

    private String mTabTitle;

    private Map<Long, WatchZonePresenter> mWatchZones = new HashMap<>();

    private CaldroidFragment mCaldroidFragment;


    public CalendarTabFragment() {

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
        View v = inflater.inflate(R.layout.content_posted_limits_calendar, container, false);

        mCaldroidFragment = new CaldroidFragment();
        Bundle args = new Bundle();
        Calendar cal = Calendar.getInstance();
        args.putInt(CaldroidFragment.MONTH, cal.get(Calendar.MONTH) + 1);
        args.putInt(CaldroidFragment.YEAR, cal.get(Calendar.YEAR));
        args.putInt(CaldroidFragment.THEME_RESOURCE, R.style.CaldroidDefaultDark);
        mCaldroidFragment.setArguments(args);

        FragmentTransaction t = getActivity().getSupportFragmentManager().beginTransaction();
        t.replace(R.id.caldroid_fragment_layout, mCaldroidFragment);
        t.commit();

        mCaldroidFragment.setTextColorForDate(R.color.app_primary, new Date());
        mCaldroidFragment.refreshView();

        for (Long watchZoneUid : mWatchZones.keySet()) {
            createPresenter(watchZoneUid);
        }

        return v;
    }

    public void addWatchZone(Long watchZoneUid) {
        if (mCaldroidFragment == null) {
            mWatchZones.put(watchZoneUid, null);
        } else if (!mWatchZones.containsKey(watchZoneUid)) {
            createPresenter(watchZoneUid);
        }
    }

    public void removeWatchZone(Long watchZoneUid) {
        if (mWatchZones.containsKey(watchZoneUid)) {
            WatchZonePresenter presenter = mWatchZones.get(watchZoneUid);
            if (presenter != null) {
                WatchZoneModelRepository.getInstance(getContext()).removeObserver(presenter.watchZoneLimitsObserver);
            }

            mWatchZones.remove(watchZoneUid);
        }
    }

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.sweepingDates = new HashMap<>();
        presenter.watchZoneLimitsObserver = new WatchZoneLimitsObserver(watchZoneUid,
                new WatchZoneLimitsObserver.WatchZoneLimitsChangedCallback() {
            @Override
            public void onLimitModelAdded(int index) {
                WatchZoneLimitModel model = presenter.watchZoneLimitsObserver.getLimitModels()
                        .get(index);
                List<LimitSchedule> schedules = model.getLimitSchedulesModel().getScheduleList();
                List<LimitScheduleDate> dates = WatchZoneUtils.getAllSweepingDatesForLimitSchedules(
                        schedules, 31, 93);
                List<Date> calendarDates = new ArrayList<>();
                for (LimitScheduleDate date : dates) {
                    ColorDrawable red = new ColorDrawable(Color.RED);
                    Date startDate = new Date(date.getStartCalendar().getTimeInMillis());
                    Date endDate = new Date(date.getEndCalendar().getTimeInMillis());
                    mCaldroidFragment.setBackgroundDrawableForDate(red, startDate);
                    mCaldroidFragment.setBackgroundDrawableForDate(red, endDate);
                    calendarDates.add(startDate);
                    calendarDates.add(endDate);
                }
                presenter.sweepingDates.put(model, calendarDates);
                mCaldroidFragment.refreshView();
            }
            @Override
            public void onLimitModelRemoved(int index) {
                WatchZoneLimitModel model = presenter.watchZoneLimitsObserver.getLimitModels()
                        .get(index);
                List<Date> dates = presenter.sweepingDates.remove(model);
                if (dates != null) {
                    for (Date date : dates) {
                        mCaldroidFragment.clearBackgroundDrawableForDate(date);
                    }
                }
                mCaldroidFragment.refreshView();
            }
            @Override
            public void onLimitModelUpdated(int index) {
                onLimitModelRemoved(index);
                onLimitModelAdded(index);
            }
            @Override
            public void onDataLoaded(List<WatchZoneLimitModel> data) {
                for (WatchZoneLimitModel model : data) {
                    List<LimitSchedule> schedules = model.getLimitSchedulesModel().getScheduleList();
                    List<LimitScheduleDate> dates = WatchZoneUtils.getAllSweepingDatesForLimitSchedules(
                            schedules, 31, 93);
                    List<Date> calendarDates = new ArrayList<>();
                    for (LimitScheduleDate date : dates) {
                        ColorDrawable red = new ColorDrawable(Color.RED);
                        Date startDate = new Date(date.getStartCalendar().getTimeInMillis());
                        Date endDate = new Date(date.getEndCalendar().getTimeInMillis());
                        mCaldroidFragment.setBackgroundDrawableForDate(red, startDate);
                        mCaldroidFragment.setBackgroundDrawableForDate(red, endDate);
                        calendarDates.add(startDate);
                        calendarDates.add(endDate);
                    }
                    presenter.sweepingDates.put(model, calendarDates);
                    mCaldroidFragment.refreshView();
                }
            }
            @Override
            public void onDataInvalid() {

            }
        });
        WatchZoneModelRepository.getInstance(getActivity()).observe(getActivity(),
                presenter.watchZoneLimitsObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private class WatchZonePresenter {
        WatchZoneLimitsObserver watchZoneLimitsObserver;
        Map<WatchZoneLimitModel, List<Date>> sweepingDates;
    }
}
