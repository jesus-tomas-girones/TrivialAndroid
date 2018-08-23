package com.trivial.upv.android.fragment;

import android.app.Activity;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.support.v7.app.AlertDialog;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.webkit.JavascriptInterface;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.ProgressBar;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.common.api.ApiException;
import com.google.android.gms.common.images.ImageManager;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesCallbackStatusCodes;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.GamesClientStatusCodes;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.TurnBasedMultiplayerClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.Participant;
import com.google.android.gms.games.multiplayer.ParticipantResult;
import com.google.android.gms.games.multiplayer.realtime.RoomConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchConfig;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatchUpdateCallback;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.CategorySelectionActivity;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.activity.RouletteActivity;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.gpg.Turn;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TrivialJSonHelper;

import java.util.ArrayList;
import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.media.MediaCodec.MetricsConstants.MODE;
import static com.google.android.gms.games.multiplayer.ParticipantResult.MATCH_RESULT_LOSS;
import static com.google.android.gms.games.multiplayer.ParticipantResult.MATCH_RESULT_TIE;
import static com.google.android.gms.games.multiplayer.ParticipantResult.MATCH_RESULT_WIN;
import static com.google.android.gms.games.multiplayer.ParticipantResult.PLACING_UNINITIALIZED;

/**
 * Created by jvg63 on 22/06/2017.
 */

public class PlayTurnBasedFragment extends Fragment {

    private static final String QUICK_GAME = "QUICK_GAME";
    private static final String CUSTOM_GAME = "CUSTOM_GAME";
    public static final short K_PUNTOS_POR_PREGUNTA = 1;
    private static final int K_MAX_PREGUNTAS = 50;
    private static final int K_MIN_PREGUNTAS = 2;

    private Button btnPartidasGuardadas;

    private Button btnNewGame;


    final static int RC_INVITATION_INBOX = 10001;
    final static int RC_LOOK_AT_MATCHES = 20001;
    final static int RC_CHOOOSE_CATEGORY = 30001;
    private static final int RC_PLAY_TURN = 30002;

    private com.google.android.gms.common.SignInButton btnConectar;
    //    private Button btnDesconectar;

//    private WebView navegador;

    List<CategoryJSON> categoriesJSON;

    private SeekBar sbNumQuizzes;
    private TextView txtNumQuizzes;
    //    private String mPlayerId;
    private String mDisplayName;
    private Button btnPlayAnyone;
    private Button btnShowIntitations;

    // Local convenience pointers
    private ProgressBar progressLayout;
    private View panelGame;

    // Player
    private Uri mImagePlayer;
    private ImageView mImgAvatar;
    private TextView mNameAvatar;
    private GoogleSignInAccount mSignedInAccount = null;

    public PlayTurnBasedFragment() {
        Game.resetGameVars();
        Game.listCategories.clear();
        categoriesJSON = TrivialJSonHelper.getInstance(getContext(), false).getCategoriesJSON();
        for (CategoryJSON category : categoriesJSON) {
            Game.listCategories.add(new String(category.getCategory()));
        }
        Game.level = (long) (Math.pow(2, categoriesJSON.size()) - 1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Game.mode = getArguments().getString(MODE);
        Game.mTurnData = null;
        Game.mMatch = null;
    }


    public static PlayTurnBasedFragment newInstance(String mode) {
        PlayTurnBasedFragment fragment = new PlayTurnBasedFragment();
        Bundle args = new Bundle();
        args.putString(MODE, mode);
        fragment.setArguments(args);
        return fragment;
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_play_turn_based, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        super.onViewCreated(view, savedInstanceState);

        changeTitleActionBar();

        setupView(view);
    }

    private void changeTitleActionBar() {
        ((CategorySelectionActivity) getActivity()).setToolbarTitle(getString(R.string.play_turn_based));
    }

    private SeekBar sbPlayers;

    private SeekBar sbTotalTime;
    private TextView txtPlayers;
    private TextView txtTotalTime;


    // Client used to sign in with Google APIs
    private GoogleSignInClient mGoogleSignInClient = null;

    // Client used to interact with the TurnBasedMultiplayer system.
    private TurnBasedMultiplayerClient mTurnBasedMultiplayerClient = null;

    // Client used to interact with the Invitation system.
    private InvitationsClient mInvitationsClient = null;

    public void signInSilently() {
        Log.d(TAG, "signInSilently()");
        mGoogleSignInClient.silentSignIn().addOnCompleteListener(getActivity(),
                new OnCompleteListener<GoogleSignInAccount>() {
                    @Override
                    public void onComplete(
                            @NonNull Task<GoogleSignInAccount> task) {
                        if (task.isSuccessful()) {
                            Log.d(TAG, "signInSilently(): success");
                            onConnected(task.getResult());
                        } else {
                            Log.d(TAG, "signInSilently(): failure", task.getException());
                            onDisconnected();
                        }
                    }
                });
    }

    @Override
    public void onResume() {
        super.onResume();
        signInSilently();


//        if (Game.listCategories.size() == TrivialJSonHelper.getInstance(getContext(), false).getCategoriesJSON().size()) {
//            txtListCategories.setText(getString(R.string.all_categories));
//        } else {
////            txtListCategories.setText("(Otras)");
//
//            StringBuilder sb = new StringBuilder();
//
//            for (int pos = 0; pos < Game.listCategories.size(); pos++) {
//                sb.append(Game.listCategories.get(pos));
//                if (pos != Game.listCategories.size() - 1) {
//                    sb.append(",\n");
//                }
//            }
//            txtListCategories.setText(sb.toString());
//        }
//        soundPool.resume(idSonido);

    }

    public class InterfazComunicacion {
        Context mContext;

        InterfazComunicacion(Context c) {
            mContext = c;
        }

        @JavascriptInterface
        public void categorySelected(int categorySelected) {
            startQuizzes(categorySelected);
        }

        @JavascriptInterface
        public void playSound() {
//            if (soundPool != null)
//                soundPool.play(idSonido, 1, 1, 1, 0, 1);
        }
    }


    final InterfazComunicacion miInterfazJava = new InterfazComunicacion(getActivity());

    // Should I be showing the turn API?
    public boolean isDoingTurn = false;

    // Create a one-on-one automatch game.
    public void onQuickMatchClicked(View view) {

        Bundle autoMatchCriteria = RoomConfig.createAutoMatchCriteria(sbPlayers.getProgress() - 1, sbPlayers.getProgress() - 1, 0);

        TurnBasedMatchConfig turnBasedMatchConfig = TurnBasedMatchConfig.builder()
                .setAutoMatchCriteria(autoMatchCriteria).build();

        showSpinner();

        // Start the match
        mTurnBasedMultiplayerClient.createMatch(turnBasedMatchConfig)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        onInitiateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem creating a match!"));
    }

    private void onCancelMatch(String matchId) {
        isDoingTurn = false;

        showWarning("Match", "This match (" + matchId + ") was canceled.  " +
                "All other players will have their game ended.", null);
    }

    // In-game controls
    // Cancel the game. Should possibly wait until the game is canceled before
    // giving roulette_up on the view.
    public void onCancelClicked(View view) {
        showSpinner();

        mTurnBasedMultiplayerClient.cancelMatch(Game.mMatch.getMatchId())
                .addOnSuccessListener(new OnSuccessListener<String>() {
                    @Override
                    public void onSuccess(String matchId) {
                        onCancelMatch(matchId);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem cancelling the match!"));

        isDoingTurn = false;
//        setViewVisibility();
    }

    private void onLeaveMatch() {
        isDoingTurn = false;
        showWarning("Left", "You've left this match.", null);
//        setViewVisibility();
    }

    // Leave the game during your turn. Note that there is a separate
    // mTurnBasedMultiplayerClient.leaveMatch() if you want to leave NOT on your turn.
    public void onLeaveClicked(View view) {
        showSpinner();
        String nextParticipantId = getNextParticipantId();

        mTurnBasedMultiplayerClient.leaveMatchDuringTurn(Game.mMatch.getMatchId(), nextParticipantId)
                .addOnSuccessListener(new OnSuccessListener<Void>() {
                    @Override
                    public void onSuccess(Void aVoid) {
                        onLeaveMatch();
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem leaving the match!"));

//        setViewVisibility();
    }

    private void setupView(final View view) {
        CategorySelectionActivity.animateViewFullScaleXY(((CategorySelectionActivity) getActivity()).getSubcategory_title(), 100, 300);

        // Initialie header
        mImgAvatar = (ImageView) view.findViewById(R.id.imgAvatar);
        mNameAvatar = (TextView) view.findViewById(R.id.txtNameAvatar);

        // Inicializa botones principales
        btnPlayAnyone = (Button) view.findViewById(R.id.btnPlayAnyone);
        btnPlayAnyone.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTurnBasedMultiplayerClient != null) {
                    onQuickMatchClicked(view);
//                showSpinner();
                } else {
                    tryLogInGPG();
                }
            }
        });
        btnNewGame = (Button) view.findViewById(R.id.btnNewGame);
        btnNewGame.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTurnBasedMultiplayerClient != null) {
                    onStartMatchClicked(view);
//                showSpinner();
                } else {
                    tryLogInGPG();
                }
            }
        });
        btnSelectMatch = (Button) view.findViewById(R.id.btnSelectMatch);
        btnSelectMatch.setOnClickListener(new View.OnClickListener()

        {
            @Override
            public void onClick(View view) {
                if (mTurnBasedMultiplayerClient != null) {
                    onCheckGamesClicked(view);
//                showSpinner();}
                } else {
                    tryLogInGPG();
                }
            }
        });
        btnShowIntitations = (Button) view.findViewById(R.id.btnShowInvitations);
        btnShowIntitations.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                if (mTurnBasedMultiplayerClient != null) {
                    btnVer_Invitaciones_Click(view);
//                showSpinner();
                } else {
                    tryLogInGPG();
                }
            }
        });

        panelGame = (View) view.findViewById(R.id.panel_game);
