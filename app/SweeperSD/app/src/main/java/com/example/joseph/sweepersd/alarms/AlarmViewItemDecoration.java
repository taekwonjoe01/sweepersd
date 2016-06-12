package com.example.joseph.sweepersd.alarms;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

/**
 * Created by joseph on 6/8/16.
 */
public class AlarmViewItemDecoration extends RecyclerView.ItemDecoration {

    private final int mVerticalSpaceHeight;

    public AlarmViewItemDecoration(int mVerticalSpaceHeight) {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        if (parent.getChildAdapterPosition(view) != parent.getAdapter().getItemCount() - 1) {
            outRect.bottom = mVerticalSpaceHeight;
        }
    }
}