package com.trivial.upv.android.activity;


import android.app.Activity;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.trivial.upv.android.R;
import com.trivial.upv.android.adapter.RouletteScorePlayerAdapter;
import com.trivial.upv.android.fragment.CustomDialogFragment;
import com.trivial.upv.android.fragment.PlayTurnBasedFragment;
import com.trivial.upv.android.model.Theme;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.gpg.Turn;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.roulette.RouletteView;
import com.trivial.upv.android.widget.roulette.ShakeListener;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import static com.trivial.upv.android.fragment.PlayTurnBasedFragment.checkPlayerMatchResult;

public class RouletteActivity extends AppCompatActivity implements ShakeListener.OnShakeListener {

    boolean isRotationEnabled = true;
    int intNumber = 0;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    RouletteView rouletteView;


    private Button btnScore, btnCancelMatch;
    private ImageButton btnBack;
    ShakeListener shakeListener;
    private TextView category_title;
    private TextView txtPoints;
    private boolean playable = false;
    private RecyclerView mRecyclerView2;
    private GridLayoutManager mLayoutManager2;
    //    private RouletteScorePlayerAdapter mAdapter2;
    private RouletteScoreCategoriesAdapter mAdapter2;
    private boolean playSound = false;

    private boolean playFinished = false;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.roulette_activity);
        playable = getIntent().getBooleanExtra("playable", false);
        setupView(true);

    }

    private String TAG = RouletteActivity.class.getSimpleName();
    private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
        @Override
        public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {
            updateMatch(turnBasedMatch, true);
        }

        @Override
        public void onTurnBasedMatchRemoved(@NonNull String matchId) {
//            Toast.makeText(RouletteActivity.this, "A match was removed.", Toast.LENGTH_SHORT).show();
            Log.w(TAG, "A mas was removed + " + matchId);
        }
    };


    private AlertDialog mAlertDialog = null;

    // Generic warning/info dialog
    public void showWarning(String title, String message, final PlayTurnBasedFragment.ActionOnClickButton actionButton) {
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(this);
        // set title
        alertDialogBuilder.setTitle(title).setMessage(message);
        // set dialog message
        alertDialogBuilder.setCancelable(false).setPositiveButton("OK",
                new DialogInterface.OnClickListener() {
                    @Override
                    public void onClick(DialogInterface dialog, int id) {
                        // if this button is clicked, close
                        // current activity
                        if (actionButton != null)
                            actionButton.onClick();
                    }
                });
        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();
        // show it
        mAlertDialog.show();
    }

    PlayTurnBasedFragment.ActionOnClickButton finishActivity = new PlayTurnBasedFragment.ActionOnClickButton() {
        @Override
        public void onClick() {
            finish();
        }
    };

    public void showMessageFinishMatch(final TurnBasedMatch turnBasedMatch) {
        if (!playFinished) {
            playFinished = true;
            String txtMatchResultPlayer = "";
            Participant participant = null;
            try {
                String participantId = null;
                participantId = turnBasedMatch.getParticipantId(Game.mPlayerId);
                participant = turnBasedMatch.getParticipant(participantId);

            } catch (IllegalStateException ex) {
            }
            txtMatchResultPlayer = checkPlayerMatchResult(participant);

            showWarning("Partida Finalizada", txtMatchResultPlayer, null);
            setupView(false);
        }
    }

    public void updateMatch(TurnBasedMatch match, boolean playSound) {
        if (match.getMatchId().equals(Game.mMatch.getMatchId())) {
            Game.mMatch = match;
            Game.mTurnData = Turn.unpersist(match.getData());
            int status = match.getStatus();
            int turnStatus = match.getTurnStatus();


            switch (status) {
                case TurnBasedMatch.MATCH_STATUS_CANCELED:
                    showWarning("Canceled!", "This game was canceled!", finishActivity);
                    return;
                case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                    showWarning("Expired!", "This game is expired.  So sad!", finishActivity);
                    return;
                case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                    if (!Game.mTurnData.isFinishedMatch()) {
                        rouletteView.setLine1("Waiting for\nan automatch partner...");
//                        showWarning("Waiting for auto-match...",
//                                "We're still waiting for an automatch partner.", null);

                    } else {
                        rouletteView.setLine1("Complete!");
//                        showWarning("Complete!",
//                                "This game is over; someone finished it, and so did you!  " +
//                                        "There is nothing to be done.", null);
                        showMessageFinishMatch(match);
                    }
                    return;
                case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                    if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                        rouletteView.setLine1("Complete!");
//                        showWarning("Complete!",
//                                "This game is over; someone finished it, and so did you!  " +
//                                        "There is nothing to be done.", null);
                        showMessageFinishMatch(match);
                        setupView(false);
                        break;
                    }
                    // Note that in this state, you must still call "Finish" yourself,
                    // so we allow this to continue.
//                    rouletteView.setLine1("Complete!");
//                showWarning("Complete!",
//                        "This game is over; someone finished it!  You can only finish it now.", actionButtonDone);
                    finishFinishedMatch();
                    return;
            }

            // OK, it's active. Check on turn status.
            switch (turnStatus) {
                case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                    if (playSound)
                        rouletteView.playSoundIsYourTurn();
                    playable = true;
                    rouletteView.setLine1("\nIt's your turn!\n ");
                    setupView(false);
                    break;
                case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                    // Should return results.

                    if (!Game.mTurnData.isFinishedMatch()) {
                        String textNextPlayer = "";
                        try {
                            textNextPlayer = "Next player\n\n" + match.getParticipant(Game.mTurnData.idParticipantTurn).getDisplayName();
                            rouletteView.setLine1(textNextPlayer);
                        } catch (Exception ex) {
                        }
                        setupView(false);
//                        showWarning("It's not your turn...", textNextPlayer, null);
                    } else {
//                        showWarning("Complete!",
//                                "This game is over; someone finished it, and so did you!  " +
//                                        "There is nothing to be done.", null);
                        rouletteView.setLine1("Complete!");
                        showMessageFinishMatch(Game.mMatch);
                    }
                    break;
                case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                    rouletteView.setLine1("Still waiting for\ninvitations!");
//                    showWarning("Good inititative!",
//                            "Still waiting for invitations.\n\nBe patient!", null);
                    break;
            }
        }
    }

    public void finishFinishedMatch() {
        Game.mTurnBasedMultiplayerClient.finishMatch(Game.mMatch.getMatchId())
                .addOnSuccessListener(
                        new OnSuccessListener<TurnBasedMatch>() {
                            @Override
                            public void onSuccess(TurnBasedMatch turnBasedMatch) {
                                rouletteView.setLine1("Complete!");
//                                showWarning("Complete!",
//                                        "This game is over; someone finished it, and so did you!  " +
//                                                "There is nothing to be done.", null);
                                showMessageFinishMatch(Game.mMatch);
                            }
                        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(RouletteActivity.this,
                                "Hay un problema finalizando la partida", Toast.LENGTH_SHORT).show();
                    }
                });
    }


    private void setupView(boolean updateRoulete) {

//        btnRotate = (Button) findViewById(R.id.buttonStart);
        btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });
        btnScore = (Button) findViewById(R.id.buttonScore);
        btnCancelMatch = (Button) findViewById(R.id.buttonAbandon);
        if (!playable)
            btnCancelMatch.setVisibility(View.INVISIBLE);
        else
            btnCancelMatch.setVisibility(View.VISIBLE);

        setupRoulette(updateRoulete);
