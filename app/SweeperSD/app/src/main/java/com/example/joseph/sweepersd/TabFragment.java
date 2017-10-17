package com.example.joseph.sweepersd;

import android.support.v4.app.Fragment;

public abstract class TabFragment extends Fragment {
    public abstract String getTabTitle();
    public abstract void setTabTitle(String tabTitle);
}
