package com.example.joseph.sweepersd.archived.model.limits;

import android.content.Context;
import android.util.Log;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by joseph on 8/27/16.
 */
public class FileLimitImporter implements LimitDbHelper.LimitImporter {
    private static final String TAG = FileLimitImporter.class.getSimpleName();

    @Override
    public List<Limit> importLimits(Context context) {
        List<Limit> limits = new ArrayList<>();

        try {
            for (int i = 1; i < 10; i++) {
                String filename = "district" + i + ".txt";
                InputStream is = context.getAssets().open(filename);
                BufferedReader in=
                        new BufferedReader(new InputStreamReader(is, "UTF-8"));
                String str;

                while ((str=in.readLine()) != null) {
                    Limit l = LimitParser.buildLimitFromLine(str);
                    if (l != null) {
                        limits.add(l);
                    } else {
                        Log.w(TAG, "Parsed bad Limit line: " + str);
                    }
                }

                in.close();
                is.close();
            }
        } catch (IOException e) {
            Log.e(TAG, e.toString());
        }

        return limits;
    }
}