//        setupScorePlayerCategories();
//        setupScorePlayerByPlayer();
        setupScorePlayerByPlayer2();
    }

    private void setupScorePlayerCategories() {
        mRecyclerView = (RecyclerView) findViewById(R.id.recycler_view_categories);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new GridLayoutManager(this, Math.min(10, intNumber));
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        // 2 vectores:
        // 1 puntuaciones por categorias
        String score[] = new String[intNumber];

        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);
        int index = Game.mTurnData.participantsTurnBased.indexOf(myParticipantId);
        if (index != -1) {
            for (int i = 0; i < intNumber; i++) {
                // Cuantas preguntas hay de esa categoríaº
                int numPreguntas;
                int numPreguntasOK;

                numPreguntas = Game.mTurnData.puntuacion[index][i][0] + Game.mTurnData.puntuacion[index][i][1];
                numPreguntasOK = Game.mTurnData.puntuacion[index][i][0];
//                score[i] = String.format("(%d/%d)", numPreguntasOK, numPreguntas);
                score[i] = String.format("%d/%d", numPreguntasOK, numPreguntas);
            }
        } else {
            for (int i = 0; i < intNumber; i++) {
//                score[i] = String.format("(%d/%d)", 0, 0);
                score[i] = String.format("%d/%d", 0, 0);
            }
        }

        mAdapter = new RouletteScorePlayerAdapter(getApplicationContext(), score, imagenes);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFitsSystemWindows(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.HORIZONTAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }


