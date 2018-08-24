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

package com.trivial.upv.android.fragment;

import android.annotation.TargetApi;
import android.app.ProgressDialog;
import android.content.DialogInterface;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.Nullable;
import android.support.v4.view.ViewCompat;
import android.support.v4.view.animation.FastOutLinearInInterpolator;
import android.util.Log;
import android.view.ContextThemeWrapper;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewTreeObserver;
import android.widget.AdapterViewAnimator;
import android.widget.ListView;
import android.widget.ProgressBar;
import android.widget.TextView;

import com.google.android.gms.games.RealTimeMultiplayerClient;
import com.google.android.gms.games.multiplayer.Participant;
import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.adapter.QuizAdapter;
import com.trivial.upv.android.adapter.ScoreAdapter;
import com.trivial.upv.android.adapter.ScoreOnlineAdapter;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.PreferencesHelper;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.Player;
import com.trivial.upv.android.model.Theme;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.gpg.ScoreOnline;
import com.trivial.upv.android.model.quiz.Quiz;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.AvatarView;
import com.trivial.upv.android.widget.quiz.AbsQuizView;

import java.util.List;

import static com.google.android.gms.games.GamesStatusCodes.STATUS_OK;
import static com.trivial.upv.android.activity.QuizActivity.ARG_ONE_PLAYER;
import static com.trivial.upv.android.activity.QuizActivity.ARG_REAL_TIME_ONLINE;
import static com.trivial.upv.android.activity.QuizActivity.ARG_TURNED_BASED_ONLINE;
import static com.trivial.upv.android.activity.QuizActivity.MAX_RETRY_TIMES;


/**
 * Encapsulates Quiz solving and displays it to the user.
 */
public class QuizFragment extends android.support.v4.app.Fragment {

    private static final String KEY_USER_INPUT = "USER_INPUT";
    private TextView mProgressText;
    private int mQuizSize;
    private ProgressBar mProgressBar;
    private Category mCategory;
    //JVG.S
    private TextView mTimeLeftText;
    private ScoreOnlineAdapter mScoreOnlineAdapter;

    public AdapterViewAnimator getQuizView() {
        return mQuizView;
    }

    //JVG.E
    private AdapterViewAnimator mQuizView;
    private ScoreAdapter mScoreAdapter;
    private QuizAdapter mQuizAdapter;
    private SolvedStateListener mSolvedStateListener;