//        navegador = (WebView) view.findViewById(R.id.webview);
        progressLayout = view.findViewById(R.id.progress_dialog);
//        navegador.getSettings().setJavaScriptEnabled(true);
//        navegador.getSettings().setBuiltInZoomControls(false);
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
//            WebView.setWebContentsDebuggingEnabled(true);
//        }
//        navegador.loadUrl("file:///android_asset/index.html");
//        navegador.addJavascriptInterface(miInterfazJava, "jsInterfazNativa");
//
//
//        navegador.setWebChromeClient(new WebChromeClient() {
//            @Override
//            public void onProgressChanged(WebView view, int progreso) {
//                barraProgreso.setProgress(0);
//                barraProgreso.setVisibility(View.VISIBLE);
//                ActividadPrincipal.this.setProgress(progreso * 1000);
//                Log.d("LOG", "" + progreso);
//                barraProgreso.incrementProgressBy(progreso);
//                if (progreso == 100) {
//                    barraProgreso.setVisibility(View.GONE);
//                }
//            }

//            @Override
//            public boolean onJsAlert(WebView view, String url, String message,
//                                     final JsResult result) {
//                new AlertDialog.Builder(getActivity()).setTitle("Mensaje")
//                        .setMessage(message).setPositiveButton
//                        (android.R.string.ok, new AlertDialog.OnClickListener() {
//                            public void onClick(DialogInterface dialog, int which) {
//                                result.confirm();
//                            }
//                        }).setCancelable(false).create().show();
//                return true;
//            }
//
//        });
//        navegador.setWebViewClient(new WebViewClient() {
//            @Override
//            public void onPageStarted(WebView view, String url, Bitmap favicon) {
//                if (dialogo != null)
//                    dialogo.dismiss();
//                dialogo = new ProgressDialog(getActivity());
//                dialogo.setMessage("Cargando...");
//                dialogo.setCancelable(true);
//                dialogo.show();
////                btnDetener.setEnabled(true);
//
//                comprobarConectividad();
////                if (comprobarConectividad()) {
////                    btnDetener.setEnabled(true);
////                } else {
////                    btnDetener.setEnabled(false);
////                }
//
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                dialogo.dismiss();
//                addCategories();
//            }
//
//            @Override
//            public void onReceivedError(WebView view, int errorCode,
//                                        String description, String failingUrl) {
//                AlertDialog.Builder builder =
//                        new AlertDialog.Builder(getActivity());
//                builder.setMessage(description).setPositiveButton("Aceptar",
//                        null).setTitle("onReceivedError");
//                builder.show();
//            }
//
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
////                String url_filtro = "http://www.androidcurso.com/";
////                if (!url.toString().equals(url_filtro)) {
////                    view.loadUrl(url_filtro);
////                }
//                return false;
//            }
//
//
//        });

        sbPlayers = (SeekBar) view.findViewById(R.id.sbPlayers);
        sbNumQuizzes = (SeekBar) view.findViewById(R.id.sbNumQuizzes);
        txtNumQuizzes = (TextView) view.findViewById(R.id.txtNumQuizzes);

        // Set default value to 0
        sbPlayers = (SeekBar) view.findViewById(R.id.sbPlayers);
        txtPlayers = (TextView) view.findViewById(R.id.txtPlayers);

        sbPlayers.setOnSeekBarChangeListener(new SeekBar.OnSeekBarChangeListener() {
            int progress = sbPlayers.getProgress();

            @Override
            public void onProgressChanged(SeekBar seekBar, int progresValue, boolean fromUser) {
                if (progresValue < 2) {
                    seekBar.setProgress(2);
                    progress = 2;
                } else {

                    progress = normalizeProgres2_4_8(progresValue);
                    sbPlayers.setProgress(progress);
                    txtPlayers.setText(getString(R.string.num_players) + progress + "/" + (seekBar.getMax() - 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Display the value in textview*/
                txtPlayers.setText(getString(R.string.num_players) + progress + "/" + (seekBar.getMax() - 1));
            }
        });

        sbNumQuizzes.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener()

                {
                    int progress = sbNumQuizzes.getProgress();

                    @Override
                    public void onProgressChanged(SeekBar seekBar,
                                                  int progresValue, boolean fromUser) {
                        if (progresValue < K_MIN_PREGUNTAS) {
                            progress = K_MIN_PREGUNTAS;

                        } else if (progresValue > K_MAX_PREGUNTAS) {
                            progress = K_MAX_PREGUNTAS;
                        } else {
                            progress = progresValue;

                        }
                        seekBar.setProgress(progress);
                        txtNumQuizzes.setText("Num. Quizzes: " + progress + "/" + K_MAX_PREGUNTAS);
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {
                        // Display the value in textview
                        txtNumQuizzes.setText("Num. Quizzes: " + progress + "/" + K_MAX_PREGUNTAS);
                    }
                });


//        soundPool = new SoundPool(1, AudioManager.STREAM_MUSIC, 0);
//        idSonido = soundPool.load(getContext(), R.raw.tick, 0);
        showSeekbarsProgress();
        mGoogleSignInClient = GoogleSignIn.getClient(

                getActivity(),
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).

                        build());

//        startSignInIntent();
    }

    public void tryLogInGPG() {
        showWarning("Google Play Games", "You aren't not log in! OK to try it", new ActionOnClickButton() {
            @Override
            public void onClick() {
                startSignInIntent();
            }
        });
    }

    private void onPlayButton(View view, boolean playable) {
        // Muestra Ruleta y elige categoria
//        navegador.setVisibility(View.VISIBLE);
//        panelGame.setVisibility(View.GONE);
//        navegador.loadUrl("javascript:girar()");
        dismissSpinner();

        Game.mTurnData = Turn.unpersist(Game.mMatch.getData());

        if (Game.mTurnData.numPreguntasContestadas < Game.mTurnData.numPreguntas || !playable) {

            Intent startIntent = new Intent(getActivity(), RouletteActivity.class);
            startIntent.putExtra("playable", playable);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                startActivityForResult(startIntent, RC_CHOOOSE_CATEGORY, null);
            } else {
                startActivityForResult(startIntent, RC_CHOOOSE_CATEGORY, null);
            }
            getActivity().overridePendingTransition(android.R.anim.slide_in_left, android.R.anim.slide_out_right);
        } else {
            if (Game.mTurnData.numTurnos >= 1)
                onFinishClicked(null);
            else {
                String nextParticipantId = getNextParticipantId();
                Game.mTurnData.numTurnos++;
                nextTurn(nextParticipantId, nextParticipantId);
            }
        }

    }


    // Open the create-game UI. You will get back an onActivityResult
