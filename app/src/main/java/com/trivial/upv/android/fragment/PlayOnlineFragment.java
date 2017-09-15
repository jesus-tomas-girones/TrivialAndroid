package com.trivial.upv.android.fragment;

import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;

import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.api.GoogleApiClient;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.CategorySelectionActivity;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.helper.gpg.BaseGameUtils;
import com.trivial.upv.android.helper.singleton.SharedPreferencesStorage;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TopekaJSonHelper;

import java.util.List;

import static android.app.Activity.RESULT_OK;

/**
 * Created by jvg63 on 22/06/2017.
 */


public class PlayOnlineFragment extends Fragment
        implements
        GoogleApiClient.ConnectionCallbacks,
        GoogleApiClient.OnConnectionFailedListener,
        View.OnClickListener {

    private static final String QUICK_GAME = "QUICK_GAME";
    private static final String CUSTOM_GAME = "CUSTOM_GAME";
    private Button btnPartidasGuardadas;
    private Button btnJugar;
    private Button btnNewGame;

    private static final int RC_SIGN_IN = 9001;

    final static int RC_INVITATION_INBOX = 10001;

    private boolean mResolvingConnectionFailure = false;
    private boolean mAutoStartSignInFlow = true;
    private boolean mSignInClicked = false;

    private com.google.android.gms.common.SignInButton btnConectar;
    //    private Button btnDesconectar;
    private Button btnShowIntitations;
    private Button btnQuickGame;

    private LinearLayout globalActions;
    private LinearLayout newGameActions;
    private LinearLayout loginActions;
    private static String NAME_PREFERENCES = "TrivialAndroid";
    private static String NAME_PREFERENCES_CONNECTED = "conectado";
    private TextView txtListCategories;

    public PlayOnlineFragment() {
        Game.resetGameVars();
        Game.listCategories.clear();
        List<CategoryJSON> categoriesJSON = TopekaJSonHelper.getInstance(getContext(), false).getCategoriesJSON();
        for (CategoryJSON category : categoriesJSON) {
            Game.listCategories.add(new String(category.getCategory()));
        }

        Game.level = (long) (Math.pow(2, categoriesJSON.size()) - 1);
    }


    public static PlayOnlineFragment newInstance() {
        return new PlayOnlineFragment();
    }

    @Override
    public View onCreateView(LayoutInflater inflater, ViewGroup container,
                             Bundle savedInstanceState) {

        return inflater.inflate(R.layout.fragment_play_online, container, false);
    }

    @Override
    public void onViewCreated(View view, Bundle savedInstanceState) {
        changeTitleActionBar();

        super.onViewCreated(view, savedInstanceState);
        setUpView(view);
    }

    private void changeTitleActionBar() {
        ((CategorySelectionActivity) getActivity()).setToolbarTitle("Play Online");
    }

    private SeekBar sbPlayers;
    private SeekBar sbQuizzes;
    private SeekBar sbTotalTime;
    private TextView txtPlayers;
    private TextView txtTotalTime;
    private TextView txtQuizzes;

    @Override
    public void onResume() {
        super.onResume();

        showSeekbarsProgress();
        ((CategorySelectionActivity) getActivity()).animateToolbarNavigateCategories(false);

        if (Game.listCategories.size() == TopekaJSonHelper.getInstance(getContext(), false).getCategoriesJSON().size()) {
            txtListCategories.setText("(All Categories)");
        } else {
//            txtListCategories.setText("(Otras)");

            StringBuilder sb = new StringBuilder();

            for (int pos = 0; pos < Game.listCategories.size(); pos++) {
                sb.append(Game.listCategories.get(pos));
                if (pos != Game.listCategories.size() - 1) {
                    sb.append(",\n");
                }
            }
            txtListCategories.setText(sb.toString());
        }
    }

    private void setUpView(final View view) {

        CategorySelectionActivity.animateViewFullScaleXY(((CategorySelectionActivity) getActivity()).getSubcategory_title(), 100, 300);

        globalActions = (LinearLayout) view.findViewById(R.id.global_actions);
        newGameActions = (LinearLayout) view.findViewById(R.id.new_game_actions);
        loginActions = (LinearLayout) view.findViewById(R.id.login_actions);
        txtListCategories = (TextView) view.findViewById(R.id.txtListCategories);
        btnInvitar = (Button) view.findViewById(R.id.btnInvite);
        btnInvitar.setOnClickListener(this);

        btnConectar = (com.google.android.gms.common.SignInButton) view.findViewById(R.id.sign_in_button);
        btnConectar.setOnClickListener(this);

//        btnDesconectar = (Button) view.findViewById(R.id.sign_out_button);
//        btnDesconectar.setOnClickListener(this);

        // Reset Data Game
        Game.resetGameVars();

//        btnConectar.setVisibility(View.VISIBLE);
        loginActions.setVisibility(View.VISIBLE);
//        btnDesconectar.setVisibility(View.GONE);

        newGameActions.setVisibility(View.GONE);
        globalActions.setVisibility(View.GONE);

        Game.mGoogleApiClient = new GoogleApiClient.Builder(getContext())
                .addConnectionCallbacks(this)
                .addOnConnectionFailedListener(this)
                .addApi(Games.API).addScope(Games.SCOPE_GAMES)
                .build();


        btnNewGame = (Button) view.findViewById(R.id.btnSelectCategories);
        btnNewGame.setOnClickListener(this);
//        btnInvitar = (Button) view.findViewById(R.id.btnInvitar);
//        btnInvitar.setOnClickListener(this);
        btnShowIntitations = (Button) view.findViewById(R.id.btnShowInvitations);
        btnShowIntitations.setOnClickListener(this);
        btnQuickGame = (Button) view.findViewById(R.id.btnPlayAnyone);
        btnQuickGame.setOnClickListener(this);

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
                    txtPlayers.setText("Num. Players: " + progress + "/" + (seekBar.getMax() - 1));
                }
            }

            @Override
            public void onStartTrackingTouch(SeekBar seekBar) {
            }

            @Override
            public void onStopTrackingTouch(SeekBar seekBar) { /* Display the value in textview*/
                txtPlayers.setText("Num. Players: " + progress + "/" + (seekBar.getMax() - 1));
            }
        });

