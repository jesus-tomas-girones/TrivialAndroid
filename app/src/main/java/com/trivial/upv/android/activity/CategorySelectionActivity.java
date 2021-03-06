/*
 * Copyright 2015 Google Inc. All Rights Reserved.
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

import android.annotation.SuppressLint;
import android.app.Activity;
import android.app.ProgressDialog;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.design.widget.NavigationView;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.content.ContextCompat;
import android.support.v4.view.GravityCompat;
import android.support.v4.view.ViewCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.Fade;
import android.transition.Slide;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.ImageButton;
import android.widget.TextView;
import android.widget.Toast;

import com.google.android.gms.auth.api.signin.GoogleSignIn;
import com.google.android.gms.auth.api.signin.GoogleSignInAccount;
import com.google.android.gms.auth.api.signin.GoogleSignInClient;
import com.google.android.gms.auth.api.signin.GoogleSignInOptions;
import com.google.android.gms.games.Games;
import com.google.android.gms.games.GamesClient;
import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.Multiplayer;
import com.google.android.gms.games.multiplayer.turnbased.TurnBasedMatch;
import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.OnFailureListener;
import com.google.android.gms.tasks.OnSuccessListener;
import com.google.android.gms.tasks.Task;
import com.trivial.upv.android.R;
import com.trivial.upv.android.databinding.ActivityCategorySelectionBinding;
import com.trivial.upv.android.databinding.NavHeaderCategorySelectionBinding;
import com.trivial.upv.android.fragment.AboutDialogFragment;
import com.trivial.upv.android.fragment.CategorySelectionFragment;
import com.trivial.upv.android.fragment.CategorySelectionTreeViewFragment;
import com.trivial.upv.android.fragment.HelpDialogFragment;
import com.trivial.upv.android.fragment.MainDialogFragment;
import com.trivial.upv.android.fragment.PlayRealTimeFragment;
import com.trivial.upv.android.fragment.PlayTurnBasedFragment;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.PreferencesHelper;
import com.trivial.upv.android.model.Player;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.model.json.CategoryJSON;
import com.trivial.upv.android.persistence.TrivialJSonHelper;
import com.trivial.upv.android.widget.AvatarView;

import java.util.List;

import static com.trivial.upv.android.activity.QuizActivity.ARG_ONE_PLAYER;
import static com.trivial.upv.android.activity.QuizActivity.ARG_REAL_TIME_ONLINE;
import static com.trivial.upv.android.activity.QuizActivity.ARG_TURNED_BASED_ONLINE;
import static com.trivial.upv.android.persistence.TrivialJSonHelper.ACTION_RESP;

public class CategorySelectionActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener {

    private static final String EXTRA_PLAYER = "player";
    private static final String TAG = CategorySelectionActivity.class.getSimpleName();
    private TextView scoreView;
    private ImageButton backButton;
    private AvatarView avatar;
    private TextView title;
    private GoogleSignInClient mGoogleSignInClient = null;
    private boolean retryGPG = false;

    public TextView getSubcategory_title() {
        return subcategory_title;
    }

    private TextView subcategory_title;
    private String fragmentNameSaved = "";
    private DrawerLayout drawer;
    private ActionBarDrawerToggle toggle;

    public static void start(Activity activity, Player player, ActivityOptionsCompat options) {
        Intent starter = getStartIntent(activity, player);
        ActivityCompat.startActivity(activity, starter, options.toBundle());
    }

    public static void start(Context context, Player player) {
        Intent starter = getStartIntent(context, player);
        context.startActivity(starter);
    }

    @NonNull
    static Intent getStartIntent(Context context, Player player) {
        Intent starter = new Intent(context, CategorySelectionActivity.class);
        starter.putExtra(EXTRA_PLAYER, player);
        return starter;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
//        setContentView(R.layout.activity_category_selection);
        ActivityCategorySelectionBinding binding = DataBindingUtil
                .setContentView(this, R.layout.activity_category_selection);
        Player player = getIntent().getParcelableExtra(EXTRA_PLAYER);
        if (!PreferencesHelper.isSignedIn(this)) {
            if (player == null) {
                player = PreferencesHelper.getPlayer(this);
            } else {
                PreferencesHelper.writeToPreferences(this, player);
            }
        }
//        binding.setPlayer(player);
        View headerView = binding.navView.getHeaderView(0);
        NavHeaderCategorySelectionBinding.bind(headerView).setPlayer(player);
//        binding.bind(headerView);

        setUpToolbar();

        if (savedInstanceState != null)
            fragmentNameSaved = savedInstanceState.getString("fragment", "");

        initActivity(savedInstanceState);

        // JVG.S
        mGoogleSignInClient = GoogleSignIn.getClient(this, new GoogleSignInOptions.Builder(GoogleSignInOptions.DEFAULT_GAMES_SIGN_IN).build());
        oneTimeGooglePlayGame = true;
        retryGPG = true;
        // JVG.E
    }

    // JVG.S
    private void loadCategories() {
        // RECEIVER PARA ACTUALIZAR PROGRESO Y CARGA DE LAS CATEGORIAS
        if (filtro == null) {
            filtro = new IntentFilter(ACTION_RESP);
            filtro.addCategory(Intent.CATEGORY_DEFAULT);
        }
        if (receiver == null) {
            receiver = new ReceptorOperacion();
            registerReceiver(receiver, filtro);
        }
//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
        if (checkInternetAccess()) {
            if (!TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded()) {
//                setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_LOCKED);
                pDialog = new ProgressDialog(this);
                pDialog.setMessage("Cargando...");
                pDialog.setIndeterminate(false);
                pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
                pDialog.setProgress(0);
                pDialog.setMax(100);
                pDialog.setCancelable(false);
                pDialog.show();

                new Thread() {
                    public void run() {
                        // Aprovisiona el modelo con las categorías
                        TrivialJSonHelper.getInstance(CategorySelectionActivity.this, true);

                    }
                }.start();

            } else {
                // Load a freme (restore activity)
                if (TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded()) {
                    if (checkJsonIsCorrect(this)) {
                        openRequiredFragment();
                    } else {
                        openSettingsWhenErrorDetectedInJson(this);
                    }
                }
            }
        } else {
            snackbar = Snackbar
                    .make(findViewById(R.id.category_container), "No hay conexión de Internet.", Snackbar.LENGTH_INDEFINITE)
                    .setAction("REINTENTAR", new View.OnClickListener() {
                        @Override
                        public void onClick(View view) {
                            dismissSnackbar();
                            loadCategories();
                        }
                    });

            snackbar.show();
        }
    }

    private void openRequiredFragment() {
        if (fragmentNameSaved == null || getSupportFragmentManager().findFragmentById(R.id.category_container) == null)
            signInSilently(true);
        else
            signInSilently(false);
        showScore();
    }


    private boolean checkInternetAccess() {
        ConnectivityManager cm = (ConnectivityManager) getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo netInfo = cm.getActiveNetworkInfo();
        return netInfo != null && netInfo.isConnectedOrConnecting();
    }


    IntentFilter filtro;

    ReceptorOperacion receiver = null;

    private Snackbar snackbar = null;

    public void showDeleteProgressConfirmation(final int position) {
        snackbar = Snackbar.make(findViewById(R.id.root_view), "¿Quieres eliminar los resultados obtenidos?", Snackbar.LENGTH_INDEFINITE).setAction("Eliminar Avance", new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                (TrivialJSonHelper.getInstance(getBaseContext(), false)).deleteProgressCategory(position);

                Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
                if (fragment instanceof CategorySelectionFragment) {
                    ((CategorySelectionFragment) fragment).getAdapter().updateCategories();
                    ((CategorySelectionFragment) fragment).getAdapter().notifyItemChanged(position);

                }

                showScore();
                dismissSnackbar();
            }
        });

        snackbar.show();
    }


    public interface OnClickSnackBarAction {
        void onClickAction();
    }

    public void showSnackbarMessage(final String msg, final String textAction, boolean showAction, final OnClickSnackBarAction action) {


        if (showAction) {
            snackbar = Snackbar.make(findViewById(R.id.root_view), msg, Snackbar.LENGTH_INDEFINITE);

            snackbar.setAction(textAction, new View.OnClickListener() {
                @Override
                public void onClick(View view) {
                    dismissSnackbar();

                    if (action != null)
                        action.onClickAction();
                }
            });
        } else {
            snackbar = Snackbar.make(findViewById(R.id.root_view), msg, Snackbar.LENGTH_SHORT);
        }

        snackbar.show();
    }

    public synchronized void setInitBlockAnimation(boolean initBlockAnimation) {
        this.initBlockAnimation = initBlockAnimation;
    }

    public synchronized boolean getInitBlockAnimation() {
        return initBlockAnimation;
    }

    boolean initBlockAnimation = false;

    public void setToolbarTitle(String title) {
        subcategory_title.setText(title);
    }


    //JVG.S
    private void onConnected(GoogleSignInAccount googleSignInAccount, final boolean loadDefaultFragmentCategories) {
//        Log.d(TAG, "onConnected(): Connection successful");

        // Retrieve the TurnBasedMatch from the connectionHint
        GamesClient gamesClient = Games.getGamesClient(this, googleSignInAccount);
        gamesClient.getActivationHint()
                .addOnSuccessListener(new OnSuccessListener<Bundle>() {
                    @Override
                    public void onSuccess(Bundle hint) {
                        if (hint != null) {
                            TurnBasedMatch match = hint.getParcelable(Multiplayer.EXTRA_TURN_BASED_MATCH);
                            final Invitation invitationAux = hint.getParcelable(Multiplayer.EXTRA_INVITATION);
                            if (invitationAux != null) {
                                Game.pendingInvitation = invitationAux;
                                attachPlayOnlineFragment(QuizActivity.ARG_REAL_TIME_ONLINE);
                                navigationView.getMenu().getItem(2).setChecked(true);
                            } else if (match != null) {
                                Game.pendingTurnBasedMatch = match;
                                attachPlayTurnBasedFragment(QuizActivity.ARG_TURNED_BASED_ONLINE);
                                navigationView.getMenu().getItem(3).setChecked(true);
                            } else {
                                if (loadDefaultFragmentCategories)
                                    attachCategoryGridFragment();
                            }
                        } else {
                            if (loadDefaultFragmentCategories)
                                attachCategoryGridFragment();
                        }
                    }
                }).addOnFailureListener(new OnFailureListener() {
            @Override
            public void onFailure(@NonNull Exception e) {
                Log.w(TAG, "No hay invitaciones");
                if (loadDefaultFragmentCategories)
                    attachCategoryGridFragment();
            }
        });
    }

    private boolean oneTimeGooglePlayGame = false;

    public void signInSilently(final boolean loadDefaultFragmentCategories) {
        if (oneTimeGooglePlayGame) {
//            Log.d(TAG, "signInSilently()");
            mGoogleSignInClient.silentSignIn().addOnCompleteListener(this,
                    new OnCompleteListener<GoogleSignInAccount>() {
                        @Override
                        public void onComplete(
                                @NonNull Task<GoogleSignInAccount> task) {
                            oneTimeGooglePlayGame = false;
                            if (task.isSuccessful()) {
//                                Log.d(TAG, "signInSilently(): success");
                                onConnected(task.getResult(), loadDefaultFragmentCategories);
                            } else {
                                Log.d(TAG, "signInSilently(): failure", task.getException());
                                if (retryGPG) {
                                    retryGPG = false;
                                    signInSilently(loadDefaultFragmentCategories);
                                } else {
                                    if (loadDefaultFragmentCategories)
                                        attachCategoryGridFragment();
                                }
                            }
                        }
                    });
        } else {
            if (loadDefaultFragmentCategories)
                attachCategoryGridFragment();
        }
    }

    //JVG.E
    public class ReceptorOperacion extends BroadcastReceiver {
        @Override
        public void onReceive(Context context, Intent intent) {
            synchronized (this) {
                String result = intent.getExtras().getString("RESULT");
                if ("OK".equals(result)) {

                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;
//                        signInSilently(true);
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }

                    if (checkJsonIsCorrect(context)) {
                        openRequiredFragment();
                    } else {
                        openSettingsWhenErrorDetectedInJson(context);
                    }

                    // Carga categorias
//                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
//                    if (fragment instanceof CategorySelectionFragment) {
//                        ((CategorySelectionFragment) fragment).animateTransitionSubcategories(null);
//                    }
//                    attachPlayTurnBasedFragment(QuizActivity.ARG_TURNED_BASED_ONLINE);
//                    Log.d("ONRECEIVE", intent.getExtras().getString("RESULT"));
                } else if ("REFRESH".equals(result)) {
                    if (pDialog != null) {
                        int value = intent.getExtras().getInt("REFRESH", 0);
                        if (value > pDialog.getProgress())
                            pDialog.setProgress(intent.getExtras().getInt("REFRESH", 0));
                    }
                } else if ("ERROR".equals(result)) {
                    snackbar = Snackbar
                            .make(findViewById(R.id.root_view), "Ha ocurrido un error cargando las categorías", Snackbar.LENGTH_INDEFINITE)
                            .setAction("CERRAR", new View.OnClickListener() {
                                @Override
                                public void onClick(View view) {
                                    dismissSnackbar();

                                    finish();
                                }
                            });
                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }

//                    Log.d("ONRECEIVE", intent.getExtras().getString("RESULT"));
                    TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).resetData();
                    snackbar.show();
                }
            }
        }

    }

    private void openSettingsWhenErrorDetectedInJson(Context context) {
        // El fichero está corrupto
        Intent intentSettings = new Intent(CategorySelectionActivity.this, SettingsActivity.class);
        context.startActivity(intentSettings);
        finish();
    }

    private boolean checkJsonIsCorrect(Context context) {
        // Hace un chequeo recorriendo la estructura de nodos para comprobar que el fichero está ok
//        try {
////            TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).getScore();
//            List<CategoryJSON> categoriesJSON = TrivialJSonHelper.getInstance(this, false).getCategoriesJSON();
//            for (int position = 0; position < categoriesJSON.size(); position++) {
//                TrivialJSonHelper.getInstance(this, false).isSolvedCurrentCategory(position);
//            }
//            return true;
//
//        } catch (Exception e) {
//            Toast.makeText(context, "El fichero .json podría contener algún error", Toast.LENGTH_SHORT).show();
//            return false;
//        }
        return true;
    }
    // JVG.E

    private void initActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
//            attachCategoryGridFragment();
            //JVG.S
        }
//      else {
//            setProgressBarVisibility(View.GONE);
        //JVG.E
//      }
        //JVG.S
//        supportPostponeEnterTransition();
        scoreView = (TextView) findViewById(R.id.score_main);
//        backButton = (ImageButton) findViewById(R.id.back);
//        title = (TextView) findViewById(R.id.title);
        subcategory_title = (TextView) findViewById(R.id.sub_category_title);
        //JVG.E

    }

    //JVG.S
    @Override
    public void onBackPressed() {

        if (snackbar != null) {
            dismissSnackbar();
        } else if (drawer.isDrawerOpen(GravityCompat.START)) {
            drawer.closeDrawer(GravityCompat.START);
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
            if (fragment instanceof CategorySelectionFragment) {
                if (!TrivialJSonHelper.getInstance(getBaseContext(), false).thereAreMorePreviusCategories()) {
                    if (!getInitBlockAnimation())
                        super.onBackPressed();

                } else {
                    goToPreviusCategory();
                }
            } else {
                super.onBackPressed();
            }
        }
    }

    public interface ActionOnFinishAnimation {
        public void onFinishedAnimation();
    }



    private void goToPreviusCategory() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment instanceof CategorySelectionFragment) {
//            TrivialJSonHelper.getInstance(getBaseContext(), false).navigatePreviusCategory();
            ((CategorySelectionFragment) fragment).animateTransitionSubcategories(null, new ActionOnFinishAnimation() {
                @Override
                public void onFinishedAnimation() {
                    TrivialJSonHelper.getInstance(getBaseContext(), false).navigatePreviusCategory();
                    showToolbarSubcategories();
                }
            });

        } else {
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void showToolbarSubcategories() {
        if (TrivialJSonHelper.getInstance(getBaseContext(), false).isInitCategory()) {
            setToolbarTitle(getResources().getString(R.string.menu_nd_category_review));
            animateToolbarNavigateCategories(true);

        } else {
//            TextView viewSubcategoryText = (TextView) findViewById(R.id.sub_category_title);
            setToolbarTitle(TrivialJSonHelper.getInstance(getBaseContext(), false).getPreviousTitleCategory());
//            Log.d("previus", "categoria_previa" + TrivialJSonHelper.getInstance(getBaseContext(), false).getPreviousTitleCategory());
            animateToolbarNavigateToSubcategories(true);
        }
    }


    public ProgressDialog pDialog;
    //JVG.E

    @Override
    protected void onResume() {
        super.onResume();

        // JVG.S
//        Log.d("TRAZA", "onStart");
        loadCategories();
        // JVG.E
    }

    @Override
    protected void onNewIntent(Intent intent) {
        super.onNewIntent(intent);
        oneTimeGooglePlayGame = true;
        retryGPG = true;
    }

    private void showScore() {
        final int score = TrivialJSonHelper.getInstance(getBaseContext(), false).getScore();
        if (scoreView != null) {
            scoreView.setText(getString(R.string.x_points, score));
        }
    }

    private boolean isFragmentCategorySelection() {
        return (getSupportFragmentManager().findFragmentById(R.id.category_container)) instanceof CategorySelectionFragment;
    }

    boolean viewedMainDialog = false;

    @Override
    protected void onStart() {
        super.onStart();
        // JVG.S
//        Log.d("TRAZA", "onStart");
//        loadCategories();
//        registerReceiver(receiver, filtro);
        // JVG.E
    }

    public NavigationView navigationView = null;

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_player);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions


        //JVG.S
        ImageButton back = (ImageButton) findViewById(R.id.back);
        backButton = (ImageButton) findViewById(R.id.back);
        backButton.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
//                goToPreviusCategory();
                onBackPressed();
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.addDrawerListener(toggle);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
//        toggle.setDrawerSlideAnimationEnabled(false);
        toggle.setDrawerIndicatorEnabled(false);
//        toggle.setHomeAsUpIndicator(R.drawable.avatar_1);

        navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.theme_blue_text));
//        avatar = (AvatarView) (navigationView.getHeaderView(0).findViewById(R.id.avatar));
//        title = (TextView) (navigationView.getHeaderView(0).findViewById(R.id.title));
        toggle.syncState();
        //JVG.E
        // JTG.S
        MainDialogFragment dialog = new MainDialogFragment();
        dialog.show(getSupportFragmentManager(), "diálogo principal");
        // JTG.E
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

//    @Override
//    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
//        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
//        if (fragment != null) {
//            fragment.onActivityResult(requestCode, resultCode, data);
//        }
//    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
//            case R.id.sign_out: {
//
//                return true;
//            }
            //JVG.E
        }
        return super.onOptionsItemSelected(item);
    }

//    //JVG.S
//    private void showActivityTreeView() {
//        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
//            ActivityCompat.startActivity(this, new Intent(this, CategorySelectionTreeViewActivity.class),
//                    ActivityOptions.makeSceneTransitionAnimation(this).toBundle());
//        } else {
//
//            ActivityCompat.startActivity(this, new Intent(this, CategorySelectionTreeViewActivity.class),
//                    null);
//        }
//    }
//    //JVG.E

    @SuppressLint("NewApi")
    private void signOut() {
        PreferencesHelper.signOut(this);
//        JVG.S
//        TopekaDatabaseHelper.reset(this);
        TrivialJSonHelper.getInstance(this, false).signOut(getBaseContext());

        dissmissDialogs();

        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment instanceof CategorySelectionFragment) {
            ((CategorySelectionFragment) fragment).getAdapter().notifyDataSetChanged();
        }
//      JVG.E
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            getWindow().setExitTransition(TransitionInflater.from(this)
                    .inflateTransition(R.transition.category_enter));
        }
        SignInActivity.start(this, false);
        finish();
    }

    // JVG.S
    private void dissmissDialogs() {
        dismissSnackbar();
        dismissProgressDialog();
    }

    private void dismissSnackbar() {
        if (snackbar != null) {
            snackbar.dismiss();
            snackbar = null;
        }
    }

    private void dismissProgressDialog() {
        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
    }
    // JVG.E

    public void attachCategoryGridFragment() {
        if (TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded()) {
//            Log.w(TAG, "attachCategoryGridFragment");
            TrivialJSonHelper.getInstance(CategorySelectionActivity.this, false).moveCurrentCategoryToInit();
            FragmentManager supportFragmentManager = getSupportFragmentManager();
            Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
            if (!(fragment instanceof CategorySelectionFragment)) {
                fragment = CategorySelectionFragment.newInstance();
            }
            supportFragmentManager
                    .beginTransaction().replace(R.id.category_container, fragment)
                    .commit();
            backButton.setVisibility(View.GONE);
            scoreView.setVisibility(View.VISIBLE);
            showScore();
        }
    }

    public void attachTreeViewFragment(String mode) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
//        Log.w(TAG, "attachTreeViewFragment");
        if (!(fragment instanceof CategorySelectionTreeViewFragment) ||
                !((CategorySelectionTreeViewFragment) fragment).getMode().equals(mode)) {
            fragment = CategorySelectionTreeViewFragment.newInstance(mode);
        } else {
            ((CategorySelectionTreeViewFragment) fragment).changeMode(mode);
        }

        // Animate
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            if (mode.equals(ARG_ONE_PLAYER)) {
                Fade slideRight = new Fade(Fade.IN);
                slideRight.setDuration(600);
                fragment.setEnterTransition(slideRight);
                Fade fade = new Fade();
                fade.setDuration(100);
                fragment.setReturnTransition(fade);
            } else if (mode.equals(ARG_REAL_TIME_ONLINE)) {
                Slide slideRight = new Slide(Gravity.START);
                slideRight.setDuration(400);
                fragment.setEnterTransition(slideRight);
                Fade fade = new Fade();
                fade.setDuration(100);
                fragment.setReturnTransition(fade);
            } else if (mode.equals(ARG_TURNED_BASED_ONLINE)) {
                Fade slideRight = new Fade(Fade.IN);
                slideRight.setDuration(600);
                fragment.setEnterTransition(slideRight);
                Fade fade = new Fade();
                fade.setDuration(100);
                fragment.setReturnTransition(fade);
            }
        }

        if (mode.equals(QuizActivity.ARG_ONE_PLAYER)) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.category_container, fragment)
                    .commit();
        } else if (mode.equals(QuizActivity.ARG_REAL_TIME_ONLINE)) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.category_container, fragment).addToBackStack(null)
                    .commit();
        } else if (mode.equals(QuizActivity.ARG_TURNED_BASED_ONLINE)) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.category_container, fragment).addToBackStack(null)
                    .commit();
        }
        scoreView.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
//        subcategory_title.setVisibility(View.GONE);
    }

    public void attachPlayOnlineFragment(String mode) {
//        Log.w(TAG, "attachPlayOnlineFragment");
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof PlayRealTimeFragment))
            fragment = PlayRealTimeFragment.newInstance(mode);

        /* Animate*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slideRight = new Slide(Gravity.START);
            slideRight.setDuration(400);
            fragment.setEnterTransition(slideRight);
            Fade fade = new Fade();
            fade.setDuration(100);
            fragment.setReturnTransition(fade);
        }

        supportFragmentManager.beginTransaction().replace(R.id.category_container, fragment).commit();
        scoreView.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
//        subcategory_title.setVisibility(View.GONE);
    }

    public void attachPlayTurnBasedFragment(String mode) {
//        Log.w(TAG, "attachPlayTurnBasedFragment");
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof PlayTurnBasedFragment))
            fragment = PlayTurnBasedFragment.newInstance(mode);

        /* Animate*/
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
            Slide slideRight = new Slide(Gravity.START);
            slideRight.setDuration(400);
            fragment.setEnterTransition(slideRight);
            Fade fade = new Fade();
            fade.setDuration(100);
            fragment.setReturnTransition(fade);
        }

        supportFragmentManager.beginTransaction().replace(R.id.category_container, fragment).commit();
        scoreView.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
