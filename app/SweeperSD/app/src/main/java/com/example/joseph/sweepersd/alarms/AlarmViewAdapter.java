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

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.SweepingPosition;
import com.example.joseph.sweepersd.utils.LocationUtils;
import com.google.android.gms.maps.model.LatLng;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
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
            mAlarmPresenters.add(new LoadedAlarmPresenter(mAlarmPresenters.size(), alarm));
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
    public void onBindViewHolder(ViewHolder holder, int position) {
        holder.mAlarmAddress.setText(mAlarmPresenters.get(position).getTitle());
        holder.mNextSweeping.setText(mAlarmPresenters.get(position).getDescription());
    }

    @Override
    public int getItemCount() {
        return mAlarmPresenters.size();
    }

    public void createAlarm(Location location, int radius) {
        LatLng center = new LatLng(location.getLatitude(), location.getLongitude());
        int position = mAlarmPresenters.size();
        LoadingAlarmPresenter presenter = new LoadingAlarmPresenter(position);
        mAlarmPresenters.add(presenter);

        CreateAlarmTask alarmTask = new CreateAlarmTask(presenter, center, radius);
        alarmTask.execute();

        notifyDataSetChanged();
    }

    public class CreateAlarmTask extends AsyncTask<Void, String, Alarm> {
        private LatLng mCenter;
        private int mRadius;
        private LoadingAlarmPresenter mAlarmPresenter;

        public CreateAlarmTask(LoadingAlarmPresenter alarmPresenter, LatLng center, int radius) {
            mCenter = center;
            mRadius = radius;
            mAlarmPresenter = alarmPresenter;
        }

        @Override
        protected Alarm doInBackground(Void... params) {
            List<LatLng> latLngs = LocationUtils.getLatLngsInRadius(mCenter, mRadius);

            List<SweepingPosition> sweepingPositions = new ArrayList<>();

            String centerAddress  = LocationUtils.getAddressForLatLnt(mContext, mCenter);
            sweepingPositions.add(
                    new SweepingPosition(LocationUtils.findLimitForAddress(centerAddress),
                            mCenter, centerAddress));
            for (int i = 0; i < latLngs.size(); i++) {
                LatLng latLng = latLngs.get(i);
                int progress = (int) (((double)i / (double)latLngs.size()) * 100);
                String progressUpdate = String.format("Loading address information: %d%%", progress);
                publishProgress(progressUpdate);
                String address  = LocationUtils.getAddressForLatLnt(mContext, mCenter);
                sweepingPositions.add(
                        new SweepingPosition(LocationUtils.findLimitForAddress(address),
                                latLng, address));
            }
            for (LatLng latLng : latLngs) {
            }
            return new Alarm(sweepingPositions);
        }

        @Override
        protected void onProgressUpdate(String... values) {
            super.onProgressUpdate(values);
            mAlarmPresenter.setProgress(values[0]);
        }

        @Override
        protected void onPostExecute(Alarm alarm) {
            LoadedAlarmPresenter newPresenter = new LoadedAlarmPresenter(mAlarmPresenter.position, alarm);
            mAlarmPresenters.remove(mAlarmPresenter.position);
            mAlarmPresenters.add(mAlarmPresenter.position, newPresenter);
            notifyItemChanged(mAlarmPresenter.position);
            super.onPostExecute(alarm);
        }
    }

    abstract class AlarmPresenter {
        int position;
        AlarmPresenter(int position) {
            this.position = position;
        }

        abstract String getTitle();
        abstract String getDescription();
    }

    class LoadingAlarmPresenter extends AlarmPresenter {
        String progress = "Loading addresses in radius: 0%";

        LoadingAlarmPresenter(int position) {
            super(position);
        }

        @Override
        String getTitle() {
            return "Creating Alarm";
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

        LoadedAlarmPresenter(int position, Alarm alarm) {
            super(position);
            this.alarm = alarm;
        }

        @Override
        String getTitle() {
            return this.alarm.getSweepingPositions().get(0).getAddress();
        }

        @Override
        String getDescription() {
            SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMMM d 'at' ha");
            fmt.setCalendar(this.alarm.getNextSweepingDate());
            String dateFormatted = fmt.format(
                    this.alarm.getNextSweepingDate().getTime());
            return dateFormatted;
        }
    }

    public static class ViewHolder extends RecyclerView.ViewHolder implements
            View.OnClickListener, View.OnLongClickListener {
        public TextView mAlarmAddress;
        public TextView mNextSweeping;
        private FrameLayout mViewLayout;

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
            Toast.makeText(v.getContext(), "Show alarm details", Toast.LENGTH_SHORT).show();
        }

        @Override
        public boolean onLongClick(View v) {
            Toast.makeText(v.getContext(), "Delete alarm", Toast.LENGTH_SHORT).show();
            return true;
        }
    }
}