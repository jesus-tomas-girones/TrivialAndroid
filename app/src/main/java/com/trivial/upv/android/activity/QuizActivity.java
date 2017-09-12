/*
 * Copyright 2015 Google Inc.
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

package com.trivial.upv.android.activity;

import android.animation.Animator;
import android.animation.AnimatorListenerAdapter;
import android.animation.AnimatorSet;
import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.annotation.TargetApi;
import android.app.Activity;
import android.app.AlertDialog;
import android.app.ProgressDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.graphics.Color;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.annotation.VisibleForTesting;
import android.support.design.widget.FloatingActionButton;
import android.support.test.espresso.contrib.CountingIdlingResource;
import android.support.v4.app.ActivityCompat;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.ViewPropertyAnimatorListenerAdapter;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.support.v4.view.animation.FastOutSlowInInterpolator;
import android.support.v7.app.AppCompatActivity;
import android.text.Html;
import android.util.Log;
import android.view.View;
import android.view.ViewAnimationUtils;
import android.view.Window;
import android.view.WindowManager;
import android.view.animation.Interpolator;
import android.widget.FrameLayout;
import android.widget.TextView;

import com.android.volley.toolbox.NetworkImageView;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesActivityResultCodes;
import com.google.android.gms.games.GamesStatusCodes;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessage;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMessageReceivedListener;
import com.google.android.gms.games.multiplayer.realtime.RealTimeMultiplayer;
import com.google.android.gms.games.multiplayer.realtime.Room;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.realtime.RoomStatusUpdateListener;
import com.google.android.gms.games.multiplayer.realtime.RoomUpdateListener;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.reflect.TypeToken;
import com.trivial.upv.android.R;
import com.trivial.upv.android.fragment.QuizFragment;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.ViewUtils;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.JsonAttributes;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.persistence.TopekaJSonHelper;
import com.trivial.upv.android.widget.TextSharedElementCallback;

import java.io.UnsupportedEncodingException;
import java.lang.reflect.Type;
import java.nio.charset.Charset;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;

import static com.google.android.gms.games.GamesStatusCodes.STATUS_OK;

public class QuizActivity extends AppCompatActivity implements
        /*GPG*/ RoomStatusUpdateListener, RoomUpdateListener, RealTimeMessageReceivedListener {

    private static final String TAG = "QuizActivity";
    private static final String IMAGE_CATEGORY = "image_category_";
    private static final String STATE_IS_PLAYING = "isPlaying";
    private static final String FRAGMENT_TAG = "Quiz";
    // JVG.S
    public static final String ARG_ONE_PLAYER = "PLAY_OFFLINE";
    public static final String ARG_ONLINE = "PLAY_TWO_PLAYERS";
    public static final int TIME_TO_ANSWER_PLAY_GAME = 3000;
    public static final String ARG_INVITATION_TO_PLAY = "INVITATION_TO_PLAY";
    // JVG.E
    private Interpolator mInterpolator;
    private Category mCategory;
    private QuizFragment mQuizFragment;
    private FloatingActionButton mQuizFab;
    private boolean mSavedStateIsPlaying;
    //JVG.S
    private NetworkImageView mIcon;
    private TextView mMoreinfo;
    private TextView mDescription;
    private TextView mVideo;
    //JVG.E
    private Animator mCircularReveal;
    private ObjectAnimator mColorChange;
    private CountingIdlingResource mCountingIdlingResource;
    private View mToolbarBack;


    private final View.OnClickListener mOnClickListener = new View.OnClickListener() {
        @Override
        public void onClick(final View v) {
            switch (v.getId()) {
                case R.id.fab_quiz:
                    startQuizFromClickOn(v);
                    break;
                case R.id.submitAnswer:
                    submitAnswer();
                    break;
                case R.id.quiz_done:
                    ActivityCompat.finishAfterTransition(QuizActivity.this);
                    break;
                case R.id.back:
                    onBackPressed();
                    break;
                default:
                    throw new UnsupportedOperationException(
                            "OnClick has not been implemented for " + getResources().
                                    getResourceName(v.getId()));
            }
        }
    };
    private TextView mLblMoreInfo;
    private TextView mLblVideo;


    public static Intent getStartIntent(Context context, Category category) {
        Intent starter = new Intent(context, QuizActivity.class);
        starter.putExtra(Category.TAG, category.getId());
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        mCountingIdlingResource = new CountingIdlingResource("Quiz");
        String categoryId = getIntent().getStringExtra(Category.TAG);
        mInterpolator = new FastOutSlowInInterpolator();
        if (null != savedInstanceState) {
            mSavedStateIsPlaying = savedInstanceState.getBoolean(STATE_IS_PLAYING);
        }
        super.onCreate(savedInstanceState);
        populate(categoryId);
        int categoryNameTextSize = getResources()
                .getDimensionPixelSize(R.dimen.category_item_text_size);
        int paddingStart = getResources().getDimensionPixelSize(R.dimen.spacing_double);
        final int startDelay = getResources().getInteger(R.integer.toolbar_transition_duration);
        ActivityCompat.setEnterSharedElementCallback(this,
                new TextSharedElementCallback(categoryNameTextSize, paddingStart) {
                    @Override
                    public void onSharedElementStart(List<String> sharedElementNames,
                                                     List<View> sharedElements,
                                                     List<View> sharedElementSnapshots) {
                        super.onSharedElementStart(sharedElementNames,
                                sharedElements,
                                sharedElementSnapshots);
                        mToolbarBack.setScaleX(0f);
                        mToolbarBack.setScaleY(0f);
                    }

                    @Override
                    public void onSharedElementEnd(List<String> sharedElementNames,
                                                   List<View> sharedElements,
                                                   List<View> sharedElementSnapshots) {
                        super.onSharedElementEnd(sharedElementNames,
                                sharedElements,
                                sharedElementSnapshots);
                        // Make sure to perform this animation after the transition has ended.
                        ViewCompat.animate(mToolbarBack)
                                .setStartDelay(startDelay)
                                .scaleX(1f)
                                .scaleY(1f)
                                .alpha(1f);
                    }
                });

        // JVG.S
        if (mCategory.getId().equals(ARG_ONLINE)) {
            iniciarPartidaEnTiempoReal();
        }
        // JVG.E
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    public void onRestoreInstanceState(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onRestoreInstanceState(savedInstanceState, persistentState);
    }

    @Override
    protected void onResume() {
        //        Handler to control the time for answer a quiz
        if (mHandlerPlayGame == null)
            mHandlerPlayGame = new Handler();

        if (mSavedStateIsPlaying) {

            mQuizFragment = (QuizFragment) getSupportFragmentManager().findFragmentByTag(
                    FRAGMENT_TAG);

            if (mQuizFragment != null) {
                if (!mQuizFragment.hasSolvedStateListener()) {
                    mQuizFragment.setSolvedStateListener(getSolvedStateListener());
                }
                findViewById(R.id.quiz_fragment_container).setVisibility(View.VISIBLE);
                mQuizFab.hide();
                mIcon.setVisibility(View.GONE);
            }
            if (mCategory.getId().equals(ARG_ONE_PLAYER)) {
                postDelayHandlerPlayGame();
            }
        } else {
            //JVG.S
            if (!mCategory.getId().equals(ARG_ONLINE)) {
                initQuizFragment();
            }
            //JVG.E
        }

        super.onResume();
    }

    @Override
    public void onSaveInstanceState(@NonNull Bundle outState) {
        mSavedStateIsPlaying = mQuizFab.getVisibility() == View.GONE;
        outState.putBoolean(STATE_IS_PLAYING, mSavedStateIsPlaying);
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onBackPressed() {
        leaveRoom();
        if (mIcon == null || mQuizFab == null) {
            // Skip the animation if icon or fab are not initialized.
            super.onBackPressed();
            return;
        }

        ViewCompat.animate(mToolbarBack)
                .scaleX(0f)
                .scaleY(0f)
                .alpha(0f)
                .setDuration(100)
                .start();

        // Scale the icon and fab to 0 size before calling onBackPressed if it exists.
        ViewCompat.animate(mIcon)
                .scaleX(.7f)
                .scaleY(.7f)
                .alpha(0f)
                .setInterpolator(mInterpolator)
                .start();

        ViewCompat.animate(mQuizFab)
                .scaleX(0f)
                .scaleY(0f)
                .setInterpolator(mInterpolator)
                .setStartDelay(100)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @SuppressLint("NewApi")
                    @Override
                    public void onAnimationEnd(View view) {
                        if (isFinishing() ||
                                (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.JELLY_BEAN_MR1)
                                        && isDestroyed())) {
                            return;
                        }
                        QuizActivity.super.onBackPressed();
                    }
                })
                .start();
    }

    private void startQuizFromClickOn(final View clickedView) {
        // JVG.S
        // mCategory = TopekaDatabaseHelper.getCategoryWith(this, categoryId);
        initializeQuizFragment(clickedView);
    }

    private void initializeQuizFragment(View clickedView) {
        // JVG.E
        initQuizFragment();
        getSupportFragmentManager()
                .beginTransaction()
                .replace(R.id.quiz_fragment_container, mQuizFragment, FRAGMENT_TAG)
                .commit();
        final FrameLayout container = (FrameLayout) findViewById(R.id.quiz_fragment_container);
        container.setBackgroundColor(ContextCompat.
                getColor(this, mCategory.getTheme().getWindowBackgroundColor()));
        revealFragmentContainer(clickedView, container);
        // the toolbar should not have more elevation than the content while playing
        setToolbarElevation(false);
    }

    private void revealFragmentContainer(final View clickedView,
                                         final FrameLayout fragmentContainer) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            revealFragmentContainerLollipop(clickedView, fragmentContainer);
        } else {
            fragmentContainer.setVisibility(View.VISIBLE);
            clickedView.setVisibility(View.GONE);
            mIcon.setVisibility(View.GONE);
        }
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void revealFragmentContainerLollipop(final View clickedView,
                                                 final FrameLayout fragmentContainer) {
        prepareCircularReveal(clickedView, fragmentContainer);

        ViewCompat.animate(clickedView)
                .scaleX(0)
                .scaleY(0)
                .alpha(0)
                .setInterpolator(mInterpolator)
                .setListener(new ViewPropertyAnimatorListenerAdapter() {
                    @Override
                    public void onAnimationEnd(View view) {
                        fragmentContainer.setVisibility(View.VISIBLE);
                        clickedView.setVisibility(View.GONE);
                    }
                })
                .start();

        fragmentContainer.setVisibility(View.VISIBLE);
        AnimatorSet animatorSet = new AnimatorSet();
        animatorSet.play(mCircularReveal).with(mColorChange);
        animatorSet.start();
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void prepareCircularReveal(View startView, FrameLayout targetView) {
        int centerX = (startView.getLeft() + startView.getRight()) / 2;
        // Subtract the start view's height to adjust for relative coordinates on screen.
        int centerY = (startView.getTop() + startView.getBottom()) / 2 - startView.getHeight();
        float endRadius = (float) Math.hypot(centerX, centerY);
        mCircularReveal = ViewAnimationUtils.createCircularReveal(
                targetView, centerX, centerY, startView.getWidth(), endRadius);
        mCircularReveal.setInterpolator(new FastOutLinearInInterpolator());

        mCircularReveal.addListener(new AnimatorListenerAdapter() {
            @Override
            public void onAnimationEnd(Animator animation) {
                mIcon.setVisibility(View.GONE);
                mCircularReveal.removeListener(this);
            }
        });
        // Adding a color animation from the FAB's color to transparent creates a dissolve like
        // effect to the circular reveal.
        int accentColor = ContextCompat.getColor(this, mCategory.getTheme().getAccentColor());
        mColorChange = ObjectAnimator.ofInt(targetView,
                ViewUtils.FOREGROUND_COLOR, accentColor, Color.TRANSPARENT);
        mColorChange.setEvaluator(new ArgbEvaluator());
        mColorChange.setInterpolator(mInterpolator);
    }

    @SuppressLint("NewApi")
    public void setToolbarElevation(boolean shouldElevate) {
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            mToolbarBack.setElevation(shouldElevate ?
                    getResources().getDimension(R.dimen.elevation_header) : 0);
        }
    }

    private void initQuizFragment() {
        if (mQuizFragment != null) {
            return;
        }
        mQuizFragment = QuizFragment.newInstance(mCategory.getId(), getSolvedStateListener());
        // the toolbar should not have more elevation than the content while playing
        setToolbarElevation(false);
    }

    @NonNull
    private QuizFragment.SolvedStateListener getSolvedStateListener() {
        return new QuizFragment.SolvedStateListener() {
            @Override
            public void onCategorySolved() {
                setResultSolved();
                setToolbarElevation(true);
                displayDoneFab();
            }

            private void displayDoneFab() {
                /* We're re-using the already existing fab and give it some
                 * new values. This has to run delayed due to the queued animation
                 * to hide the fab initially.
                 */
                if (null != mCircularReveal && mCircularReveal.isRunning()) {
                    mCircularReveal.addListener(new AnimatorListenerAdapter() {
                        @Override
                        public void onAnimationEnd(Animator animation) {
                            showQuizFabWithDoneIcon();
                            mCircularReveal.removeListener(this);
                        }
                    });
                } else {
                    showQuizFabWithDoneIcon();
                }
            }

            private void showQuizFabWithDoneIcon() {
                mQuizFab.setImageResource(R.drawable.ic_tick);
                mQuizFab.setId(R.id.quiz_done);
                mQuizFab.setVisibility(View.VISIBLE);
                mQuizFab.setScaleX(0f);
                mQuizFab.setScaleY(0f);
                ViewCompat.animate(mQuizFab)
                        .scaleX(1)
                        .scaleY(1)
                        .setInterpolator(mInterpolator)
                        .setListener(null)
                        .start();
            }
        };
    }

    private void setResultSolved() {
        Intent categoryIntent = new Intent();
        categoryIntent.putExtra(JsonAttributes.ID, mCategory.getId());
        setResult(R.id.solved, categoryIntent);
    }

    /**
     * Proceeds the quiz to it's next state.
     */
    public void proceed() {
        submitAnswer();
    }

    /**
     * Solely exists for testing purposes and making sure Espresso does not get confused.
     */
    public void lockIdlingResource() {
        mCountingIdlingResource.increment();
    }

    public boolean submitAnswer() {
        mCountingIdlingResource.decrement();
        if (!mQuizFragment.showNextPage()) {
            mQuizFragment.showSummary();
            setResultSolved();
            return false;
        }
        setToolbarElevation(false);
        return true;
    }

    @SuppressLint("NewApi")
    private void populate(String categoryId) {
        if (null == categoryId) {
            Log.w(TAG, "Didn't find a category. Finishing");
            finish();
        }
        // JVG.S
        // mCategory = TopekaDatabaseHelper.getCategoryWith(this, categoryId);
        if (categoryId.equals(ARG_ONE_PLAYER)) {
            mCategory = TopekaJSonHelper.getInstance(getBaseContext(), false).getCategoryPlayGameOffLine();
        } else if (categoryId.equals(ARG_ONLINE)) {
            mCategory = Game.category;
        } else {
            mCategory = TopekaJSonHelper.getInstance(getBaseContext(), false).getCategoryWith(categoryId);
        }
        // JVG.E

        setTheme(mCategory.getTheme().getStyleId());
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            Window window = getWindow();
            window.setStatusBarColor(ContextCompat.getColor(this,
                    mCategory.getTheme().getPrimaryDarkColor()));
        }
        initLayout(mCategory.getId());
        initToolbar(mCategory);
    }

    public static void openFile(Context context, String url) {
        // Create URI
        Uri uri = Uri.parse(url);

        Intent intent = new Intent(Intent.ACTION_VIEW);
        // Check what kind of file you are trying to open, by comparing the url with extensions.
        // When the if condition is matched, plugin sets the correct intent (mime) type,
        // so Android knew what application to use to open the file
        if (url.toString().contains(".doc") || url.toString().contains(".docx")) {
            // Word document
            intent.setDataAndType(uri, "application/msword");
        } else if (url.toString().contains(".pdf")) {
            // PDF file
            intent.setDataAndType(uri, "application/pdf");
        } else if (url.toString().contains(".ppt") || url.toString().contains(".pptx")) {
            // Powerpoint file
            intent.setDataAndType(uri, "application/vnd.ms-powerpoint");
        } else if (url.toString().contains(".xls") || url.toString().contains(".xlsx")) {
            // Excel file
            intent.setDataAndType(uri, "application/vnd.ms-excel");
        } else if (url.toString().contains(".zip") || url.toString().contains(".rar")) {
            // ZIP Files
            intent.setDataAndType(uri, "application/zip");
        } else if (url.toString().contains(".rtf")) {
            // RTF file
            intent.setDataAndType(uri, "application/rtf");
        } else if (url.toString().contains(".wav") || url.toString().contains(".mp3")) {
            // WAV audio file
            intent.setDataAndType(uri, "audio/x-wav");
        } else if (url.toString().contains(".gif")) {
            // GIF file
            intent.setDataAndType(uri, "image/gif");
        } else if (url.toString().contains(".jpg") || url.toString().contains(".jpeg") || url.toString().contains(".png")) {
            // JPG file
            intent.setDataAndType(uri, "image/jpeg");
        } else if (url.toString().contains(".txt")) {
            // Text file
            intent.setDataAndType(uri, "text/plain");
        } else if (url.toString().contains(".3gp") || url.toString().contains(".mpg") || url.toString().contains(".mpeg") || url.toString().contains(".mpe") || url.toString().contains(".mp4") || url.toString().contains(".avi")) {
            // Video files
            intent.setDataAndType(uri, "video/*");
        } else if (url.toString().startsWith("http")) {
            // HTML  file
            intent.setDataAndType(uri, "text/html");
        } else {
            //if you want you can also define the intent type for any other file

            //additionally use else clause below, to manage other unknown extensions
            //in this case, Android will show all applications installed on the device
            //so you can choose which application to use
            intent.setDataAndType(uri, "*/*");
        }

        intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
        context.startActivity(intent);

    }

    private void initLayout(String categoryId) {
        setContentView(R.layout.activity_quiz);
        //noinspection PrivateResource
        mIcon = (NetworkImageView) findViewById(R.id.icon);
        mDescription = (TextView) findViewById(R.id.descripton);

        mDescription = (TextView) findViewById(R.id.descripton);
        if (mCategory.getDescription() != null)
            mDescription.setText(mCategory.getDescription());

        mLblMoreInfo = (TextView) findViewById(R.id.lbl_moreinfo);
        mMoreinfo = (TextView) findViewById(R.id.more_info);
        if (mCategory.getMoreInfo() != null) {
            mMoreinfo.setText(mCategory.getMoreInfo());
        } else {
            mLblMoreInfo.setVisibility(View.GONE);
        }
        mMoreinfo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mMoreinfo.getText().toString().isEmpty())
                    openFile(QuizActivity.this, mMoreinfo.getText().toString());
            }
        });

        mLblVideo = (TextView) findViewById(R.id.lbl_video);
        mVideo = (TextView) findViewById(R.id.video);
        if (mCategory.getVideo() != null) {
            mVideo.setText(mCategory.getVideo());
        } else {
            mLblVideo.setVisibility(View.GONE);
        }
        mVideo.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!mVideo.getText().toString().isEmpty())
                    openFile(QuizActivity.this, mVideo.getText().toString());
            }
        });
        //JVG.S