// and figure out what to do.
    public void onStartMatchClicked(View view) {
        final int NUMERO_MINIMO_OPONENTES = sbPlayers.getProgress() - 1, NUMERO_MAXIMO_OPONENTES = sbPlayers.getProgress() - 1;

        boolean allowAutoMatch = false;
        Games.getTurnBasedMultiplayerClient(getActivity(), GoogleSignIn.getLastSignedInAccount(getActivity())).getSelectOpponentsIntent(NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES, allowAutoMatch)
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_SELECT_PLAYERS);
                    }
                })
                .addOnFailureListener(createFailureListener(getString(R.string.error_get_select_opponents)));
    }


    public void startSignInIntent() {
        startActivityForResult(mGoogleSignInClient.getSignInIntent(),
                RC_SIGN_IN);
    }

    public void signOut() {
        mGoogleSignInClient.signOut().addOnCompleteListener(getActivity(),
                new OnCompleteListener<Void>() {
                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        if (task.isSuccessful()) {
                            onDisconnected();
                        } else {
                            Toast.makeText(getActivity(),
                                    "Error al desconectar el cliente.", Toast.LENGTH_LONG);
                        }
                    }
                });
    }

    public void onDisconnected() {
//        findViewById(R.id.sign_in_button).setVisibility(View.VISIBLE);
//        findViewById(R.id.sign_out_button).setVisibility(View.GONE);
        Log.d(getClass().getSimpleName(), "DISCONNECTED GPG");
        Log.d(TAG, "onDisconnected()");

        mTurnBasedMultiplayerClient = null;
        mInvitationsClient = null;
//        setViewVisibility();
        if (!retry) {
            retry = true;
            startSignInIntent();
        } else {
            showWarning("Google Play Games", "No ha sido posible conectar con Google Play Games", null);
        }
    }

    boolean retry = false;

    private void onConnected(GoogleSignInAccount googleSignInAccount) {
        if (mSignedInAccount != googleSignInAccount) {
            mSignedInAccount = googleSignInAccount;
            Log.d(TAG, "onConnected(): connected to Google APIs");
            retry = false;
            mTurnBasedMultiplayerClient = Games.getTurnBasedMultiplayerClient(getActivity(), googleSignInAccount);
            mInvitationsClient = Games.getInvitationsClient(getActivity(), googleSignInAccount);

            Games.getPlayersClient(getActivity(), googleSignInAccount)
                    .getCurrentPlayer()
                    .addOnSuccessListener(
                            new OnSuccessListener<Player>() {
                                @Override
                                public void onSuccess(Player player) {
                                    mImagePlayer = player.getIconImageUri();
                                    ImageManager imageManager = ImageManager.create(getActivity());
                                    imageManager.loadImage(mImgAvatar, mImagePlayer);

                                    mDisplayName = player.getDisplayName();
                                    Game.mPlayerId = player.getPlayerId();


//                                mNameAvatar.setText(Game.mPlayerId + " -  " + mDisplayName);
                                    mNameAvatar.setText(mDisplayName);
//                                setViewVisibility();
                                }
                            })
                    .addOnFailureListener(createFailureListener("There was a problem getting the player!"));

            Log.d(TAG, "onConnected(): Connection successful");

            // Retrieve the TurnBasedMatch from the connectionHint
            GamesClient gamesClient = Games.getGamesClient(getActivity(), googleSignInAccount);
            gamesClient.getActivationHint()
                    .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                        @Override
                        public void onSuccess(Bundle hint) {
                            if (hint != null) {
                                TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
                                Invitation invitationAux = hint.getParcelable(Multiplayer.EXTRA_INVITATION);
                                if (invitationAux != null)
                                    Game.pendingInvitation = invitationAux;
                                if (match != null) {
//                                updateMatch(match);
                                    askForAcceptInvitation(match, null);
                                } else if (Game.pendingTurnBasedMatch != null) {
                                    askForAcceptInvitation(Game.pendingTurnBasedMatch, null);
                                    Game.pendingTurnBasedMatch = null;
                                }
                            } else {
                                if (Game.pendingTurnBasedMatch != null) {
                                    askForAcceptInvitation(Game.pendingTurnBasedMatch, null);
                                    Game.pendingTurnBasedMatch = null;
                                }
                            }
                        }
                    })
                    .addOnFailureListener(createFailureListener(
                            "There was a problem getting the activation hint!"));

