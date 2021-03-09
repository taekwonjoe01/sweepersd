package com.example.joseph.sweepersd.watchzone;

import androidx.lifecycle.Observer;
import android.content.Context;
import androidx.appcompat.widget.PopupMenu;
import android.util.AttributeSet;
import android.view.MenuItem;
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
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;

import org.apache.commons.lang3.text.WordUtils;

public class ShortSummaryLayout extends RelativeLayout implements PopupMenu.OnMenuItemClickListener {

    private WatchZoneModel mWatchZoneModel = null;
    private SummaryDisplayMode mDisplayMode = SummaryDisplayMode.LIST;
    private int mUpdatingProgress = -1;

    public enum SummaryDisplayMode {
        LIST,
        LIST_SELECTED,
        EXPLORER,
        EXPLORER_TITLE,
        DETAILS,
        DETAILS_TITLE
    }

    public interface SummaryLayoutCallback {
        void onSummaryActionClicked();
        void onLayoutClicked();
        void onMoreInfoClicked();
    }

    private ImageView mMenu;
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

        mMenu = findViewById(R.id.imageview_menu);
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

    public void setDisplayMode(SummaryDisplayMode summaryDisplayMode) {
        mDisplayMode = summaryDisplayMode;
        if (mWatchZoneModel != null) {
            setPresentation();
        }
    }

    public void setUpdatingProgress(int progress) {
        mUpdatingProgress = progress;
        if (mWatchZoneModel != null) {
            setPresentation();
        }
    }

    public void set(WatchZoneModel watchZoneModel, SummaryDisplayMode summaryDisplayMode, int progress) {
        mWatchZoneModel = watchZoneModel;
        mDisplayMode = summaryDisplayMode;
        mUpdatingProgress = progress;
        setPresentation();
    }

    public void setCallback(SummaryLayoutCallback callback) {
        mCallback = callback;
    }

    /**
     * TODO - This should be generalized similar to the action button. Perhaps the parent should provide
     * the menu and listener interface for menu item clicks.
     */
    @Override
    public boolean onMenuItemClick(MenuItem item) {
        switch (item.getItemId()) {
            case R.id.action_delete:
                WatchZoneModelRepository.getInstance(getContext()).deleteWatchZone(mWatchZoneModel.watchZone.getUid());
                return true;
            case R.id.action_save:
                if (mCallback != null) {
                    mCallback.onSummaryActionClicked();
                }
                break;
        }
        return false;
    }

    private void setList() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.backgroundWhite));

        mMenu.setVisibility(View.GONE);

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.secondaryTextColor));

        mLayoutMoreInfo.setVisibility(View.VISIBLE);
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

        mStatusText.setTextColor(getResources().getColor(R.color.secondaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }

        mActionButton.setVisibility(View.INVISIBLE);
    }

    private void setListSelected() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.backgroundWhite));

        mMenu.setVisibility(View.GONE);

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.secondaryTextColor));

        mLayoutMoreInfo.setVisibility(View.VISIBLE);
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

        mStatusText.setTextColor(getResources().getColor(R.color.secondaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }

        mActionButton.setVisibility(View.INVISIBLE);
    }

    private void setExplorer() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.backgroundWhite));

        mMenu.setVisibility(View.GONE);

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.secondaryTextColor));

        mLayoutMoreInfo.setVisibility(View.VISIBLE);
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

        mStatusText.setTextColor(getResources().getColor(R.color.secondaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }

        mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_favorite_black_18dp),
                null, null, null);
        mActionButton.setText(R.string.summary_action_save);
        mActionButton.setVisibility(View.VISIBLE);
    }

    private void setExplorerTitle() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.primaryColor));

        final PopupMenu popup = new PopupMenu(getContext(), mMenu);
        popup.getMenuInflater().inflate(R.menu.menu_activity_explorer, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        mMenu.setVisibility(View.VISIBLE);
        mMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.primaryTextColor));

        mLayoutMoreInfo.setVisibility(View.GONE);
        mActionButton.setVisibility(View.INVISIBLE);
        // Any status could be being updated by the WatchZoneModelUpdater...
        Integer progress = null;
        if (mUpdatingProgress != -1) {
            progress = mUpdatingProgress;
        }

        mStatusText.setTextColor(getResources().getColor(R.color.primaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }
    }

    private void setDetails() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.backgroundWhite));

        mMenu.setVisibility(View.GONE);

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.secondaryTextColor));

        mLayoutMoreInfo.setVisibility(View.VISIBLE);
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

        mStatusText.setTextColor(getResources().getColor(R.color.secondaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }

        mActionButton.setCompoundDrawablesWithIntrinsicBounds(
                getResources().getDrawable(R.drawable.ic_customize_black_18dp),
                null, null, null);
        mActionButton.setText(R.string.summary_action_customize);
        // TODO - Set to visible when customization is available.
        mActionButton.setVisibility(View.INVISIBLE);
    }

    private void setDetailsTitle() {
        final WatchZone watchZone = mWatchZoneModel.watchZone;
        String label = watchZone.getLabel();

        setBackgroundColor(getResources().getColor(R.color.primaryColor));

        final PopupMenu popup = new PopupMenu(getContext(), mMenu);
        popup.getMenuInflater().inflate(R.menu.menu_activity_details, popup.getMenu());
        popup.setOnMenuItemClickListener(this);
        mMenu.setVisibility(View.VISIBLE);
        mMenu.setOnClickListener(new OnClickListener() {
            @Override
            public void onClick(View v) {
                popup.show();
            }
        });

        mLabel.setText(WordUtils.capitalize(label));
        mLabel.setTextColor(getResources().getColor(R.color.primaryTextColor));

        mLayoutMoreInfo.setVisibility(View.GONE);
        mActionButton.setVisibility(View.INVISIBLE);
        // Any status could be being updated by the WatchZoneModelUpdater...
        Integer progress = null;
        if (mUpdatingProgress != -1) {
            progress = mUpdatingProgress;
        }

        mStatusText.setTextColor(getResources().getColor(R.color.primaryTextColor));
        if (progress != null) {
            mStatusIcon.setVisibility(View.GONE);
            mProgressBar.setVisibility(View.VISIBLE);
            mStatusText.setText("Updating Watch Zone (" + progress + "%)...");
        } else {
            mStatusIcon.setVisibility(View.VISIBLE);
            mProgressBar.setVisibility(View.GONE);

            setSummaryTextAndIcon();
        }
    }

    private void setSummaryTextAndIcon() {
        SummaryHelper.WatchZoneModelSummary summary = SummaryHelper.getStatusFromModel(mWatchZoneModel);

        switch (summary.summaryStatus) {
            case NOW:
                mStatusIcon.setBackground(
                        getResources().getDrawable(R.drawable.ic_local_parking_red_24dp));
                mStatusText.setText("Street sweeping happening now!");
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

    private void setPresentation() {
        switch (mDisplayMode) {
            case LIST:
                setList();
                break;
            case LIST_SELECTED:
                setListSelected();
                break;
            case EXPLORER:
                setExplorer();
                break;
            case EXPLORER_TITLE:
                setExplorerTitle();
                break;
            case DETAILS:
                setDetails();
                break;
            case DETAILS_TITLE:
                setDetailsTitle();
                break;
        }
    }
}