//        int resId = getResources().getIdentifier(IMAGE_CATEGORY + categoryId, DRAWABLE,
//                getApplicationContext().getPackageName());
//        mIcon.setImageResource(resId);
//        mIcon.setImageResource(resId);

        mIcon.setFitsSystemWindows(true);
        mIcon.setImageUrl(mCategory.getImg(), VolleySingleton.getInstance(

                getBaseContext()).

                getImageLoader());

        //JVG.E
        ViewCompat.animate(mIcon)
                .

                        scaleX(1)
                .

                        scaleY(1)
                .

                        alpha(1)
                .

                        setInterpolator(mInterpolator)
                .

                        setStartDelay(300)
                .

                        start();

        mQuizFab = (FloatingActionButton)

                findViewById(R.id.fab_quiz);
        mQuizFab.setImageResource(R.drawable.ic_play);
        if (mSavedStateIsPlaying)

        {
            mQuizFab.hide();
        } else

        {
            mQuizFab.show();
        }
        mQuizFab.setOnClickListener(mOnClickListener);

        // JVG.S
        // mCategory = TopekaDatabaseHelper.getCategoryWith(this, categoryId);
        if (categoryId.equals(ARG_ONLINE) || mCategory.getQuizzes()==null)

        {
            mQuizFab.hide();
        }
        // JVG.E
    }

    private void initToolbar(Category category) {
        mToolbarBack = findViewById(R.id.back);
        mToolbarBack.setOnClickListener(mOnClickListener);
        TextView titleView = (TextView) findViewById(R.id.category_title);
        titleView.setText(category.getName());
        titleView.setTextColor(ContextCompat.getColor(this,
                category.getTheme().getTextPrimaryColor()));
        if (mSavedStateIsPlaying) {
            // the toolbar should not have more elevation than the content while playing
            setToolbarElevation(false);
        }
    }

    @SuppressWarnings("unused")
    @VisibleForTesting
    public CountingIdlingResource getCountingIdlingResource() {
        return mCountingIdlingResource;
    }


    //JVG.S
    public synchronized int getTimeToNextItem() {
        return timeToNextItem;
    }

    public synchronized void setTimeToNextItem(int timeToNextItem) {
        this.timeToNextItem = timeToNextItem;
    }

    public int timeToNextItem = 0;


    public boolean hasInitialicedAllPlayers() {
        Log.d("TRAZAGPG", "nº participantes" + Game.numParticipantsOK() + " finishedParticipants:" + Game.mFinishedParticipants.size());
        return (Game.numParticipantsOK() == Game.mFinishedParticipants.size());
    }

    private Runnable mRunnablePlayGame = new Runnable() {
        @Override
        public void run() {
//            Log.d("HANDLER", "RUN HANDLER");
            //JVG.E
            if (timeToNextItem <= 0) {
//                if (mCategory.getId().equals(ARG_ONE_PLAYER)) {
//                    mQuizFragment.getQuizView().getCurrentView().findViewById(R.id.submitAnswer).callOnClick();
//                }
//                else {
                // Finish;
                getQuizFragment().showSummary();
//                }
            } else {
                timeToNextItem -= 1000;
                mQuizFragment.setTimeLeftText(timeToNextItem);
                postDelayHandlerPlayGame();
            }
            // EJECUTO
        }
    };


    public void postDelayHandlerPlayGame() {
        if (mQuizFragment != null) {
            final int nextItem = mQuizFragment.getQuizView().getDisplayedChild() + 1;
            final int count = mQuizFragment.getQuizView().getAdapter().getCount();
            if (nextItem < count || (nextItem == count && timeToNextItem >= 0)) {
                mHandlerPlayGame.postDelayed(mRunnablePlayGame,
                        1000);
//                Log.d("HANDLER", "PROGRAMO_EJECUCIÓN: " + timeToNextItem + "-" + "nextItem=" + nextItem + "; count=" + count);
            } else {
//                Log.d("HANDLER", "YA NO PROGRAMO MÁS EJECUCIONES");
            }
        }
    }

    private Handler mHandlerPlayGame;