//        setViewVisibility();

            // As a demonstration, we are registering this activity as a handler for
            // invitation and match events.

            // This is *NOT* required; if you do not register a handler for
            // invitation events, you will get standard notifications instead.
            // Standard notifications may be preferable behavior in many cases.
            mInvitationsClient.registerInvitationCallback(mInvitationCallback);

            // Likewise, we are registering the optional MatchUpdateListener, which
            // will replace notifications you would get otherwise. You do *NOT* have
            // to register a MatchUpdateListener.
            mTurnBasedMultiplayerClient.registerTurnBasedMatchUpdateCallback(mMatchUpdateCallback);

            ((CategorySelectionActivity) getActivity()).animateToolbarNavigateCategories(false);
        }
    }


    public void updateMatch(TurnBasedMatch match) {
        Game.mMatch = match;

        int status = match.getStatus();
        int turnStatus = match.getTurnStatus();
        Game.mTurnData = Turn.unpersist(match.getData());

        switch (status) {
            case TurnBasedMatch.MATCH_STATUS_CANCELED:
                showWarning("Canceled!", "This game was canceled!", null);
                return;
            case TurnBasedMatch.MATCH_STATUS_EXPIRED:
                showWarning("Expired!", "This game is expired.  So sad!", null);
                return;
            case TurnBasedMatch.MATCH_STATUS_AUTO_MATCHING:
                showWarning("Waiting for auto-match...",
                        "We're still waiting for an automatch partner.", actionButtonDone);
                return;
            case TurnBasedMatch.MATCH_STATUS_COMPLETE:
                if (turnStatus == TurnBasedMatch.MATCH_TURN_STATUS_COMPLETE) {
                    showWarning("Complete!",
                            "This game is over; someone finished it, and so did you!  " +
                                    "There is nothing to be done.", actionButtonDone);
                    break;
                }

                // Note that in this state, you must still call "Finish" yourself,
                // so we allow this to continue.
//                showWarning("Complete!",
//                        "This game is over; someone finished it!  You can only finish it now.", actionButtonDone);

                finishFinishedMatch();
                return;
        }

        // OK, it's active. Check on turn status.
        switch (turnStatus) {
            case TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN:
                if (match.getData() != null) {
                    // This is a game that has already started, so I'll just roulette_rotate
                    setGameplayUI();
                } else {
                    startMatch(match);
                }
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_THEIR_TURN:
                // Should return results.
                showWarning("Alas...", "It's not your turn.", actionButtonDone);
                break;
            case TurnBasedMatch.MATCH_TURN_STATUS_INVITED:
                showWarning("Good inititative!",
                        "Still waiting for invitations.\n\nBe patient!", actionButtonDone);

        }

//        mTurnData = null;
        // Switch to gameplay view.
//        setViewVisibility();

    }


    private ActionOnClickButton actionButtonDone = new ActionOnClickButton() {
        @Override
        public void onClick() {
            onPlayButton(null, false);
        }
    };


    public interface ActionOnClickButton {
        void onClick();
    }

    ;

    // Switch to gameplay view.
    public void setGameplayUI() {
        isDoingTurn = true;
//        setViewVisibility();
//        mDataView.setText("" + mTurnData.getNumQuizz());
//        mTurnTextView.setText(getString(R.string.turn_label, mTurnData.getNumQuizz() + 1));
        onPlayButton(null, true);
    }


    private InvitationCallback mInvitationCallback = new InvitationCallback() {
        // Handle notification events.
        @Override
        public void onInvitationReceived(@NonNull Invitation invitation) {
//            Toast.makeText(
//                    getActivity(),
//                    "An invitation has arrived from "
//                            + invitation.getInviter().getDisplayName(), Toast.LENGTH_SHORT)
//                    .show();

            treatInvitation(invitation);
        }

        @Override
        public void onInvitationRemoved(@NonNull String invitationId) {
            Toast.makeText(getActivity(), "An invitation was removed.", Toast.LENGTH_SHORT)
                    .show();
        }
    };

    public void treatInvitation(@NonNull Invitation invitation) {
        final String fromInvitation = invitation.getInviter().getDisplayName();

        if (invitation.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED) {
            Games.getTurnBasedMultiplayerClient(getActivity(),
                    GoogleSignIn.getLastSignedInAccount(getActivity())).acceptInvitation(invitation.getInvitationId()).addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                @Override
                public void onSuccess(TurnBasedMatch match) {
                    if (match != null) {
                        askForAcceptInvitation(match, fromInvitation);
//                            updateMatch(match);
                    }
                }
            }).addOnFailureListener(createFailureListener("Error getting de invitation."));
        }
    }

    // This is a helper functio that will do all the setup to create a simple failure message.
// Add it to any task and in the case of an failure, it will report the string in an alert
// dialog.
    private OnFailureListener createFailureListener(final String string) {
        return new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                handleException(e, string);
            }
        };
    }

    private void handleException(Exception exception, String details) {
        int status = 0;
        dismissSpinner();

        if (exception instanceof TurnBasedMultiplayerClient.MatchOutOfDateApiException) {
            TurnBasedMultiplayerClient.MatchOutOfDateApiException matchOutOfDateApiException =
                    (TurnBasedMultiplayerClient.MatchOutOfDateApiException) exception;

            new android.app.AlertDialog.Builder(getActivity())
                    .setMessage("Match was out of date, updating with latest match data...")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();

            TurnBasedMatch match = matchOutOfDateApiException.getMatch();
            updateMatch(match);

            return;
        }

        if (exception instanceof ApiException) {
            ApiException apiException = (ApiException) exception;
            status = apiException.getStatusCode();
        }

        if (!checkStatusCode(status)) {
            return;
        }

        String message = getString(R.string.status_exception_error, details, status, exception);

        new android.app.AlertDialog.Builder(getActivity())
                .setMessage(message)
                .setNeutralButton(android.R.string.ok, null)
                .show();
    }

    // Returns false if something went wrong, probably. This should handle
