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
import android.app.ActivityOptions;
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
import android.text.method.LinkMovementMethod;
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

import com.google.android.gms.games.multiplayer.Invitation;
import com.google.android.gms.games.multiplayer.OnInvitationReceivedListener;
import com.trivial.upv.android.R;
import com.trivial.upv.android.databinding.ActivityCategorySelectionBinding;
import com.trivial.upv.android.databinding.NavHeaderCategorySelectionBinding;
import com.trivial.upv.android.fragment.AboutDialogFragment;
import com.trivial.upv.android.fragment.CategorySelectionFragment;
import com.trivial.upv.android.fragment.CategorySelectionTreeViewFragment;
import com.trivial.upv.android.fragment.HelpDialogFragment;
import com.trivial.upv.android.fragment.MainDialogFragment;
import com.trivial.upv.android.fragment.PlayOnlineFragment;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.PreferencesHelper;
import com.trivial.upv.android.model.Player;
import com.trivial.upv.android.model.gpg.Game;
import com.trivial.upv.android.persistence.TopekaJSonHelper;
import com.trivial.upv.android.widget.AvatarView;

import static com.trivial.upv.android.activity.QuizActivity.ARG_ONE_PLAYER;
import static com.trivial.upv.android.persistence.TopekaJSonHelper.ACTION_RESP;

public class CategorySelectionActivity extends AppCompatActivity implements NavigationView.OnNavigationItemSelectedListener, OnInvitationReceivedListener {

    private static final String EXTRA_PLAYER = "player";
    private TextView scoreView;
    private ImageButton backButton;
    private AvatarView avatar;
    private TextView title;

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
    }

    // JVG.S
    private void loadCategories() {
        // RECEIVER PARA ACTUALIZAR PROGRESO Y CARGA DE LAS CATEGORIAS
        if (filtro == null) {
            filtro = new IntentFilter(ACTION_RESP);
            filtro.addCategory(Intent.CATEGORY_DEFAULT);
        }
        if (receiver == null)
            receiver = new ReceptorOperacion();

//        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);

        if (!TopekaJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded()) {
            if (checkInternetAccess()) {
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
                        TopekaJSonHelper.getInstance(CategorySelectionActivity.this, true);

                    }
                }.start();

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
                (TopekaJSonHelper.getInstance(getBaseContext(), false)).deleteProgressCategory(position);

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

    @Override
    public void onInvitationReceived(final Invitation invitation) {
        OnClickSnackBarAction actionInvitation = new OnClickSnackBarAction() {
            @Override
            public void onClickAction() {
                Log.d("TRAZA", "OnInvitationReceived");
                Game.mIncomingInvitationId = invitation.getInvitationId();
                Game.category = TopekaJSonHelper.getInstance(getApplicationContext(), false).createCategoryPlayTimeReal(10, null);
                Intent startIntent = QuizActivity.getStartIntent(CategorySelectionActivity.this, Game.category);
                if (android.os.Build.VERSION.SDK_INT >= android.os.Build.VERSION_CODES.LOLLIPOP)
                    ActivityCompat.startActivity(CategorySelectionActivity.this, startIntent, ActivityOptions.makeSceneTransitionAnimation(CategorySelectionActivity.this).toBundle());
                else {
                    ActivityCompat.startActivity(CategorySelectionActivity.this, startIntent, null);
                }
            }
        };

        if (invitation != null && invitation.getInvitationId() != null)
            showSnackbarMessage("Invitation Received from " + invitation.getInviter().getDisplayName(), "Accept?", true, actionInvitation);

    }


    @Override
    public void onInvitationRemoved(String invitationId) {
        if (Game.mIncomingInvitationId != null && Game.mIncomingInvitationId.equals(invitationId)) {
            Game.mIncomingInvitationId = null;
        }
    }

    public interface OnClickSnackBarAction {
        public void onClickAction();
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


    public class ReceptorOperacion extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            synchronized (this) {
                String result = intent.getExtras().getString("RESULT");


                if ("OK".equals(result)) {
                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;
//                        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_UNSPECIFIED);
                    }

                    // Carga categorias
//                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
//                    if (fragment instanceof CategorySelectionFragment) {
//                        ((CategorySelectionFragment) fragment).animateTransitionSubcategories(null);

//                    }
                    attachCategoryGridFragment();

                    Log.d("ONRECEIVE", intent.getExtras().getString("RESULT"));

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

                    Log.d("ONRECEIVE", intent.getExtras().getString("RESULT"));

                    TopekaJSonHelper.getInstance(CategorySelectionActivity.this, false).resetData();

                    snackbar.show();
                }
            }
        }

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
        backButton = (ImageButton) findViewById(R.id.back);
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
                if (!TopekaJSonHelper.getInstance(getBaseContext(), false).thereAreMorePreviusCategories()) {
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

    private void goToPreviusCategory() {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment instanceof CategorySelectionFragment) {
            TopekaJSonHelper.getInstance(getBaseContext(), false).navigatePreviusCategory();
            ((CategorySelectionFragment) fragment).animateTransitionSubcategories(null);
            showToolbarSubcategories();
        } else {
            FragmentManager fm = getSupportFragmentManager();
            fm.popBackStack(null, FragmentManager.POP_BACK_STACK_INCLUSIVE);
        }
    }

    public void showToolbarSubcategories() {
        if (TopekaJSonHelper.getInstance(getBaseContext(), false).isInitCategory()) {
            setToolbarTitle(getResources().getString(R.string.menu_nd_category_review));
            animateToolbarNavigateCategories(true);

        } else {
//            TextView viewSubcategoryText = (TextView) findViewById(R.id.sub_category_title);
            setToolbarTitle(TopekaJSonHelper.getInstance(getBaseContext(), false).getPreviousTitleCategory());
            Log.d("previus", "categoria_previa" + TopekaJSonHelper.getInstance(getBaseContext(), false).getPreviousTitleCategory());
            animateToolbarNavigateToSubcategories(true);
        }
    }


    public ProgressDialog pDialog;
    //JVG.E

    @Override
    protected void onResume() {
        super.onResume();

        // Load a freme (restore activity)
        if (TopekaJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded() && getSupportFragmentManager().findFragmentById(R.id.category_container) == null && fragmentNameSaved.isEmpty()) {
            attachCategoryGridFragment();
        }
        showScore();
    }

    private void showScore() {
        final int score = TopekaJSonHelper.getInstance(getBaseContext(), false).getScore();
        if (scoreView != null) {
            scoreView.setText(getString(R.string.x_points, score));
        }
    }

    private boolean isFragmentCategorySelection() {
        return (getSupportFragmentManager().findFragmentById(R.id.category_container)) instanceof CategorySelectionFragment;
    }

    @Override
    protected void onStart() {
        super.onStart();
        // JVG.S
        Log.d("TRAZA", "onStart");
        loadCategories();
        registerReceiver(receiver, filtro);
       // JVG.E
       // JTG.S
       MainDialogFragment dialog = new MainDialogFragment();
       dialog.show(getSupportFragmentManager(), "diálogo principal");
       // JTG.E
    }

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_player);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions


        //JVG.S
        ImageButton back = (ImageButton) findViewById(R.id.back);
        back.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                goToPreviusCategory();
            }
        });

        drawer = (DrawerLayout) findViewById(R.id.drawer_layout);
        toggle = new ActionBarDrawerToggle(
                this, drawer, toolbar, R.string.navigation_drawer_open, R.string.navigation_drawer_close);
        drawer.setDrawerListener(toggle);

        getSupportActionBar().setDisplayShowTitleEnabled(false);
