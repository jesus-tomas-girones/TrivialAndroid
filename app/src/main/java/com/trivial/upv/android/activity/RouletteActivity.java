package com.trivial.upv.android.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.TextView;

import com.trivial.upv.android.R;
import com.trivial.upv.android.adapter.RouletteScorePlayerAdapter;
import com.trivial.upv.android.fragment.CustomDialogFragment;
import com.trivial.upv.android.model.Theme;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.roulette.RouletteView;
import com.trivial.upv.android.widget.roulette.ShakeListener;

import java.util.List;
import java.util.Locale;

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


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.roulette_activity);
        setupView();
    }

    private void setupView() {
        playable = getIntent().getBooleanExtra("playable", false);
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

        setupRoulette();
        setupScorePlayer();
    }

    private void setupScorePlayer() {
        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
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
        // 2 imagenes

        mAdapter = new RouletteScorePlayerAdapter(getApplicationContext(), score, imagenes);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFitsSystemWindows(true);
        DividerItemDecoration dividerItemDecoration = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.HORIZONTAL);
        mRecyclerView.addItemDecoration(dividerItemDecoration);
    }

    private void setupRoulette() {
        category_title = (TextView) findViewById(R.id.category_title);
        txtPoints = (TextView) findViewById(R.id.txtPoints);
        if (Game.mTurnData.numPreguntasContestadas < Game.mTurnData.numPreguntas)
            category_title.setText(String.format(Locale.getDefault(), "Quizz (%d/%d)", Game.mTurnData.numPreguntasContestadas + 1, Game.mTurnData.numPreguntas));
        else
            category_title.setText(String.format(Locale.getDefault(), "Finished Quizz"));

        txtPoints.setText(String.format(Locale.getDefault(), "Pts: %d", Game.mTurnData.calculateScorePlayer(Game.mPlayerId)));
        rouletteView = (RouletteView) findViewById(R.id.rouletteView);
        rouletteView.setRotationEventListener(rotateEventLinestener, playable);

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
        rouletteView.setNumSectors(intNumber, imagenes, mThemes);

        if (playable) {
            shakeListener = new ShakeListener(this);
            shakeListener.setOnShakeListener(this);
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        if (shakeListener != null)
            shakeListener.resume();
        rouletteView.resumeSound();
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

    RouletteView.RotateEventLisstener rotateEventLinestener = new RouletteView.RotateEventLisstener() {
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
                    intent.putExtra("category", (int)Game.mTurnData.categories.get(category));
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
        if (this.isRotationEnabled) {
            rouletteView.rotate(10);
        }
    }

    private String imagenes[];
    Theme mThemes[];

    public void leaveMatch(View v) {
        setResult(Activity.RESULT_CANCELED);
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
        if (isRotationEnabled)
            rouletteView.rotate(speed);
    }

    public void showMatchScore(View view) {
        Intent intent = new Intent(getApplicationContext(), ScoreTurbasedActivity.class);
        intent.putExtra("playable", playable);
        startActivity(intent);
        overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }
}