//JVG.E

    @Override
    protected void onStop() {
        //JVG.E
        cancelPostDelayHandlerPlayGame();
        if (mHandlerPlayGame != null) {
            mHandlerPlayGame = null;
        }

        Log.d(TAG, "**** got onStop");

        // if we're in a room, leave it.
        leaveRoom();

        // stop trying to keep the screen on
        stopKeepingScreenOn();

        //JVG.E
        super.onStop();
    }

    public void cancelPostDelayHandlerPlayGame() {
        if (mHandlerPlayGame != null)
            mHandlerPlayGame.removeCallbacks(mRunnablePlayGame);
//        Log.d("HANDLER", "DESPROGRAMO_EJECUCIÓN");
    }

    public QuizFragment getQuizFragment() {
        return mQuizFragment;
    }


    /****** GOOGLE PLAY GAMES**********************/
//JVG.S
    private void iniciarPartidaEnTiempoReal() {
// Create room waiting for a invitee
//        timeToNextItem = Game.totalTime;

        final int NUMERO_MINIMO_OPONENTES = Game.minAutoMatchPlayers, NUMERO_MAXIMO_OPONENTES = Game.maxAutoMatchPlayers;
        Bundle autoMatchCriteria;


        // Join to an invitation
        if (Game.mIncomingInvitationId != null) {
            // Accept the given invitation.
            Log.d(TAG, "Accepting invitation: " + Game.mIncomingInvitationId);
            RoomConfig.Builder roomConfigBuilder = RoomConfig.builder(this);
            roomConfigBuilder.setInvitationIdToAccept(Game.mIncomingInvitationId)
                    .setMessageReceivedListener(this)
                    .setRoomStatusUpdateListener(this);

            keepScreenOn();
            Game.resetGameVars();
            Game.timeStamp = System.currentTimeMillis();
            Games.RealTimeMultiplayer.join(Game.mGoogleApiClient, roomConfigBuilder.build());
            Game.mIncomingInvitationId = null;
            return;
        }

        // Create match
        // From invitation
        if (Game.invitees != null && Game.invitees.size() > 0) {

            // get the automatch criteria
            autoMatchCriteria = null;

            if (Game.minAutoMatchPlayers > 0 || Game.maxAutoMatchPlayers > 0) {
                autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                        Game.minAutoMatchPlayers, Game.maxAutoMatchPlayers, 0);
                Log.d(TAG, "Automatch criteria: " + autoMatchCriteria);
            }
        } else {
            // Create room for a quick match (automatchcriteria)
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES, 0);
        }

        // create the room
        Log.d(TAG, "Creating room...");
        RoomConfig.Builder rtmConfigBuilder = RoomConfig.builder(this);
        if (Game.invitees != null && Game.invitees.size() > 0)
            rtmConfigBuilder.addPlayersToInvite(Game.invitees);
        rtmConfigBuilder.setMessageReceivedListener(this);
        rtmConfigBuilder.setRoomStatusUpdateListener(this);
        if (autoMatchCriteria != null) {
            rtmConfigBuilder.setAutoMatchCriteria(autoMatchCriteria);
            rtmConfigBuilder.setVariant((int)Game.level);
        }
        keepScreenOn();
        Game.resetGameVars();
        Game.timeStamp = System.currentTimeMillis();
        Games.RealTimeMultiplayer.create(Game.mGoogleApiClient, rtmConfigBuilder.build());
        Log.d(TAG, "Room created, waiting for it to be ready...");
    }

    final static int RC_WAITING_ROOM = 10002;
