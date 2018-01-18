package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.Observer;
import android.content.Context;
import android.util.AttributeSet;
import android.view.View;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.ProgressBar;
import android.widget.RelativeLayout;
import android.widget.TextView;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.utils.SummaryHelper;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;

import org.apache.commons.lang3.text.WordUtils;

public class ShortSummaryLayout extends RelativeLayout {

    private WatchZoneModel mWatchZoneModel = null;
    private SummaryAction mSummaryAction = SummaryAction.None;
    private int mUpdatingProgress = -1;

    public enum SummaryAction {
        None,
        Save,
        Customize
    }

    public interface SummaryLayoutCallback {
        void onSummaryActionClicked();
        void onLayoutClicked();
        void onMoreInfoClicked();
    }

    private TextView mLabel;
    private ProgressBar mProgressBar;
    private ImageView mStatusIcon;
    private TextView mStatusText;
    private LinearLayout mSummaryDetails;
    private TextView mSummaryPostedLimits;
    private TextView mSummaryStreets;
    private RelativeLayout mLayoutMoreInfo;
    private Button mActionButton;

    private SummaryLayoutCallback mCallback;

    private Observer<WatchZoneModel> mObserver;

    public ShortSummaryLayout(Context context) {
        super(context);
        init();
    }

    public ShortSummaryLayout(Context context, AttributeSet attrs) {
        super(context, attrs);
        init();
    }

    public ShortSummaryLayout(Context context, AttributeSet attrs, int defStyle) {
        super(context, attrs, defStyle);
        init();
    }

    private void init() {
        inflate(getContext(), R.layout.layout_watchzone_summary_short, this);

        mLabel = findViewById(R.id.text_watch_zone_label);
        mProgressBar = findViewById(R.id.progressbar_updating_watchzone);
        mStatusIcon = findViewById(R.id.imageview_status_icon);
        mStatusText = findViewById(R.id.textview_status);
        mSummaryDetails = findViewById(R.id.layout_summary_details);
        mSummaryPostedLimits = findViewById(R.id.textview_number_posted_limits);
        mSummaryStreets = findViewById(R.id.textview_number_streets);
        mLayoutMoreInfo = findViewById(R.id.layout_more_info_group);
        mActionButton = findViewById(R.id.button_summary_action);
    }

    public void setWatchZoneModel(WatchZoneModel watchZoneModel) {
        mWatchZoneModel = watchZoneModel;
        setPresentation();
    }

    public void setSummaryAction(SummaryAction summaryAction) {
        mSummaryAction = summaryAction;
        setPresentation();
    }

    public void setUpdatingProgress(int progress) {
        mUpdatingProgress = progress;
        setPresentation();
    }

    public void set(WatchZoneModel watchZoneModel, SummaryAction summaryAction, int progress) {
        mWatchZoneModel = watchZoneModel;
        mSummaryAction = summaryAction;
        mUpdatingProgress = progress;
        setPresentation();
    }

    public void setCallback(SummaryLayoutCallback callback) {
        mCallback = callback;
    }

    private void setPresentation() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        mLabel.setText(WordUtils.capitalize(label));

        mLayoutMoreInfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onMoreInfoClicked();
                }
            }
        });
        setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onLayoutClicked();
                }
            }
        });
        mActionButton.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                if (mCallback != null) {
                    mCallback.onSummaryActionClicked();
                }
            }
        });
        // Any status could be being updated by the WatchZoneModelUpdater...
        Integer progress = null;
        if (mUpdatingProgress != -1) {
            progress = mUpdatingProgress;
        }

        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            SummaryHelper.WatchZoneModelSummary summary = SummaryHelper.getStatusFromModel(mWatchZoneModel);

            switch (summary.summaryStatus) {
                case NOW:
                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_red_24dp));
                    mStatusText.setText("Happening now!");
                    break;
                case SOON:
                    mStatusIcon.setBackground(
                        getResources().getDrawable(R.drawable.ic_local_parking_yellow_24dp));
                    mStatusText.setText(SummaryHelper.getNextSweepingString(summary.dateForStatus));
                    break;
                case LATER:
                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_green_24dp));
                    mStatusText.setText(SummaryHelper.getNextSweepingString(summary.dateForStatus));
                    break;
                case NEVER:
                    mStatusIcon.setBackground(
                            getResources().getDrawable(R.drawable.ic_local_parking_black_24dp));
                    mStatusText.setText("No sweeping in this zone.");
                    break;
            }
        }

        if (mSummaryAction == SummaryAction.None) {
            mActionButton.setVisibility(View.INVISIBLE);
        } else if (mSummaryAction == SummaryAction.Customize) {
            mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_customize_black_18dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_customize);
            mActionButton.setVisibility(View.VISIBLE);
        } else if (mSummaryAction == SummaryAction.Save) {
            mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                    getResources().getDrawable(R.drawable.ic_favorite_black_18dp),
                    null, null, null);
            mActionButton.setText(R.string.summary_action_save);
            mActionButton.setVisibility(View.VISIBLE);
        }
    }
}
