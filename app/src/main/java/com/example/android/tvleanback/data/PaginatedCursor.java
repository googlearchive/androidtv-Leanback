/*
 * Copyright (C) 2015 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.example.android.tvleanback.data;

import android.database.AbstractCursor;
import android.database.Cursor;

/**
 * A sample paginated cursor which will pre-fetch and cache rows.
 */
public class PaginatedCursor extends AbstractCursor {
    /**
     * The number of items that should be loaded each time.
     */
    private static final int PAGE_SIZE = 10;

    /**
     * The threshold of number of items left that a new page should be loaded.
     */
    private static final int PAGE_THRESHOLD = PAGE_SIZE / 2;

    private final Cursor mCursor;
    private final int mRowCount;
    private final boolean[] mCachedRows;
    private final String[] mColumnNames;
    private final int mColumnCount;
    private final int[] mColumnTypes;

    private final byte[][][] mByteArrayDataCache;
    private final float[][] mFloatDataCache;
    private final int[][] mIntDataCache;
    private final String[][] mStringDataCache;
    /**
     * Index mapping from column index into the data type specific cache index;
     */
    private final int[] mByteArrayCacheIndexMap;
    private final int[] mFloatCacheIndexMap;
    private final int[] mIntCacheIndexMap;
    private final int[] mStringCacheIndexMap;
    private int mByteArrayCacheColumnSize;
    private int mFloatCacheColumnSize;
    private int mIntCacheColumnSize;
    private int mStringCacheColumnSize;
    private int mLastCachePosition;

    public PaginatedCursor(Cursor cursor) {
        super();
        mCursor = cursor;
        mRowCount = mCursor.getCount();
        mCachedRows = new boolean[mRowCount];
        mColumnNames = mCursor.getColumnNames();
        mColumnCount = mCursor.getColumnCount();
        mColumnTypes = new int[mColumnCount];

        mByteArrayCacheColumnSize = 0;
        mFloatCacheColumnSize = 0;
        mIntCacheColumnSize = 0;
        mStringCacheColumnSize = 0;

        mByteArrayCacheIndexMap = new int[mColumnCount];
        mFloatCacheIndexMap = new int[mColumnCount];
        mIntCacheIndexMap = new int[mColumnCount];
        mStringCacheIndexMap = new int[mColumnCount];

        mCursor.moveToFirst();
        for (int i = 0; i < mColumnCount; i++) {
            int type = mCursor.getType(i);
            mColumnTypes[i] = type;
            switch (type) {
                case Cursor.FIELD_TYPE_BLOB:
                    mByteArrayCacheIndexMap[i] = mByteArrayCacheColumnSize++;
                    break;
                case Cursor.FIELD_TYPE_FLOAT:
                    mFloatCacheIndexMap[i] = mFloatCacheColumnSize++;
                    break;
                case Cursor.FIELD_TYPE_INTEGER:
                    mIntCacheIndexMap[i] = mIntCacheColumnSize++;
                    break;
                case Cursor.FIELD_TYPE_STRING:
                    mStringCacheIndexMap[i] = mStringCacheColumnSize++;
                    break;
            }
        }

        mByteArrayDataCache = mByteArrayCacheColumnSize > 0 ? new byte[mRowCount][][] : null;
        mFloatDataCache = mFloatCacheColumnSize > 0 ? new float[mRowCount][] : null;
        mIntDataCache = mIntCacheColumnSize > 0 ? new int[mRowCount][] : null;
        mStringDataCache = mStringCacheColumnSize > 0 ? new String[mRowCount][] : null;

        for (int i = 0; i < mRowCount; i++) {
            mCachedRows[i] = false;
            if (mByteArrayDataCache != null) {
                mByteArrayDataCache[i] = new byte[mByteArrayCacheColumnSize][];
            }
            if (mFloatDataCache != null) {
                mFloatDataCache[i] = new float[mFloatCacheColumnSize];
            }
            if (mIntDataCache != null) {
                mIntDataCache[i] = new int[mIntCacheColumnSize];
            }
            if (mStringDataCache != null) {
                mStringDataCache[i] = new String[mStringCacheColumnSize];
            }
        }

        // Cache at the initialization stage.
        loadCacheStartingFromPosition(0);
    }

    /**
     * Try to load un-cached data with size {@link PAGE_SIZE} starting from given index.
     */
    private void loadCacheStartingFromPosition(int index) {
        mCursor.moveToPosition(index);
        for (int row = index; row < (index + PAGE_SIZE) && row < mRowCount; row++) {
            if (!mCachedRows[row]) {
                for (int col = 0; col < mColumnCount; col++) {
                    switch (mCursor.getType(col)) {
                        case Cursor.FIELD_TYPE_BLOB:
                            mByteArrayDataCache[row][mByteArrayCacheIndexMap[col]] =
                                    mCursor.getBlob(col);
                            break;
                        case Cursor.FIELD_TYPE_FLOAT:
                            mFloatDataCache[row][mFloatCacheIndexMap[col]] = mCursor.getFloat(col);
                            break;
                        case Cursor.FIELD_TYPE_INTEGER:
                            mIntDataCache[row][mIntCacheIndexMap[col]] = mCursor.getInt(col);
                            break;
                        case Cursor.FIELD_TYPE_STRING:
                            mStringDataCache[row][mStringCacheIndexMap[col]] =
                                    mCursor.getString(col);
                            break;
                    }
                }
                mCachedRows[row] = true;
            }
            mCursor.moveToNext();
        }
        mLastCachePosition = Math.min(index + PAGE_SIZE, mRowCount) - 1;
    }

    @Override
    public boolean onMove(int oldPosition, int newPosition) {
        // If it's a consecutive move and haven't exceeds the threshold, do nothing.
        if ((newPosition - oldPosition) != 1 ||
                (newPosition + PAGE_THRESHOLD) <= mLastCachePosition) {
            loadCacheStartingFromPosition(newPosition);
        }
        return true;
    }

    @Override
    public int getType(int column) {
        return mColumnTypes[column];
    }

    @Override
    public int getCount() {
        return mRowCount;
    }

    @Override
    public String[] getColumnNames() {
        return mColumnNames;
    }

    @Override
    public String getString(int column) {
        return mStringDataCache[mPos][mStringCacheIndexMap[column]];
    }

    @Override
    public short getShort(int column) {
        return (short) mIntDataCache[mPos][mIntCacheIndexMap[column]];
    }

    @Override
    public int getInt(int column) {
        return mIntDataCache[mPos][mIntCacheIndexMap[column]];
    }

    @Override
    public long getLong(int column) {
        return mIntDataCache[mPos][mIntCacheIndexMap[column]];
    }

    @Override
    public float getFloat(int column) {
        return mFloatDataCache[mPos][mFloatCacheIndexMap[column]];
    }

    @Override
    public double getDouble(int column) {
        return mFloatDataCache[mPos][mFloatCacheIndexMap[column]];
    }

    @Override
    public byte[] getBlob(int column) {
        return mByteArrayDataCache[mPos][mByteArrayCacheIndexMap[column]];
    }

    @Override
    public boolean isNull(int column) {
        return mColumnTypes[column] == Cursor.FIELD_TYPE_NULL;
    }

}
