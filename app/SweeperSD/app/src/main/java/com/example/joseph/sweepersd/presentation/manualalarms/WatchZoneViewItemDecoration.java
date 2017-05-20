package com.example.joseph.sweepersd.presentation.manualalarms;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by joseph on 6/8/16.
 */
public class WatchZoneViewItemDecoration extends RecyclerView.ItemDecoration {

    private final int mVerticalSpaceHeight;

    public WatchZoneViewItemDecoration(int mVerticalSpaceHeight) {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.right = mVerticalSpaceHeight;
        if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = mVerticalSpaceHeight;
        }
    }
}