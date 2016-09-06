package com.example.joseph.sweepersd.presentation.manualalarms;

import android.content.Context;
import android.support.v7.widget.RecyclerView;
import android.text.TextUtils;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.alarms.Alarm;
import com.example.joseph.sweepersd.model.alarms.AlarmManager;
import com.example.joseph.sweepersd.model.alarms.AlarmUpdateManager;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.util.ArrayList;
import java.util.List;

/**
 * Adapter for showing manual Alarms.
 */
public class AlarmViewAdapter extends RecyclerView.Adapter<AlarmViewAdapter.ViewHolder> {
    private final Context mContext;
    private final AlarmManager mAlarmManager;
    private final List<AlarmPresenter> mAlarmPresenters;

    public AlarmViewAdapter(Context context, AlarmManager alarmManager) {
        mContext = context;
        mAlarmManager = alarmManager;
        mAlarmManager.addAlarmChangeListener(mAlarmChangeListener);
        mAlarmPresenters = new ArrayList<>();
        for (Long createdTimestamp : mAlarmManager.getAlarms()) {
            Alarm alarm = mAlarmManager.getAlarm(createdTimestamp);
            if (mAlarmManager.getUpdatingAlarms().contains(createdTimestamp)) {
                UpdatingPresenter presenter = new UpdatingPresenter(
                        mAlarmPresenters.size(), alarm);
                mAlarmPresenters.add(presenter);
            } else {
                NonUpdatingAlarmPresenter presenter =
                        new NonUpdatingAlarmPresenter(mAlarmPresenters.size(), alarm, "TODO");
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
    public void onBindViewHolder(final ViewHolder holder, final int position) {
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
                /*AlarmPresenter presenter = mAlarmPresenters.get(position);
                presenter.setDeleted();
                mAlarmPresenters.remove(presenter);
                mAlarmManager.removeAlarm(presenter.alarm);*/
                mAlarmManager.deleteAlarm(mAlarmPresenters.get(position).alarm.getCreatedTimestamp());
                Toast.makeText(v.getContext(), "Delete alarm", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
    }

    @Override
    public int getItemCount() {
        return mAlarmPresenters.size();
    }

    public void createAlarm(LatLng location, int radius) {
        mAlarmManager.createAlarm(location, radius);
    }

    private AlarmManager.AlarmChangeListener mAlarmChangeListener =
            new AlarmManager.AlarmChangeListener() {
        @Override
        public void onAlarmUpdated(Long createdTimestamp) {
            for (AlarmPresenter p : mAlarmPresenters) {
                if (p.alarm.getCreatedTimestamp() == createdTimestamp) {
                    p.alarm = mAlarmManager.getAlarm(createdTimestamp);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onAlarmCreated(Long createdTimestamp) {
            UpdatingPresenter presenter = new UpdatingPresenter(
                    mAlarmPresenters.size(), mAlarmManager.getAlarm(createdTimestamp));
            mAlarmPresenters.add(presenter);

            notifyDataSetChanged();
        }

        @Override
        public void onAlarmDeleted(Long createdTimestamp) {
            int position = -1;
            for (int i = 0; i < mAlarmPresenters.size(); i++) {
                AlarmPresenter p = mAlarmPresenters.get(i);
                if (p.alarm.getCreatedTimestamp() == createdTimestamp) {
                    position = i;
                }
            }
            if (position > 0) {
                mAlarmPresenters.remove(position);
            }
            notifyItemRemoved(position);
        }
    };

    /*public class LoadAlarmSweepingPositionsTask extends AsyncTask<Void, String, List<SweepingAddress>> {
        private Alarm mAlarm;
        private UpdatingPresenter mAlarmPresenter;

        public LoadAlarmSweepingPositionsTask(UpdatingPresenter alarmPresenter, Alarm alarm) {
            mAlarm = alarm;
            mAlarmPresenter = alarmPresenter;
        }

        @Override
        protected List<SweepingAddress> doInBackground(Void... params) {
            // TODO
            List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(mAlarm.getCenter(),
                    mAlarm.getRadius());

            List<SweepingAddress> sweepingAddresses = new ArrayList<>();

            String centerAddress  = LocationUtils.getAddressForLatLnt(mContext, mAlarm.getCenter());
            sweepingAddresses.add(new SweepingAddress(mAlarm.getCenter(), centerAddress));
            for (int i = 0; i < latLngs.size(); i++) {
                LatLng latLng = latLngs.get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress = (int) (((double)i / (double)latLngs.size()) * 100);
                String progressUpdate = String.format("Loading addresses in radius: %d%%", progress);
                publishProgress(progressUpdate);

                String address  = LocationUtils.getAddressForLatLnt(mContext, mAlarm.getCenter());
                sweepingAddresses.add(
                        new SweepingAddress(latLng, address));
            }
            return sweepingAddresses;
            return null;
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mAlarmPresenter.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(List<SweepingAddress> sweepingAddresses) {
            if (!mAlarmPresenter.isDeleted && !isCancelled() && sweepingAddresses != null) {
                LoadingLimitsPresenter newPresenter =
                        new LoadingLimitsPresenter(mAlarmPresenter.position, mAlarm);
                mAlarmPresenters.remove(mAlarmPresenter.position);
                mAlarmPresenters.add(mAlarmPresenter.position, newPresenter);
                notifyItemChanged(mAlarmPresenter.position);

                mAlarm.setSweepingAddresses(sweepingAddresses);
                //mAlarmManager.saveAlarm(mAlarm);

                //new LoadLimitsTask(newPresenter, mAlarm).execute();
            }
            super.onPostExecute(sweepingAddresses);
        }
    }*/

    /*public class LoadLimitsTask extends AsyncTask<Void, String, HashMap<SweepingAddress, Limit>> {
        private final Alarm mAlarm;
        private LoadingLimitsPresenter mAlarmPresenter;
        private String mDescriptionResult;

        public LoadLimitsTask(LoadingLimitsPresenter alarmPresenter, Alarm alarm) {
            mAlarm = alarm;
            mAlarmPresenter = alarmPresenter;
        }

        @Override
        protected HashMap<SweepingAddress, Limit> doInBackground(Void... params) {
            HashMap<SweepingAddress, Limit> results = new HashMap<>();
            for (int i = 0; i < mAlarm.getSweepingAddresses().size(); i++) {
                SweepingAddress position = mAlarm.getSweepingAddresses().get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress =
                        (int) (((double)i / (double)mAlarm.getSweepingAddresses().size()) * 50);
                String progressUpdate = String.format("Loading limit information: %d%%", progress);
                publishProgress(progressUpdate);

                Limit limit = LocationUtils.findLimitForAddress(position.getAddress());
                results.put(position, limit);
            }

            GregorianCalendar nextSweepingTime = null;
            for (int i = 0; i < mAlarm.getSweepingAddresses().size(); i++) {
                SweepingAddress position = mAlarm.getSweepingAddresses().get(i);

                if (mAlarmPresenter.isDeleted || isCancelled()) {
                    return null;
                }
                int progress =
                     50 + (int) (((double)i / (double)mAlarm.getSweepingAddresses().size()) * 50);
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
        protected void onPostExecute(HashMap<SweepingAddress, Limit> results) {
            if (!mAlarmPresenter.isDeleted && !isCancelled() && results != null) {
                NonUpdatingAlarmPresenter newPresenter =
                        new NonUpdatingAlarmPresenter(mAlarmPresenter.position, mAlarm, results,
                                mDescriptionResult);
                mAlarmPresenters.remove(mAlarmPresenter.position);
                mAlarmPresenters.add(mAlarmPresenter.position, newPresenter);
                notifyItemChanged(mAlarmPresenter.position);
            }
            super.onPostExecute(results);
        }
    }*/

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

    class UpdatingPresenter extends AlarmPresenter implements
            AlarmUpdateManager.AlarmProgressListener {
        String progress = "Progress: 0%";
        String title = null;

        UpdatingPresenter(int position, Alarm alarm) {
            super(position, alarm);
            mAlarmManager.addAlarmProgressListener(this);
            setProgress(mAlarmManager.getProgressForAlarm(alarm.getCreatedTimestamp()));
        }

        @Override
        String getTitle() {
            if (TextUtils.isEmpty(title)) {
                title = LocationUtils.getAddressForLatLnt(mContext, this.alarm.getCenter());
            }
            return title;
        }

        @Override
        String getDescription() {
            return progress;
        }

        void setProgress(int progress) {
            this.progress = String.format("Progress: %d%%", progress);
            notifyItemChanged(this.position);
        }

        @Override
        public void onAlarmUpdateComplete(long createdTimestamp) {
            NonUpdatingAlarmPresenter newPresenter =
                    new NonUpdatingAlarmPresenter(this.position, this.alarm, "TODO");
            mAlarmPresenters.remove(this.position);
            mAlarmPresenters.add(this.position, newPresenter);
            notifyItemChanged(this.position);
        }

        @Override
        public void onAlarmUpdateProgress(long createdTimestamp, int progress) {
            if (createdTimestamp == this.alarm.getCreatedTimestamp()) {
                setProgress(progress);
            }
        }
    }

    class NonUpdatingAlarmPresenter extends AlarmPresenter {
        Alarm alarm;
        String description;
        String title;

        NonUpdatingAlarmPresenter(int position, Alarm alarm, String description) {
            super(position, alarm);
            this.alarm = alarm;
            this.description = description;
        }

        @Override
        String getTitle() {
            if (TextUtils.isEmpty(title)) {
                title = LocationUtils.getAddressForLatLnt(mContext, this.alarm.getCenter());
            }
            return title;
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

    /*private GregorianCalendar getNextSweepingDate(HashMap<SweepingAddress, Limit> limitsMap) {
        GregorianCalendar result = null;
        for (SweepingAddress pos : mSweepingPositions) {
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

    private List<GregorianCalendar> getNextSweepingDates(int maxDays, HashMap<SweepingAddress, Limit> limitsMap) {
        List<GregorianCalendar> result = new ArrayList<>();
        for (SweepingAddress pos : mSweepingPositions) {
            result.addAll(LocationUtils.getSweepingDaysForLimit(
                    LocationUtils.findLimitForAddress(pos.getAddress()), maxDays));
        }
        return result;
    }*/
}