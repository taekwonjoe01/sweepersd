package com.example.joseph.sweepersd.presentation.manualalarms;

import android.content.Context;
import android.content.Intent;
import android.os.AsyncTask;
import android.os.Bundle;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.Switch;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.model.watchzone.WatchZone;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneManager;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneUpdateManager;
import com.example.joseph.sweepersd.model.watchzone.WatchZoneUtils;
import com.google.android.gms.maps.model.LatLng;

import org.apache.commons.lang3.text.WordUtils;

import java.text.SimpleDateFormat;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

/**
 * Adapter for showing manual Alarms.
 */
public class WatchZoneViewAdapter extends RecyclerView.Adapter<WatchZoneViewAdapter.ViewHolder> {
    private static final String TAG = WatchZoneViewAdapter.class.getSimpleName();

    private final Context mContext;
    private final WatchZoneManager mWatchZoneManager;
    private final List<WatchZonePresenter> mWatchZonePresenters;

    private AsyncTask<Void, Long, Void> mLoadWatchZonesTask;

    public WatchZoneViewAdapter(Context context) {
        mContext = context;
        mWatchZonePresenters = new CopyOnWriteArrayList<>();

        mWatchZoneManager = new WatchZoneManager(mContext);
        mWatchZoneManager.addWatchZoneChangeListener(mWatchZoneChangeListener);

        for (Long timestamp : mWatchZoneManager.getWatchZones()) {
            mWatchZonePresenters.add(new LoadingWatchZonePresenter(mWatchZonePresenters.size(), timestamp));
        }

    }

    @Override
    public void onAttachedToRecyclerView(RecyclerView recyclerView) {
        super.onAttachedToRecyclerView(recyclerView);

        mLoadWatchZonesTask = new LoadWatchZonesTask();
        mLoadWatchZonesTask.execute();
    }

    @Override
    public void onDetachedFromRecyclerView(RecyclerView recyclerView) {
        super.onDetachedFromRecyclerView(recyclerView);

        mLoadWatchZonesTask.cancel(false);
    }

