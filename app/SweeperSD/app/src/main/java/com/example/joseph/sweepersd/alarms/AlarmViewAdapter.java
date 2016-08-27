package com.example.joseph.sweepersd.alarms;

import android.content.Context;
import android.location.Location;
import android.os.AsyncTask;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.limits.Limit;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.SweepingPosition;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.GregorianCalendar;
import java.util.HashMap;
import java.util.List;

/**
 * Adapter for showing manual Alarms.
 */
public class AlarmViewAdapter extends RecyclerView.Adapter<AlarmViewAdapter.ViewHolder> {
    private final Context mContext;
    private final AlarmModel mAlarmModel;
    private final List<AlarmPresenter> mAlarmPresenters;

    public AlarmViewAdapter(Context context, AlarmModel alarmModel) {
        mContext = context;
        mAlarmModel = alarmModel;
        mAlarmPresenters = new ArrayList<>();
        for (Alarm alarm : mAlarmModel.getAlarms()) {
            if (alarm.getSweepingPositions() == null) {
                LoadingAddressesPresenter presenter = new LoadingAddressesPresenter(
                        mAlarmPresenters.size(), alarm);
                new LoadAlarmSweepingPositionsTask(presenter, alarm).execute();
                mAlarmPresenters.add(presenter);
            } else {
                LoadingLimitsPresenter presenter =
                        new LoadingLimitsPresenter(mAlarmPresenters.size(), alarm);
                new LoadLimitsTask(presenter, alarm).execute();
                mAlarmPresenters.add(presenter);
            }
        }
    }