//    private void setupScorePlayerByPlayer() {
//        int totalCategories = Game.mTurnData.categories.size();
//        int totalPlayers = Game.mTurnData.numJugadores;
//
//        mRecyclerView2 = (RecyclerView) findViewById(R.id.my_recycler_view2);
//        // use this setting to improve performance if you know that changes
//        // in content do not change the layout size of the RecyclerView
//        mRecyclerView2.setHasFixedSize(true);
//
//        // use a linear layout manager
//        mLayoutManager2 = new GridLayoutManager(this, Math.min(10, totalPlayers));
//        mRecyclerView2.setLayoutManager(mLayoutManager2);
//
//        String score2[] = new String[totalPlayers];
//        String imgPlayers[] = new String[totalPlayers];
//
//        // Pivot the first player is the firs position
//        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);
//        int index = Game.mTurnData.participantsTurnBased.indexOf(myParticipantId);
//        String tmpString = score2[index];
//        score2[0] = tmpString;
//        int posCont = 1;
//        for (int i = 0; i < totalPlayers; i++) {
//
//            int numCategories = 0;
//            for (int j = 0; j < Game.mTurnData.categories.size(); j++) {
//                if (Game.mTurnData.puntuacion[i][j][0] > 0) {
//                    numCategories++;
//                }
//            }
//            int posInsert;
//            if (index != i) {
//                posInsert = posCont;
//                posCont++;
//            } else {
//                posInsert = 0;
//            }
//            score2[posInsert] = String.format(Locale.getDefault(), "%d/%d", numCategories, totalCategories);
//            ArrayList<Participant> participants = Game.mMatch.getParticipants();
//            if (i < participants.size() && participants.get(i) != null) {
//                imgPlayers[posInsert] = participants.get(i).getIconImageUri().toString();
//            } else {
//                imgPlayers[posInsert] = "default";
//            }
//        }
//        mAdapter2 = new RouletteScorePlayerAdapter(getApplicationContext(), score2, imgPlayers);
//        mRecyclerView2.setAdapter(mAdapter2);
//        mRecyclerView2.setFitsSystemWindows(true);
//        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView2.getContext(),
//                DividerItemDecoration.HORIZONTAL);
//        mRecyclerView2.addItemDecoration(dividerItemDecoration);
//    }

    private void setupScorePlayerByPlayer2() {
        int totalPlayers = Game.mTurnData.numJugadores;

        mRecyclerView2 = (RecyclerView) findViewById(R.id.recycler_view_players);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
        mRecyclerView2.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager2 = new GridLayoutManager(this, Math.min(10, totalPlayers));
        mRecyclerView2.setLayoutManager(mLayoutManager2);

        String imgPlayers[] = new String[totalPlayers];

        // Pivot the first player is the firs position
        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);
        int index = Game.mTurnData.participantsTurnBased.indexOf(myParticipantId);

        int indexCurrentTurnPlayer = -1;
        if (Game.mTurnData.idParticipantTurn != null) {
            indexCurrentTurnPlayer = Game.mMatch.getParticipantIds().indexOf(Game.mTurnData.idParticipantTurn);
        } else {
            indexCurrentTurnPlayer = Game.mTurnData.participantsTurnBased.size();
            if (indexCurrentTurnPlayer < Game.mTurnData.numJugadores) {

            } else {
                indexCurrentTurnPlayer = Game.mTurnData.numJugadores - 1;
            }
        }
        int posCont = 1;
        List[] imagesCategories = new List[Game.mTurnData.numJugadores];
        List[] themesCategories = new List[Game.mTurnData.numJugadores];

        List<CategoryJSON> categories = TrivialJSonHelper.getInstance(this, false).getCategoriesJSON();

        boolean assigned = false;
        for (int i = 0; i < totalPlayers; i++) {
            int posInsert;
            if (index != i) {
                posInsert = posCont;
                posCont++;
            } else {
                posInsert = 0;
            }

            if (indexCurrentTurnPlayer == i && !assigned) {
                indexCurrentTurnPlayer = posInsert;
                assigned = true;
            }

            imagesCategories[posInsert] = new ArrayList<Integer>();
            themesCategories[posInsert] = new ArrayList<Integer>();
            for (int j = 0; j < Game.mTurnData.categories.size(); j++) {
                int indexCategory = Game.mTurnData.categories.get(j);
                CategoryJSON category = null;
                if (indexCategory < categories.size()) {
                    category = categories.get(indexCategory);
                } else {
                    // The game is corrupt
                    Log.w(TAG, "The game is corrupt");
                    finish();
                }

                if (Game.mTurnData.puntuacion[i][j][0] > 0) {
                    themesCategories[posInsert].add(Theme.valueOf(category.getTheme()));
                    imagesCategories[posInsert].add(category.getImg());
                } else {
                    themesCategories[posInsert].add(null);
                    imagesCategories[posInsert].add(null);
                }
            }

            ArrayList<Participant> participants = Game.mMatch.getParticipants();
            if (i < participants.size() && participants.get(i) != null) {
                imgPlayers[posInsert] = participants.get(i).getIconImageUri().toString();
            } else {
                imgPlayers[posInsert] = "default";
            }
        }

        mAdapter2 = new

                RouletteScoreCategoriesAdapter(getApplicationContext(), indexCurrentTurnPlayer, imgPlayers, imagesCategories, themesCategories);
        mRecyclerView2.setAdapter(mAdapter2);
        mRecyclerView2.setFitsSystemWindows(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView2.getContext(),
                DividerItemDecoration.HORIZONTAL);
        mRecyclerView2.addItemDecoration(dividerItemDecoration);
    }

    private void setupRoulette(boolean update) {
        category_title = (TextView) findViewById(R.id.category_title);
        txtPoints = (TextView) findViewById(R.id.txtPoints);
//        if (!Game.mTurnData.isFinishedMatch()) //Game.mTurnData.numPreguntasContestadas < Game.mTurnData.numPreguntas)
        category_title.setText(String.format(Locale.getDefault(), "Categories (%d/%d)", Game.mTurnData.getNunCategoriesOKFromPlayer(Game.mPlayerId), Game.mTurnData.categories.size()));
//        else
//            category_title.setText(String.format(Locale.getDefault(), "Finished Match"));

        txtPoints.setText(String.format(Locale.getDefault(), "Pts: %d", Game.mTurnData.calculateScorePlayer(Game.mPlayerId)));
        rouletteView = (RouletteView) findViewById(R.id.rouletteView);
        rouletteView.setRotationEventListener(rotateEventListener, playable);

        // Obtiene las categorías
        List<CategoryJSON> categories = TrivialJSonHelper.getInstance(this, false).getCategoriesJSON();
        this.intNumber = Game.mTurnData.categories.size();
//        this.intNumber = 3;

        mThemes = new Theme[this.intNumber];
        imagenes = new String[this.intNumber];

        for (int i = 0; i < this.intNumber; i++) {
//            CategoryJSON category = categories.get(i);
            int indice = Game.mTurnData.categories.get(i);
            CategoryJSON category = null;
            if (indice < categories.size()) {
                category = categories.get(indice);
                mThemes[i] = Theme.valueOf(category.getTheme());
                imagenes[i] = category.getImg();
            } else {
                // The game is corrupt
            }
        }
        rouletteView.setNumSectors(false, intNumber, imagenes, mThemes);

        if (playable) {
            shakeListener = new ShakeListener(this);
            shakeListener.setOnShakeListener(this);
        } else {
            shakeListener = null;
        }

        if (update) {
            updateMatch(Game.mMatch, false);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shakeListener != null)
            shakeListener.resume();
        rouletteView.resumeSound();
        if (Game.mTurnBasedMultiplayerClient != null)
            Game.mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);

    }

    @Override
    protected void onPause() {
        super.onPause();
        if (shakeListener != null)
            shakeListener.pause();
        rouletteView.pauseSound();

    }

    public interface FinishCounterEvent {
        void onFinishedCount();

    }

    RouletteView.RotateEventLisstener rotateEventListener = new RouletteView.RotateEventLisstener() {
        @Override
        public void rotateStart() {
            isRotationEnabled = false;
//            btnRotate.setVisibility(View.INVISIBLE);
            btnScore.setVisibility(View.INVISIBLE);
            btnCancelMatch.setVisibility(View.INVISIBLE);
        }

        @Override
        public void rotateEnd(final int category) {
//            btnRotate.setVisibility(View.VISIBLE);
//            isRotationEnabled = true;
//            validateSectors();
//            Toast toast = Toast.makeText(RouletteActivity.this, category + "", Toast.LENGTH_SHORT);
//            toast.setGravity(49, 0, 0);
//            toast.show();
//            Log.d("CATEGORY", category + "");
            // PlayQuiz
            CustomDialogFragment.showDialog(getSupportFragmentManager(), new FinishCounterEvent() {
                @Override
                public void onFinishedCount() {
                    Intent intent = new Intent();
                    intent.putExtra("category", (int) Game.mTurnData.categories.get(category));
                    setResult(Activity.RESULT_OK, intent);
                    finish();
                    overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
                }
            });

        }
    };

    @Override
    protected void onDestroy() {
        super.onDestroy();
        rouletteView.destroySound();
        if (Game.mTurnBasedMultiplayerClient != null)
            Game.mTurnBasedMultiplayerClient.unregisterTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
    }

    @Override
    public void onBackPressed() {
        if (isRotationEnabled) {
            rouletteView.destroySound();

            Intent intent = new Intent();
            intent.putExtra("category", -1);
            setResult(Activity.RESULT_OK, intent);
            super.onBackPressed();
        }

    }

    public void onClickButtonRotation(View v) {
        if (isRotationEnabled) {
            rouletteView.rotate(10);
        }
    }

    private String imagenes[];
    Theme mThemes[];

    public void leaveMatch(View v) {
        Intent intent = new Intent();
        intent.putExtra("cancel", true);
        setResult(Activity.RESULT_OK, intent);
        finish();
//        if (this.intNumber < 10) {
//            this.intNumber++;
//            rouletteView.setNumSectors(this.intNumber, imagenes, mThemes);
//            rouletteView.invalidate();
//            validateSectors();
//        }
    }


    @Override
    public void finish() {
        super.finish();
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    @Override
    public void onShake(float speed) {
        if (isRotationEnabled) {
//            Log.d("SHAKESPEED", ""+ speed);
            rotateEventListener.rotateStart();
            rouletteView.rotate(speed * 1.5f);
        }

    }

    public void showMatchScore(View view) {
        Intent intent = new Intent(getApplicationContext(), ScoreTurbasedActivity.class);
        intent.putExtra("playable", playable);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
