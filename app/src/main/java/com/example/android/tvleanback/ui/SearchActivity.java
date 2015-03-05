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

package com.example.android.tvleanback.ui;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v17.leanback.widget.SpeechRecognitionCallback;
import android.util.Log;

import com.example.android.tvleanback.R;

/*
 * SearchActivity for SearchFragment
 */
public class SearchActivity extends Activity {
    /**
     * Called when the activity is first created.
     */

    private static final String TAG = "SearchActivity";
    private static boolean DEBUG = true;
    /**
     * SpeechRecognitionCallback is not required and if not provided recognition will be handled
     * using internal speech recognizer, in which case you must have RECORD_AUDIO permission
     */
    private static final int REQUEST_SPEECH = 1;
    private SearchFragment mFragment;
    private SpeechRecognitionCallback mSpeechRecognitionCallback;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.search);

        mFragment = (SearchFragment) getFragmentManager().findFragmentById(R.id.search_fragment);

        mSpeechRecognitionCallback = new SpeechRecognitionCallback() {
            @Override
            public void recognizeSpeech() {
                if (DEBUG) Log.v(TAG, "recognizeSpeech");
                startActivityForResult(mFragment.getRecognizerIntent(), REQUEST_SPEECH);
            }
        };
        mFragment.setSpeechRecognitionCallback(mSpeechRecognitionCallback);
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        if (DEBUG) Log.v(TAG, "onActivityResult requestCode=" + requestCode +
                " resultCode=" + resultCode +
                " data=" + data);
        if (requestCode == REQUEST_SPEECH && resultCode == RESULT_OK) {
            mFragment.setSearchQuery(data, true);
        }
    }

    @Override
    public boolean onSearchRequested() {
        startActivity(new Intent(this, SearchActivity.class));
        return true;
    }
}
