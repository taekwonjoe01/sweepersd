package com.example.joseph.sweepersd.watchzone;

import android.graphics.Rect;
import android.support.v7.widget.RecyclerView;
import android.view.View;

public class LimitItemDecoration extends RecyclerView.ItemDecoration {

    private final int mVerticalSpaceHeight;

    public LimitItemDecoration(int mVerticalSpaceHeight) {
        this.mVerticalSpaceHeight = mVerticalSpaceHeight;
    }

    @Override
    public void getItemOffsets(Rect outRect, View view, RecyclerView parent,
                               RecyclerView.State state) {
        outRect.bottom = mVerticalSpaceHeight;
        outRect.right = mVerticalSpaceHeight;
    }
}