package com.example.joseph.sweepersd;

import androidx.fragment.app.Fragment;

public abstract class TabFragment extends Fragment {
    public abstract String getTabTitle();
    public abstract void setTabTitle(String tabTitle);
}