//        getSupportActionBar().setDefaultDisplayHomeAsUpEnabled(true);
//        toggle.setDrawerSlideAnimationEnabled(false);
        toggle.setDrawerIndicatorEnabled(false);
//        toggle.setHomeAsUpIndicator(R.drawable.avatar_1);

        NavigationView navigationView = (NavigationView) findViewById(R.id.nav_view);
        navigationView.setNavigationItemSelectedListener(this);
        navigationView.setBackgroundColor(ContextCompat.getColor(getBaseContext(), R.color.theme_blue_text));
//        avatar = (AvatarView) (navigationView.getHeaderView(0).findViewById(R.id.avatar));
//        title = (TextView) (navigationView.getHeaderView(0).findViewById(R.id.title));
        toggle.syncState();
        //JVG.E
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        getMenuInflater().inflate(R.menu.menu_category, menu);
        return true;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
        if (fragment != null) {
            fragment.onActivityResult(requestCode, resultCode, data);
        }
    }

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
        TopekaJSonHelper.getInstance(this, false).signOut(getBaseContext());

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
        if (TopekaJSonHelper.getInstance(CategorySelectionActivity.this, false).isLoaded()) {
            TopekaJSonHelper.getInstance(CategorySelectionActivity.this, false).moveCurrentCategoryToInit();
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
        if (!(fragment instanceof CategorySelectionTreeViewFragment) ||
                ((CategorySelectionTreeViewFragment) fragment).getMode() != mode) {
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
            } else {
                Slide slideRight = new Slide(Gravity.START);
                slideRight.setDuration(400);
                fragment.setEnterTransition(slideRight);
                Fade fade = new Fade();
                fade.setDuration(100);
                fragment.setReturnTransition(fade);
            }
        }

        if (mode == QuizActivity.ARG_ONE_PLAYER) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.category_container, fragment)
                    .commit();
        } else if (mode == QuizActivity.ARG_ONLINE) {
            supportFragmentManager.beginTransaction()
                    .replace(R.id.category_container, fragment).addToBackStack(null)
                    .commit();
        }
        scoreView.setVisibility(View.GONE);
        backButton.setVisibility(View.GONE);
//        subcategory_title.setVisibility(View.GONE);
    }

    public void attachPlayOnlineFragment(String mode) {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof PlayOnlineFragment))
            fragment = PlayOnlineFragment.newInstance();

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
        Log.d("TRAZA", "onRestore");

    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        Log.d("TRAZA", "onSave");
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
//            attachTreeViewFragment(QuizActivity.ARG_ONLINE);
            attachPlayOnlineFragment(QuizActivity.ARG_ONLINE);
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
        if (Game.mGoogleApiClient != null && Game.mGoogleApiClient.isConnected()) {
//            Games.signOut(mGoogleApiClient);
            Game.mGoogleApiClient.disconnect();
        }
        //Log.d("DESTROY CALLED","DESTROY");
        super.onDestroy();
    }
}

