package com.example.joseph.sweepersd.watchzone;

import android.os.Bundle;
import android.support.annotation.Nullable;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.TabFragment;

/**
 * Created by josephhutchins on 10/17/17.
 */

public class WatchZoneNotificationsTabFragment extends TabFragment {

    private String mTabTitle;


    public WatchZoneNotificationsTabFragment() {

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
        View view = inflater.inflate(R.layout.content_tab_watchzone_notification_settings,
                container, false);

        return view;
    }
}
