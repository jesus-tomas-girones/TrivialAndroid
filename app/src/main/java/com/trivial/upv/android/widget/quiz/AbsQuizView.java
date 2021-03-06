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

package com.trivial.upv.android.widget.quiz;

import android.animation.ArgbEvaluator;
import android.animation.ObjectAnimator;
import android.annotation.SuppressLint;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Bitmap;
import android.graphics.Color;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.LevelListDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.support.annotation.ColorInt;
import android.support.design.widget.Snackbar;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.MarginLayoutParamsCompat;
import android.support.v4.view.animation.LinearOutSlowInInterpolator;
import android.text.Spanned;
import android.text.method.ScrollingMovementMethod;
import android.util.Log;
import android.util.Property;
import android.view.Gravity;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.animation.Interpolator;
import android.view.inputmethod.InputMethodManager;
import android.widget.FrameLayout;
import android.widget.LinearLayout;
import android.widget.TextView;

import com.android.volley.VolleyError;
import com.android.volley.toolbox.ImageLoader;
import com.pixplicity.htmlcompat.HtmlCompat;
import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.ViewUtils;
import com.trivial.upv.android.helper.singleton.volley.VolleySingleton;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.quiz.FourQuarterQuiz;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.model.quiz.QuizType;
import com.trivial.upv.android.model.quiz.SelectItemQuiz;
import com.trivial.upv.android.widget.fab.CheckableFab;

import org.xml.sax.Attributes;

import static com.trivial.upv.android.activity.QuizActivity.ARG_ONE_PLAYER;
import static com.trivial.upv.android.activity.QuizActivity.ARG_REAL_TIME_ONLINE;
import static com.trivial.upv.android.activity.QuizActivity.ARG_TURNED_BASED_ONLINE;

/**
 * This is the base class for displaying a {@link Quiz}.
 * <p>
 * Subclasses need to implement {@link AbsQuizView#createQuizContentView()}
 * in order to allow solution of a quiz.
 * </p>
 * <p>
 * Also {@link AbsQuizView#allowAnswer(boolean)} needs to be called with
 * <code>true</code> in order to mark the quiz solved.
 * </p>
 *
 * @param <Q> The type of {@link Quiz} you want to
 *            display.
 */
public abstract class AbsQuizView<Q extends Quiz> extends FrameLayout implements HtmlCompat.ImageGetter {

    private static final int ANSWER_HIDE_DELAY = 500;
    private static final int FOREGROUND_COLOR_CHANGE_DELAY = 750;
    private final int mSpacingDouble;
    private final LayoutInflater mLayoutInflater;
    private final Category mCategory;
    private final Q mQuiz;
    private final Interpolator mLinearOutSlowInInterpolator;
    private final Handler mHandler;
    private final InputMethodManager mInputMethodManager;
    private boolean mAnswered;
    private TextView mQuestionView;
    private CheckableFab mSubmitAnswer;
    private Runnable mHideFabRunnable;
    private Runnable mMoveOffScreenRunnable;


    /**
     * Enables creation of views for quizzes.
     *
     * @param context  The context for this view.
     * @param category The {@link Category} this view is running in.
     * @param quiz     The actual {@link Quiz} that is going to be displayed.
     */
    public AbsQuizView(Context context, Category category, Q quiz)

