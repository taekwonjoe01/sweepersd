package com.example.joseph.sweepersd.watchzone;

import android.arch.lifecycle.LifecycleOwner;
import android.arch.lifecycle.Observer;
import android.arch.lifecycle.ViewModel;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.util.DiffUtil;
import android.support.v7.util.ListUpdateCallback;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;

import com.example.joseph.sweepersd.R;
import com.example.joseph.sweepersd.watchzone.model.WatchZone;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModel;
import com.example.joseph.sweepersd.watchzone.model.WatchZoneModelRepository;
import com.example.joseph.sweepersd.watchzone.model.WatchZonePoint;
import com.google.android.gms.maps.GoogleMap;
import com.google.android.gms.maps.OnMapReadyCallback;
import com.google.android.gms.maps.SupportMapFragment;
import com.google.android.gms.maps.model.Circle;
import com.google.android.gms.maps.model.MarkerOptions;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

public class WatchZoneMapFragment extends Fragment implements OnMapReadyCallback {
    private GoogleMap mGoogleMap;

    private List<WatchZoneViewModel> mValidatedModels = new ArrayList<>();
    private List<WatchZoneModel> mNonValidatedModels = new ArrayList<>();

    @Nullable
    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container, @Nullable Bundle savedInstanceState) {
        View v = inflater.inflate(R.layout.content_watchzone_map_fragment, container, false);

        // Obtain the SupportMapFragment and get notified when the map is ready to be used.
        SupportMapFragment mapFragment = (SupportMapFragment) getActivity().getSupportFragmentManager()
                .findFragmentById(R.id.map);
        mapFragment.getMapAsync(this);
        return v;
    }

    @Override
    public void onMapReady(GoogleMap googleMap) {
        mGoogleMap = googleMap;
        invalidate();
    }

    public void setWatchZoneModels(List<WatchZoneModel> models) {
        mNonValidatedModels = models;
    }

    private void invalidate() {
        if (mGoogleMap == null) {
            return;
        }

        if (mNonValidatedModels == null) {
            return;
        }

        DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
            @Override
            public int getOldListSize() {
                return mValidatedModels.size();
            }

            @Override
            public int getNewListSize() {
                return mNonValidatedModels.size();
            }

            @Override
            public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                return mValidatedModels.get(oldItemPosition).mModel.getWatchZoneUid() ==
                        mNonValidatedModels.get(newItemPosition).getWatchZoneUid();
            }

            @Override
            public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                return !mValidatedModels.get(oldItemPosition).mModel
                        .isChanged(mNonValidatedModels.get(newItemPosition));
            }
        }, false);
        result.dispatchUpdatesTo(new ListUpdateCallback() {
            @Override
            public void onInserted(int position, int count) {
                for (int i = 0 ; i < count; i++) {
                    int index = position + i;
                    WatchZoneModel model = mNonValidatedModels.get(index);
                    WatchZoneViewModel viewModel = new WatchZoneViewModel(model);
                    mValidatedModels.add(index, viewModel);
                    viewModel.add();
                }
            }

            @Override
            public void onRemoved(int position, int count) {
                for (int i = 0 ; i < count; i++) {
                    int index = position + i;
                    WatchZoneViewModel viewModel = mValidatedModels.remove(index);
                    viewModel.remove();
                }
            }

            @Override
            public void onMoved(int fromPosition, int toPosition) {
                // Detect moves is false.
            }

            @Override
            public void onChanged(int position, int count, Object payload) {
                for (int i = 0 ; i < count; i++) {
                    int index = position + i;
                    WatchZoneViewModel viewModel = mValidatedModels.get(index);
                    viewModel.update();
                }
            }
        });
    }

    abstract class WatchZonePointsViewModel {
        final LifecycleOwner mOwner;
        final WatchZoneModel mModel;
        final Observer<WatchZoneModel> mObserver = new Observer<WatchZoneModel>() {
            @Override
            public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
                update();
            }
        };

        protected List<WatchZonePoint> mWatchZonePoints = new ArrayList<>();

        abstract void onWatchZonePointAdded(int position);
        abstract void onWatchZonePointRemoved(int position);
        abstract void onWatchZonePointUpdated(int position);

        public WatchZonePointsViewModel(LifecycleOwner owner, WatchZoneModel model) {
            mOwner = owner;
            mModel = model;
        }

        void update() {
            WatchZone watchZone = mModel.getWatchZone();
            if (watchZone != null) {
                final List<WatchZonePoint> watchZonePoints =
                        mModel.getWatchZonePointsModel().getWatchZonePointsList();
                DiffUtil.DiffResult result = DiffUtil.calculateDiff(new DiffUtil.Callback() {
                    @Override
                    public int getOldListSize() {
                        return mWatchZonePoints.size();
                    }

                    @Override
                    public int getNewListSize() {
                        return watchZonePoints.size();
                    }

                    @Override
                    public boolean areItemsTheSame(int oldItemPosition, int newItemPosition) {
                        return mWatchZonePoints.get(oldItemPosition).getUid() ==
                                watchZonePoints.get(newItemPosition).getUid();
                    }

                    @Override
                    public boolean areContentsTheSame(int oldItemPosition, int newItemPosition) {
                        return !mWatchZonePoints.get(oldItemPosition).isChanged(
                                watchZonePoints.get(newItemPosition));
                    }
                }, false);
                result.dispatchUpdatesTo(new ListUpdateCallback() {
                    @Override
                    public void onInserted(int position, int count) {
                        for (int i = 0; i < count; i++) {
                            int index = position + i;
                            mWatchZonePoints.add(index, watchZonePoints.get(index));
                            onWatchZonePointAdded(index);
                        }
                    }

                    @Override
                    public void onRemoved(int position, int count) {
                        for (int i = 0; i < count; i++) {
                            int index = position + i;
                            onWatchZonePointRemoved(index);
                            mWatchZonePoints.remove(index);
                        }
                    }

                    @Override
                    public void onMoved(int fromPosition, int toPosition) {
                        // Do nothing.
                    }

                    @Override
                    public void onChanged(int position, int count, Object payload) {
                        for (int i = 0; i < count; i++) {
                            int index = position + i;
                            onWatchZonePointUpdated(index);
                        }
                    }
                });
            }
        }
    }

    abstract class WatchZoneViewModel {
        final LifecycleOwner mOwner;
        final Long mWatchZoneUid;
        final Observer<WatchZoneModelRepository> mObserver = new Observer<WatchZoneModelRepository>() {
            @Override
            public void onChanged(@Nullable WatchZoneModelRepository repository) {
                update();
            }
        };

        protected WatchZone mWatchZone;
        protected List<WatchZonePoint> mWatchZonePoints;

        abstract void onWatchZoneUpdated();

        public WatchZoneViewModel(LifecycleOwner owner, Long watchZoneUid) {
            mOwner = owner;
            mWatchZoneUid = watchZoneUid;
            WatchZoneModelRepository.getInstance()
        }

        void update() {
            WatchZone watchZone = mModel.getWatchZone();
            if (watchZone != null) {
                if (mWatchZone == null || mWatchZone.isChanged(watchZone)) {
                    mWatchZone = watchZone;
                    onWatchZoneUpdated();
                }
            }
        }
    }

    abstract class WatchZoneLimitsViewModel {
        final LifecycleOwner mOwner;
        final WatchZoneModel mModel;
        final Observer<WatchZoneModel> mObserver = new Observer<WatchZoneModel>() {
            @Override
            public void onChanged(@Nullable WatchZoneModel watchZoneModel) {
                update();
            }
        };

        protected WatchZone mWatchZone;
        protected List<WatchZonePoint> mWatchZonePoints;


        abstract void onWatchZoneUpdated();
        abstract void onWatchZonePointsUpdated(DiffUtil.DiffResult result);
        abstract void onLimitModelsUpdated();

        public WatchZoneViewModel(LifecycleOwner owner, WatchZoneModel model) {
            mOwner = owner;
            mModel = model;
        }
    }
}