//    int jugadorLocal = 1;

    StringBuilder sbMessageData = null;

    @Override
    public void onRealTimeMessageReceived(RealTimeMessage realTimeMessage) {
        synchronized (this) {
            Log.d("TRAZAGPG", "RECEIVE: " + realTimeMessage.getMessageData().toString());
            byte[] messageData = realTimeMessage.getMessageData();
            try {
                String tmpDataMsg = new String(messageData, "UTF-8");
                Log.d("TRAZAGPG", "RECEIVE: " + tmpDataMsg);
                switch (tmpDataMsg.charAt(0)) {
                    case 'P':
//                        Toast.makeText(this, "Your oppenent do " + (int) messageData[1] + " PTS", Toast.LENGTH_LONG).show();
//                        initQuizFragment();
                        byte[] auxData = new byte[messageData.length - 1];
                        for (int i = 1; i < messageData.length; i++) {
                            auxData[i - 1] = messageData[i];
                        }

                        String tmpScore = new String(auxData, "UTF-8");
                        String[] splitScore = tmpScore.split("\\|");

                        getQuizFragment().generateScoreOnline(realTimeMessage.getSenderParticipantId(), Integer.parseInt(splitScore[0]), Integer.parseInt(splitScore[1]));

                        break;

                    case 'S':
                        sbMessageData = new StringBuilder();
                    case 'C':
                        byte[] tmpBytes = messageData;
                        byte[] tmpBytes2 = new byte[tmpBytes.length - 1];

                        for (int j = 1; j < tmpBytes.length; j++) {
                            tmpBytes2[j - 1] = tmpBytes[j];
                        }

                        sbMessageData.append(new String(tmpBytes2, "UTF-8"));

                        break;


                    case 'E':
                        if (Game.category != null)
                            Game.category.getQuizzes().clear();

                        Type type = new TypeToken<Category>() {
                        }.getType();

                        GsonBuilder builder = new GsonBuilder();
                        builder.registerTypeAdapter(Quiz.class, new TopekaJSonHelper.QuizDeserializer());
                        Gson gson = builder.create();

                        Log.d("TRAZAGPGE", sbMessageData.toString());
                        Game.category = gson.fromJson(sbMessageData.toString(), type);
                        mCategory = Game.category;


                        // END MESSAGE
                        byte[] tmpTime = new byte[messageData.length - 1];
                        for (int i = 1; i < messageData.length; i++) {
                            tmpTime[i - 1] = messageData[i];
                        }

                        String[] data = new String(tmpTime, "UTF-8").split("\\|");
                        Game.totalTime = Integer.parseInt(data[0]);
                        Game.master = data[1];

                        check.add(tmpTime);

                        for (Participant p : Game.mParticipants) {
                            if (p.getParticipantId().equals(Game.master)) {
                                retryMessage(new byte[]{'T'}, p.getParticipantId(), 1, "Why: Sending Data to Opponent", new CallBackRetry() {
                                    @Override
                                    public void sendActions() {
                                        initQuizFragment();
                                    }
                                });
                            }
                        }
                        break;

                    case 'T':
                        if (hasInitialicedAllPlayers()) {
                            for (Participant p : Game.mParticipants) {
                                if (!p.getParticipantId().equals(Game.mMyId) && p.isConnectedToRoom() && p.getStatus() == Participant.STATUS_JOINED) {
                                    retryMessage(new byte[]{'U'}, p.getParticipantId(), 1, "Why: Sending Start Message", null);
                                }
                            }
                            hideWaitingProgress();
                            mQuizFab.show();
                            startQuizFromClickOn(mQuizFab);
                        }
                        break;
                    case 'U':
                        hideWaitingProgress();
                        mQuizFab.show();
                        startQuizFromClickOn(mQuizFab);
                        break;

                    case 'D':
                        byte[] tmpTimeStamp = new byte[messageData.length - 1];

                        for (int j = 1; j < messageData.length; j++) {
                            tmpTimeStamp[j - 1] = messageData[j];
                        }
                        Game.mFinishedParticipants.put(realTimeMessage.getSenderParticipantId(), new Long(new String(tmpTimeStamp)));

                        Log.d("TRAZAGPG", Game.numParticipantsOK() + " " + Game.mFinishedParticipants.size());
                        for (Map.Entry<String, Long> key : Game.mFinishedParticipants.entrySet()) {
                            Log.d("TRAZAGPG", key.getKey() + " " + key.getValue());

                        }
                        if (Game.numParticipantsOK() == Game.mFinishedParticipants.size()) {

                            long minValue = Long.MAX_VALUE;
                            for (Map.Entry<String, Long> key : Game.mFinishedParticipants.entrySet()) {
                                if (key.getValue() < minValue) {
                                    minValue = key.getValue();
                                }
                            }

                            if (Game.timeStamp <= minValue) {

                                minValue = Game.timeStamp;

//                                Game.mFinishedParticipants.put(Game.mMyId, Game.timeStamp);

                                // Calcula si hay 2 con el tiempo ==
                                ArrayList<String> sameTimestamp = new ArrayList();

                                sameTimestamp.add(Game.mMyId);

                                for (Map.Entry<String, Long> key : Game.mFinishedParticipants.entrySet()) {
                                    if (key.getValue() == minValue) {
                                        sameTimestamp.add(key.getKey());
                                    }
                                }

                                if (sameTimestamp.size() == 1) {
                                    Game.jugadorLocal = 1;
                                    enviarQuizzesToOpponent();
                                    Log.d("CREADOR1", minValue + " " + Game.timeStamp + " " + Game.mMyId);
                                } else {
                                    // Obtiene menor id
                                    String tmpIdParticipant = null;
                                    for (Participant p : Game.mParticipants) {
                                        for (String id : sameTimestamp) {
                                            if (p.getParticipantId().equals(id)) {
                                                if (tmpIdParticipant == null) {
                                                    tmpIdParticipant = id;
                                                } else {
                                                    if (p.getParticipantId().compareTo(tmpIdParticipant) < 0) {
                                                        tmpIdParticipant = id;
                                                    }
                                                }
                                            }
                                        }
                                    }

                                    if (tmpIdParticipant.equals(Game.mMyId)) {
                                        Game.jugadorLocal = 1;
                                        enviarQuizzesToOpponent();
                                        Log.d("CREADOR2", minValue + " " + Game.timeStamp + " " + Game.mMyId);
                                    } else {
                                        Game.jugadorLocal = 2;
//                                        enviarQuizzesToOpponent();
                                        Log.d("NO_CREADOR1", minValue + " " + Game.timeStamp + " " + Game.mMyId);
                                    }
                                }
                            } else {
                                Log.d("NO_CREADOR2", minValue + " " + Game.timeStamp + " " + Game.mMyId);
                                Game.jugadorLocal = 2;
                            }
                        }
                        break;

                    default:
                }
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.d("TRAZAGPG", "Code UTF-8 not supported");
            }
        }
    }

    @Override
    public void onPeerDeclined(Room room, List<String> arg1) {
        actualizaRoom(room);
    }

    @Override
    public void onP2PDisconnected(String participant) {
    }

    @Override
    public void onPeerJoined(Room room, List<String> arg1) {
        actualizaRoom(room);
    }

    @Override
    public void onRoomAutoMatching(Room room) {
        actualizaRoom(room);
    }

    @Override
    public void onPeersConnected(Room room, List<String> peers) {
        actualizaRoom(room);
    }

    @Override
    public void onRoomConnecting(Room room) {
        actualizaRoom(room);
    }

    @Override
    public void onPeerLeft(Room room, List<String> peersWhoLeft) {
        actualizaRoom(room);
    }

    @Override
    public void onDisconnectedFromRoom(Room room) {
        Game.mRoomId = null;
        if (!mCategory.isSolved())
            showGameError("Why: Disconnect From Room", true);
    }

    @Override
    public void onPeersDisconnected(Room room, List<String> peers) {
        actualizaRoom(room);
    }

    @Override
    public void onConnectedToRoom(Room room) {
        Log.d(TAG, "onConnectedToRoom.");
        Game.mParticipants = room.getParticipants();
        Game.mMyId = room.getParticipantId(Games.Players.getCurrentPlayerId(Game.mGoogleApiClient));

        Game.myName = Games.Players.getCurrentPlayer(Game.mGoogleApiClient).getDisplayName();

        if (Game.mRoomId == null) {
            Game.mRoomId = room.getRoomId();
        }

        // print out the list of participants (for debug purposes)
        Log.d(TAG, "Room ID: " + Game.mRoomId);
        Log.d(TAG, "My ID " + Game.mMyId);
        Log.d(TAG, "<< CONNECTED TO ROOM>>");
    }

    @Override
    public void onPeerInvitedToRoom(Room room, List<String> arg1) {
        actualizaRoom(room);
    }

    @Override
    public void onP2PConnected(String participant) {
    }

    @Override
    public void onRoomCreated(int statusCode, Room room) {
        Log.d(TAG, "onRoomCreated(" + statusCode + ", " + room + ")");

        if (statusCode != GamesStatusCodes.STATUS_OK) {
            Log.e(TAG, "*** Error: onRoomCreated, status " + statusCode);
            showGameError("Why: Room Created With Errors!", true);
            return;
        }

        Game.mRoomId = room.getRoomId();

        // show the waiting room UI
        showWaitingRoom(room);

    }

    @Override
    public void onLeftRoom(int statusCode, String roomId) {
        // we have left the room; return to main screen.
        Log.d(TAG, "onLeftRoom, code " + statusCode);
        switchToMainScreen();
    }

    private void switchToMainScreen() {
        finish();
    }


    @Override
    public void onJoinedRoom(int statusCode, Room room) {
        if (statusCode != STATUS_OK) {
            showGameError("Why: Join Error", true);
            return;
        }
        showWaitingRoom(room);
    }

    @Override
    public void onRoomConnected(int statusCode, Room room) {
        if (statusCode != STATUS_OK) {
            showGameError("Why: Connected Error", true);
            return;
        }
        actualizaRoom(room);
    }

    void actualizaRoom(Room room) {

        if (mCategory.isSolved()) {
            if (getQuizFragment() != null)
                getQuizFragment().checkScoreNumParticipants();
        }

        if (room != null) {
            Game.mParticipants = room.getParticipants();
        }
        if (Game.mParticipants != null) {
            //updatePeerScoresDisplay();
        }
    }

    public static int MAX_RETRY_TIMES = 5;

    // Show error message about game being cancelled and return to main screen.
    public void showGameError(String msg, final boolean exit) {
//        BaseGameUtils.makeSimpleDialog(this, getString(R.string.game_problem)).show();
//        switchToMainScreen();
        cancelPostDelayHandlerPlayGame();
        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setMessage(Html.fromHtml("<font color='#AAAAAA'>" + "Oops! Something wrong happened!" + "\n" + msg + "</font>")).setTitle("Information").setPositiveButton("OK", new DialogInterface.OnClickListener() {
            public void onClick(DialogInterface dialog, int id) {
                dialog.dismiss();
                if (exit)
                    switchToMainScreen();
            }

        });
        builder.setCancelable(false);

        builder.show();

    }


    final static int RC_SELECT_PLAYERS = 10000;

    void showWaitingRoom(Room room) {
        // minimum number of players required for our game
        // For simplicity, we require everyone to join the game before we start it
        // (this is signaled by Integer.MAX_VALUE).
        final int MIN_PLAYERS = Game.numPlayers;
        Intent i = Games.RealTimeMultiplayer.getWaitingRoomIntent(Game.mGoogleApiClient, room, MIN_PLAYERS);

        // show waiting room UI
        startActivityForResult(i, RC_WAITING_ROOM);
    }