    @Override
    public AlarmViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                   int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_alarm_list_item, parent, false);

        ViewHolder vh = new ViewHolder(v);
        return vh;
    }

    @Override
    public void onBindViewHolder(ViewHolder holder, final int position) {
        holder.mAlarmAddress.setText(mAlarmPresenters.get(position).getTitle());
        holder.mNextSweeping.setText(mAlarmPresenters.get(position).getDescription());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Show alarm details", Toast.LENGTH_SHORT).show();
            }
        };
        holder.mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                AlarmPresenter presenter = mAlarmPresenters.get(position);
                presenter.setDeleted();
                mAlarmPresenters.remove(presenter);
                mAlarmModel.removeAlarm(presenter.alarm);
                Toast.makeText(v.getContext(), "Delete alarm", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
    }

    @Override
    public int getItemCount() {
        return mAlarmPresenters.size();
    }

    public void createAlarm(Location location, int radius) {
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        int position = mAlarmPresenters.size();

        Alarm newAlarm = new Alarm(System.currentTimeMillis()/1000, center, radius, null);
        mAlarmModel.saveAlarm(newAlarm);

        LoadingAddressesPresenter presenter = new LoadingAddressesPresenter(position, newAlarm);
        mAlarmPresenters.add(presenter);

        LoadAlarmSweepingPositionsTask alarmTask =
                new LoadAlarmSweepingPositionsTask(presenter, newAlarm);
        alarmTask.execute();

        notifyDataSetChanged();
    }

    public class LoadAlarmSweepingPositionsTask extends AsyncTask<Void, String, List<SweepingPosition>> {
        private Alarm mAlarm;
        private LoadingAddressesPresenter mAlarmPresenter;

        public LoadAlarmSweepingPositionsTask(LoadingAddressesPresenter alarmPresenter, Alarm alarm) {
            mAlarm = alarm;
            mAlarmPresenter = alarmPresenter;
        }

        @Override
        protected List<SweepingPosition> doInBackground(Void... params) {
            List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(mAlarm.getCenter(),
                    mAlarm.getRadius());

            List<SweepingPosition> sweepingPositions = new ArrayList<>();

            String centerAddress  = LocationUtils.getAddressForLatLnt(mContext, mAlarm.getCenter());
            sweepingPositions.add(new SweepingPosition(mAlarm.getCenter(), centerAddress));
            for (int i = 0; i < latLngs.size(); i++) {
                LatLng latLng = latLngs.get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress = (int) (((double)i / (double)latLngs.size()) * 100);
                String progressUpdate = String.format("Loading addresses in radius: %d%%", progress);
                publishProgress(progressUpdate);

                String address  = LocationUtils.getAddressForLatLnt(mContext, mAlarm.getCenter());
                sweepingPositions.add(
                        new SweepingPosition(latLng, address));
            }
            return sweepingPositions;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mAlarmPresenter.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<SweepingPosition> sweepingPositions) {
            if (!mAlarmPresenter.isDeleted && !isCancelled() && sweepingPositions != null) {
                LoadingLimitsPresenter newPresenter =
                        new LoadingLimitsPresenter(mAlarmPresenter.position, mAlarm);
                mAlarmPresenters.remove(mAlarmPresenter.position);
                mAlarmPresenters.add(mAlarmPresenter.position, newPresenter);
                notifyItemChanged(mAlarmPresenter.position);

                mAlarm.setSweepingPositions(sweepingPositions);
                mAlarmModel.saveAlarm(mAlarm);

                new LoadLimitsTask(newPresenter, mAlarm).execute();
            }
            super.onPostExecute(sweepingPositions);
        }
    }

    public class LoadLimitsTask extends AsyncTask<Void, String, HashMap<SweepingPosition, Limit>> {
        private final Alarm mAlarm;
        private LoadingLimitsPresenter mAlarmPresenter;
        private String mDescriptionResult;

        public LoadLimitsTask(LoadingLimitsPresenter alarmPresenter, Alarm alarm) {
            mAlarm = alarm;
            mAlarmPresenter = alarmPresenter;
        }

        @Override
        protected HashMap<SweepingPosition, Limit> doInBackground(Void... params) {
            HashMap<SweepingPosition, Limit> results = new HashMap<>();
            for (int i = 0; i < mAlarm.getSweepingPositions().size(); i++) {
                SweepingPosition position = mAlarm.getSweepingPositions().get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress =
                        (int) (((double)i / (double)mAlarm.getSweepingPositions().size()) * 50);
                String progressUpdate = String.format("Loading limit information: %d%%", progress);
                publishProgress(progressUpdate);

                Limit limit = LocationUtils.findLimitForAddress(position.getAddress());
                results.put(position, limit);
            }

            GregorianCalendar nextSweepingTime = null;
            for (int i = 0; i < mAlarm.getSweepingPositions().size(); i++) {
                SweepingPosition position = mAlarm.getSweepingPositions().get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress =
                     50 + (int) (((double)i / (double)mAlarm.getSweepingPositions().size()) * 50);
                String progressUpdate = String.format("Finding next sweeping date: %d%%", progress);
                publishProgress(progressUpdate);

                List<GregorianCalendar> sweepingDays = LocationUtils.getSweepingDaysForLimit(
                        LocationUtils.findLimitForAddress(position.getAddress()), 31);
                if (!sweepingDays.isEmpty()) {
                    GregorianCalendar potentialNew = sweepingDays.get(0);
                    if (nextSweepingTime == null ||
                            potentialNew.getTime().getTime() < nextSweepingTime.getTime().getTime()) {
                        nextSweepingTime = potentialNew;
                    }
                }
            }
            if (nextSweepingTime != null) {
                SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMMM d 'at' ha");
                fmt.setCalendar(nextSweepingTime);
                mDescriptionResult = fmt.format(nextSweepingTime.getTime());
            }
            return results;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mAlarmPresenter.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(HashMap<SweepingPosition, Limit> results) {
            if (!mAlarmPresenter.isDeleted && !isCancelled() && results != null) {
                LoadedAlarmPresenter newPresenter =
                        new LoadedAlarmPresenter(mAlarmPresenter.position, mAlarm, results,
                                mDescriptionResult);
                mAlarmPresenters.remove(mAlarmPresenter.position);
                mAlarmPresenters.add(mAlarmPresenter.position, newPresenter);
                notifyItemChanged(mAlarmPresenter.position);
            }
            super.onPostExecute(results);
        }
    }

    abstract class AlarmPresenter {
        int position;
        Alarm alarm;
        boolean isDeleted;

        AlarmPresenter(int position, Alarm alarm) {
            this.position = position;
            this.alarm = alarm;
            isDeleted = false;
        }

        abstract String getTitle();
        abstract String getDescription();

        public void setDeleted() {
            isDeleted = true;
        }
    }

    class LoadingAddressesPresenter extends AlarmPresenter {
        String progress = "Loading addresses in radius: 0%";

        LoadingAddressesPresenter(int position, Alarm alarm) {
            super(position, alarm);
        }

        @Override
        String getTitle() {
            return "Initializing alarm...";
        }

        @Override
        String getDescription() {
            return progress;
        }

        void setProgress(String progress) {
            this.progress = progress;
            notifyItemChanged(this.position);
        }
    }

    class LoadingLimitsPresenter extends AlarmPresenter {
        String progress = "Loading limit information: 0%";

        LoadingLimitsPresenter(int position, Alarm alarm) {
            super(position, alarm);
        }

        @Override
        String getTitle() {
            return this.alarm.getSweepingPositions().get(0).getAddress();
        }

        @Override
        String getDescription() {
            return progress;
        }

        void setProgress(String progress) {
            this.progress = progress;
            notifyItemChanged(this.position);
        }
    }

    class LoadedAlarmPresenter extends AlarmPresenter {
        Alarm alarm;
        String description;
        HashMap<SweepingPosition, Limit> limits;

        LoadedAlarmPresenter(int position, Alarm alarm, HashMap<SweepingPosition, Limit> limits,
                             String description) {
            super(position, alarm);
            this.alarm = alarm;
            this.limits = limits;
            this.description = description;
        }

        @Override
        String getTitle() {
            return this.alarm.getSweepingPositions().get(0).getAddress();
        }

        @Override
        String getDescription() {
            if (this.description != null) {
                return this.description;
            } else {
                return "No upcoming sweeping";
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        public TextView mAlarmAddress;
        public TextView mNextSweeping;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        public ViewHolder(View v) {
            super(v);
            mAlarmAddress = (TextView) v.findViewById(R.id.textview_alarm_address);
            mNextSweeping = (TextView) v.findViewById(R.id.textview_alarm_next_sweep);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);
        }

        @Override
        public void onClick(View v) {
            if (mOnClickListener != null) {
                mOnClickListener.onClick(v);
            }
        }

        @Override
        public boolean onLongClick(View v) {
            if (mLongClickListener != null) {
                return mLongClickListener.onLongClick(v);
            }
            return false;
        }
    }

    /*private GregorianCalendar getNextSweepingDate(HashMap<SweepingPosition, Limit> limitsMap) {
        GregorianCalendar result = null;
        for (SweepingPosition pos : mSweepingPositions) {
            List<GregorianCalendar> sweepingDays = LocationUtils.getSweepingDaysForLimit(
                    LocationUtils.findLimitForAddress(pos.getAddress()), 31);
            if (!sweepingDays.isEmpty()) {
                GregorianCalendar potentialNew = sweepingDays.get(0);
                if (result == null ||
                        potentialNew.getTime().getTime() < result.getTime().getTime()) {
                    result = potentialNew;
                }
            }
        }
        return result;
    }

    private List<GregorianCalendar> getNextSweepingDates(int maxDays, HashMap<SweepingPosition, Limit> limitsMap) {
        List<GregorianCalendar> result = new ArrayList<>();
        for (SweepingPosition pos : mSweepingPositions) {
            result.addAll(LocationUtils.getSweepingDaysForLimit(
                    LocationUtils.findLimitForAddress(pos.getAddress()), maxDays));
        }
        return result;
    }*/
}