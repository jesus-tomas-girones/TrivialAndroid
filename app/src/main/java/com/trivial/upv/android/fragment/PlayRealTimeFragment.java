package com.trivial.upv.android.fragment;

import android.app.ActivityOptions;
import android.app.AlertDialog;
import android.content.DialogInterface;
import android.content.Intent;
import android.net.Uri;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.support.v4.app.Fragment;
import android.util.Log;
import android.view.LayoutInflater;
import android.view.View;
import android.view.ViewGroup;
import android.widget.Button;
import android.widget.ImageView;
import android.widget.LinearLayout;
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
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.InvitationsClient;
import com.google.android.gms.games.Player;
import com.google.android.gms.games.PlayersClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.InvitationCallback;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.trivial.upv.android.R;
import com.trivial.upv.android.activity.CategorySelectionActivity;
import com.trivial.upv.android.activity.QuizActivity;
import com.trivial.upv.android.helper.singleton.SharedPreferencesStorage;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TrivialJSonHelper;

import java.util.List;

import static android.app.Activity.RESULT_OK;
import static android.media.MediaCodec.MetricsConstants.MODE;

/**
 * Created by jvg63 on 22/06/2017.
 */

public class PlayRealTimeFragment extends Fragment
        implements

        View.OnClickListener {

    private static final String QUICK_GAME = "QUICK_GAME";
    private static final String CUSTOM_GAME = "CUSTOM_GAME";
    private Button btnNewGame;

    private static final int RC_SIGN_IN = 9001;
    final static int RC_INVITATION_INBOX = 10001;
    //    private com.google.android.gms.common.SignInButton btnConectar;
    private Button btnShowIntitations;
    private Button btnQuickGame;

    private LinearLayout globalActions;
    private LinearLayout newGameActions;
    //    private LinearLayout loginActions;
    private TextView txtListCategories;

    // Header Player
    private String mDisplayName;
    private Uri mImagePlayer;
    private ImageView mImgAvatar;
    private TextView mNameAvatar;

    private GoogleSignInClient mGoogleSignInClient;
    private boolean retry = false;
//    private GoogleSignInAccount mSignedInAccount = null;

    public PlayRealTimeFragment() {
        Game.resetGameVars();
        Game.listCategories.clear();
        List<CategoryJSON> categoriesJSON = TrivialJSonHelper.getInstance(getContext(), false).getCategoriesJSON();
        for (CategoryJSON category : categoriesJSON) {
            Game.listCategories.add(new String(category.getCategory()));
        }

        Game.level = (long) (Math.pow(2, categoriesJSON.size()) - 1);
    }

    @Override
    public void onCreate(@Nullable Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        Game.mode = getArguments().getString(MODE);
    }

    public static PlayRealTimeFragment newInstance(String mode) {
        PlayRealTimeFragment fragment = new PlayRealTimeFragment();
        Bundle args = new Bundle();
        args.putString(MODE, mode);
        fragment.setArguments(args);
        return fragment;
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
        setupView(view);
    }

    private void changeTitleActionBar() {
        ((CategorySelectionActivity) getActivity()).setToolbarTitle(getString(R.string.play_online));
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
        signInSilently();

        if (Game.listCategories.size() == TrivialJSonHelper.getInstance(getContext(), false).getCategoriesJSON().size()) {
            txtListCategories.setText(getString(R.string.all_categories));
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

    private void setupView(final View view) {

        CategorySelectionActivity.animateViewFullScaleXY(((CategorySelectionActivity) getActivity()).getSubcategory_title(), 100, 300);

        globalActions = (LinearLayout) view.findViewById(R.id.global_actions);
        newGameActions = (LinearLayout) view.findViewById(R.id.new_game_actions);
//        loginActions = (LinearLayout) view.findViewById(R.id.login_actions);
        txtListCategories = (TextView) view.findViewById(R.id.txtListCategories);
        btnInvitar = (Button) view.findViewById(R.id.btnInvite);
        btnInvitar.setOnClickListener(this);

//        btnConectar = (com.google.android.gms.common.SignInButton) view.findViewById(R.id.sign_in_button);
//        btnConectar.setOnClickListener(this);

//        btnDesconectar = (Button) view.findViewById(R.id.sign_out_button);
//        btnDesconectar.setOnClickListener(this);

        // Reset Data Game
        Game.resetGameVars();

//        btnConectar.setVisibility(View.VISIBLE);
//        loginActions.setVisibility(View.VISIBLE);
//        btnDesconectar.setVisibility(View.GONE);

//        newGameActions.setVisibility(View.GONE);
//        globalActions.setVisibility(View.GONE);
//        startSignInIntent();

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
        showSeekbarsProgress();

        // Initialie header
        mImgAvatar = (ImageView) view.findViewById(R.id.imgAvatar);
        mNameAvatar = (TextView) view.findViewById(R.id.txtNameAvatar);

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
        mGoogleSignInClient = GoogleSignIn.getClient(getActivity(),
                new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());
    }

    public void startSignInIntent() {
        if (mGoogleSignInClient != null)
            startActivityForResult(mGoogleSignInClient.getSignInIntent(), RC_SIGN_IN);
        else
            showWarningConnection(actionOnClickButton);
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

    final static int RC_SELECT_PLAYERS = 10000;
    private Button btnInvitar;

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
        ((CategorySelectionActivity) getActivity()).attachTreeViewFragment(QuizActivity.ARG_REAL_TIME_ONLINE);
    }

    private void modifyVisibilityButtons(boolean visible) {
        btnShowIntitations.setEnabled(visible);
        btnQuickGame.setEnabled(visible);
        btnNewGame.setEnabled(visible);
        btnInvitar.setEnabled(visible);
    }

    private void disableButtons() {
        modifyVisibilityButtons(false);
    }

    private void enableButtons() {
        modifyVisibilityButtons(true);
    }


    private void onConnected(GoogleSignInAccount googleSignInAccount) {
//        if (mSignedInAccount != googleSignInAccount) {
//        mSignedInAccount = googleSignInAccount;
        Log.d(TAG, "onConnected(): connected to Google APIs");
        retry = false;
        enableButtons();
        mInvitationsClient = Games.getInvitationsClient(getActivity(), googleSignInAccount);

        PlayersClient playersClient = Games.getPlayersClient(getActivity(),
                googleSignInAccount);
        playersClient.getCurrentPlayer()
                .addOnSuccessListener(new OnSuccessListener<Player>() {
                    @Override
                    public void onSuccess(Player player) {
                        inicializarRealTimeGameOnConnected(player);
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(),
                                "Hay un problema para obtener el id del jugador!"
                                , Toast.LENGTH_LONG).show();
                    }
                });

        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(getActivity(), googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle bundle) {
                        if (bundle != null) {
                            final TurnBasedMatch match = bundle.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
                            if (match != null) {
                                new AlertDialog.Builder(getActivity())
                                        .setMessage("La invitación corresponde a una PARTIDA POR TURNOS de " + Game.pendingTurnBasedMatch.getParticipants().get(0).getDisplayName())
                                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                                            @Override
                                            public void onClick(DialogInterface dialogInterface, int i) {
                                                Game.pendingTurnBasedMatch = match;
                                                ((CategorySelectionActivity) getActivity()).attachPlayTurnBasedFragment(QuizActivity.ARG_TURNED_BASED_ONLINE);
                                                ((CategorySelectionActivity) getActivity()).navigationView.getMenu().getItem(3).setChecked(true);
                                            }
                                        })
                                        .show();
                                return;
                            }
                            Invitation invitation = bundle.getParcelable(Multiplayer.EXTRA_INVITATION);
                            if (invitation != null) {
                                manageInvitation(invitation);
                            } else if (Game.pendingInvitation != null) {
                                manageInvitation(Game.pendingInvitation);
                                Game.pendingInvitation = null;
                            }
                        } else {
                            if (Game.pendingInvitation != null) {
                                manageInvitation(Game.pendingInvitation);
                                Game.pendingInvitation = null;
                            }
                        }
                    }
                })
                .addOnFailureListener(new OnFailureListener() {
                    @Override
                    public void onFailure(@NonNull Exception e) {
                        Toast.makeText(getActivity(),
                                "There was a problem getting the activation hint!"
                                , Toast.LENGTH_LONG).show();

                    }
                });


        // As a demonstration, we are registering this activity as a handler for
        // invitation and match events.

        // This is *NOT* required; if you do not register a handler for
        // invitation events, you will get standard notifications instead.
        // Standard notifications may be preferable behavior in many cases.
        mInvitationsClient.registerInvitationCallback(mInvitationCallback);

        ((CategorySelectionActivity) getActivity()).animateToolbarNavigateCategories(false);
    }