    {
        super(context);
        mQuiz = quiz;
        mCategory = category;
        mSpacingDouble = getResources().getDimensionPixelSize(R.dimen.spacing_double);
        mLayoutInflater = LayoutInflater.from(context);
//        mSubmitAnswer = getSubmitButton();
        mLinearOutSlowInInterpolator = new LinearOutSlowInInterpolator();
        mHandler = new Handler();
        mInputMethodManager = (InputMethodManager) context.getSystemService
                (Context.INPUT_METHOD_SERVICE);

        setId(quiz.getId());
        setUpQuestionView();
        LinearLayout container = createContainerLayout(context);
        View quizContentView = getInitializedContentView();
        addContentView(container, quizContentView);
        addOnLayoutChangeListener(new OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                removeOnLayoutChangeListener(this);
                mSubmitAnswer = getSubmitButton();
                addFloatingActionButton();
            }
        });
    }

    @Override
    public Drawable getDrawable(String source, Attributes attr) {
        final LevelListDrawable drawableTmp = new LevelListDrawable();
        Drawable empty;

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            empty = getResources().getDrawable(R.drawable.ic_cross, getContext().getTheme());
        } else {
            empty = getResources().getDrawable(R.drawable.ic_cross);
        }
        drawableTmp.addLevel(0, 0, empty);
        drawableTmp.setBounds(0, 0, 300, 300);

        VolleySingleton.getInstance(getContext()).getImageLoader().get(source, new ImageLoader.ImageListener() {
            @Override
            public void onResponse(ImageLoader.ImageContainer response, boolean isImmediate) {
                Bitmap bitmap = response.getBitmap();
                if (response.getBitmap() != null) {
                    BitmapDrawable drawable = new BitmapDrawable(getResources(), bitmap);
                    drawableTmp.addLevel(1, 1, drawable);
                    drawableTmp.setBounds(0, 0, bitmap.getWidth(), bitmap.getHeight());
                    drawableTmp.setLevel(1);
                    // i don't know yet a better way to refresh TextView
                    // mTv.invalidate() doesn't work as expected
                    CharSequence t = mQuestionView.getText();
                    mQuestionView.setText(t);
                    //mTv.invalidate();
                }
            }

            @Override
            public void onErrorResponse(VolleyError error) {
                Log.d("VOLLEY", "Error cargando icon!");
            }
        });

        return drawableTmp;
    }

    /**
     * Sets the behaviour for all question views.
     */
    private void setUpQuestionView() {
        mQuestionView = (TextView) mLayoutInflater.inflate(R.layout.question, this, false);
        mQuestionView.setBackgroundColor(ContextCompat.getColor(getContext(),
                mCategory.getTheme().getPrimaryColor()));
//JVG.S
//        mQuestionView.setText(getQuiz().getQuestion());
        //        Spanned spanned = Html.fromHtml(imgs, this, null);
//        mTv.setText(spanned);
        Spanned fromHtml = HtmlCompat.fromHtml(getContext(), getQuiz().getQuestion(), 0, this);
// You may want to provide an ImageGetter, TagHandler and SpanCallback:
//Spanned fromHtml = HtmlCompat.fromHtml(context, source, 0,
//        imageGetter, tagHandler, spanCallback);
        //viewHolder.mQuizView.setMovementMethod(LinkMovementMethod.getInstance());
        mQuestionView.setText(fromHtml);
        mQuestionView.setMovementMethod(new ScrollingMovementMethod());
//JVG.E
    }

    private LinearLayout createContainerLayout(Context context) {
        LinearLayout container = new LinearLayout(context);
        container.setId(R.id.absQuizViewContainer);
        container.setOrientation(LinearLayout.VERTICAL);
        return container;
    }

    private View getInitializedContentView() {
        View quizContentView = createQuizContentView();
        quizContentView.setId(R.id.quiz_content);
        quizContentView.setSaveEnabled(true);
        setDefaultPadding(quizContentView);
        if (quizContentView instanceof ViewGroup) {
            ((ViewGroup) quizContentView).setClipToPadding(false);
        }
        setMinHeightInternal(quizContentView);
        return quizContentView;
    }

    private void addContentView(LinearLayout container, View quizContentView) {
        LayoutParams layoutParams = new LayoutParams(LayoutParams.MATCH_PARENT,
                LayoutParams.WRAP_CONTENT);
        container.addView(mQuestionView, layoutParams);
        container.addView(quizContentView, layoutParams);
        addView(container, layoutParams);
    }

    private void addFloatingActionButton() {
        final int fabSize = getResources().getDimensionPixelSize(R.dimen.size_fab);
        int bottomOfQuestionView = findViewById(R.id.question_view).getBottom();
        final LayoutParams fabLayoutParams = new LayoutParams(fabSize, fabSize,
                Gravity.END | Gravity.TOP);
        final int halfAFab = fabSize / 2;
        fabLayoutParams.setMargins(0, // left
                bottomOfQuestionView - halfAFab, //top
                0, // right
                mSpacingDouble); // bottom
        MarginLayoutParamsCompat.setMarginEnd(fabLayoutParams, mSpacingDouble);
        if (ApiLevelHelper.isLowerThan(Build.VERSION_CODES.LOLLIPOP)) {
            // Account for the fab's emulated shadow.
            fabLayoutParams.topMargin -= (mSubmitAnswer.getPaddingTop() / 2);
        }
        addView(mSubmitAnswer, fabLayoutParams);
    }

    private CheckableFab getSubmitButton() {
        if (null == mSubmitAnswer) {
            mSubmitAnswer = (CheckableFab) getLayoutInflater()
                    .inflate(R.layout.answer_submit, this, false);
            mSubmitAnswer.hide();
            mSubmitAnswer.setOnClickListener(new OnClickListener() {
                @Override
                public void onClick(final View v) {
                    //JVG.S
                    if (mCategory.getId().equals(ARG_ONE_PLAYER)) {
                        ((QuizActivity) getContext()).cancelPostDelayHandlerPlayGame();
                    } else if (mCategory.getId().equals(ARG_REAL_TIME_ONLINE)) {
                        ((QuizActivity) getContext()).cancelPostDelayHandlerPlayGame();
                    } else if (((QuizActivity)getContext()).isMatchTurnBased()) {
                        ((QuizActivity) getContext()).cancelPostDelayHandlerPlayGame();
                    }
                    submitAnswer(v);
                    if (mInputMethodManager.isAcceptingText()) {
                        mInputMethodManager.hideSoftInputFromWindow(v.getWindowToken(), 0);
                    }
                    mSubmitAnswer.setEnabled(false);
                }
            });
        }
        return mSubmitAnswer;
    }

    private void setDefaultPadding(View view) {
        view.setPadding(mSpacingDouble, mSpacingDouble, mSpacingDouble, mSpacingDouble);
    }

    protected LayoutInflater getLayoutInflater() {
        return mLayoutInflater;
    }

    /**
     * Implementations should create the content view for the type of
     * {@link Quiz} they want to display.
     *
     * @return the created view to solve the quiz.
     */
    protected abstract View createQuizContentView();

    /**
     * Implementations must make sure that the answer provided is evaluated and correctly rated.
     *
     * @return <code>true</code> if the question has been correctly answered, else
     * <code>false</code>.
     */
    protected abstract boolean isAnswerCorrect();

    /**
     * Save the user input to a bundle for orientation changes.
     *
     * @return The bundle containing the user's input.
     */
    public abstract Bundle getUserInput();

    /**
     * Restore the user's input.
     *
     * @param savedInput The input that the user made in a prior instance of this view.
     */
    public abstract void setUserInput(Bundle savedInput);

    public Q getQuiz() {
        return mQuiz;
    }

    protected boolean isAnswered() {
        return mAnswered;
    }

    /**
     * Sets the quiz to answered or unanswered.
     *
     * @param answered <code>true</code> if an answer was roulette_selection, else <code>false</code>.
     */
    protected void allowAnswer(final boolean answered) {
        if (null != mSubmitAnswer) {
            if (answered) {
                mSubmitAnswer.show();
            } else {
                mSubmitAnswer.hide();
            }
            mAnswered = answered;
        }
    }

    /**
     * Sets the quiz to answered if it not already has been answered.
     * Otherwise does nothing.
     */
    protected void allowAnswer() {
        if (!isAnswered()) {
            allowAnswer(true);
        }
    }

    /**
     * Allows children to submit an answer via code.
     */
    protected void submitAnswer() {
        submitAnswer(findViewById(R.id.submitAnswer));
    }

    @SuppressWarnings("UnusedParameters")
    private void submitAnswer(final View v) {
        final boolean answerCorrect = isAnswerCorrect();
        mQuiz.setSolved(true);

        // JVG.S
//        performScoreAnimation(answerCorrect);
        // JVG.E
        // Show comments about wrong answers if is the case
        performScoreAnimationWithCheckCommentsAvailable(answerCorrect);
        // JVG.S
    }

    /**
     * Animates the view nicely when the answer has been submitted.
     *
     * @param answerCorrect <code>true</code> if the answer was correct, else <code>false</code>.
     */
    private void performScoreAnimation(final boolean answerCorrect) {
        ((QuizActivity) getContext()).lockIdlingResource();
        // Decide which background color to use.
        final int backgroundColor = ContextCompat.getColor(getContext(),
                answerCorrect ? R.color.green : R.color.red);
        adjustFab(answerCorrect, backgroundColor);
        resizeView();
        moveViewOffScreen(answerCorrect);
        // Animate the foreground color to match the background color.
        // This overlays all content within the current view.
        animateForegroundColor(backgroundColor);
    }

    // JVG.S
    private void performScoreAnimationWithCheckCommentsAvailable(final boolean answerCorrect) {
        int posAnswer = -1;
        String comments = "0";
        if (getQuiz().getType().equals(QuizType.SINGLE_SELECT) || getQuiz().getType().equals(QuizType.FOUR_QUARTER)) {
            Bundle userInput = getUserInput();
            for (String strings : userInput.keySet()) {
                if (userInput.get(strings) instanceof Integer)
                    posAnswer = (int) userInput.get(strings);
                else if (userInput.get(strings) instanceof boolean[]) {
                    boolean[] tmpAnswers = (boolean[]) userInput.get(strings);

                    for (int j = 0; j < tmpAnswers.length; j++) {
                        if (tmpAnswers[j]) {
                            posAnswer = j;
                            break;
                        }
                    }
                }
            }

            if (posAnswer >= 0) {
                Quiz quiz = getQuiz();
                if (quiz instanceof FourQuarterQuiz) {
                    comments = ((FourQuarterQuiz) quiz).getComments()[posAnswer];
                } else if (quiz instanceof SelectItemQuiz) {
                    comments = ((SelectItemQuiz) quiz).getComments()[posAnswer];
                }
            }
        }

        if (!mCategory.getId().equals(ARG_REAL_TIME_ONLINE) && !((QuizActivity)getContext()).isMatchTurnBased() && !answerCorrect && posAnswer >= 0 && comments != null && !comments.isEmpty()) {

            Snackbar snackbar = Snackbar
                    .make(findViewById(R.id.absQuizViewContainer), comments, Snackbar.LENGTH_INDEFINITE)
                    .setAction("CONTINUAR ...", new OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            performScoreAnimation(answerCorrect);
                        }
                    });
            View snackbarView = snackbar.getView();
            TextView tv = (TextView) snackbarView.findViewById(android.support.design.R.id.snackbar_text);
            tv.setMaxLines(25);
            snackbar.show();
        } else {
            performScoreAnimation(answerCorrect);
        }
    }
    // JVG.E

    @SuppressLint("NewApi")
    private void adjustFab(boolean answerCorrect, int backgroundColor) {
        mSubmitAnswer.setChecked(answerCorrect);
        mSubmitAnswer.setBackgroundTintList(ColorStateList.valueOf(backgroundColor));
        mHideFabRunnable = new Runnable() {
            @Override
            public void run() {
                mSubmitAnswer.hide();
            }
        };
        mHandler.postDelayed(mHideFabRunnable, ANSWER_HIDE_DELAY);
    }

    private void resizeView() {
        final float widthHeightRatio = (float) getHeight() / (float) getWidth();
        // Animate X and Y scaling separately to allow different roulette_rotate delays.
        // object animators for x and y with different durations and then run them independently
        resizeViewProperty(View.SCALE_X, .5f, 200);
        resizeViewProperty(View.SCALE_Y, .5f / widthHeightRatio, 300);
    }

    private void resizeViewProperty(Property<View, Float> property,
                                    float targetScale, int durationOffset) {
        ObjectAnimator animator = ObjectAnimator.ofFloat(this, property,
                1f, targetScale);
        animator.setInterpolator(mLinearOutSlowInInterpolator);
        animator.setStartDelay(FOREGROUND_COLOR_CHANGE_DELAY + durationOffset);
        animator.start();
    }

    @Override
    protected void onDetachedFromWindow() {
        if (mHideFabRunnable != null) {
            mHandler.removeCallbacks(mHideFabRunnable);
        }
        if (mMoveOffScreenRunnable != null) {
            mHandler.removeCallbacks(mMoveOffScreenRunnable);
        }
        super.onDetachedFromWindow();
    }

    private void animateForegroundColor(@ColorInt final int targetColor) {
        ObjectAnimator animator = ObjectAnimator.ofInt(this, ViewUtils.FOREGROUND_COLOR,
                Color.TRANSPARENT, targetColor);
        animator.setEvaluator(new ArgbEvaluator());
        animator.setStartDelay(FOREGROUND_COLOR_CHANGE_DELAY);
        animator.start();
    }

    private void moveViewOffScreen(final boolean answerCorrect) {
        // Move the current view off the screen.
        mMoveOffScreenRunnable = new Runnable() {
            @Override
            public void run() {
                mCategory.setScore(getQuiz(), answerCorrect);
                if (getContext() instanceof QuizActivity) {
                    ((QuizActivity) getContext()).proceed();
                }
            }
        }

        ;
        mHandler.postDelayed(mMoveOffScreenRunnable,
                FOREGROUND_COLOR_CHANGE_DELAY * 2);
    }

    private void setMinHeightInternal(View view) {
        view.setMinimumHeight(getResources().getDimensionPixelSize(R.dimen.min_height_question));
    }
}