// more cases, and probably report more accurate results.
    private boolean checkStatusCode(int statusCode) {
        switch (statusCode) {
            case GamesCallbackStatusCodes.OK:
                return true;

            case GamesClientStatusCodes.MULTIPLAYER_ERROR_NOT_TRUSTED_TESTER:
                showErrorMessage(R.string.status_multiplayer_error_not_trusted_tester);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_ALREADY_REMATCHED:
                showErrorMessage(R.string.match_error_already_rematched);
                break;
            case GamesClientStatusCodes.NETWORK_ERROR_OPERATION_FAILED:
                showErrorMessage(R.string.network_error_operation_failed);
                break;
            case GamesClientStatusCodes.INTERNAL_ERROR:
                showErrorMessage(R.string.internal_error);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_INACTIVE_MATCH:
                showErrorMessage(R.string.match_error_inactive_match);
                break;
            case GamesClientStatusCodes.MATCH_ERROR_LOCALLY_MODIFIED:
                showErrorMessage(R.string.match_error_locally_modified);
                break;
            default:
                showErrorMessage(R.string.unexpected_status);
                Log.d(TAG, "Did not have warning or string to deal with: "
                        + statusCode);
        }

        return false;
    }


    private android.app.AlertDialog mAlertDialog;

    // Generic warning/info dialog
    public void showWarning(String title, String message, final ActionOnClickButton actionButton) {

        dismissSpinner();
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());

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

    public void showErrorMessage(int stringId) {
        showWarning("Warning", getResources().getString(stringId), null);
    }


    private TurnBasedMatchUpdateCallback mMatchUpdateCallback = new TurnBasedMatchUpdateCallback() {
        @Override
        public void onTurnBasedMatchReceived(@NonNull TurnBasedMatch turnBasedMatch) {
            Toast.makeText(getActivity(), "A match was updated.", Toast.LENGTH_LONG).show();
        }

        @Override
        public void onTurnBasedMatchRemoved(@NonNull String matchId) {
            Toast.makeText(getActivity(), "A match was removed.", Toast.LENGTH_SHORT).show();
        }
    };

    private static final int RC_SIGN_IN = 9001;

    // Displays your inbox. You will get back onActivityResult where
// you will need to figure out what you clicked on.
    public void onCheckGamesClicked(View view) {
        mTurnBasedMultiplayerClient.getInboxIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_LOOK_AT_MATCHES);
                    }
                })
                .addOnFailureListener(createFailureListener(getString(R.string.error_get_inbox_intent)));
    }


    private void addCategories() {
//        for (CategoryJSON category : categoriesJSON) {
//            navegador.loadUrl("javascript:addSegment(\"" + category.getCategory().substring(0,Math.min(category.getCategory().length(),20)) + "\",\"" + category.getTheme() + "\",\"" + category.getImg() + "\")");
//        }

    }
//
//    private static SoundPool soundPool;
//    int idSonido = 0;

    private boolean comprobarConectividad() {
        ConnectivityManager connectivityManager =
                (ConnectivityManager) getActivity().getSystemService(
                        Context.CONNECTIVITY_SERVICE);
        NetworkInfo info = connectivityManager.getActiveNetworkInfo();
        if ((info == null || !info.isConnected() || !info.isAvailable())) {
            Toast.makeText(getActivity(),
                    "Oops! No tienes conexión a internet",
                    Toast.LENGTH_LONG).show();
            return false;
        }
        return true;
    }


    final static int RC_SELECT_PLAYERS = 10000;
    private Button btnSelectMatch;
    final static String TAG = "PLAYTURNBASED";


    @Override
    public void onActivityResult(int requestCode, int resultCode, Intent intent) {
        switch (requestCode) {
            case RC_SIGN_IN:
                Task<GoogleSignInAccount> task =
                        GoogleSignIn.getSignedInAccountFromIntent(intent);
                try {
                    GoogleSignInAccount account =
                            task.getResult(ApiException.class);
                    onConnected(account);
                } catch (ApiException apiException) {
                    String message = apiException.getMessage();
                    if (message == null || message.isEmpty()) {
                        message = "Error al conectar el cliente.";
                    }
                    onDisconnected();
//                    new AlertDialog.Builder(getActivity())
//                            .setMessage(message)
//                            .setNeutralButton(android.R.string.ok, null)
//                            .show();
                }
                break;


            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(resultCode, intent);
                break;

            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the roulette_selection invitation:
                handleInvitationInboxResult(resultCode, intent);
                break;
            case RC_LOOK_AT_MATCHES:
                if (resultCode != Activity.RESULT_OK) {
                    logBadActivityResult(requestCode, resultCode,
                            "User cancelled returning from the 'Select Match' dialog.");
                    return;
                }

                TurnBasedMatch match = intent.getParcelableExtra(Multiplayer.EXTRA_TURN_BASED_MATCH);
                if (match != null) {
                    showSpinner();
                    updateMatch(match);
                }

                Log.d(TAG, "Match = " + match);
                break;
            case RC_CHOOOSE_CATEGORY:

                if (resultCode != RESULT_OK)
                    onLeaveClicked(null);
                else {
                    int categoryAux = intent.getIntExtra("category", 0);
                    // If no play do nathing
                    if (categoryAux != -1)
                        startQuizzes(categoryAux);
                }
                break;

            case RC_PLAY_TURN:

//                if (resultCode != RESULT_OK)
//                    onLeaveClicked(null);
//                else {
                // Actualiza puntuación
                onDoneClicked(null);
                    /*if (Game.category.getScore()>0) {
                        // Continua Jugando

                    }else {
                        // Pasa Turno
                    }*/

//                }
                break;
        }


        super.onActivityResult(requestCode, resultCode, intent);
    }

    private void logBadActivityResult(int requestCode, int resultCode, String message) {
        Log.i(TAG, "Bad activity result(" + resultCode + ") for request (" + requestCode + "): "
                + message);
    }

