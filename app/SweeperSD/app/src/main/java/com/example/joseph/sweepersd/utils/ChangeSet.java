package com.example.joseph.sweepersd.utils;

import java.util.ArrayList;
import java.util.List;

/**
 * Created by josephhutchins on 11/14/17.
 */
public class ChangeSet {
    public List<Long> addedUids;
    public List<Long> changedUids;
    public List<Long> removedUids;

    public ChangeSet() {
        addedUids = new ArrayList<>();
        changedUids = new ArrayList<>();
        removedUids = new ArrayList<>();
    }
}
