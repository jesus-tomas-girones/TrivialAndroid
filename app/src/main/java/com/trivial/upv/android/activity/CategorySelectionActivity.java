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
import android.content.DialogInterface;
import android.content.Intent;
import android.content.IntentFilter;
import android.databinding.DataBindingUtil;
import android.os.Build;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.transition.TransitionInflater;
import android.util.Log;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.widget.TextView;
import android.widget.Toast;

import com.android.volley.Request;
import com.android.volley.RequestQueue;
import com.trivial.upv.android.R;
import com.trivial.upv.android.databinding.ActivityCategorySelectionBinding;
import com.trivial.upv.android.fragment.CategorySelectionFragment;
import com.trivial.upv.android.helper.ApiLevelHelper;
import com.trivial.upv.android.helper.PreferencesHelper;
import com.trivial.upv.android.helper.singleton.VolleySingleton;
import com.trivial.upv.android.model.Player;
import com.trivial.upv.android.persistence.TopekaJSonHelper;

import static com.trivial.upv.android.persistence.TopekaJSonHelper.ACTION_RESP;

public class CategorySelectionActivity extends AppCompatActivity {

    //JVG.S
    public static final String CARGA_JSON = "CARGA_JSON";
    //JVG.E
    private static final String EXTRA_PLAYER = "player";

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
        binding.setPlayer(player);
        setUpToolbar();
        // JVG.S
        initActivity(savedInstanceState);
        //JVG.E

        // JVG.S
        pDialog = new ProgressDialog(this);
        pDialog.setMessage("Cargando...");
        pDialog.setIndeterminate(false);
        pDialog.setProgressStyle(ProgressDialog.STYLE_HORIZONTAL);
        pDialog.setProgress(0);
        pDialog.setMax(100);
        pDialog.setCancelable(true);
        pDialog.setOnCancelListener(new DialogInterface.OnCancelListener() {
            @Override
            public void onCancel(DialogInterface dialog) {
                if (pDialog != null) {
                    pDialog.dismiss();
                    pDialog = null;
                }
                VolleySingleton.getColaPeticiones().

                        cancelAll(new RequestQueue.RequestFilter() {
                            @Override
                            public boolean apply(Request<?> request) {
                                return true;
                            }
                        });
            }
        });
        pDialog.show();

        filtro = new IntentFilter(ACTION_RESP); filtro.addCategory(Intent.CATEGORY_DEFAULT);
        receiver = new ReceptorOperacion();
        TopekaJSonHelper.getInstance(this);

        // JVG.E
    }

    IntentFilter filtro;

    ReceptorOperacion receiver = null;

    public class ReceptorOperacion extends BroadcastReceiver {

        @Override
        public void onReceive(Context context, Intent intent) {

            synchronized (this) {
                String result = intent.getExtras().getString("RESULT");


                if ("OK".equals(result)) {
                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;
                    }

                    // Carga categorias
                    Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
                    if (fragment instanceof CategorySelectionFragment) {
                        ((CategorySelectionFragment) fragment).setUpQuizGrid();
                    }

                } else if ("REFRESH".equals(result)) {
                    if (pDialog != null) {
                        pDialog.setProgress(intent.getExtras().getInt("REFRESH", 0));
                    }
                } else if ("ERROR".equals(result)) {
                    Toast.makeText(getBaseContext(), "Ha ocurrido un error cargando las categorías", Toast.LENGTH_SHORT).show();
                    if (pDialog != null) {
                        pDialog.dismiss();
                        pDialog = null;

                    }
                    VolleySingleton.getColaPeticiones().cancelAll(new RequestQueue.RequestFilter() {
                        @Override
                        public boolean apply(Request<?> request) {
                            return true;
                        }
                    });
                    Log.d("ONRECEIVE", intent.getExtras().getString("RESULT"));
                }
            }
        }

    }


    private void initActivity(Bundle savedInstanceState) {
        if (savedInstanceState == null) {
            attachCategoryGridFragment();
        } else {
            setProgressBarVisibility(View.GONE);
        }
        supportPostponeEnterTransition();
    }

    @Override
    public void onBackPressed() {
        if (!TopekaJSonHelper.thereAreMorePreviusCategories()) {
            super.onBackPressed();
        } else {
            Fragment fragment = getSupportFragmentManager().findFragmentById(R.id.category_container);
            if (fragment instanceof CategorySelectionFragment) {
                TopekaJSonHelper.navigatePreviusCategory();
                ((CategorySelectionFragment) fragment).getAdapter().updateCategories();
                ((CategorySelectionFragment) fragment).getAdapter().notifyDataSetChanged();
                ((CategorySelectionFragment) fragment).getAdapter().notifyDataSetChanged();
            }
        }
    }

    public ProgressDialog pDialog;
    //JVG.S


    //JVG.E

    @Override
    protected void onResume() {
        super.onResume();
        TextView scoreView = (TextView) findViewById(R.id.score);
//      JVG.S
//        final int score = TopekaDatabaseHelper.getScore(this);
        final int score = TopekaJSonHelper.getScore();

//      JVG.E
        scoreView.setText(getString(R.string.x_points, score));
    }

    /*JVG.S*/
    @Override
    protected void onStart() {
        super.onStart();
        registerReceiver(receiver, filtro);
    } //JVG.E

    private void setUpToolbar() {
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar_player);
        setSupportActionBar(toolbar);
        //noinspection ConstantConditions
        getSupportActionBar().setDisplayShowTitleEnabled(false);
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
            case R.id.sign_out: {
                signOut();
                return true;
            }
        }
        return super.onOptionsItemSelected(item);
    }

    @SuppressLint("NewApi")
    private void signOut() {
        PreferencesHelper.signOut(this);
//        JVG.S
//        TopekaDatabaseHelper.reset(this);
        TopekaJSonHelper.reset();
//        JVG.E
        if (ApiLevelHelper.isAtLeast(Build.VERSION_CODES.LOLLIPOP)) {
            getWindow().setExitTransition(TransitionInflater.from(this)
                    .inflateTransition(R.transition.category_enter));
        }
        SignInActivity.start(this, false);
        finish();
    }

    private void attachCategoryGridFragment() {
        FragmentManager supportFragmentManager = getSupportFragmentManager();
        Fragment fragment = supportFragmentManager.findFragmentById(R.id.category_container);
        if (!(fragment instanceof CategorySelectionFragment)) {
            fragment = CategorySelectionFragment.newInstance();
        }
        supportFragmentManager.beginTransaction()
                .replace(R.id.category_container, fragment)
                .commit();
        setProgressBarVisibility(View.GONE);
    }

    private void setProgressBarVisibility(int visibility) {
        findViewById(R.id.progress).setVisibility(visibility);
    }

    @Override
    protected void onRestoreInstanceState(Bundle savedInstanceState) {
        super.onRestoreInstanceState(savedInstanceState);
    }

    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);

        if (pDialog != null) {
            pDialog.dismiss();
            pDialog = null;
        }
        VolleySingleton.getColaPeticiones().cancelAll(new RequestQueue.RequestFilter() {
            @Override
            public boolean apply(Request<?> request) {
                return true;
            }
        });
    }

    @Override
    protected void onStop() {
        if (receiver != null) {
            unregisterReceiver(receiver);
            receiver = null;
        }
        super.onStop();
    }
}