//    public TurnBasedMatch Game.mMatch;
//    private Turn mTurnData;

    private AlertDialog mDialogoAlerta;


    public void showAlertMessage(String title, String message, final ActionOnClickButton actionOnClickButton) {
        AlertDialog.Builder alertDialogBuilder =
                new AlertDialog.Builder(getActivity());
        alertDialogBuilder.setTitle(title).setMessage(message);
        alertDialogBuilder.setCancelable(false)
                .setPositiveButton("OK",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(
                                    DialogInterface dialog, int id) {
                                mDialogoAlerta.dismiss();
                                if (actionOnClickButton != null)
                                    actionOnClickButton.onClick();
                            }
                        });
        mDialogoAlerta = alertDialogBuilder.create();
        mDialogoAlerta.show();
    }


    // Handle the result of the invitation inbox UI, where the player can pick an invitation
// to accept. We react by accepting the roulette_selection invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
//            switchToMainScreen();
            return;
        }
        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);

        // accept invitation
        if (inv != null)
            acceptInviteToRoom(inv.getInvitationId());
        else {
            new android.app.AlertDialog.Builder(getActivity())
                    .setMessage("La invitación corresponde a una PARTIDA EN TIEMPO REAL")
                    .setNeutralButton(android.R.string.ok, null)
                    .show();
            Game.pendingInvitation = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);
            ((CategorySelectionActivity) getActivity()).attachPlayOnlineFragment(QuizActivity.ARG_REAL_TIME_ONLINE);
            ;
        }
    }


    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);

        onPlayButton(null, true);
//        switchToScreen(R.id.screen_wait);
    }


    private void startQuizzes(int categorySelected) {
        Intent startIntent;
//        Toast.makeText(getActivity(), "CREANDO PARTIDA", Toast.LENGTH_SHORT).show();

        Game.numQuizzes = 1;
        Game.totalTime = 60;

        Game.categorySelected = categorySelected;
        Game.category = TrivialJSonHelper.getInstance(getContext(), false).createCategoryTurnBased(categorySelected);
        startIntent = QuizActivity.getStartIntent(getActivity(), Game.category);

        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            startActivityForResult(startIntent, RC_PLAY_TURN, null);
        else {
            startActivityForResult(startIntent, RC_PLAY_TURN, null);
        }
        getActivity().overridePendingTransition(android.R.anim.fade_in, android.R.anim.fade_out);
    }


    final static int REQUEST_LEADERBOARD = 100;
    final static int REQUEST_ACHIEVEMENTS = 101;
    final static int REQUEST_QUESTS = 102;
    private Button btnMarcadores;
    private Button btnLogros;
    private Button btnMisiones;


    @Override
    public void onDestroy() {
        super.onDestroy();
//        if (soundPool != null) {
//            soundPool.stop(idSonido);
//            soundPool.release();
//        }
    }

    @Override
    public void onPause() {
        super.onPause();
//        if (soundPool != null) {
//            soundPool.pause(idSonido);
//        }

        // Unregister the invitation callbacks; they will be re-registered via
        // onResume->signInSilently->onConnected.
        if (mInvitationsClient != null) {
            mInvitationsClient.unregisterInvitationCallback(mInvitationCallback);
        }

        if (mTurnBasedMultiplayerClient != null) {
            mTurnBasedMultiplayerClient.unregisterTurnBasedMatchUpdateCallback(mMatchUpdateCallback);
        }
    }


    /**
     * Get the next participant. In this function, we assume that we are
     * round-robin, with all known players going before all automatch players.
     * This is not a requirement; players can go in any order. However, you can
     * take turns in any order.
     *
     * @return participantId of next player, or null if automatching
     */
    public String getNextParticipantId() {

        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);

        ArrayList<String> participantIds = Game.mMatch.getParticipantIds();

        int desiredIndex = -1;

        for (int i = 0; i < participantIds.size(); i++) {
            if (participantIds.get(i).equals(myParticipantId)) {
                desiredIndex = i + 1;
            }
        }

        if (desiredIndex < participantIds.size()) {
            return participantIds.get(desiredIndex);
        }

        if (Game.mMatch.getAvailableAutoMatchSlots() <= 0) {
            // You've run out of automatch slots, so we roulette_rotate over.
            return participantIds.get(0);
        } else {
            // You have not yet fully automatched, so null will find a new
            // person to play against.
            return null;
        }
    }

    // Upload your new gamestate, then take a turn, and pass it on to the next