//        sbTotalTime = (SeekBar) view.findViewById(R.id.sbTotalTime);
//        txtTotalTime = (TextView) view.findViewById(R.id.txtTotalTime);
//
//        sbTotalTime.setOnSeekBarChangeListener(
//                new SeekBar.OnSeekBarChangeListener() {
//                    TextView txtDisplay = (TextView) view.findViewById(R.id.txtTotalTime);
//                    int progress = sbTotalTime.getProgress();
//
//                    @Override
//                    public void onProgressChanged(SeekBar seekBar,
//                                                  int progresValue, boolean fromUser) {
//                        if (progresValue < 5) {
//                            seekBar.setProgress(5);
//                            progress = 5;
//                        } else
//                            progress = progresValue;
//                    }
//
//                    @Override
//                    public void onStartTrackingTouch(SeekBar seekBar) {
//                    }
//
//                    @Override
//                    public void onStopTrackingTouch(SeekBar seekBar) {
//                        // Display the value in textview
//                        txtDisplay.setText("Total Time: " + progress + "/" + seekBar.getMax());
//                    }
//                });
//
//        sbQuizzes = (SeekBar) view.findViewById(R.id.sbQuizzes);
//        txtQuizzes = (TextView) view.findViewById(R.id.txtQuizzes);
//
//        sbQuizzes.setOnSeekBarChangeListener(
//                new SeekBar.OnSeekBarChangeListener() {
//
//                    int progress = sbQuizzes.getProgress();
//
//                    @Override
//                    public void onProgressChanged(SeekBar seekBar,
//                                                  int progresValue, boolean fromUser) {
//                        if (progresValue < 1) {
//                            seekBar.setProgress(1);
//                            progress = 1;
//                        } else
//                            progress = progresValue;
//                    }
//
//                    @Override
//                    public void onStartTrackingTouch(SeekBar seekBar) {
//                    }
//
//                    @Override
//                    public void onStopTrackingTouch(SeekBar seekBar) {
//                        // Display the value in textview
//                        txtQuizzes.setText("Num. Quizzes: " + progress + "/" + seekBar.getMax());
//                    }
//                });
//

    }

    private int normalizeProgres2_4_8(int progresValue) {
        int diff_2 = Math.abs(progresValue - 2);
        int diff_4 = Math.abs(progresValue - 4);
        int diff_8 = Math.abs(progresValue - 8);
        int minDiff = Math.min(diff_2, diff_4);
        minDiff = Math.min(minDiff, diff_8);

        int tmpValue = 0;
        if (minDiff == diff_8) {
            tmpValue = 8;
        } else if (minDiff == diff_4) {
            tmpValue = 4;
        } else if (minDiff == diff_2) {
            tmpValue = 2;
        }

        return tmpValue;
    }

    final static int RC_SELECT_PLAYERS = 10000;
    private Button btnInvitar;
    private Button btnPartidaPorTurnos;
    final static String TAG = "PLAYONLINEFRAGMENT";


    public void selectCategories(View v) {

        Game.jugadorLocal = 0;
//        Game.tmpNumQuizzes = sbQuizzes.getProgress();
//        Game.tmpTotalTime = sbTotalTime.getProgress();
        SharedPreferencesStorage sps = SharedPreferencesStorage.getInstance(getContext());
        Game.numPlayers = Game.tmpNumPlayers = sbPlayers.getProgress();
        Game.numQuizzes = Game.tmpNumQuizzes = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10);
        Game.totalTime = Game.tmpTotalTime = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_TOTAL_TIME, 250);

        // Choose category in custom Game
        ((CategorySelectionActivity) getActivity()).attachTreeViewFragment(QuizActivity.ARG_ONLINE);
    }


    @Override
    public void onConnected(@Nullable Bundle connectionHint) {
//        Toast.makeText(this, "CONECTADO", Toast.LENGTH_SHORT).show();
//        btnConectar.setVisibility(View.GONE);
        loginActions.setVisibility(View.GONE);
//        btnDesconectar.setVisibility(View.VISIBLE);
//        findViewById(R.id.sign_in_button).setVisibility(View.GONE);
//        findViewById(R.id.sign_out_button).setVisibility(View.VISIBLE);
        globalActions.setVisibility(View.VISIBLE);
        newGameActions.setVisibility(View.VISIBLE);
// Para que funcione el regalo en lugar de la notificación
//        Games.Requests.registerRequestListener(Game.mGoogleApiClient, mRequestListener);

//        estadisticasJugador();

        Log.d(TAG, "onConnected() called. Sign in successful!");

        Log.d(TAG, "Sign-in succeeded.");

        // register listener so we are notified if we receive an invitation to play
        // while we are in the game
        Games.Invitations.registerInvitationListener(Game.mGoogleApiClient, (CategorySelectionActivity) getActivity());

        if (connectionHint != null) {
            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
            final Invitation inv = connectionHint
                    .getParcelable(Multiplayer.EXTRA_INVITATION);
            if (inv != null && inv.getInvitationId() != null) {
                // retrieve and cache the invitation ID
                Log.d(TAG, "onConnected: connection hint has a room invite!");


                CategorySelectionActivity.OnClickSnackBarAction actionInvitation = new CategorySelectionActivity.OnClickSnackBarAction() {
                    @Override
                    public void onClickAction() {
                        acceptInviteToRoom(inv.getInvitationId());
                    }
                };

                ((CategorySelectionActivity) getActivity()).showSnackbarMessage("Invitation Received from " + inv.getInviter().getDisplayName(), "Accept?", true, actionInvitation);

                return;
            }
        }
//        switchToMainScreen();
    }

    @Override
    public void onConnectionSuspended(int i) {
        Log.d(TAG, "onConnectionSuspended() called. Trying to reconnect.");
        Game.mGoogleApiClient.connect();
    }


    @Override
    public void onConnectionFailed(@NonNull ConnectionResult connectionResult) {
        Log.d(TAG, "onConnectionFailed() called, result: " + connectionResult);

        if (mResolvingConnectionFailure) {
            Log.d(TAG, "onConnectionFailed() ignoring connection failure; already resolving.");
            return;
        }

        if (mSignInClicked || mAutoStartSignInFlow) {
            mAutoStartSignInFlow = false;
            mSignInClicked = false;
            mResolvingConnectionFailure = BaseGameUtils.resolveConnectionFailure(getActivity(), Game.mGoogleApiClient,
                    connectionResult, RC_SIGN_IN, R.string.signin_other_error);
        }

    }

    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
        switch (requestCode) {
            case RC_SIGN_IN:
                mSignInClicked = false;
                mResolvingConnectionFailure = false;
                if (responseCode == RESULT_OK) {
                    Game.mGoogleApiClient.connect();
//                    SharedPreferences.Editor editor = getContext().getSharedPreferences(NAME_PREFERENCES, MODE_PRIVATE).edit();
//                    editor.putInt(NAME_PREFERENCES_CONNECTED, 1);
//                    editor.commit();
                } else {
                    BaseGameUtils.showActivityResultError(getActivity(), requestCode, responseCode, R.string.unknown_error);
                }
                break;


            case RC_SELECT_PLAYERS:
                // we got the result from the "select players" UI -- ready to create the room
                handleSelectPlayersResult(responseCode, intent);
                break;

            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the selected invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }


    // Handle the result of the "Select players UI" we launched when the user clicked the
    // "Invite friends" button. We react by creating a room with those players.
    private void handleSelectPlayersResult(int response, Intent data) {
        if (response != RESULT_OK) {
            Log.w(TAG, "*** select players UI cancelled, " + response);
//            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Select players UI succeeded.");

        // get the invitee list
//        final ArrayList<String> invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Game.resetGameVars();
        Game.invitees = data.getStringArrayListExtra(Games.EXTRA_PLAYER_IDS);
        Game.minAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MIN_AUTOMATCH_PLAYERS, 0);
        Game.maxAutoMatchPlayers = data.getIntExtra(Multiplayer.EXTRA_MAX_AUTOMATCH_PLAYERS, 0);

        Log.d(TAG, "Invitee count: " + Game.invitees.size());

        Game.jugadorLocal = 0;
//        Game.tmpNumQuizzes = sbQuizzes.getProgress();
//        Game.tmpTotalTime = sbTotalTime.getProgress();
        SharedPreferencesStorage sps = SharedPreferencesStorage.getInstance(getContext());
        Game.numPlayers = Game.tmpNumPlayers = sbPlayers.getProgress();
        Game.numQuizzes = Game.tmpNumQuizzes = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10);
        Game.totalTime = Game.tmpTotalTime = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_TOTAL_TIME, 250);


        Game.category = TopekaJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(SharedPreferencesStorage.getInstance(getContext()).readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10), Game.listCategories);
        startQuizzes();
    }


    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the selected invitation, if any.
    private void handleInvitationInboxResult(int response, Intent data) {
        if (response != RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
//            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        acceptInviteToRoom(inv.getInvitationId());
    }


    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);

        Game.resetGameVars();
        Game.mIncomingInvitationId = invId;
        Game.category = TopekaJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(10, null);
        startQuizzes();
