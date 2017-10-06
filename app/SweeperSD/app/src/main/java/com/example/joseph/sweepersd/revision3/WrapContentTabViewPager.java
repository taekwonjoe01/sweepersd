package com.example.joseph.sweepersd.revision3;

import android.content.Context;
import android.support.v4.view.ViewPager;
import android.util.AttributeSet;
import android.util.Log;
import android.view.View;

public class WrapContentTabViewPager extends ViewPager {
    public WrapContentTabViewPager(Context context) {
        super(context);
    }

    public WrapContentTabViewPager(Context context, AttributeSet attrs) {
        super(context, attrs);
    }

    @Override
    protected void onMeasure(int widthMeasureSpec, int heightMeasureSpec) {
        int height = 0;
        if (getChildCount() > 0) {
            for (int i = 1; i < getChildCount(); i++) {
                View child = getChildAt(i);
                child.measure(widthMeasureSpec, MeasureSpec.makeMeasureSpec(0, MeasureSpec.UNSPECIFIED));
                int h = child.getMeasuredHeight();
                if (h > height) height = h;
            }

            heightMeasureSpec = MeasureSpec.makeMeasureSpec(height, MeasureSpec.EXACTLY)
                    + getChildAt(0).getMeasuredHeight();
        }

        super.onMeasure(widthMeasureSpec, heightMeasureSpec);
    }
}