//    }


    public void inicializarRealTimeGameOnConnected(Player player) {
        mImagePlayer = player.getIconImageUri();
        ImageManager imageManager = ImageManager.create(getActivity());
        imageManager.loadImage(mImgAvatar, mImagePlayer);

        mDisplayName = player.getDisplayName();
        mNameAvatar.setText(mDisplayName);
        Game.mPlayerId = player.getPlayerId();

//                                mNameAvatar.setText(Game.mPlayerId + " -  " + mDisplayName);
        mNameAvatar.setText(mDisplayName);

//        Toast.makeText(this, "CONECTADO", Toast.LENGTH_SHORT).show();
//        btnConectar.setVisibility(View.GONE);
//        loginActions.setVisibility(View.GONE);
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
//        Games.Invitations.registerInvitationListener(Game.mGoogleApiClient, (CategorySelectionActivity) getActivity());
//
//        if (connectionHint != null) {
//            Log.d(TAG, "onConnected: connection hint provided. Checking for invite.");
//            final Invitation inv = connectionHint
//                    .getParcelable(Multiplayer.EXTRA_INVITATION);
//            if (inv != null && inv.getInvitationId() != null) {
//                // retrieve and cache the invitation ID
//                Log.d(TAG, "onConnected: connection hint has a room invite!");
//
//
//                CategorySelectionActivity.OnClickSnackBarAction actionInvitation = new CategorySelectionActivity.OnClickSnackBarAction() {
//                    @Override
//                    public void onClickAction() {
//                        acceptInviteToRoom(inv.getInvitationId());
//                    }
//                };
//
//                ((CategorySelectionActivity) getActivity()).showSnackbarMessage("Invitation Received from " + inv.getInviter().getDisplayName(), "Accept?", true, actionInvitation);
//
//                return;
//            }
//        }
//        switchToMainScreen();
    }


    @Override
    public void onActivityResult(int requestCode, int responseCode, Intent intent) {
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
                handleSelectPlayersResult(responseCode, intent);
                break;

            case RC_INVITATION_INBOX:
                // we got the result from the "select invitation" UI (invitation inbox). We're
                // ready to accept the roulette_selection invitation:
                handleInvitationInboxResult(responseCode, intent);
                break;
        }
        super.onActivityResult(requestCode, responseCode, intent);
    }

    private void onDisconnected() {
        mGoogleSignInClient = null;

        if (!retry) {
            retry = true;
            startSignInIntent();

        }

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


        Game.category = TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(SharedPreferencesStorage.getInstance(getContext()).readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10), Game.listCategories, QuizActivity.ARG_REAL_TIME_ONLINE, -1);
        startQuizzes();
    }


    // Handle the result of the invitation inbox UI, where the player can pick an invitation
    // to accept. We react by accepting the roulette_selection invitation, if any.
    private void handleInvitationInboxResult(int response, final Intent data) {
        if (response != RESULT_OK) {
            Log.w(TAG, "*** invitation inbox UI cancelled, " + response);
//            switchToMainScreen();
            return;
        }

        Log.d(TAG, "Invitation inbox UI succeeded.");
        Invitation inv = data.getExtras().getParcelable(Multiplayer.EXTRA_INVITATION);

        // accept invitation
        if (inv != null) {
            acceptInviteToRoom(inv.getInvitationId());
        } else {
            final TurnBasedMatch match = data.getExtras().getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
            new AlertDialog.Builder(getActivity())
                    .setMessage("La invitación corresponde a una PARTIDA POR TURNOS de " + match.getParticipants().get(0).getDisplayName())
                    .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                        @Override
                        public void onClick(DialogInterface dialogInterface, int i) {
                            Game.pendingTurnBasedMatch = match;
                            ((CategorySelectionActivity) getActivity()).attachPlayTurnBasedFragment(QuizActivity.ARG_TURNED_BASED_ONLINE);
                            ((CategorySelectionActivity) getActivity()).navigationView.getMenu().getItem(3).setChecked(true);
                        }
                    })
                    .show();

        }

    }


    // Accept the given invitation.
    void acceptInviteToRoom(String invId) {
        // accept the invitation
        Log.d(TAG, "Accepting invitation: " + invId);

        Game.resetGameVars();
        Game.mIncomingInvitationId = invId;
        Game.category = TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(10, null, QuizActivity.ARG_REAL_TIME_ONLINE, -1);
        startQuizzes();
//        switchToScreen(R.id.screen_wait);
    }


    private void startQuizzes() {
        Intent startIntent;

        startIntent = QuizActivity.getStartIntent(getActivity(), Game.category);
        if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
            startActivity(startIntent, null);
        else {
            startActivity(startIntent, null);
        }
    }

    @Override
    public void onClick(View v) {

        switch (v.getId()) {

            case R.id.btnSelectCategories:
                disableButtons();
                selectCategories(v);
                break;
            case R.id.btnPlayAnyone:
                disableButtons();
                playAnyOne(v);
                break;
            case R.id.btnShowInvitations:
                disableButtons();
                btnVer_Invitaciones_Click(v);
                break;
            case R.id.btnInvite:
                disableButtons();
                btnInvitar_Click();
                break;
//            case R.id.sign_in_button:
//                // start the sign-in flow
//                Log.d(TAG, "Sign-in button clicked");
////                mSignInClicked = true;
//                startSignInIntent();
//                break;
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
        if (mGoogleSignInClient != null) {
            // QuizFragment
            SharedPreferencesStorage sps = SharedPreferencesStorage.getInstance(getContext());
//        Game.numPlayers = Game.tmpNumPlayers = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_PLAYERS, 2);
            Game.numPlayers = Game.tmpNumPlayers = sbPlayers.getProgress();
            Game.numQuizzes = Game.tmpNumQuizzes = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10);
            Game.totalTime = Game.tmpTotalTime = sps.readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_TOTAL_TIME, 250);
            Game.minAutoMatchPlayers = Game.numPlayers - 1;
            Game.maxAutoMatchPlayers = Game.numPlayers - 1;

            Game.category = TrivialJSonHelper.getInstance(getContext(), false).createCategoryPlayTimeReal(
                    SharedPreferencesStorage.getInstance(getContext()).readIntPreference(SharedPreferencesStorage.PREF_URL_MODE_ONLINE_NUM_QUIZZES, 10),
                    Game.listCategories, QuizActivity.ARG_REAL_TIME_ONLINE, -1);

            Intent intent = QuizActivity.getStartIntent(getContext(), Game.category);
            startActivity(intent);
        } else
            showWarningConnection(actionOnClickButton);
    }


    private void showSeekbarsProgress() {
        txtPlayers.setText(getString(R.string.num_players) + sbPlayers.getProgress() + "/" + (sbPlayers.getMax() - 1));
//        txtTotalTime.setText("Total Time: " + sbTotalTime.getProgress() + "/" + sbTotalTime.getMax());
//        txtQuizzes.setText("Num. Quizzes: " + sbQuizzes.getProgress() + "/" + sbQuizzes.getMax());
    }


    public void btnInvitar_Click() {
        if (mGoogleSignInClient != null) {
            final int NUMERO_MINIMO_OPONENTES = sbPlayers.getProgress() - 1, NUMERO_MAXIMO_OPONENTES = sbPlayers.getProgress() - 1;
            // show list of invitable players
//        Intent intent = Games.RealTimeMultiplayer.getSelectOpponentsIntent(Game.mGoogleApiClient, NUMERO_MINIMO_OPONENTES, NUMERO_MAXIMO_OPONENTES);
//        switchToScreen(R.id.screen_wait);
//        startActivityForResult(intent, RC_SELECT_PLAYERS);
//        Games.Achievements.unlock(Game.mGoogleApiClient, getString(R.string.logro_invitar));

            boolean allowAutoMatch = false;
            Games.getTurnBasedMultiplayerClient(getActivity(),
                    GoogleSignIn.getLastSignedInAccount(getActivity()))
                    .getSelectOpponentsIntent(NUMERO_MINIMO_OPONENTES,
                            NUMERO_MAXIMO_OPONENTES, allowAutoMatch)
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, RC_SELECT_PLAYERS);
                        }
                    });
        } else
            showWarningConnection(actionOnClickButton);
    }


    //// TODO LIST INVITACIONES
    private InvitationCallback mInvitationCallback = new InvitationCallback() {
        // Handle notification events.
        @Override
        public void onInvitationReceived(@NonNull final Invitation invitation) {
//            Toast.makeText(
//                    getActivity(),
//                    "An invitation has arrived from "
//                            + invitation.getInviter().getDisplayName(), Toast.LENGTH_SHORT)
//                    .show();
            if (invitation.getInvitationType() == Invitation.INVITATION_TYPE_REAL_TIME) {
                manageInvitation(invitation);
            } else if (invitation.getInvitationType() == Invitation.INVITATION_TYPE_TURN_BASED) {

                new AlertDialog.Builder(getActivity())
                        .setMessage("La invitación corresponde a una PARTIDA POR TURNOS de " + invitation.getInviter().getDisplayName())
                        .setNeutralButton(android.R.string.ok, new DialogInterface.OnClickListener() {
                            @Override
                            public void onClick(DialogInterface dialogInterface, int i) {
                                Game.pendingTurnInvitation = invitation;
                                ((CategorySelectionActivity) getActivity()).onNavigationItemSelected(((CategorySelectionActivity) getActivity()).navigationView.getMenu().getItem(3));
                                ((CategorySelectionActivity) getActivity()).navigationView.getMenu().getItem(3).setChecked(true);
                            }
                        })
                        .show();
            }
        }

        @Override
        public void onInvitationRemoved(@NonNull String invitationId) {
//            Toast.makeText(getActivity(), "An invitation was removed.", Toast.LENGTH_SHORT).show();
            Log.w("TRAZA", "An invitation " + invitationId + " was removed.");
//            if (Game.mIncomingInvitationId != null && Game.mIncomingInvitationId.equals(invitationId)) {
//                Game.mIncomingInvitationId = null;
//            }
        }
    };


    public void manageInvitation(final Invitation invitation) {
        if (invitation != null && invitation.getInvitationId() != null) {
            String invitationFrom = invitation.getInviter().getDisplayName();
            android.app.AlertDialog.Builder alertDialogBuilder = new android.app.AlertDialog.Builder(getActivity());
            alertDialogBuilder.setMessage("Do you want tu accept the invitation from " + invitationFrom + "?");
            alertDialogBuilder
                    .setCancelable(false)
                    .setPositiveButton("Sure, accept!",
                            new DialogInterface.OnClickListener() {
                                @Override
                                public void onClick(DialogInterface dialog, int id) {
                                    Log.d("TRAZA", "OnInvitationReceived");
                                    Game.mIncomingInvitationId = invitation.getInvitationId();
                                    Game.category = TrivialJSonHelper.getInstance(getActivity(), false).createCategoryPlayTimeReal(10, null, QuizActivity.ARG_REAL_TIME_ONLINE, -1);
                                    Intent startIntent = QuizActivity.getStartIntent(getActivity(), Game.category);
                                    if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                                        startActivity(startIntent, null);
                                    else {
                                        startActivity(startIntent, null);
                                    }
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
    }


    // Client used to interact with the Invitation system.
    private InvitationsClient mInvitationsClient = null;


    public void btnVer_Invitaciones_Click(View view) {
        if (mGoogleSignInClient != null) {
            Games.getInvitationsClient(getActivity(), GoogleSignIn.getLastSignedInAccount(getActivity()))
                    .getInvitationInboxIntent()
                    .addOnSuccessListener(new OnSuccessListener<Intent>() {
                        @Override
                        public void onSuccess(Intent intent) {
                            startActivityForResult(intent, RC_INVITATION_INBOX);
                        }
                    });
        } else
            showWarningConnection(actionOnClickButton);
    }

    private android.app.AlertDialog mAlertDialog = null;

    // Generic warning/info dialog
    public void showWarningConnection(final ActionOnClickButton actionOnClickButton) {
        String title = "Google Play Games";
        String message = "You aren't not log in! OK to try it";

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
                        if (actionOnClickButton != null)
                            actionOnClickButton.onClick();
                    }
                });

        // create alert dialog
        mAlertDialog = alertDialogBuilder.create();

        // show it
        mAlertDialog.show();
    }

    @Override
    public void onPause() {
        super.onPause();
        // Unregister the invitation callbacks; they will be re-registered via
        // onResume->signInSilently->onConnected.
        if (mInvitationsClient != null) {
            mInvitationsClient.unregisterInvitationCallback(mInvitationCallback);
        }
    }

    private interface ActionOnClickButton {
        void onClick();
    }

    private ActionOnClickButton actionOnClickButton = new ActionOnClickButton() {
        @Override
        public void onClick() {
            startSignInIntent();
        }
    };
}