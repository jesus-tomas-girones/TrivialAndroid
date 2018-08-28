package com.trivial.upv.android.activity;


import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.DividerItemDecoration;
import android.support.v7.widget.GridLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;

import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.trivial.upv.android.R;
import com.trivial.upv.android.adapter.RouletteScorePlayerAdapter;
import com.trivial.upv.android.model.Category;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.gpg.Turn;
import com.trivial.upv.android.persistence.TrivialJSonHelper;

import java.util.ArrayList;
import java.util.List;
import java.util.Locale;

import javax.xml.transform.Result;

import static com.trivial.upv.android.fragment.PlayTurnBasedFragment.K_PUNTOS_POR_PREGUNTA;

public class ScoreTurbasedActivity extends AppCompatActivity {

    int intNumber = 0;

    private RecyclerView mRecyclerView;
    private RecyclerView.Adapter mAdapter;
    private RecyclerView.LayoutManager mLayoutManager;

    private ImageButton btnBack;
    private TextView category_title;


    @Override
    protected void onCreate(Bundle savedInstanceState) {
//        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
//        requestWindowFeature(Window.FEATURE_NO_TITLE)
        super.onCreate(savedInstanceState);
        setContentView(R.layout.score_turnbased_activity);
        setupView();
    }

    private void setupView() {
        category_title = (TextView) findViewById(R.id.category_title);
        if (!Game.mTurnData.isFinishedMatch()) //Game.mTurnData.numPreguntasContestadas < Game.mTurnData.numPreguntas)
            category_title.setText(String.format(Locale.getDefault(), "Categories (%d/%d)", Game.mTurnData.getNunCategoriesOKFromPlayer(Game.mPlayerId), Game.mTurnData.categories.size()));
        else
            category_title.setText(String.format(Locale.getDefault(), "Finished Match"));
        btnBack = (ImageButton) findViewById(R.id.back);
        btnBack.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                onBackPressed();
            }
        });

        List<Category> allCategories = TrivialJSonHelper.getInstance(this, false).getCategories(false);
        List<Category> categories = new ArrayList<>();
        for (short categoryAux : Game.mTurnData.categories) {
            int indice = categoryAux;
            categories.add(allCategories.get(indice));
        }


        this.intNumber = (Game.mTurnData.numJugadores + 2) * (categories.size() + 2);


        imagenes = new String[this.intNumber];
        String score[] = new String[intNumber];


        for (int j = 0; j < categories.size() + 2; j++) {
            for (int i = 0; i < Game.mTurnData.numJugadores + 2; i++) {
                int posAux = i + j * (Game.mTurnData.numJugadores + 2);
                // (0,0)
                if (i == 0 && j == 0) {
                    imagenes[posAux] = "";
                    score[posAux] = "";
                }
                // Cabecera con imágenes del jugador
                else if (j == 0 && i < Game.mTurnData.numJugadores + 1) {
                    ArrayList<Participant> participants = Game.mMatch.getParticipants();
                    if (i - 1 < participants.size() && participants.get(i - 1) != null) {
                        imagenes[posAux] = participants.get(i - 1).getIconImageUri().toString();
                    } else {
                        imagenes[posAux] = "default";
                    }
                    score[posAux] = "";
                }
                // Score Total Category
                else if (i == Game.mTurnData.numJugadores + 1 && j > 0 && j < categories.size() + 1) {
                    imagenes[posAux] = "";
                    int totalQuizzes = 0;
                    int totalQuizzesAnsweredOK = 0;
                    for (int cont = 0; cont < Game.mTurnData.numJugadores; cont++) {
                        totalQuizzes += Game.mTurnData.puntuacion[cont][j - 1][0] + Game.mTurnData.puntuacion[cont][j - 1][1];
                        totalQuizzesAnsweredOK += Game.mTurnData.puntuacion[cont][j - 1][0];
//                        if (Game.mTurnData.puntuacion[cont][2] == Game.mTurnData.categories.get(j - 1)) {
//                            totalQuizzes++;
//                            if (Game.mTurnData.puntuacion[cont][1] > 0) {
//                                totalQuizzesAnsweredOK++;
//                            }
//
//                        }
                    }
                    score[posAux] = String.format(Locale.getDefault(), "%d/%d", totalQuizzesAnsweredOK, totalQuizzes);

                } else if (j == categories.size() + 1) {
                    // Score Total Player
                    imagenes[posAux] = "";
                    if (i > 0 && i < Game.mTurnData.numJugadores + 1) {
                        int totalQuizzes = 0;
                        int totalQuizzesAnsweredOK = 0;
                        for (int cont = 0; cont < Game.mTurnData.categories.size(); cont++) {
                            totalQuizzes += Game.mTurnData.puntuacion[i - 1][cont][0] + Game.mTurnData.puntuacion[i - 1][cont][1];
                            totalQuizzesAnsweredOK += Game.mTurnData.puntuacion[i - 1][cont][0];
//                            if ((Game.mTurnData.puntuacion[cont][0] == i - 1)) {
//                                totalQuizzes++;
//                                if (Game.mTurnData.puntuacion[cont][1] > 0) {
//                                    totalQuizzesAnsweredOK++;
//                                }
//                            }
                        }

                        String auxResult = "";
                        if (Game.mTurnData.isFinishedMatch()) {
                            if (i - 1 < Game.mMatch.getParticipants().size()) {
                                if (Game.mMatch.getParticipants().get(i - 1).getResult() != null) {
                                    switch (Game.mMatch.getParticipants().get(i - 1).getResult().getResult()) {
                                        case ParticipantResult.MATCH_RESULT_WIN:
                                            auxResult = "WIN";
                                            break;
                                        case ParticipantResult.MATCH_RESULT_LOSS:
                                            auxResult = "LOST";
                                            break;
                                        case ParticipantResult.MATCH_RESULT_TIE:
                                            auxResult = "TIE";
                                            break;
                                    }
                                } else {
                                    int scoreAux = Game.mTurnData.calculateScorePlayer(Game.mPlayerId);
                                    if (scoreAux > 0)
                                        auxResult = "WIN";
                                    else
                                        auxResult = "LOST";
                                }
                            } else
                                auxResult = "LOST";
                            score[posAux] = String.format(Locale.getDefault(), "%d/%d\nPts: %d\n(%s)", totalQuizzesAnsweredOK, totalQuizzes, totalQuizzesAnsweredOK * K_PUNTOS_POR_PREGUNTA, auxResult);
                        } else {
                            score[posAux] = String.format(Locale.getDefault(), "%d/%d\nPts: %d", totalQuizzesAnsweredOK, totalQuizzes, totalQuizzesAnsweredOK * K_PUNTOS_POR_PREGUNTA);
                        }
                    } else if (i == Game.mTurnData.numJugadores + 1) {
                        // Total Score
                        int totalQuizzes = 0;
                        int totalQuizzesAnsweredOK = 0;
                        for (int x = 0; x < Game.mTurnData.numJugadores; x++) {
                            for (int y = 0; y < Game.mTurnData.categories.size(); y++) {
                                totalQuizzesAnsweredOK += Game.mTurnData.puntuacion[x][y][0];
                                totalQuizzes += Game.mTurnData.puntuacion[x][y][0] + Game.mTurnData.puntuacion[x][y][1];
                            }
//                            if (Game.mTurnData.puntuacion[cont][1] > 0)
//                                totalQuizzesAnsweredOK++;
                        }
                        score[posAux] = String.format(Locale.getDefault(), "%d/%d", totalQuizzesAnsweredOK, totalQuizzes);
                    } else {
                        score[posAux] = "";
                    }

                } else if (i == 0) {
                    Category category = categories.get(j - 1);
                    imagenes[posAux] = category.getImg();
                    score[posAux] = "";

                } else {
                    imagenes[posAux] = "";
                    if (j > 0) {
                        // Score Total Categories x Player
                        int totalQuizzes = 0;
                        int totalQuizzesAnsweredOK = 0;

                        totalQuizzes = Game.mTurnData.puntuacion[i - 1][j - 1][0] + Game.mTurnData.puntuacion[i - 1][j - 1][1];
                        totalQuizzesAnsweredOK += Game.mTurnData.puntuacion[i - 1][j - 1][0];
//
//                        for (int cont = 0; cont < Game.mTurnData.numPreguntasContestadas; cont++) {
//                            if (Game.mTurnData.puntuacion[cont][2] == Game.mTurnData.categories.get(j - 1) && Game.mTurnData.puntuacion[cont][0] == i - 1) {
//                                totalQuizzes++;
//                                if (Game.mTurnData.puntuacion[cont][1] > 0) {
//                                    totalQuizzesAnsweredOK++;
//                                }
//                            }
//                        }
                        score[posAux] = String.format(Locale.getDefault(), "%d/%d", totalQuizzesAnsweredOK, totalQuizzes);
                    } else
                        score[posAux] = "";
                }
            }
        }

        mRecyclerView = (RecyclerView) findViewById(R.id.my_recycler_view);
        // use this setting to improve performance if you know that changes
        // in content do not change the layout size of the RecyclerView
