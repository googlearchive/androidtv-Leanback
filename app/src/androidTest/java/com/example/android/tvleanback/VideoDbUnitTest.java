package com.example.android.tvleanback;

import android.content.ContentValues;
import android.database.Cursor;
import android.support.test.InstrumentationRegistry;
import android.support.test.runner.AndroidJUnit4;
import android.test.ActivityInstrumentationTestCase2;
import android.test.AndroidTestCase;
import android.util.Log;

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
public class VideoDbUnitTest extends AndroidTestCase {

    private static final String TAG = "VideoDbTest";

    public VideoDbUnitTest() { }

    @Before
    public void setUp() throws Exception {
        super.setUp();
        Log.d(TAG, "Setting up");
    }

    @Test
    public void getVideosFromLocalJson() throws JSONException {
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

        VideoDbBuilder videoDbBuilder = new VideoDbBuilder();
        List<ContentValues> contentValuesList = videoDbBuilder.buildMedia(myMedia);
        assert(contentValuesList.get(0).getAsString(VideoContract.VideoEntry.COLUMN_CATEGORY).equals("Google+"));
        assert(contentValuesList.get(1).getAsString(VideoContract.VideoEntry.COLUMN_CATEGORY).equals("Google+"));
        assert(contentValuesList.get(0).getAsString(VideoContract.VideoEntry.COLUMN_STUDIO).equals("Google+"));
        assert(contentValuesList.get(1).getAsString(VideoContract.VideoEntry.COLUMN_STUDIO).equals("Google+"));
        assert(contentValuesList.get(0).getAsString(VideoContract.VideoEntry.COLUMN_NAME).equals("New Dad"));
        assert(contentValuesList.get(1).getAsString(VideoContract.VideoEntry.COLUMN_NAME).equals("Pet Dog"));
        assert(contentValuesList.get(1).getAsString(VideoContract.VideoEntry.COLUMN_DESC).equals("Google+ lets you share videos of your pets"));
    }

    @Test
    public void getVideosFromServer() throws IOException, JSONException {
        String serverUrl = "https://storage.googleapis.com/android-tv/android_tv_videos_new.json";
        VideoDbBuilder videoDbBuilder = new VideoDbBuilder();
        List<ContentValues> contentValuesList = videoDbBuilder.fetch(serverUrl);
        assert(contentValuesList.size() > 0);
        assert(!contentValuesList.get(0).getAsString(VideoContract.VideoEntry.COLUMN_NAME).isEmpty());
    }

    @After
    public void tearDown() throws Exception {
        super.tearDown();
    }
}