    @Override
    public WatchZoneViewAdapter.ViewHolder onCreateViewHolder(ViewGroup parent,
                                                              int viewType) {
        View v = LayoutInflater.from(parent.getContext())
                .inflate(R.layout.layout_watch_zone_list_item, parent, false);

        ViewHolder vh = new ViewHolder(mContext, v);
        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        final WatchZonePresenter presenter = mWatchZonePresenters.get(position);
        holder.mWatchZoneLabel.setText(presenter.getLabel());

        holder.mOnClickListener = new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                /*Toast.makeText(v.getContext(), "Show watchZone details", Toast.LENGTH_SHORT).show();

                GregorianCalendar today = new GregorianCalendar(
                        TimeZone.getTimeZone("America/Los_Angeles"), Locale.US);
                long alarmTime = today.getTime().getTime() + 20000;
                AlarmManager alarmMgr;
                PendingIntent alarmIntent;

                alarmMgr = (AlarmManager) mContext.getSystemService(Context.ALARM_SERVICE);
                Intent intent = new Intent(mContext, WatchZoneAlarmReceiver.class);
                intent.setType(presenter.watchZoneTimestamp + "");
                alarmIntent = PendingIntent.getBroadcast(mContext, 0, intent, 0);


                alarmMgr.set(AlarmManager.RTC_WAKEUP, alarmTime, alarmIntent);
                Log.i(TAG, "Alarm scheduled for " + new Date(alarmTime).toString());*/
                Intent intent = new Intent(mContext, WatchZoneDetailsActivity.class);
                Bundle b = new Bundle();
                b.putLong(WatchZoneDetailsActivity.KEY_WATCHZONE_ID, presenter.watchZoneTimestamp);
                intent.putExtras(b);
                mContext.startActivity(intent);
            }
        };
        holder.mLongClickListener = new View.OnLongClickListener() {
            @Override
            public boolean onLongClick(View v) {
                //mWatchZoneManager.deleteWatchZone(mWatchZonePresenters.get(presenter.position).watchZoneTimestamp);
                //Toast.makeText(v.getContext(), "Delete watchZone", Toast.LENGTH_SHORT).show();
                return true;
            }
        };
        if (presenter instanceof NonUpdatingWatchZonePresenter) {
            holder.mDetailsGroup.setVisibility(View.VISIBLE);
            holder.mLoadingGroup.setVisibility(View.INVISIBLE);
            long nextSweepingTime = WatchZoneUtils.getNextSweepingTimeFromAddresses(
                    ((NonUpdatingWatchZonePresenter) presenter).watchZone.getSweepingAddresses());
            String dateString = mContext.getResources().getString(R.string.watch_zone_no_sweeping);
            if (nextSweepingTime != 0) {
                SimpleDateFormat format = new SimpleDateFormat("EEE, MMM dd");
                dateString = format.format(nextSweepingTime);
            }
            holder.mAlarmNextSweeping.setText(dateString);
        } else {
            holder.mDetailsGroup.setVisibility(View.GONE);
            holder.mLoadingGroup.setVisibility(View.VISIBLE);
            if (presenter instanceof LoadingWatchZonePresenter) {
                holder.mLoadingProgress.setProgress(0);
            } else {
                // instanceof UpdatingWatchZonePresenter
                UpdatingPresenter p = (UpdatingPresenter) presenter;
                holder.mLoadingProgress.setProgress(p.progress);
            }
        }
    }

    @Override
    public int getItemCount() {
        return mWatchZonePresenters.size();
    }

    public void createAlarm(String label, LatLng location, int radius) {
        mWatchZoneManager.createWatchZone(label, location, radius);
    }

    private WatchZoneManager.WatchZoneChangeListener mWatchZoneChangeListener =
            new WatchZoneManager.WatchZoneChangeListener() {
        @Override
        public void onWatchZoneUpdated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneUpdated " + createdTimestamp);

            for (WatchZonePresenter p : mWatchZonePresenters) {
                if (p.watchZoneTimestamp == createdTimestamp) {
                    if (mWatchZoneManager.getUpdatingWatchZones().contains(createdTimestamp)) {
                        UpdatingPresenter presenter = new UpdatingPresenter(
                                p.position, createdTimestamp);
                        mWatchZonePresenters.remove(p.position);
                        mWatchZonePresenters.add(p.position, presenter);
                    } else {
                        NonUpdatingWatchZonePresenter presenter =
                                new NonUpdatingWatchZonePresenter(p.position, createdTimestamp);
                        mWatchZonePresenters.remove(p.position);
                        mWatchZonePresenters.add(p.position, presenter);
                    }
                    notifyItemChanged(p.position);
                }
            }
        }

        @Override
        public void onWatchZoneCreated(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneCreated " + createdTimestamp);
            UpdatingPresenter presenter = new UpdatingPresenter(
                    mWatchZonePresenters.size(), createdTimestamp);
            mWatchZonePresenters.add(presenter);

            notifyDataSetChanged();
        }

        @Override
        public void onWatchZoneDeleted(long createdTimestamp) {
            Log.d(TAG, "onWatchZoneDeleted " + createdTimestamp);
            int position = -1;
            for (int i = 0; i < mWatchZonePresenters.size(); i++) {
                WatchZonePresenter p = mWatchZonePresenters.get(i);
                Log.d(TAG, "timestamp: " + p.watchZoneTimestamp);
                if (p.watchZoneTimestamp == createdTimestamp) {
                    position = i;
                    Log.d(TAG, "position being set to: " + i);
                }
            }
            if (position > -1) {
                Log.d(TAG, "removing item at position: " + position);
                mWatchZonePresenters.remove(position);
                for (int i = position; i < mWatchZonePresenters.size(); i++) {
                    mWatchZonePresenters.get(i).position--;
                }
                notifyItemRemoved(position);
            }
        }
    };

    public class LoadWatchZonesTask extends AsyncTask<Void, Long, Void> {
        private int mPosition = 0;

        public LoadWatchZonesTask() {
        }

        @Override
        protected Void doInBackground(Void... params) {
            List<Long> timestamps = mWatchZoneManager.getWatchZones();
            for (Long timestamp : timestamps) {
                if (isCancelled()) {
                    return null;
                }
                mWatchZoneManager.getWatchZoneComplete(timestamp);
                publishProgress(timestamp);
            }

            return null;
        }

        @Override
        protected void onProgressUpdate(Long... timestamp) {
            super.onProgressUpdate(timestamp);
            if (mWatchZoneManager.getUpdatingWatchZones().contains(timestamp[0])) {
                UpdatingPresenter presenter = new UpdatingPresenter(
                        mPosition, timestamp[0]);
                mWatchZonePresenters.remove(mPosition);
                mWatchZonePresenters.add(mPosition, presenter);
            } else {
                NonUpdatingWatchZonePresenter presenter =
                        new NonUpdatingWatchZonePresenter(mPosition, timestamp[0]);
                mWatchZonePresenters.remove(mPosition);
                mWatchZonePresenters.add(mPosition, presenter);
            }
            notifyItemChanged(mPosition);
            mPosition++;
        }

        @Override
        protected void onPostExecute(Void aVoid) {
            super.onPostExecute(aVoid);
        }
    }

    abstract class WatchZonePresenter {
        int position;
        Long watchZoneTimestamp;
        boolean isDeleted;

        WatchZonePresenter(int position, Long watchZoneTimestamp) {
            this.position = position;
            this.watchZoneTimestamp = watchZoneTimestamp;
            isDeleted = false;
        }

        abstract String getLabel();
        abstract boolean getAlarmEnabled();

        public void setDeleted() {
            isDeleted = true;
        }
    }

    class LoadingWatchZonePresenter extends WatchZonePresenter {
        private WatchZone mWatchZoneBrief;

        LoadingWatchZonePresenter(int position, Long watchZoneTimestamp) {
            super(position, watchZoneTimestamp);
            mWatchZoneBrief = mWatchZoneManager.getWatchZoneBrief(watchZoneTimestamp);
        }

        @Override
        String getLabel() {
            return WordUtils.capitalize(mWatchZoneBrief.getLabel());
        }

        @Override
        boolean getAlarmEnabled() {
            return false;
        }
    }

    class UpdatingPresenter extends WatchZonePresenter implements
            WatchZoneUpdateManager.WatchZoneProgressListener {
        WatchZone watchZone;
        int progress;

        UpdatingPresenter(int position, Long watchZone) {
            super(position, watchZone);
            this.watchZone = mWatchZoneManager.getWatchZoneComplete(watchZoneTimestamp);
            mWatchZoneManager.addWatchZoneProgressListener(this);
            setProgress(mWatchZoneManager.getProgressForWatchZone(watchZone));
        }

        @Override
        String getLabel() {
            return WordUtils.capitalize(watchZone.getLabel());
        }

        @Override
        boolean getAlarmEnabled() {
            return false;
        }

        void setProgress(int progress) {
            this.progress = progress;
            notifyItemChanged(this.position);
        }

        @Override
        public void onWatchZoneUpdateComplete(long createdTimestamp) {
            if (mWatchZonePresenters.contains(this) &&
                    createdTimestamp == this.watchZoneTimestamp) {
                NonUpdatingWatchZonePresenter newPresenter =
                        new NonUpdatingWatchZonePresenter(this.position, createdTimestamp);
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

        NonUpdatingWatchZonePresenter(int position, Long watchZoneTimestamp) {
            super(position, watchZoneTimestamp);
            this.watchZone = mWatchZoneManager.getWatchZoneComplete(watchZoneTimestamp);
            mWatchZoneManager.addWatchZoneProgressListener(this);
        }

        @Override
        String getLabel() {
            return WordUtils.capitalize(watchZone.getLabel());
        }

        @Override
        boolean getAlarmEnabled() {
            return false;
        }

        @Override
        public void onWatchZoneUpdateComplete(long createdTimestamp) {
        }

        @Override
        public void onWatchZoneUpdateProgress(long createdTimestamp, int progress) {
            if (mWatchZonePresenters.contains(this) &&
                    createdTimestamp == this.watchZoneTimestamp) {
                UpdatingPresenter newPresenter =
                        new UpdatingPresenter(this.position, createdTimestamp);
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

        public TextView mWatchZoneLabel;
        public Switch mAlarmEnabled;
        public TextView mAlarmNextSweeping;
        public LinearLayout mLoadingGroup;
        public LinearLayout mDetailsGroup;
        public ProgressBar mLoadingProgress;
        private FrameLayout mViewLayout;
        public View.OnLongClickListener mLongClickListener;
        public View.OnClickListener mOnClickListener;

        //public RecyclerView mLimitRecyclerView;
        //private RecyclerView.LayoutManager mLayoutManager;
        //private WatchZoneViewItemDecoration mLimitViewItemDecoration;

        public ViewHolder(Context context, View v) {
            super(v);
            mContext = context;
            mWatchZoneLabel = (TextView) v.findViewById(R.id.textview_watchzone_label);
            mAlarmEnabled = (Switch) v.findViewById(R.id.switch_enable_alarm);
            mAlarmNextSweeping = (TextView) v.findViewById(R.id.textview_next_sweeping);
            mLoadingGroup = (LinearLayout) v.findViewById(R.id.watchzone_loading_group);
            mDetailsGroup = (LinearLayout) v.findViewById(R.id.watchzone_details_group);
            mLoadingProgress = (ProgressBar) v.findViewById(R.id.progress_loading);
            mViewLayout = (FrameLayout) v.findViewById(R.id.list_item_layout);

            mViewLayout.setOnClickListener(this);
            mViewLayout.setOnLongClickListener(this);

            //mLayoutManager = new LinearLayoutManager(mContext, LinearLayout.VERTICAL, false);
            //mLimitRecyclerView.setLayoutManager(mLayoutManager);

            //int itemMargin =
                   // mContext.getResources().getDimensionPixelSize(R.dimen.limit_view_item_space);
            //mLimitViewItemDecoration = new WatchZoneViewItemDecoration(itemMargin);

            //mLimitRecyclerView.addItemDecoration(mLimitViewItemDecoration);

            /*RecyclerView.ItemAnimator animator = mLimitRecyclerView.getItemAnimator();
            if (animator instanceof SimpleItemAnimator) {
                ((SimpleItemAnimator) animator).setSupportsChangeAnimations(false);
            }*/
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
}