// player.
    public void onDoneClicked(View view) {

        Game.mTurnData = Turn.unpersist(Game.mMatch.getData());
        if (Game.mTurnData != null) {

            String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);
            String nextParticipantId = getNextParticipantId();
            // Create the next turn
            if (Game.mTurnData.participantsTurnBased.indexOf(myParticipantId) == -1)
                Game.mTurnData.participantsTurnBased.add(myParticipantId);

            Game.mTurnData.puntuacion[Game.mTurnData.numPreguntasContestadas][0] = (short) Game.mTurnData.participantsTurnBased.indexOf(myParticipantId);
            Game.mTurnData.puntuacion[Game.mTurnData.numPreguntasContestadas][1] = (Game.category.getScore() > 0) ? (short) (Game.category.getScore() * K_PUNTOS_POR_PREGUNTA) : 0;
            Game.mTurnData.puntuacion[Game.mTurnData.numPreguntasContestadas][2] = (short) Game.categorySelected;

            Game.mTurnData.numPreguntasContestadas++;

            final boolean finalPartida = Game.mTurnData.numPreguntasContestadas >= Game.mTurnData.numPreguntas;
            Log.d(getClass().getSimpleName(), "finalPartida=" + finalPartida + " currentQuizz=" + Game.mTurnData.numPreguntasContestadas);

            if (finalPartida) {
                if (Game.mTurnData.numTurnos >= 1) {
                    Game.mTurnData.numTurnos++;
                    showSpinner();
                    finishMatch();
                } else {
                    Game.mTurnData.numTurnos++;
                    nextTurn(nextParticipantId, nextParticipantId);
                }
            } else {
                if (Game.category.getScore() == 0)
                    Game.mTurnData.numTurnos++;
                nextTurn(myParticipantId, nextParticipantId);
            }
        }

    }

    public void nextTurn(String myParticipantId, String nextParticipantId) {
        showSpinner();
        mTurnBasedMultiplayerClient.takeTurn(Game.mMatch.getMatchId(),
                Game.mTurnData.persist(), Game.category == null || Game.category.getScore() == 0 ? nextParticipantId : myParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        Game.mMatch = turnBasedMatch;
                        onUpdateMatch(turnBasedMatch);
                        Game.mTurnData = Turn.unpersist(Game.mMatch.getData());
                        if (Game.mTurnData.numPreguntasContestadas == Game.mTurnData.numPreguntas) {
                            showMessageFinishMatch(turnBasedMatch, false);
                        }
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
        Game.mTurnData = null;
    }

    public void finishFinishedMatch() {
        mTurnBasedMultiplayerClient.finishMatch(Game.mMatch.getMatchId())
                .addOnSuccessListener(
                        new OnSuccessListener<TurnBasedMatch>() {
                            @Override
                            public void onSuccess(TurnBasedMatch turnBasedMatch) {

                                showWarning("Complete!",
                                        "This game is over; someone finished it, and so did you!  " +
                                                "There is nothing to be done.", actionButtonDone);
//                                Toast.makeText(getActivity(),
//                                        "Fin de la partida.",
//                                        Toast.LENGTH_LONG).show();
                                Game.mTurnData = null;
//                                onUpdateMatch(turnBasedMatch);
                            }
                        })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(),
                                "Hay un problema finalizando la partida", Toast.LENGTH_SHORT).show();
                    }
                });
    }

    public void finishMatch() {
//        Game.mTurnData = Turn.unpersist(Game.mMatch.getData());
        List<String> winners = getWinners();
        List<ParticipantResult> list = new ArrayList<ParticipantResult>();
        for (Participant p : Game.mMatch.getParticipants()) {
            ParticipantResult participantResult = null;
            if (winners.indexOf(p.getParticipantId()) >= 0) {
                if (winners.size() > 1)
                    participantResult = new ParticipantResult(p.getParticipantId(), MATCH_RESULT_TIE, PLACING_UNINITIALIZED);
                else
                    participantResult = new ParticipantResult(p.getParticipantId(), MATCH_RESULT_WIN, PLACING_UNINITIALIZED);
            } else {
                participantResult = new ParticipantResult(p.getParticipantId(), MATCH_RESULT_LOSS, PLACING_UNINITIALIZED);
            }
            list.add(participantResult);
        }
        mTurnBasedMultiplayerClient.finishMatch(Game.mMatch.getMatchId(), Game.mTurnData.persist(), list)
                .addOnSuccessListener(
                        new OnSuccessListener<TurnBasedMatch>() {
                            @Override
                            public void onSuccess(final TurnBasedMatch turnBasedMatch) {
                                dismissSpinner();

                                showMessageFinishMatch(turnBasedMatch, true);
                            }
                        })
                .addOnFailureListener(createFailureListener("Hay un problema finalizando la partida"));
    }

    public void showMessageFinishMatch(final TurnBasedMatch turnBasedMatch, boolean finishMatch) {
        Game.mTurnData = Turn.unpersist(turnBasedMatch.getData());

        String txtMatchResultPlayer = "";
        Participant participant = null;
        try {
            String participantId = null;
            participantId = turnBasedMatch.getParticipantId(Game.mPlayerId);
            participant = turnBasedMatch.getParticipant(participantId);

        } catch (IllegalStateException ex) {
        }
        txtMatchResultPlayer = checkPlayerMatchResult(participant);

        // The new player doesn't play any turn.
        if (Game.mTurnData.numPreguntasContestadas == Game.mTurnData.numPreguntas && finishMatch && Game.mTurnData.numTurnos == 1) {
            txtMatchResultPlayer = "You've joind at the end of the match. Indeed, " + txtMatchResultPlayer;
        }

        showAlertMessage("Partida Finalizada", txtMatchResultPlayer, new ActionOnClickButton() {
            @Override
            public void onClick() {
                Game.mTurnData = null;
                onUpdateMatch(turnBasedMatch);
            }
        });
    }

    private String checkPlayerMatchResult(Participant participant) {
        String auxText = "";

        if (participant == null) {
            auxText = " NOT PLAYED ";
        } else {
            if (participant.getResult() == null) {
                // First Turn
                auxText = " YOU WIN!";
            } else
                switch (participant.getResult().getResult()) {
                    case MATCH_RESULT_TIE:
                        auxText = " YOU TIED!";
                        break;
                    case MATCH_RESULT_WIN:
                        auxText = " YOU WIN!";
                        break;
                    case MATCH_RESULT_LOSS:
                        auxText = " YOU LOST!";
                        break;
                }
        }
        return auxText;
    }

    private List<String> getWinners() {
        int maxAnswers = 0;

        List<String> players = new ArrayList<>();

        for (int i = 0; i < Game.mTurnData.participantsTurnBased.size(); i++) {
            int auxAnswers = 0;
            for (int cont = 0; cont < Game.mTurnData.numPreguntas; cont++) {
                if ((Game.mTurnData.puntuacion[cont][0] == i) && Game.mTurnData.puntuacion[cont][1] > 0) {
                    auxAnswers++;
                }
            }
            if (maxAnswers < auxAnswers) {
                maxAnswers = auxAnswers;
                players.clear();
                players.add(Game.mTurnData.participantsTurnBased.get(i));
            } else if (maxAnswers == auxAnswers) {
                players.add(Game.mTurnData.participantsTurnBased.get(i));
            } else {
                // Less score
            }
        }

        return players;
    }


    // Rematch dialog
    public void askForRematch(final TurnBasedMatch match) {
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage("Do you want a rematch?");
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Sure, rematch!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                rematch();
                            }
                        })
                .setNegativeButton("No.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                continueOnUpdateMatch(match);
                            }
                        });

        alertDialogBuilder.show();
    }


    // Rematch dialog
    public void askForAcceptInvitation(final TurnBasedMatch match, String userInvitation) {
        String invitationFrom = userInvitation;
        if (invitationFrom == null)
            invitationFrom = getFirstPlayerGame(match);
        android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());
        alertDialogBuilder.setMessage("Do you want tu accept the invitation from " + invitationFrom + "?");
        alertDialogBuilder
                .setCancelable(false)
                .setPositiveButton("Sure, accept!",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
                                updateMatch(match);
                            }
                        })
                .setNegativeButton("No.",
                        new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialog, int id) {
//                                continueOnUpdateMatch(match);
                            }
                        });

        alertDialogBuilder.show();
    }

    private String getFirstPlayerGame(TurnBasedMatch match) {
        if (match.getParticipants() != null)
            return match.getParticipants().get(0).getDisplayName();
        return "";
    }


    // If you choose to rematch, then call it and wait for a response.
    public void rematch() {
        showSpinner();
        mTurnBasedMultiplayerClient.rematch(Game.mMatch.getMatchId())
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        onInitiateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem starting a rematch!"));
        Game.mMatch = null;
        isDoingTurn = false;
    }


    public void onUpdateMatch(TurnBasedMatch match) {
//        dismissSpinner();

        if (match.canRematch()) {
            askForRematch(match);
        } else {
            continueOnUpdateMatch(match);
        }
    }

    public void continueOnUpdateMatch(TurnBasedMatch match) {
        isDoingTurn = (match.getTurnStatus() == TurnBasedMatch.MATCH_TURN_STATUS_MY_TURN);
        if (isDoingTurn) {
            updateMatch(match);
            return;
        } else {
            dismissSpinner();
        }
//        setViewVisibility();
    }

    public void onFinishClicked(View view) {
        showSpinner();
        finishMatch();
        isDoingTurn = false;
//        setViewVisibility();
    }

    private int normalizeProgres2_4_8(int progresValue) {
//        int diff_2 = Math.abs(progresValue - 2);
//        int diff_4 = Math.abs(progresValue - 4);
//        int diff_8 = Math.abs(progresValue - 8);
//        int minDiff = Math.min(diff_2, diff_4);
//        minDiff = Math.min(minDiff, diff_8);
//
//        int tmpValue = 0;
//        if (minDiff == diff_8) {
//            tmpValue = 8;
//        } else if (minDiff == diff_4) {
//            tmpValue = 4;
//        } else if (minDiff == diff_2) {
//            tmpValue = 2;
//        }

//        return tmpValue;
        if (progresValue > 8)
            progresValue = 8;

        return progresValue;
    }


    private void showSeekbarsProgress() {
        txtPlayers.setText(getString(R.string.num_players) + sbPlayers.getProgress() + "/" + (sbPlayers.getMax() - 1));
//        txtTotalTime.setText("Total Time: " + sbTotalTime.getProgress() + "/" + sbTotalTime.getMax());
        txtNumQuizzes.setText("Num. Quizzes: " + sbNumQuizzes.getProgress() + "/" + K_MAX_PREGUNTAS);
    }

    //