//        switchToScreen(R.id.screen_wait);
    }


    private void startQuizzes() {
        Intent startIntent;

        startIntent = QuizActivity.getStartIntent(getActivity(), Game.category);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            ActivityCompat.startActivity(getActivity(), startIntent, null);
        else {
            ActivityCompat.startActivity(getActivity(), startIntent, null);
        }
    }

    public void btnInvitar_Click() {
        final int NUMERO_MINIMO_OPONENTES = sbPlayers.getProgress() - 1, NUMERO_MAXIMO_OPONENTES = sbPlayers.getProgress() - 1;
        // show list of invitable players
        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(Game.mGoogleApiClient, NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES);
        //switchToScreen(R.id.screen_wait);
        getActivity().startActivityForResult(intent, RC_SELECT_PLAYERS);
//        Games.Achievements.unlock(Game.mGoogleApiClient, getString(R.string.logro_invitar));
    }


    final static int REQUEST_LEADERBOARD = 100;
    final static int REQUEST_ACHIEVEMENTS = 101;
    final static int REQUEST_QUESTS = 102;
    private Button btnMarcadores;
    private Button btnLogros;
    private Button btnMisiones;


    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btnSelectCategories:
                selectCategories(v);
                break;
            case R.id.btnPlayAnyone:
                playAnyOne(v);
                break;
            case R.id.btnShowInvitations:
                btnVer_Invitaciones_Click(v);
                break;
            case R.id.btnInvite:
                btnInvitar_Click();
                break;
            case R.id.sign_in_button:
                // start the sign-in flow
                Log.d(TAG, "Sign-in button clicked");
                mSignInClicked = true;
                Game.mGoogleApiClient.connect();
                break;
