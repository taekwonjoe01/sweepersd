package com.example.joseph.sweepersd.alarms;

import android.location.Address;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.FrameLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.example.joseph.sweepersd.R;

import java.text.SimpleDateFormat;

/**
 * Adapter for showing manual Alarms.
 */
public class AlarmViewAdapter extends RecyclerView.Adapter<AlarmViewAdapter.ViewHolder> {
    private final AlarmModel mAlarmModel;

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

    public AlarmViewAdapter(AlarmModel alarmModel) {
        mAlarmModel = alarmModel;
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
        Address address =
                mAlarmModel.getAlarms().get(position).getLocationDetails().addresses.get(0);
        String addressString = address.getAddressLine(0);
        holder.mAlarmAddress.setText(addressString);

        SimpleDateFormat fmt = new SimpleDateFormat("EEEE, MMMM d 'at' ha");
        fmt.setCalendar(mAlarmModel.getAlarms().get(position).getNextSweepingDate());
        String dateFormatted = fmt.format(
                mAlarmModel.getAlarms().get(position).getNextSweepingDate().getTime());
        holder.mNextSweeping.setText(dateFormatted);

    }

    @Override
    public int getItemCount() {
        return mAlarmModel.getAlarms().size();
    }
}