// Handle the result of the "Select players UI" we launched when the user clicked the
// "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
//            switchToMainScreen();
            return;
        }

        showSpinner();

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
//        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Game.resetGameVars();
        Game.invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Game.minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        Game.maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

        Bundle autoMatchCriteria = null;

        if (Game.minAutoMatchPlayers > 0) {
            autoMatchCriteria = RoomConfig.createAutoMatchCriteria(
                    Game.minAutoMatchPlayers, Game.maxAutoMatchPlayers, 0);
        } else {
            autoMatchCriteria = null;
        }


        TurnBasedMatchConfig.Builder builder = TurnBasedMatchConfig.builder()
                .addInvitedPlayers(Game.invitees)
                .setAutoMatchCriteria(autoMatchCriteria);

        Games.getTurnBasedMultiplayerClient(getActivity(),
                GoogleSignIn.getLastSignedInAccount(getActivity())).createMatch(builder.build())
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
//                Log.d(TAG, "Invitee count: " + Game.invitees.size());

//                        Game.jugadorLocal = 0;
//        Game.tmpNumQuizzes = sbQuizzes.getProgress();
//        Game.tmpTotalTime = sbTotalTime.getProgress();

//                Game.category = TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(SharedPreferencesStorage.getInstance(getContext()).readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10), Game.listCategories);
//                startQuizzes();
                        onInitiateMatch(turnBasedMatch);
                        Log.d(TAG, "Juego por Turnos Creado");
                    }
                }).addOnFailureListener(createFailureListener("There was a problem creating a match!"));


    }

    private void onInitiateMatch(TurnBasedMatch match) {
//        dismissSpinner();

        if (match.getData() != null) {
            // This is a game that has already started, so I'll just roulette_rotate
            updateMatch(match);
            return;
        }

        startMatch(match);
    }

    // startMatch() happens in response to the createTurnBasedMatch()
// above. This is only called on success, so we should have a
// valid match object. We're taking this opportunity to setup the
// game, saving our initial state. Calling takeTurn() will
// callback to OnTurnBasedMatchUpdated(), which will show the game
// UI.
    public void startMatch(TurnBasedMatch match) {
//        showSpinner();
        Game.mMatch = match;
        String myParticipantId = Game.mMatch.getParticipantId(Game.mPlayerId);

        Game.mTurnData = new Turn();
        Game.mTurnData.numPreguntas = sbNumQuizzes.getProgress();

        Game.mTurnData.numPreguntasContestadas = 0;
        Game.mTurnData.numTurnos = 0;
        Game.mTurnData.numJugadores = sbPlayers.getProgress();
        if (Game.mTurnData.participantsTurnBased != null)
            Game.mTurnData.participantsTurnBased.clear();
        else
            Game.mTurnData.participantsTurnBased = new ArrayList<>();

        if (Game.mTurnData.participantsTurnBased.indexOf(myParticipantId) == -1)
            Game.mTurnData.participantsTurnBased.add(myParticipantId);

        Game.mTurnData.puntuacion = new short[Game.mTurnData.numPreguntas][3];
        for (int pos = 0; pos < Game.mTurnData.numPreguntas; pos++) {
            Game.mTurnData.puntuacion[pos][0] = 0;
            Game.mTurnData.puntuacion[pos][1] = 0;
            Game.mTurnData.puntuacion[pos][2] = 0;
        }


        mTurnBasedMultiplayerClient.takeTurn(match.getMatchId(),
                Game.mTurnData.persist(), myParticipantId)
                .addOnSuccessListener(new OnSuccessListener<TurnBasedMatch>() {
                    @Override
                    public void onSuccess(TurnBasedMatch turnBasedMatch) {
                        updateMatch(turnBasedMatch);
                    }
                })
                .addOnFailureListener(createFailureListener("There was a problem taking a turn!"));
    }

    // Helpful dialogs
    public void showSpinner() {
        CustomDialogFragment.showDialog(getFragmentManager(), null);
        Log.d("CONTROL", "showSpinner");
//        progressLayout.setVisibility(View.VISIBLE);
    }

    public void dismissSpinner() {
//        progressLayout.setVisibility(View.GONE);
        Log.d("CONTROL", "DISMISSPINNER");
        CustomDialogFragment.dismissDialog();

    }

    public void btnVer_Invitaciones_Click(View view) {
        Games.getInvitationsClient(getActivity(), GoogleSignIn.getLastSignedInAccount(getActivity()))
                .getInvitationInboxIntent()
                .addOnSuccessListener(new OnSuccessListener<Intent>() {
                    @Override
                    public void onSuccess(Intent intent) {
                        startActivityForResult(intent, RC_INVITATION_INBOX);
                    }
                });
    }
}