//JVG.E


    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        super.onActivityResult(requestCode, responseCode, intent);
        switch (requestCode) {
//            case RC_SIGN_IN:
//                mSignInClicked = false;
//                mResolvingConnectionFailure = false;
//                if (responseCode == RESULT_OK) {
//                    Game.mGoogleApiClient.connect();
//                    SharedPreferences.Editor editor = getSharedPreferences(NAME_PREFERENCES, MODE_PRIVATE).edit();
//                    editor.putInt(NAME_PREFERENCES_CONNECTED, 1);
//                    editor.commit();
//                } else {
//                    BaseGameUtils.showActivityResultError(this, requestCode, responseCode, R.string.unknown_error);
//                }
//                break;
//            case RC_SELECT_PLAYERS:
//                if (responseCode != Activity.RESULT_OK) {
//                    return;
//                }
//                final ArrayList<String> invitees = intent.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
//                Bundle autoMatchCriteria = null;
//                int minAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
//                int maxAutoMatchPlayers = intent.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);
//                if (minAutoMatchPlayers > 0) {
//                    autoMatchCriteria = RoomConfig.createAutoMatchCriteria(minAutoMatchPlayers, maxAutoMatchPlayers, 0);
//                } else {
//                    autoMatchCriteria = null;
//                }
//                TurnBasedMatchConfig tbmc = TurnBasedMatchConfig.builder().addInvitedPlayers(invitees).setAutoMatchCriteria(autoMatchCriteria).build();
//                Games.TurnBasedMultiplayer.createMatch(Game.mGoogleApiClient, tbmc);
//                break;

            case RC_WAITING_ROOM:
                // we got the result from the "waiting room" UI.
                if (responseCode == AppCompatActivity.RESULT_OK) {
                    // ready to start playing
                    Log.d(TAG, "Starting game (waiting room returned OK).");
                    // JVG.S

                    showWaitingProgress();
                    numeroJugadorLocal();
//                    enviarQuizzesToOpponent();
                } else if (responseCode == GamesActivityResultCodes.RESULT_LEFT_ROOM) {
                    // player indicated that they want to leave the room
                    leaveRoom();
                } else if (responseCode == Activity.RESULT_CANCELED) {
                    // Dialog was cancelled (user pressed back key, for instance). In our game,
                    // this means leaving the room too. In more elaborate games, this could mean
                    // something else (like minimizing the waiting room UI).
                    leaveRoom();
                }
                break;
        }
    }

    private ProgressDialog pWaitingProgress;


    public void showWaitingProgress() {
        if (mCategory.getId().equals(QuizActivity.ARG_ONLINE)) {
            pWaitingProgress = new ProgressDialog(this);
            pWaitingProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    hideWaitingProgress();
                    finish();
                }
            });
            pWaitingProgress.setTitle("Waiting Player to be Ready...");
            pWaitingProgress.show();
        }
    }

    public void hideWaitingProgress() {
        if (pWaitingProgress != null) {
            pWaitingProgress.dismiss();
            pWaitingProgress = null;
        }

    }

    // Leave the room.
    void leaveRoom() {
        if (mCategory.getId().equals(ARG_ONLINE)) {
            Log.d(TAG, "Leaving room.");
//        mSecondsLeft = 0;
            stopKeepingScreenOn();
            if (Game.mRoomId != null) {
                Games.RealTimeMultiplayer.leave(Game.mGoogleApiClient, this, Game.mRoomId);
                Game.mRoomId = null;
            }

        }
    }

    private void numeroJugadorLocal() {
//        Game.jugadorLocal = 1;
//        for (Participant p : Game.mParticipants) {
//            if (p.getParticipantId().equals(Game.mMyId)) continue;
//            if (p.getStatus() != Participant.STATUS_JOINED) continue;
//            if (p.getParticipantId().compareTo(Game.mMyId) < 0) Game.jugadorLocal = 2;
//        }

        byte[] tmpTimeStamp = Long.valueOf(Game.timeStamp).toString().getBytes();
        byte[] timeStamp = new byte[tmpTimeStamp.length + 1];

        for (int i = 0; i < tmpTimeStamp.length; i++) {
            timeStamp[i + 1] = tmpTimeStamp[i];
        }

        timeStamp[0] = (byte) 'D';
        Log.d("IDENTIFICACION", Game.timeStamp + " " + timeStamp);

        for (Participant p : Game.mParticipants) {
            if (!p.getParticipantId().equals(Game.mMyId)) {
                retryMessage(timeStamp, p.getParticipantId(), 1, "Why: Sending TimeStamp!", null);
            }
        }

//        Log.d("IDENTIFICACION", "" + Game.mMyId + " JUGADOR_LOCAL:" + Game.jugadorLocal);
    }

    private ArrayList<byte[]> check = new ArrayList<>();

    private void enviarQuizzesToOpponent() {
//        if (Game.jugadorLocal == 1) {
        Log.d("TRAZAGPG", "Envindo quizzes");
        Type type = new TypeToken<Category>() {
        }.getType();

        GsonBuilder builder = new GsonBuilder();
        Gson gson = builder.create();

        String json = gson.toJson(Game.category, type);
        Log.d("TRAZAGPG", "Conversión original:" + json);

        byte[] categoryByte = json.getBytes(Charset.forName("UTF-8"));

        int lengthMsg = categoryByte.length;

        int bytesTransfered = 0;

        int numBlock = 0;

        check.clear();

        while (lengthMsg - bytesTransfered > 0) {
            byte[] tmpMsg;

            if ((lengthMsg - bytesTransfered) / 1399 > 0)
                tmpMsg = new byte[1400];
            else
                tmpMsg = new byte[(lengthMsg - bytesTransfered) % 1399 + 1];

            if (numBlock == 0)
                tmpMsg[0] = 'S';
            else
                tmpMsg[0] = 'C';

            Log.d("TRAZAGPG", "bloque: " + numBlock + "; " + "bytesTransfered:" + bytesTransfered + " ; TotalBytes: " + lengthMsg + " bloque length: " + tmpMsg.length);
            for (int i = 1; i < tmpMsg.length; i++) {
                tmpMsg[i] = categoryByte[bytesTransfered];
                bytesTransfered++;
            }
            try {
                Log.d("TRAZAGPG", "bloque: " + new String(tmpMsg, "UTF-8"));
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
            }

            numBlock++;

            check.add(tmpMsg);
        }


//            StringBuilder sb = new StringBuilder();
//
//            for (int i = 0; i < check.size(); i++) {
//                byte[] tmpBytes = check.get(i);
//                byte[] tmpBytes2 = new byte[tmpBytes.length - 1];
//
//                for (int j = 1; j < tmpBytes.length; j++) {
//                    tmpBytes2[j - 1] = tmpBytes[j];
//                }
//
//                try {
//                    sb.append(new String(tmpBytes2, "UTF-8"));
//                } catch (UnsupportedEncodingException e) {
//                    e.printStackTrace();
//                }
//            }

//            if (sb.toString().equals(json)) {
        Log.d("TRAZAGPG", "Iniciando transferencia");

        // END MESSAGE
        byte[] time = String.valueOf(Game.totalTime + "|" + Game.mMyId).getBytes();
        byte[] tmpTime = new byte[time.length + 1];
        for (int i = 0; i < time.length; i++) {
            tmpTime[i + 1] = time[i];
        }
        tmpTime[0] = 'E';

        check.add(tmpTime);

        for (Participant p : Game.mParticipants) {
            if (!p.getParticipantId().equals(Game.mMyId) && p.isConnectedToRoom() && p.getStatus() == Participant.STATUS_JOINED) {
                sendMessageToParticipant(0, p.getParticipantId());
            }
        }
//            } else {
//                Log.d("TRAZAGPG", "ERROR VALIDANDO MENSAJE :" + Game.jugadorLocal);
//                Log.d("TRAZAGPG", sb.toString());
//                Log.d("TRAZAGPG", json);
//            }
//        }
//        Log.d("TRAZAGPG", "ERROR VALIDANDO MENSAJE :" + Game.jugadorLocal);
    }

    private void retryMessage(final byte[] tmpMessage, final String participantId, final int numTimesSended, final String msgError, final CallBackRetry callback) {
        if (Game.mRoomId != null) {
            Games.RealTimeMultiplayer.sendReliableMessage(Game.mGoogleApiClient, new RealTimeMultiplayer.ReliableMessageSentCallback() {
                @Override
                public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                    if (statusCode == STATUS_OK) {
                        Log.d("GPG", "Mensaje Enviado" + (char) tmpMessage[0]);
                        if (callback != null) {
                            callback.sendActions();
                        }
                    } else {
                        Log.d("GPG", "Error enviando mensaje: " + numTimesSended + " " + (char) tmpMessage[0]);
                        if (numTimesSended <= MAX_RETRY_TIMES) {
                            retryMessage(tmpMessage, participantId, numTimesSended + 1, msgError, callback);
                        } else {
                            showGameError(msgError, false);
                        }
                    }
                }
            }, tmpMessage, Game.mRoomId, participantId);
        }
    }

    private void sendMessageToParticipant(final int pos, final String participantId) {
        if (pos < check.size()) {
            retryMessage(check.get(pos), participantId, 1, "Why: Sending Quizzes!", new CallBackRetry() {
                @Override
                public void sendActions() {
                    sendMessageToParticipant(pos + 1, participantId);
                }
            });
        }
    }

       /*
     * MISC SECTION. Miscellaneous methods.
     */


    // Sets the flag to keep this screen on. It's recommended to do that during
    // the
    // handshake when setting up a game, because if the screen turns off, the
    // game will be
    // cancelled.
    void keepScreenOn() {
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    // Clears the flag that keeps the screen on.
    void stopKeepingScreenOn() {
        getWindow().clearFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
    }

    public interface CallBackRetry {
        public void sendActions();
    }
}