    public static QuizFragment newInstance(String categoryId,
                                           SolvedStateListener solvedStateListener) {
        if (categoryId == null) {
            throw new IllegalArgumentException("The category can not be null");
        }
        Bundle args = new Bundle();
        args.putString(Category.TAG, categoryId);
        QuizFragment fragment = new QuizFragment();
        if (solvedStateListener != null) {
            fragment.mSolvedStateListener = solvedStateListener;
        }
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public void onCreate(Bundle savedInstanceState) {
        String categoryId = getArguments().getString(Category.TAG);
        //JVG.S
        //mCategory = TopekaDatabaseHelper.getCategoryWith(getActivity(), categoryId);
        if (categoryId.equals(ARG_ONE_PLAYER)) {
            mCategory = Game.category;
        } else if (((QuizActivity) getActivity()).isMatchOnline() || ((QuizActivity) getActivity()).isMatchTurnBased()) {
            mCategory = Game.category;
        } else {
            mCategory = TrivialJSonHelper.getInstance(getContext(), false).getCategoryWith(categoryId);
        }
        //JVG.E
        super.onCreate(savedInstanceState);
    }

    @Override
    public View onCreateView(LayoutInflater inflater, @Nullable ViewGroup container,
                             @Nullable Bundle savedInstanceState) {
        // Create a themed Context and custom LayoutInflater
        // to get nicely themed views in this Fragment.
        final Theme theme = mCategory.getTheme();
        final ContextThemeWrapper context = new ContextThemeWrapper(getActivity(),
                theme.getStyleId());
        final LayoutInflater themedInflater = LayoutInflater.from(context);
        return themedInflater.inflate(R.layout.fragment_quiz, container, false);
    }

    @Override
    public void onViewCreated(View view, @Nullable Bundle savedInstanceState) {
        mQuizView = (AdapterViewAnimator) view.findViewById(R.id.quiz_view);
        // JVG.S
        mTimeLeftText = (TextView) view.findViewById(R.id.time_left);
        scoreOnline.add(new ScoreOnline.Score("", "", "STATE", 0, 0));
        // JVG.E
        decideOnViewToDisplay();
        setQuizViewAnimations();
        final AvatarView avatar = (AvatarView) view.findViewById(R.id.avatar);
        setAvatarDrawable(avatar);
        initProgressToolbar(view);
        super.onViewCreated(view, savedInstanceState);
    }

    @TargetApi(Build.VERSION_CODES.LOLLIPOP)
    private void setQuizViewAnimations() {
        if (ApiLevelHelper.isLowerThan(Build.VERSION_CODES.LOLLIPOP)) {
            return;
        }
        mQuizView.setInAnimation(getActivity(), R.animator.slide_in_bottom);
        mQuizView.setOutAnimation(getActivity(), R.animator.slide_out_top);
    }

    private void initProgressToolbar(View view) {
        final int firstUnsolvedQuizPosition = mCategory.getFirstUnsolvedQuizPosition();
        final List<Quiz> quizzes = mCategory.getQuizzes();
        mQuizSize = quizzes.size();
        mProgressText = (TextView) view.findViewById(R.id.progress_text);
        mProgressBar = ((ProgressBar) view.findViewById(R.id.progress));
        mProgressBar.setMax(mQuizSize);


        setProgress(firstUnsolvedQuizPosition);
    }

    private void setProgress(int currentQuizPosition) {
        if (!isAdded()) {
            return;
        }
        mProgressText.setText(getString(R.string.quiz_of_quizzes, String.valueOf(currentQuizPosition), String.valueOf(mQuizSize)));
        mProgressBar.setProgress(currentQuizPosition);
    }

    @SuppressWarnings("ConstantConditions")
    private void setAvatarDrawable(AvatarView avatarView) {
        Player player = PreferencesHelper.getPlayer(getActivity());
        avatarView.setAvatar(player.getAvatar().getDrawableId());
        ViewCompat.animate(avatarView)
                .setInterpolator(new FastOutLinearInInterpolator())
                .setStartDelay(500)
                .scaleX(1)
                .scaleY(1)
                .start();
    }


    public void setTimeLeftText(int time) {
        this.mTimeLeftText.setText(String.valueOf(time / 1000));
    }

    private void decideOnViewToDisplay() {
        final boolean isSolved = mCategory.isSolved();
        if (isSolved) {
            showSummary();
            if (null != mSolvedStateListener) {
                mSolvedStateListener.onCategorySolved();
            }
        } else {
            mQuizView.setAdapter(getQuizAdapter());
            mQuizView.setSelection(mCategory.getFirstUnsolvedQuizPosition());
            //JVG.S
            mQuizView.getViewTreeObserver().
                    addOnPreDrawListener(new ViewTreeObserver.OnPreDrawListener() {
                        @Override
                        public boolean onPreDraw() {
                            mQuizView.getViewTreeObserver().removeOnPreDrawListener(this);
                            // Play game offline
                            if (mCategory.getId().equals(ARG_ONE_PLAYER)) {
                                ((QuizActivity) getActivity()).setTimeToNextItem(Game.totalTime * 1000);
//                            TIME X QUIZ
//                                ((QuizActivity) getActivity()).setTimeToNextItem(TIME_TO_ANSWER_PLAY_GAME);
                                setTimeLeftText(Game.totalTime * 1000);
                                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
                            } else if (mCategory.getId().equals(ARG_REAL_TIME_ONLINE)) {
                                ((QuizActivity) getActivity()).setTimeToNextItem(Game.totalTime * 1000);
                                setTimeLeftText(Game.totalTime * 1000);
                                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
                            } else if (((QuizActivity) getActivity()).isMatchTurnBased()) {
                                ((QuizActivity) getActivity()).setTimeToNextItem(Game.K_TIME_TO_ANSWER_TURN_BASED * 1000);
                                setTimeLeftText(Game.K_TIME_TO_ANSWER_TURN_BASED * 1000);
                                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
                            }
                            return true;
                        }
                    });
            //JVG.E
        }
    }

    @Override
    public void onSaveInstanceState(Bundle outState) {
        View focusedChild = mQuizView.getFocusedChild();
        if (focusedChild instanceof ViewGroup) {
            View currentView = ((ViewGroup) focusedChild).getChildAt(0);
            if (currentView instanceof AbsQuizView) {
                outState.putBundle(KEY_USER_INPUT, ((AbsQuizView) currentView).getUserInput());
            }
        }
        super.onSaveInstanceState(outState);
    }

    @Override
    public void onViewStateRestored(Bundle savedInstanceState) {
        restoreQuizState(savedInstanceState);
        super.onViewStateRestored(savedInstanceState);
    }

    private void restoreQuizState(final Bundle savedInstanceState) {
        if (null == savedInstanceState) {
            return;
        }
        mQuizView.addOnLayoutChangeListener(new View.OnLayoutChangeListener() {
            @Override
            public void onLayoutChange(View v, int left, int top, int right, int bottom,
                                       int oldLeft,
                                       int oldTop, int oldRight, int oldBottom) {
                mQuizView.removeOnLayoutChangeListener(this);
                View currentChild = mQuizView.getChildAt(0);
                if (currentChild instanceof ViewGroup) {
                    final View potentialQuizView = ((ViewGroup) currentChild).getChildAt(0);
                    if (potentialQuizView instanceof AbsQuizView) {
                        ((AbsQuizView) potentialQuizView).setUserInput(savedInstanceState.
                                getBundle(KEY_USER_INPUT));
                    }
                }
            }
        });

    }

    private QuizAdapter getQuizAdapter() {
        if (null == mQuizAdapter) {
            mQuizAdapter = new QuizAdapter(getActivity(), mCategory);
        }
        return mQuizAdapter;
    }

    /**
     * Displays the next page.
     *
     * @return <code>true</code> if there's another quiz to solve, else <code>false</code>.
     */
    public boolean showNextPage() {
        if (null == mQuizView) {
            return false;
        }
        int nextItem = mQuizView.getDisplayedChild() + 1;
        setProgress(nextItem);
        final int count = mQuizView.getAdapter().getCount();
        if (nextItem < count) {
            mQuizView.showNext();
            //JVG.S
            if (mCategory.getId().equals(ARG_ONE_PLAYER)) {
                // TIME X QUIZZ
//                ((QuizActivity) getActivity()).setTimeToNextItem((TIME_TO_ANSWER_PLAY_GAME));
//                setTimeLeftText(TIME_TO_ANSWER_PLAY_GAME);
                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
            } else if (mCategory.getId().equals(ARG_REAL_TIME_ONLINE)) {
//                TIME X TOTAL QUIZZES
//                ((QuizActivity) getActivity()).setTimeToNextItem((TIME_TO_ANSWER_PLAY_GAME));
//                setTimeLeftText(TIME_TO_ANSWER_PLAY_GAME);
                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
            } else if (((QuizActivity) getActivity()).isMatchTurnBased()) {
                ((QuizActivity) getActivity()).postDelayHandlerPlayGame();
            }
            /// Actualizar el estado de los test
            //TopekaDatabaseHelper.updateCategory(getActivity(), mCategory);
            switch (mCategory.getId()) {
                case ARG_ONE_PLAYER:
                case ARG_REAL_TIME_ONLINE:
                    break;
                default:
                    // Update score
                    new Thread() {
                        @Override
                        public void run() {
                            TrivialJSonHelper.getInstance(getContext(), false).updateCategory();
                        }
                    }.start();
            }

            //JVG.E
            return true;
        }
        markCategorySolved();
        return false;
    }

    private void markCategorySolved() {
        mCategory.setSolved(true);

        //JVG.S
        ///TopekaDatabaseHelper.updateCategory(getActivity(), mCategory);
        /// Actualizar el estado de los test
        switch (mCategory.getId()) {
            case ARG_ONE_PLAYER:
            case ARG_REAL_TIME_ONLINE:
                break;
            default:
                // Update score
                new Thread() {
                    @Override
                    public void run() {
                        TrivialJSonHelper.getInstance(getContext(), false).updateCategory();
                    }
                }.start();
        }
        //JVG.E
    }

    //JVG.S
    // Score Play Online
    public void showSummaryOnline() {
        mCategory.setSolved(true);
        final ListView scorecardOnLineView = (ListView) getView().findViewById(R.id.scorecard_online);
        mScoreOnlineAdapter = getScoreOnlineAdapter();
        scorecardOnLineView.setAdapter(mScoreOnlineAdapter);
        scorecardOnLineView.setVisibility(View.VISIBLE);
        mQuizView.setVisibility(View.GONE);

        //JVG.E
        mTimeLeftText.setText(getString(R.string.x_points, mCategory.getScore()));
        if (mCategory.getId().equals(QuizActivity.ARG_ONE_PLAYER)) {
//            mTimeLeftText.setVisibility(View.INVISIBLE);
        } else if (mCategory.getId().equals(QuizActivity.ARG_REAL_TIME_ONLINE)) {
//            mTimeLeftText.setVisibility(View.INVISIBLE);

            byte[] message = new String(mCategory.getScore() + "|" + (((QuizActivity) getActivity()).timeToNextItem / 1000)).getBytes();

            byte[] tmpMessage = new byte[message.length + 1];

            tmpMessage[0] = 'P';
            for (int i = 0; i < message.length; i++) {
                tmpMessage[i + 1] = message[i];
            }

            for (Participant p : Game.mParticipants) {
                if (!p.getParticipantId().equals(Game.mMyId) && p.isConnectedToRoom() && p.getStatus() == Participant.STATUS_JOINED) {
                    sendMessageScore(tmpMessage, p.getParticipantId(), 1);
                }
            }
        }
        //JVG.E
    }

    private void sendMessageScore(final byte[] tmpMessage, final String participantId, final int numTimesSended) {
        ((QuizActivity) getActivity()).mRealTimeMultiplayerClient.sendReliableMessage(tmpMessage, Game.mRoomId, participantId, new RealTimeMultiplayerClient.ReliableMessageSentCallback() {
            @Override
            public void onRealTimeMessageSent(int statusCode, int tokenId, String recipientParticipantId) {
                if (statusCode == STATUS_OK) {

                } else {
                    Log.d("GPG", "Error enviando mensaje tipo P" + numTimesSended);
                    if (numTimesSended <= MAX_RETRY_TIMES) {
                        sendMessageScore(tmpMessage, participantId, numTimesSended + 1);
                    } else {
                        ((QuizActivity) getActivity()).showGameError("Why: Sending Score!", false);
                    }
                }
            }
        });
    }

    // JVG.S
    ProgressDialog pWaitingProgress = null;

    public void showSummary() {
        if (mCategory.getId().equals(QuizActivity.ARG_REAL_TIME_ONLINE)) {
            pWaitingProgress = new ProgressDialog(getContext());
            pWaitingProgress.setTitle("Waiting Players...");
            pWaitingProgress.show();
            pWaitingProgress.setOnCancelListener(new DialogInterface.OnCancelListener() {
                @Override
                public void onCancel(DialogInterface dialog) {
                    pWaitingProgress.dismiss();
                    pWaitingProgress = null;
                }
            });
            showSummaryOnline();
        } else {
            showSummaryOffLine();
        }
    }
    //JVG.E

    public void showSummaryOffLine() {
        @SuppressWarnings("ConstantConditions") final ListView scorecardView = (ListView) getView().findViewById(R.id.scorecard);
        mScoreAdapter = getScoreAdapter();
        scorecardView.setAdapter(mScoreAdapter);
        scorecardView.setVisibility(View.VISIBLE);
        mQuizView.setVisibility(View.GONE);
        //JVG.E
        mTimeLeftText.setText(getString(R.string.x_points, mCategory.getScore()));
        if (mCategory.getId().equals(QuizActivity.ARG_ONE_PLAYER)) {
//            mTimeLeftText.setVisibility(View.INVISIBLE);
        }
        //JVG.E
    }

    public boolean hasSolvedStateListener() {
        return mSolvedStateListener != null;
    }

    public void setSolvedStateListener(SolvedStateListener solvedStateListener) {
        mSolvedStateListener = solvedStateListener;
        if (mCategory.isSolved() && null != mSolvedStateListener) {
            mSolvedStateListener.onCategorySolved();
        }
    }

    private ScoreAdapter getScoreAdapter() {
        if (null == mScoreAdapter) {
            mScoreAdapter = new ScoreAdapter(mCategory, getContext());
        }
        return mScoreAdapter;
    }

    // JVG.
    public ScoreOnline scoreOnline = new ScoreOnline();

    public ScoreOnlineAdapter getScoreOnlineAdapter() {
        generateScoreOnline(null, mCategory.getScore(), ((QuizActivity) getActivity()).timeToNextItem / 1000);
        if (null == mScoreOnlineAdapter) {
            mScoreOnlineAdapter = new ScoreOnlineAdapter(scoreOnline, getContext());
        }

        return mScoreOnlineAdapter;
    }

    public void generateScoreOnline(String participant, int points, int timeLeft) {

//        Log.d("TRAZASCORE", ((participant == null) ? Game.mMyId : participant) + " " + " " + points + " " + timeLeft);
//        scoreOnline.getmScoreOnline().clear();
//        for (Participant p : Game.mParticipants) {
//            if (p.getParticipantId().equals(Game.mMyId)) {
//                ScoreOnline.Score score = new ScoreOnline.Score(Game.myName, mCategory.getScore(), ((QuizActivity)getActivity()).timeToNextItem);
//                scoreOnline.add(score);
//            }
//
//            if (p.getStatus() != Participant.STATUS_JOINED) {
//                ScoreOnline.Score score = new ScoreOnline.Score(p.getDisplayName(), 0, 0);
//                scoreOnline.add(score);
//            }
//
//            if (participant != null && p.getParticipantId().equals(participant)) {
//                ScoreOnline.Score score = new ScoreOnline.Score(p.getDisplayName(), points, timeLeft);
//                scoreOnline.add(score);
//            }
//
//
//        }

        String tmpParticipant;
        // Actualiza puntuación local
        if (participant == null) {
            tmpParticipant = Game.mMyId;
        } else {
            tmpParticipant = participant;
        }

        // FIRST FIND IF THERE IS ANY RECORD WITH tmpParticipant update
        // Else create a new score
        boolean alreadyExists = false;
        for (ScoreOnline.Score score : scoreOnline.getScoreOnline()) {
            if (score.getParticipant().equals(tmpParticipant)) {
                // Update
                alreadyExists = true;

                score.setTimeLeft(timeLeft);
                score.setStatus("FINISHED");
                score.setPoints(points);
            }
        }


        // Create new register (not exists id participant)
        if (!alreadyExists) {
            if (participant == null) {
                ScoreOnline.Score score = new ScoreOnline.Score(Game.mMyId, Game.myName, "FINISHED", mCategory.getScore(), ((QuizActivity) getActivity()).timeToNextItem / 1000);
                scoreOnline.add(score);
            } else {
                // Actualiza resto puntuaciones
                for (Participant p : Game.mParticipants) {
                    if (p.getParticipantId().equals(participant)) {
                        ScoreOnline.Score score;
                        if (p.getStatus() != Participant.STATUS_JOINED) {

                            score = new ScoreOnline.Score(p.getParticipantId(), p.getDisplayName(), "UNKNOWN", 0, 0);
                        } else {
                            score = new ScoreOnline.Score(p.getParticipantId(), p.getDisplayName(), "FINISHED", points, timeLeft);

                        }
                        scoreOnline.add(score);
                    }
                }
            }
        }

        // Add the rest of the participantsTurnBased
        for (Participant p : Game.mParticipants) {
            int cont = 0;
            for (ScoreOnline.Score score : scoreOnline.getScoreOnline()) {
                if (p.getParticipantId().equals(score.getParticipant())) break;
                cont++;
            }
            if (cont == scoreOnline.getScoreOnline().size()) {
                ScoreOnline.Score score = new ScoreOnline.Score(p.getParticipantId(), p.getDisplayName(), "PENDING", 0, 0);
                scoreOnline.add(score);
            }
        }
        scoreOnline.updateIsWinner();

        if (mScoreOnlineAdapter != null) {
            mScoreOnlineAdapter.notifyDataSetChanged();
        }


        checkScoreNumParticipants();
    }

    public void checkScoreNumParticipants() {
//        Log.d("TRAZA", "Closing Dialog!" + "" + scoreOnline.areAnyPendingScore());
        if (!scoreOnline.areAnyPendingScore()) {
            if (pWaitingProgress != null) {
                pWaitingProgress.dismiss();
                pWaitingProgress = null;
            }
        } else {
            if (pWaitingProgress != null) {
                pWaitingProgress.hide();
                pWaitingProgress.show();
            }
        }
    }
// JVG.E

    /**
     * Interface definition for a callback to be invoked when the quiz is started.
     */
    public interface SolvedStateListener {

        /**
         * This method will be invoked when the category has been solved.
         */
        void onCategorySolved();
    }
}