//            case R.id.sign_out_button:
//                Log.d(TAG, "Sign-out button clicked");
//                mSignInClicked = false;
//                Games.signOut(Game.mGoogleApiClient);
//                Game.mGoogleApiClient.disconnect();
//
//                loginActions.setVisibility(View.VISIBLE);
//                globalActions.setVisibility(View.GONE);
//                newGameActions.setVisibility(View.GONE);
//                break;
        }
    }

    //Click actions

    // QuickGame
    private void playAnyOne(View v) {
        // QuizFragment
        SharedPreferencesStorage sps = SharedPreferencesStorage.getInstance(getContext());
//        Game.numPlayers = Game.tmpNumPlayers = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_PLAYERS, 2);
        Game.numPlayers = Game.tmpNumPlayers = sbPlayers.getProgress();
        Game.numQuizzes = Game.tmpNumQuizzes = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10);
        Game.totalTime = Game.tmpTotalTime = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_TOTAL_TIME, 250);
        Game.minAutoMatchPlayers = Game.numPlayers - 1;
        Game.maxAutoMatchPlayers = Game.numPlayers - 1;

        Game.category = TopekaJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(
                SharedPreferencesStorage.getInstance(getContext()).readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10),
                Game.listCategories);

        Intent intent = QuizActivity.getStartIntent(getContext(), Game.category);
        startActivity(intent);
    }


    private void showSeekbarsProgress() {
        txtPlayers.setText("Num. Players: " + sbPlayers.getProgress() + "/" + (sbPlayers.getMax() - 1));
//        txtTotalTime.setText("Total Time: " + sbTotalTime.getProgress() + "/" + sbTotalTime.getMax());
//        txtQuizzes.setText("Num. Quizzes: " + sbQuizzes.getProgress() + "/" + sbQuizzes.getMax());
    }

    // Activity just got to the foreground. We switch to the wait screen because we will now
    // go through the sign-in flow (remember that, yes, every time the Activity comes back to the
    // foreground we go through the sign-in flow -- but if the user is already authenticated,
    // this flow simply succeeds and is imperceptible).
    @Override
    public void onStart() {
        if (Game.mGoogleApiClient == null) {
//            switchToScreen(R.id.screen_sign_in);
        } else if (!Game.mGoogleApiClient.isConnected()) {
            Log.d(TAG, "Connecting client.");
//            switchToScreen(R.id.screen_wait);
            Game.mGoogleApiClient.connect();
        } else {
            Log.w(TAG,
                    "GameHelper: client was already connected on onStart()");
        }
        super.onStart();
    }

    public void btnVer_Invitaciones_Click(View view) {
        // show list of pending invitations
        Intent intent = Games.Invitations.getInvitationInboxIntent(Game.mGoogleApiClient);
//        switchToScreen(R.id.screen_wait);
        getActivity().startActivityForResult(intent, RC_INVITATION_INBOX);
    }
}