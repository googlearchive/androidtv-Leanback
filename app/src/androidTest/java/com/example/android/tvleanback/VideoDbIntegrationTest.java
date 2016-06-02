package com.example.android.tvleanback;

import android.content.ContentValues;
import android.content.Intent;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;

import com.example.android.tvleanback.data.FetchVideoService;
import com.example.android.tvleanback.data.VideoContract;
import com.example.android.tvleanback.data.VideoDbBuilder;
import com.example.android.tvleanback.data.VideoDbHelper;
import com.example.android.tvleanback.ui.MainActivity;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import org.junit.runner.RunWith;

import java.io.IOException;
import java.util.List;

@RunWith(AndroidJUnit4.class)
public class VideoDbIntegrationTest extends ActivityInstrumentationTestCase2<MainActivity> {

    public VideoDbIntegrationTest() {
        super(MainActivity.class);
    }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        injectInstrumentation(InstrumentationRegistry.getInstrumentation());
        getActivity();
    }

    @Test
    public void resetAndRedownloadDatabase() throws InterruptedException {
        VideoDbHelper mVideoDbHelper = new VideoDbHelper(getActivity());
        // Clear database by downgrading
        mVideoDbHelper.onDowngrade(mVideoDbHelper.getReadableDatabase(), 0, 0);
        String[] queryColumns = new String[] {
                VideoContract.VideoEntry._ID,
                VideoContract.VideoEntry.COLUMN_NAME,
                VideoContract.VideoEntry.COLUMN_CATEGORY,
                VideoContract.VideoEntry.COLUMN_DESC,
                VideoContract.VideoEntry.COLUMN_VIDEO_URL,
                VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL,
                VideoContract.VideoEntry.COLUMN_STUDIO,
        };
        Cursor mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                null,
                null,
                null,
                null,
                null
        );
        assertEquals(mCursor.getCount(), 0); // Confirm database is empty
        mCursor.close();
        try {
            getActivity().startService(new Intent(getActivity(), FetchVideoService.class));
            Thread.sleep(1000*30);
            mCursor = mVideoDbHelper.getReadableDatabase().query(
                    VideoContract.VideoEntry.TABLE_NAME,
                    queryColumns,
                    null,
                    null,
                    null,
                    null,
                    null
            );
            assertFalse(mCursor.getCount() == 0); // Confirm database is no longer empty
            mCursor.close();
        } catch (InterruptedException e) {
            e.printStackTrace();
            throw new InterruptedException("Thread was interrupted, cannot check download");
        }
    }

    @Test
    public void resetAndInsertLocalVideos() throws JSONException {
        VideoDbHelper mVideoDbHelper = new VideoDbHelper(getActivity());
        mVideoDbHelper.onDowngrade(mVideoDbHelper.getReadableDatabase(), 0, 0);
        String[] queryColumns = new String[] {
                VideoContract.VideoEntry._ID,
                VideoContract.VideoEntry.COLUMN_NAME,
                VideoContract.VideoEntry.COLUMN_CATEGORY,
                VideoContract.VideoEntry.COLUMN_DESC,
                VideoContract.VideoEntry.COLUMN_VIDEO_URL,
                VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL,
                VideoContract.VideoEntry.COLUMN_STUDIO,
        };
        Cursor mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                null,
                null,
                null,
                null,
                null
        );
        assertEquals(mCursor.getCount(), 0); // Confirm database is empty
        mCursor.close();

        // Create some test videos
        JSONArray mediaArray = new JSONArray();
        JSONObject video1 = new JSONObject();
        video1.put(VideoDbBuilder.TAG_TITLE, "New Dad")
                .put(VideoDbBuilder.TAG_DESCRIPTION, "Google+ Instant Upload backs up your photos")
                .put(VideoDbBuilder.TAG_STUDIO, "Google+");
        JSONObject video2 = new JSONObject();
        video2.put(VideoDbBuilder.TAG_TITLE, "Pet Dog")
                .put(VideoDbBuilder.TAG_DESCRIPTION, "Google+ lets you share videos of your pets")
                .put(VideoDbBuilder.TAG_STUDIO, "Google+");
        mediaArray.put(video1);
        JSONObject myMediaGooglePlus = new JSONObject();
        myMediaGooglePlus.put(VideoDbBuilder.TAG_CATEGORY, "Google+")
                .put(VideoDbBuilder.TAG_MEDIA, mediaArray);
        JSONObject myMedia = new JSONObject();
        JSONArray mediaCategories = new JSONArray();
        mediaCategories.put(myMediaGooglePlus);
        myMedia.put(VideoDbBuilder.TAG_GOOGLE_VIDEOS, mediaCategories);

        VideoDbBuilder videoDbBuilder = new VideoDbBuilder(getActivity());
        List<ContentValues> contentValuesList = videoDbBuilder.buildMedia(myMedia);
        ContentValues[] downloadedVideoContentValues = contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
        getActivity().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                downloadedVideoContentValues);

        // Test our makeshift database
        mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                null,
                null,
                null,
                null,
                null
        );
        assert(mCursor.getCount() == 2); // Confirm database was populated
        mCursor.close();

        mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                VideoContract.VideoEntry.COLUMN_NAME+" = ?",
                new String[] {"New Dad"},
                null,
                null,
                null
        );
        assert(mCursor.moveToFirst());
        assert(mCursor.getString(mCursor.getColumnIndexOrThrow(VideoContract.VideoEntry.COLUMN_STUDIO)).equals("Google+"));
        mCursor.close();
    }

    @Test
    public void resetAndInsertOnlineVideos() throws JSONException, IOException {
        VideoDbHelper mVideoDbHelper = new VideoDbHelper(getActivity());
        mVideoDbHelper.onDowngrade(mVideoDbHelper.getReadableDatabase(), 0, 0);
        String[] queryColumns = new String[] {
                VideoContract.VideoEntry._ID,
                VideoContract.VideoEntry.COLUMN_NAME,
                VideoContract.VideoEntry.COLUMN_CATEGORY,
                VideoContract.VideoEntry.COLUMN_DESC,
                VideoContract.VideoEntry.COLUMN_VIDEO_URL,
                VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL,
                VideoContract.VideoEntry.COLUMN_STUDIO,
        };
        Cursor mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                null,
                null,
                null,
                null,
                null
        );
        assertEquals(mCursor.getCount(), 0); // Confirm database is empty
        mCursor.close();

        // Create some test videos
        VideoDbBuilder videoDbBuilder = new VideoDbBuilder(getActivity());
        List<ContentValues> contentValuesList = videoDbBuilder.fetch(getActivity().getResources().getString(R.string.catalog_url));
        // Insert into database
        ContentValues[] downloadedVideoContentValues = contentValuesList.toArray(new ContentValues[contentValuesList.size()]);
        getActivity().getContentResolver().bulkInsert(VideoContract.VideoEntry.CONTENT_URI,
                downloadedVideoContentValues);

        // Test our makeshift database
        mCursor = mVideoDbHelper.getReadableDatabase().query(
                VideoContract.VideoEntry.TABLE_NAME,
                queryColumns,
                null,
                null,
                null,
                null,
                null
        );
        assertFalse(mCursor.getCount() == 0); // Confirm database was populated
        mCursor.close();
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}
