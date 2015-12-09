package com.example.android.tvleanback.model;

import android.database.Cursor;
import android.support.v17.leanback.database.CursorMapper;

import com.example.android.tvleanback.data.VideoContract;

/**
 * VideoCursorMapper maps a database Cursor to a Video object.
 */
public final class VideoCursorMapper extends CursorMapper {

    private static int idIndex;
    private static int nameIndex;
    private static int descIndex;
    private static int videoUrlIndex;
    private static int bgImageUrlIndex;
    private static int cardImageUrlIndex;
    private static int studioIndex;
    private static int categoryIndex;

    @Override
    protected void bindColumns(Cursor cursor) {
        idIndex = cursor.getColumnIndex(VideoContract.VideoEntry._ID);
        nameIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_NAME);
        descIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_DESC);
        videoUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_VIDEO_URL);
        bgImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_BG_IMAGE_URL);
        cardImageUrlIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CARD_IMG);
        studioIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_STUDIO);
        categoryIndex = cursor.getColumnIndex(VideoContract.VideoEntry.COLUMN_CATEGORY);
    }

    @Override
    protected Object bind(Cursor cursor) {

        // Get the values of the video.
        long id = cursor.getLong(idIndex);
        String category = cursor.getString(categoryIndex);
        String title = cursor.getString(nameIndex);
        String desc = cursor.getString(descIndex);
        String videoUrl = cursor.getString(videoUrlIndex);
        String bgImageUrl = cursor.getString(bgImageUrlIndex);
        String cardImageUrl = cursor.getString(cardImageUrlIndex);
        String studio = cursor.getString(studioIndex);

        // Build a Video object to be processed.
        return new Video.VideoBuilder()
                .id(id)
                .title(title)
                .category(category)
                .description(desc)
                .videoUrl(videoUrl)
                .bgImageUrl(bgImageUrl)
                .cardImageUrl(cardImageUrl)
                .studio(studio)
                .build();
    }
}