//        mRecyclerView.setHasFixedSize(true);

        // use a linear layout manager
        mLayoutManager = new GridLayoutManager(this, Game.mTurnData.numJugadores + 2);
        mRecyclerView.setLayoutManager(mLayoutManager);

        // specify an adapter (see also next example)
        // 2 vectores:
        // 1 puntuaciones por categorias
//        String score[] = new String[intNumber + 2];
//        score[0] = "";
//        score[score.length - 1] = "";
//        int index = Game.mTurnData.participantsTurnBased.indexOf(Game.mPlayerId);
//        if (index != -1) {
//            for (int i = 1; i < score.length - 1; i++) {
//                // Cuantas preguntas hay de esa categoría
//                int numPreguntas = 0;
//                int numPreguntasOK = 0;
//
//                for (int j = 0; j < Game.mTurnData.numPreguntasContestadas; j++) {
//                    if (Game.mTurnData.puntuacion[j][2] == (short) i && Game.mTurnData.puntuacion[j][0] == index) {
//                        numPreguntas++;
//                        if (Game.mTurnData.puntuacion[j][1] > 0)
//                            numPreguntasOK++;
//                    }
//
//
//                }
////                score[i] = String.format("(%d/%d)", numPreguntasOK, numPreguntas);
//                score[i] = String.format("%d/%d", numPreguntasOK, numPreguntas);
//            }
//        } else {
//            for (int i = 0; i < intNumber; i++) {
////                score[i] = String.format("(%d/%d)", 0, 0);
//                score[i] = String.format("%d/%d", 0, 0);
//            }
//        }
        // 2 imagenes

        mAdapter = new RouletteScorePlayerAdapter(getApplicationContext(), score, imagenes);
        mRecyclerView.setAdapter(mAdapter);
        mRecyclerView.setFitsSystemWindows(true);
        DividerItemDecoration dividerItemDecorationH = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.HORIZONTAL);
        DividerItemDecoration dividerItemDecorationV = new DividerItemDecoration(mRecyclerView.getContext(),
                DividerItemDecoration.VERTICAL);
        mRecyclerView.addItemDecoration(dividerItemDecorationH);
        mRecyclerView.addItemDecoration(dividerItemDecorationV);


    }

    @Override
    protected void onResume() {
        super.onResume();
    }

    @Override
    protected void onPause() {
        super.onPause();
    }


    @Override
    public void onBackPressed() {

        Intent intent = new Intent();
        intent.putExtra("category", -1);
        setResult(Activity.RESULT_OK, intent);
        finish();
        super.onBackPressed();

    }


    private String imagenes[];

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
        overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.fade_out);
    }


}
