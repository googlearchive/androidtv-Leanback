/*
 * Copyright (C) 2014 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License"); you may not use this file except
 * in compliance with the License. You may obtain a copy of the License at
 *
 * http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software distributed under the License
 * is distributed on an "AS IS" BASIS, WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express
 * or implied. See the License for the specific language governing permissions and limitations under
 * the License.
 */

package com.example.android.tvleanback.recommendation;

import android.content.ContentProvider;
import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.BitmapFactory;
import android.net.Uri;
import android.os.ParcelFileDescriptor;
import android.renderscript.Allocation;
import android.renderscript.Element;
import android.renderscript.RenderScript;
import android.renderscript.ScriptIntrinsicBlur;
import android.util.Log;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URL;

public class RecommendationContentProvider extends ContentProvider {

    public static String AUTHORITY = "com.example.android.tvleanback.provider";
    public static String CONTENT_URI = "content://" + AUTHORITY + "/";

    private Context mContext;

    @Override
    public ParcelFileDescriptor openFile(Uri uri, String mode) throws FileNotFoundException {
        ParcelFileDescriptor[] pipe = null;
        String path = uri.getPath();
        try {
            String url = path.replaceFirst("/", "");
            pipe = ParcelFileDescriptor.createPipe();
            new TransferThread(mContext, new URL(url).openStream(), new ParcelFileDescriptor.AutoCloseOutputStream(pipe[1])).start();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return pipe != null ? pipe[0] : null;
    }

    static class TransferThread extends Thread {
        InputStream in;
        OutputStream out;
        Context context;

        public TransferThread(final Context context, final InputStream in, final OutputStream out) {
            this.in = in;
            this.out = out;
            this.context = context;
        }

        @Override
        public void run() {
            Bitmap bitmap = createBlurBitmap(context, BitmapFactory.decodeStream(in));
            bitmap.compress(Bitmap.CompressFormat.JPEG, 100, out);
            try {
                in.close();
                out.flush();
                out.close();
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }

    @Override
    public boolean onCreate() {
        mContext = getContext();
        return true;
    }

    @Override
    public Cursor query(Uri uri, String[] projection,
                        String selection, String[] selectionArgs, String sortOrder) {
        return null;
    }

    @Override
    public String getType(Uri uri) {
        return "image/*";
    }

    @Override
    public Uri insert(Uri uri, ContentValues values) {
        return null;
    }

    @Override
    public int delete(Uri uri, String selection, String[] selectionArgs) {
        return 0;
    }

    @Override
    public int update(Uri uri, ContentValues values, String selection, String[] selectionArgs) {
        return 0;
    }

    private static Bitmap createBlurBitmap(final Context context, final Bitmap bitmap) {
        final RenderScript rs = RenderScript.create(context);
        final Allocation input = Allocation.createFromBitmap(rs, bitmap, Allocation.MipmapControl.MIPMAP_NONE, Allocation.USAGE_SCRIPT);
        final Allocation output = Allocation.createTyped(rs, input.getType());
        final ScriptIntrinsicBlur script = ScriptIntrinsicBlur.create(rs, Element.U8_4(rs));

        //change your blur radius
        script.setRadius(5.f);
        script.setInput(input);
        script.forEach(output);
        output.copyTo(bitmap);
        return bitmap;
    }

}