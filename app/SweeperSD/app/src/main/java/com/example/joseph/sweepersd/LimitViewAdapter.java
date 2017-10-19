package com.example.joseph.sweepersd;

import android.support.annotation.NonNull;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.TextView;

import com.example.joseph.sweepersd.limit.Limit;
import com.example.joseph.sweepersd.limit.LimitParser;
import com.example.joseph.sweepersd.limit.LimitSchedule;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneLimitModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class LimitViewAdapter extends RecyclerView.Adapter<LimitViewAdapter.ViewHolder> {
    private static final String TAG = LimitViewAdapter.class.getSimpleName();

    private List<PostedSign> mPostedSigns;
    private Map<WatchZoneLimitModel, List<PostedSign>> mMap;

    public LimitViewAdapter() {
        mPostedSigns = new ArrayList<>();
        mMap = new HashMap<>();
    }

    public void addLimitModel(WatchZoneLimitModel limitModel) {
        if (!mMap.containsKey(limitModel)) {
            addSignsForModel(limitModel);
        }
    }

    public void removeLimitModel(WatchZoneLimitModel limitModel) {
        if (mMap.containsKey(limitModel)) {

        }
    }

    public void updateLimitModel(WatchZoneLimitModel limitModel) {
        if (mMap.containsKey(limitModel)) {

        }
    }

    @Override
    public LimitViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                          int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.content_posted_limits, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        PostedSign sign = mPostedSigns.get(position);

        holder.mStartRange.setText(sign.startRange);
        holder.mEndRange.setText(sign.endRange);
        holder.mStreet.setText(sign.street.toUpperCase());
        holder.mStartTime.setText(sign.startTime.toUpperCase());
        holder.mStartAMPM.setText(sign.startAMPM.toUpperCase());
        holder.mEndTime.setText(sign.endTime.toUpperCase());
        holder.mEndAMPM.setText(sign.endAMPM.toUpperCase());
        holder.mPrefix.setText(sign.prefix);
        holder.mDay.setText(sign.day.toUpperCase());
    }

    @Override
    public int getItemCount() {
        return mPostedSigns.size();
    }

    private void addSignsForModel(WatchZoneLimitModel limitModel) {
        List<LimitSchedule> schedules = limitModel.getLimitSchedulesModel().getScheduleList();
        Map<LimitTuple, List<LimitSchedule>> sortedSchedules = new HashMap<>();
        for (LimitSchedule s : schedules) {
            LimitTuple tuple = new LimitTuple(s.getDayNumber(),
                    s.getStartHour(), s.getEndHour());
            List<LimitSchedule> schedulesForDay = sortedSchedules.get(tuple);
            if (schedulesForDay == null) {
                schedulesForDay = new ArrayList<>();
            }
            schedulesForDay.add(s);

            sortedSchedules.put(tuple, schedulesForDay);
        }
        List<PostedSign> signsToAdd = new ArrayList<>();
        for (LimitTuple tuple : sortedSchedules.keySet()) {
            PostedSign newSign = new PostedSign();
            newSign.startRange = Integer.toString(limitModel.getLimit().getStartRange());
            newSign.endRange = Integer.toString(limitModel.getLimit().getEndRange());
            newSign.street = limitModel.getLimit().getStreet();
            newSign.day = LimitParser.getDay(tuple.day);
            newSign.startTime = tuple.startHour < 13 ? Integer.toString(tuple.startHour) :
                    Integer.toString(tuple.startHour - 12);
            newSign.startAMPM = tuple.startHour < 13 ? "AM" : "PM";
            newSign.endTime = tuple.endHour < 13 ? Integer.toString(tuple.endHour) :
                    Integer.toString(tuple.endHour - 12);
            newSign.endAMPM = tuple.startHour < 13 ? "AM" : "PM";

            List<LimitSchedule> uniques = sortedSchedules.get(tuple);
            List<Integer> weekNumbers = new ArrayList<>();
            for (LimitSchedule unique : uniques) {
                if (!weekNumbers.contains(unique.getWeekNumber())) {
                    weekNumbers.add(unique.getWeekNumber());
                }
            }
            Collections.sort(weekNumbers);

            /*if (weekNumbers.size() > 3) {
                newSign.prefix = "EVERY";
            } else {*/
                newSign.prefix = "";
                for (Integer number : weekNumbers) {
                    if (!newSign.prefix.equals("")) {
                        newSign.prefix += ", ";
                    }
                    newSign.prefix += LimitParser.getPrefix(number);
                }
            //}

            signsToAdd.add(newSign);
        }

        Collections.sort(signsToAdd);

        mMap.put(limitModel, signsToAdd);
        int itemRangeStart = mPostedSigns.size();
        mPostedSigns.addAll(signsToAdd);
        notifyItemRangeInserted(itemRangeStart, signsToAdd.size());
    }

    private void removeSignsForModel(WatchZoneLimitModel limitModel) {
        List<PostedSign> signs = mMap.remove(limitModel);
        if (signs != null && !signs.isEmpty()) {
            int indexOfFirst = mPostedSigns.indexOf(signs.get(0));
            for (int i = 0; i < signs.size(); i++) {
                mPostedSigns.remove(indexOfFirst);
            }
            notifyItemRangeRemoved(indexOfFirst, signs.size());
        }
    }

    private class LimitTuple {
        final int day;
        final int startHour;
        final int endHour;

        LimitTuple(int day, int start, int end) {
            this.day = day;
            this.startHour = start;
            this.endHour = end;
        }

        @Override
        public boolean equals(Object obj) {
            LimitTuple other = (LimitTuple) obj;
            return this.day == other.day && this.startHour == other.startHour && this.endHour == other.endHour;
        }

        @Override
        public int hashCode() {
            return day * startHour * endHour;
        }
    }

    private class PostedSign implements Comparable<PostedSign> {
        String startRange;
        String endRange;
        String street;
        String startTime;
        String startAMPM;
        String endTime;
        String endAMPM;
        String prefix;
        String day;

        @Override
        public int compareTo(@NonNull PostedSign o) {
            return this.street.compareTo(o.street);
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder {
        public TextView mStartRange;
        public TextView mEndRange;
        public TextView mStreet;
        public TextView mStartTime;
        public TextView mStartAMPM;
        public TextView mEndTime;
        public TextView mEndAMPM;
        public TextView mPrefix;
        public TextView mDay;

        public ViewHolder(View v) {
            super(v);
            mStartRange = v.findViewById(R.id.textview_start_range);
            mEndRange = v.findViewById(R.id.textview_end_range);
            mStreet = v.findViewById(R.id.textview_street);
            mStartTime = v.findViewById(R.id.textview_start_time);
            mStartAMPM = v.findViewById(R.id.textview_start_ampm);
            mEndTime = v.findViewById(R.id.textview_end_time);
            mEndAMPM = v.findViewById(R.id.textview_end_ampm);
            mPrefix = v.findViewById(R.id.textview_week_prefix);
            mDay = v.findViewById(R.id.textview_day);
        }
    }
}
