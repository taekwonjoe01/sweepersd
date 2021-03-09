package com.example.joseph.sweepersd.watchzone;

import android.graphics.Rect;
import androidx.recyclerview.widget.RecyclerView;
import android.view.View;

public class WatchZoneViewItemDecoration extends RecyclerView.ItemDecoration {

    private final int mVerticalSpaceHeight;

    public WatchZoneViewItemDecoration(int mVerticalSpaceHeight) {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.bottom = mVerticalSpaceHeight;
        outRect.right = mVerticalSpaceHeight;
    }
}