//        subcategory_title.setVisibility(View.GONE);
    }

//        /// JVG.S
//    private void setProgressBarVisibility(int visibility) {
//        /// findViewById(R.id.progress).setVisibility(visibility);
//    }
//        /// JVG.E

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);

        fragmentNameSaved = savedInstanceState.getString("fragment", "");
//        Log.d("TRAZA", "onRestore");

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
//        Log.d("TRAZA", "onSave");
        Fragment tmpFragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (tmpFragment != null)
            outState.putString("fragment", tmpFragment.getClass().getName().toString());
    }

    @Override
    protected void onStop() {
        //  JVG.S
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        dissmissDialogs();
        //  JVG.E

        super.onStop();
    }

    // Scale in X and Y a view, with a duration and a start delay
    public static void animateViewFullScaleXY(View view, int startDelay, int duration) {
        view.setScaleX(0f);
        view.setScaleY(0f);

        ViewCompat.animate(view)
                .setDuration(duration)
                .setStartDelay(startDelay)
                .scaleX(1f)
                .scaleY(1f)
                .alpha(1f);
    }

    // JVG.S
    public void animateToolbarNavigateCategories(boolean showScore) {
        backButton.setVisibility(View.GONE);
        if (showScore)
            scoreView.setVisibility(View.VISIBLE);

        animateViewFullScaleXY(subcategory_title, 200, 300);

        animateViewFullScaleXY(scoreView, 400, 300);


        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_UNLOCKED);
        toggle.setDrawerIndicatorEnabled(true);
    }

    public void animateToolbarNavigateToSubcategories(boolean showScore) {
        backButton.setVisibility(View.VISIBLE);

        if (showScore)
            scoreView.setVisibility(View.VISIBLE);

        animateViewFullScaleXY(backButton, 200, 300);

        animateViewFullScaleXY(subcategory_title, 300, 300);

        animateViewFullScaleXY(scoreView, 400, 300);

        drawer.setDrawerLockMode(DrawerLayout.LOCK_MODE_LOCKED_CLOSED);
        toggle.setDrawerIndicatorEnabled(false);

    }
    //JVG.E

    @SuppressWarnings("StatementWithEmptyBody")
    @Override
    public boolean onNavigationItemSelected(MenuItem item) {
        // Handle navigation view item clicks here.
        int id = item.getItemId();

        if (id == R.id.nav_category_selection) {
            // Handle the camera action
            attachCategoryGridFragment();
        } else if (id == R.id.nav_tree_view_play_offline) {
            attachTreeViewFragment(ARG_ONE_PLAYER);

        } else if (id == R.id.nav_tree_view_quick_match) {
//          //  Intent starter = new Intent(this, GPGActivity.class);
//            starter.putExtra(EXTRA_PLAYER, player);
//            return starter;
//            attachTreeViewFragment(QuizActivity.ARG_REAL_TIME_ONLINE);
            attachPlayOnlineFragment(QuizActivity.ARG_REAL_TIME_ONLINE);

        } else if (id == R.id.nav_tree_view_turn_base_multplayer) {
//          //  Intent starter = new Intent(this, GPGActivity.class);
//            starter.putExtra(EXTRA_PLAYER, player);
//            return starter;
//            attachTreeViewFragment(QuizActivity.ARG_REAL_TIME_ONLINE);
            attachPlayTurnBasedFragment(QuizActivity.ARG_TURNED_BASED_ONLINE);
        } else if (id == R.id.nav_signout) {
            signOut();
        } else if (id == R.id.nav_settings) {
            Intent startIntent = SettingsActivity.getStartIntent(this);
            if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP) {
                ActivityCompat.startActivity(this, startIntent, null);
            } else {
                ActivityCompat.startActivity(this, startIntent, null);
            }
//JTG.S
        } else if (id == R.id.nav_about) {
            AboutDialogFragment dialog = new AboutDialogFragment();
            dialog.show(getSupportFragmentManager(), "about dialog");
        } else if (id == R.id.nav_help) {
            HelpDialogFragment dialog = new HelpDialogFragment();
            dialog.show(getSupportFragmentManager(), "help dialog");
        }
//JTG.E
        drawer.closeDrawer(GravityCompat.START);
        return true;
    }

    // Disconect from GPG
    @Override
    protected void onDestroy() {
        // stop GoogleApiClient
//        if (Game.mGoogleApiClient != null && Game.mGoogleApiClient.isConnected()) {
//            Games.signOut(mGoogleApiClient);
//            Game.mGoogleApiClient.disconnect();
//        }
        //Log.d("DESTROY CALLED","DESTROY");
        super.onDestroy();
    }
}

