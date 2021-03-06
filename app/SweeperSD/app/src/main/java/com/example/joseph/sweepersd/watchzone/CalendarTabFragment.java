package com.example.joseph.sweepersd.watchzone;

import android.graphics.drawable.ColorDrawable;
import android.os.Bundle;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.TabFragment;
import com.example.joseph.sweepersd.limit.LimitModel;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.utils.ChangeSet;
import com.example.joseph.sweepersd.watchzone.model.LimitScheduleDate;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitsObserver;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneUtils;
import com.hutchins.tbd.R;
import com.roomorama.caldroid.CaldroidFragment;

import java.util.ArrayList;
import java.util.Calendar;
import java.util.Date;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

import androidx.annotation.Nullable;
import androidx.fragment.app.FragmentTransaction;

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
        View v = inflater.inflate(R.layout.fragment_calendar, container, false);

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

        mCaldroidFragment.setTextColorForDate(R.color.secondaryColor, new Date());
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

    private void createPresenter(final Long watchZoneUid) {
        final WatchZonePresenter presenter = new WatchZonePresenter();
        presenter.sweepingDates = new HashMap<>();
        presenter.watchZoneLimitsObserver = new WatchZoneLimitsObserver(watchZoneUid,
                new WatchZoneLimitsObserver.WatchZoneLimitsChangedCallback() {
            @Override
            public void onLimitsChanged(Map<Long, LimitModel> data,
                                        ChangeSet changeSet) {
                for (Long uid : changeSet.removedUids) {
                    LimitModel removedModel = null;
                    for (LimitModel model : presenter.sweepingDates.keySet()) {
                        if (model.limit.getUid() == uid) {
                            removedModel = model;
                            break;
                        }
                    }
                    if (removedModel != null) {
                        List<Date> dates = presenter.sweepingDates.remove(removedModel);
                        if (dates != null) {
                            for (Date date : dates) {
                                mCaldroidFragment.clearBackgroundDrawableForDate(date);
                            }
                        }
                    }
                }
                for (Long uid : changeSet.changedUids) {
                    LimitModel model = presenter.watchZoneLimitsObserver.getLimitModels()
                            .get(uid);
                    List<Date> dates = presenter.sweepingDates.remove(model);
                    if (dates != null) {
                        for (Date date : dates) {
                            mCaldroidFragment.clearBackgroundDrawableForDate(date);
                        }
                    }

                    List<LimitSchedule> schedulesList = model.schedules;
                    List<LimitScheduleDate> scheduleDates = WatchZoneUtils.getAllSweepingDatesForLimitSchedules(
                            schedulesList, 31, 93);
                    List<Date> calendarDates = new ArrayList<>();
                    for (LimitScheduleDate date : scheduleDates) {
                        ColorDrawable red = new ColorDrawable(getResources().getColor(R.color.statusRed));
                        Date startDate = new Date(date.getStartCalendar().getTimeInMillis());
                        Date endDate = new Date(date.getEndCalendar().getTimeInMillis());
                        mCaldroidFragment.setBackgroundDrawableForDate(red, startDate);
                        mCaldroidFragment.setBackgroundDrawableForDate(red, endDate);
                        calendarDates.add(startDate);
                        calendarDates.add(endDate);
                    }
                    presenter.sweepingDates.put(model, calendarDates);
                }
                for (Long uid : changeSet.addedUids) {
                    LimitModel model = presenter.watchZoneLimitsObserver.getLimitModels()
                            .get(uid);
                    List<LimitSchedule> schedulesList = model.schedules;
                    List<LimitScheduleDate> dates = WatchZoneUtils.getAllSweepingDatesForLimitSchedules(
                            schedulesList, 31, 93);
                    List<Date> calendarDates = new ArrayList<>();
                    for (LimitScheduleDate date : dates) {
                        ColorDrawable red = new ColorDrawable(getResources().getColor(R.color.statusRed));
                        Date startDate = new Date(date.getStartCalendar().getTimeInMillis());
                        Date endDate = new Date(date.getEndCalendar().getTimeInMillis());
                        mCaldroidFragment.setBackgroundDrawableForDate(red, startDate);
                        mCaldroidFragment.setBackgroundDrawableForDate(red, endDate);
                        calendarDates.add(startDate);
                        calendarDates.add(endDate);
                    }
                    presenter.sweepingDates.put(model, calendarDates);
                }
                mCaldroidFragment.refreshView();
            }
            @Override
            public void onDataLoaded(Map<Long, LimitModel> data) {
                for (Long uid : data.keySet()) {
                    LimitModel model = presenter.watchZoneLimitsObserver.getLimitModels()
                            .get(uid);
                    List<LimitSchedule> schedulesList = model.schedules;
                    List<LimitScheduleDate> dates = WatchZoneUtils.getAllSweepingDatesForLimitSchedules(
                            schedulesList, 31, 93);
                    List<Date> calendarDates = new ArrayList<>();
                    for (LimitScheduleDate date : dates) {
                        ColorDrawable red = new ColorDrawable(getResources().getColor(R.color.statusRed));
                        Date startDate = new Date(date.getStartCalendar().getTimeInMillis());
                        Date endDate = new Date(date.getEndCalendar().getTimeInMillis());
                        mCaldroidFragment.setBackgroundDrawableForDate(red, startDate);
                        mCaldroidFragment.setBackgroundDrawableForDate(red, endDate);
                        calendarDates.add(startDate);
                        calendarDates.add(endDate);
                    }
                    presenter.sweepingDates.put(model, calendarDates);
                }
                mCaldroidFragment.refreshView();
            }
            @Override
            public void onDataInvalid() {
                Set<LimitModel> invalidModels = new HashSet<>(presenter.sweepingDates.keySet());
                for (LimitModel model : invalidModels) {
                    List<Date> dates = presenter.sweepingDates.remove(model);
                    if (dates != null) {
                        for (Date date : dates) {
                            mCaldroidFragment.clearBackgroundDrawableForDate(date);
                        }
                    }
                }
            }
        });
        WatchZoneModelRepository.getInstance(getActivity()).getZoneModelForUid(watchZoneUid).observe(getActivity(),
                presenter.watchZoneLimitsObserver);
        mWatchZones.put(watchZoneUid, presenter);
    }

    private class WatchZonePresenter {
        WatchZoneLimitsObserver watchZoneLimitsObserver;
        Map<LimitModel, List<Date>> sweepingDates;
    }
}
