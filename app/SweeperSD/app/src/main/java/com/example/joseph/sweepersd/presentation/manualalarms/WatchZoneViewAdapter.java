package com.example.joseph.sweepersd.presentation.manualalarms;

import android.app.AlarmManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.support.v7.widget.SimpleItemAnimator;
import android.text.TextUtils;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.AddressValidatorManager;
import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneAlarmReceiver;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneManager;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneUpdateManager;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.text.WordUtils;

import java.util.ArrayList;
import java.util.Date;
import java.util.GregorianCalendar;
import java.util.List;
import java.util.Locale;
import java.util.TimeZone;

/**
 * Adapter for showing manual Alarms.
 */
public class WatchZoneViewAdapter extends RecyclerView.Adapter<WatchZoneViewAdapter.ViewHolder> {
    private static final String TAG = WatchZoneViewAdapter.class.getSimpleName();

    private final Context mContext;
    private WatchZoneManager mWatchZoneManager;
    private final List<WatchZonePresenter> mWatchZonePresenters;

    public WatchZoneViewAdapter(Context context) {
        mContext = context;
        mWatchZonePresenters = new ArrayList<>();

        new LoadAlarmsTask().execute();
    }

    @Override
    public WatchZoneViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_alarm_list_item, parent, false);

        ViewHolder vh = new ViewHolder(mContext, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final WatchZonePresenter presenter = mWatchZonePresenters.get(position);
        holder.mAlarmAddress.setText(presenter.getTitle());
        holder.mRadius.setText(presenter.getRadius());
        holder.mStatus.setText(presenter.getStatus());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(v.getContext(), "Show watchZone details", Toast.LENGTH_SHORT).show();

                GregorianCalendar today = new GregorianCalendar(
                        TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
                long alarmTime = today.getTime().getTime() + 20000;
                AlarmManager alarmMgr;
                PendingIntent alarmIntent;

                alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(mContext, WatchZoneAlarmReceiver.class);
                intent.setType(presenter.watchZone.getCreatedTimestamp() + "");
                alarmIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);


                alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
                Log.i(TAG, "Alarm scheduled for " + new Date(alarmTime).toString());
            }
        };
        holder.mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                /*WatchZonePresenter presenter = mWatchZonePresenters.get(position);
                presenter.setDeleted();
                mWatchZonePresenters.remove(presenter);
                mWatchZoneManager.removeAlarm(presenter.watchZone);*/
                mWatchZoneManager.deleteWatchZone(mWatchZonePresenters.get(position).watchZone.getCreatedTimestamp());
                Toast.makeText(v.getContext(), "Delete watchZone", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        holder.mRefreshButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                mWatchZoneManager.refreshWatchZone(mWatchZonePresenters.get(position).watchZone.getCreatedTimestamp());
            }
        });
        if (presenter instanceof NonUpdatingWatchZonePresenter) {
            holder.mLimitRecyclerView.setVisibility(View.VISIBLE);
            holder.mStatus.setVisibility(View.GONE);
            holder.mLimitRecyclerView.setAdapter(((NonUpdatingWatchZonePresenter) presenter).adapter);
        } else {
            holder.mLimitRecyclerView.setVisibility(View.GONE);
            holder.mStatus.setVisibility(View.VISIBLE);
            holder.mLimitRecyclerView.setAdapter(null);
        }
    }

    @Override
    public int getItemCount() {
        return mWatchZonePresenters.size();
    }

    public void createAlarm(LatLng location, int radius) {
        mWatchZoneManager.createWatchZone(location, radius);
    }

    private WatchZoneManager.WatchZoneChangeListener mWatchZoneChangeListener =
            new WatchZoneManager.WatchZoneChangeListener() {
        @Override
        public void onWatchZoneUpdated(Long createdTimestamp) {
            Log.d(TAG, "onWatchZoneUpdated " + createdTimestamp);
            for (WatchZonePresenter p : mWatchZonePresenters) {
                if (p.watchZone.getCreatedTimestamp() == createdTimestamp) {
                    p.watchZone = mWatchZoneManager.getWatchZone(createdTimestamp);
                }
            }
            notifyDataSetChanged();
        }

        @Override
        public void onWatchZoneCreated(Long createdTimestamp) {
            Log.d(TAG, "onWatchZoneCreated " + createdTimestamp);
            UpdatingPresenter presenter = new UpdatingPresenter(
                    mWatchZonePresenters.size(), mWatchZoneManager.getWatchZone(createdTimestamp));
            mWatchZonePresenters.add(presenter);

            notifyDataSetChanged();
        }

        @Override
        public void onWatchZoneDeleted(Long createdTimestamp) {
            Log.d(TAG, "onWatchZoneDeleted " + createdTimestamp);
            int position = -1;
            for (int i = 0; i < mWatchZonePresenters.size(); i++) {
                WatchZonePresenter p = mWatchZonePresenters.get(i);
                if (p.watchZone.getCreatedTimestamp() == createdTimestamp) {
                    position = i;
                }
            }
            if (position > -1) {
                mWatchZonePresenters.remove(position);
            }
            for (int i = position; i < mWatchZonePresenters.size(); i++) {
                mWatchZonePresenters.get(i).position--;
            }
            notifyItemRemoved(position);
        }
    };

    /*public class LoadAlarmSweepingPositionsTask extends AsyncTask<Void, String, List<SweepingAddress>> {
        private WatchZone mAlarm;
        private UpdatingPresenter mAlarmPresenter;

        public LoadAlarmSweepingPositionsTask(UpdatingPresenter alarmPresenter, WatchZone watchZone) {
            mAlarm = watchZone;
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
                mWatchZonePresenters.remove(mAlarmPresenter.position);
                mWatchZonePresenters.add(mAlarmPresenter.position, newPresenter);
                notifyItemChanged(mAlarmPresenter.position);

                mAlarm.setSweepingAddresses(sweepingAddresses);
                //mWatchZoneManager.saveWatchZone(mAlarm);

                //new LoadAlarmsTask(newPresenter, mAlarm).execute();
            }
            super.onPostExecute(sweepingAddresses);
        }
    }*/

    public class LoadAlarmsTask extends AsyncTask<Void, Void, Void> {

        public LoadAlarmsTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            mWatchZoneManager = new WatchZoneManager(mContext);
            mWatchZoneManager.addWatchZoneChangeListener(mWatchZoneChangeListener);

            return null;
        }
        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
            for (Long createdTimestamp : mWatchZoneManager.getWatchZones()) {
                WatchZone watchZone = mWatchZoneManager.getWatchZone(createdTimestamp);

                if (mWatchZoneManager.getUpdatingWatchZones().contains(createdTimestamp)) {
                    UpdatingPresenter presenter = new UpdatingPresenter(
                            mWatchZonePresenters.size(), watchZone);
                    mWatchZonePresenters.add(presenter);
                } else {
                    NonUpdatingWatchZonePresenter presenter =
                            new NonUpdatingWatchZonePresenter(mWatchZonePresenters.size(), watchZone);
                    mWatchZonePresenters.add(presenter);
                }
            }
            notifyDataSetChanged();
        }
    }

    abstract class WatchZonePresenter {
        int position;
        WatchZone watchZone;
        boolean isDeleted;

        WatchZonePresenter(int position, WatchZone watchZone) {
            this.position = position;
            this.watchZone = watchZone;
            isDeleted = false;
        }

        abstract String getTitle();
        abstract String getRadius();
        abstract String getStatus();

        public void setDeleted() {
            isDeleted = true;
        }
    }

    class UpdatingPresenter extends WatchZonePresenter implements
            WatchZoneUpdateManager.WatchZoneProgressListener {
        String progress = "Refreshing watchZone location. \nProgress: 0%";

        UpdatingPresenter(int position, WatchZone watchZone) {
            super(position, watchZone);
            mWatchZoneManager.addWatchZoneProgressListener(this);
            setProgress(mWatchZoneManager.getProgressForWatchZone(watchZone.getCreatedTimestamp()));
        }

        @Override
        String getTitle() {
            if (TextUtils.isEmpty(watchZone.getAddress()) || "Unknown".equals(watchZone.getAddress())) {
                return "Fetching address...";
            }
            return WordUtils.capitalize(watchZone.getAddress());
        }

        @Override
        String getRadius() {
            return watchZone.getRadius() + "ft radius";
        }

        @Override
        String getStatus() {
            return progress;
        }

        void setProgress(int progress) {
            this.progress = String.format("Refreshing watchZone location. \nProgress: %d%%", progress);
            notifyItemChanged(this.position);
        }

        @Override
        public void onWatchZoneUpdateComplete(long createdTimestamp) {
            if (mWatchZonePresenters.contains(this) &&
                    createdTimestamp == this.watchZone.getCreatedTimestamp()) {
                NonUpdatingWatchZonePresenter newPresenter =
                        new NonUpdatingWatchZonePresenter(this.position, this.watchZone);
                mWatchZonePresenters.remove(this.position);
                mWatchZonePresenters.add(this.position, newPresenter);
                notifyItemChanged(this.position);

                mWatchZoneManager.removeWatchZoneProgressListener(this);
            }
        }

        @Override
        public void onWatchZoneUpdateProgress(long createdTimestamp, int progress) {
            if (createdTimestamp == this.watchZone.getCreatedTimestamp()) {
                setProgress(progress);
            }
        }
    }

    class NonUpdatingWatchZonePresenter extends WatchZonePresenter implements
            WatchZoneUpdateManager.WatchZoneProgressListener {
        WatchZone watchZone;

        LimitViewAdapter adapter;

        NonUpdatingWatchZonePresenter(int position, WatchZone watchZone) {
            super(position, watchZone);
            this.watchZone = watchZone;

            this.adapter = new LimitViewAdapter(mContext, this.watchZone);

            mWatchZoneManager.addWatchZoneProgressListener(this);
        }

        @Override
        String getTitle() {
            return WordUtils.capitalize(watchZone.getAddress());
        }

        @Override
        String getRadius() {
            return watchZone.getRadius() + "ft radius";
        }

        @Override
        String getStatus() {
            if (watchZone.getSweepingAddresses().isEmpty()) {
                if (AddressValidatorManager.getInstance(mContext).getValidationProgress()
                        != AddressValidatorManager.INVALID_PROGRESS) {
                    return "Database update in progress, please wait.";
                } else {
                    return "No sweeping information found.";
                }
            } else {
                return "";
            }
        }

        @Override
        public void onWatchZoneUpdateComplete(long createdTimestamp) {
        }

        @Override
        public void onWatchZoneUpdateProgress(long createdTimestamp, int progress) {
            if (mWatchZonePresenters.contains(this) &&
                    createdTimestamp == this.watchZone.getCreatedTimestamp()) {
                UpdatingPresenter newPresenter =
                        new UpdatingPresenter(this.position, this.watchZone);
                newPresenter.setProgress(progress);
                mWatchZonePresenters.remove(this.position);
                mWatchZonePresenters.add(this.position, newPresenter);
                notifyItemChanged(this.position);

                mWatchZoneManager.removeWatchZoneProgressListener(this);
            }
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        private final Context mContext;

        public TextView mAlarmAddress;
        public TextView mRadius;
        public TextView mStatus;
        public Button mRefreshButton;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        public RecyclerView mLimitRecyclerView;
        private RecyclerView.LayoutManager mLayoutManager;
        private WatchZoneViewItemDecoration mLimitViewItemDecoration;

        public ViewHolder(Context context, View v) {
            super(v);
            mContext = context;
            mAlarmAddress = (TextView) v.findViewById(R.id.textview_alarm_address);
            mRadius = (TextView) v.findViewById(R.id.textview_alarm_radius);
            mStatus = (TextView) v.findViewById(R.id.textview_status);
            mRefreshButton = (Button) v.findViewById(R.id.button_refresh);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);
            mLimitRecyclerView = (RecyclerView) v.findViewById(R.id.limit_recycler_view);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);

            mLimitRecyclerView.setHasFixedSize(true);
            mLayoutManager = new LinearLayoutManager(mContext, LinearLayout.HORIZONTAL, false);
            mLimitRecyclerView.setLayoutManager(mLayoutManager);

            int itemMargin =
                    mContext.getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
            mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

            mLimitRecyclerView.addItemDecoration(mLimitViewItemDecoration);

            RecyclerView.ItemAnimator animator = mLimitRecyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }
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