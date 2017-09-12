package com.trivial.upv.android.activity;

import android.app.AlertDialog;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.os.Bundle;
import android.preference.EditTextPreference;
import android.preference.Preference;
import android.preference.PreferenceFragment;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.text.Html;
import android.view.MenuItem;
import android.view.View;

import com.trivial.upv.android.R;

/**
 * Created by jvg63 on 05/09/2017.
 */

public class SettingsActivity extends AppCompatActivity {
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.preferences_activity);

//        getSupportActionBar().setTitle(Html.fromHtml("<font color='#FFFFFF'>Settings</font>"));
//        getSupportActionBar().setHomeAsUpIndicator(new ColorDrawable(Color.parseColor("#FFFFFF")));
        // Display the fragment as the main content.

        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        getSupportActionBar().setDisplayHomeAsUpEnabled(true);
//        toolbar.setBackgroundColor(Color.BLUE);
//        toolbar.setPopupTheme(R.style.AppTheme_PopupOverlay);
        toolbar.setVisibility(View.VISIBLE);

        getFragmentManager().beginTransaction()
                .replace(R.id.content, new SettingsFragment())
                .commit();
    }



    public static Intent getStartIntent(Context context) {
        Intent starter = new Intent(context, SettingsActivity.class);
//        starter.putExtra(Category.TAG, category.getId());
        return starter;
    }


    public static class SettingsFragment extends PreferenceFragment {
        @Override
        public void onCreate(Bundle savedInstanceState) {
            super.onCreate(savedInstanceState);

            // Load the preferences from an XML resource
            addPreferencesFromResource(R.xml.preferences);


            EditTextPreference edit_Pref = (EditTextPreference)
                    getPreferenceScreen().findPreference("url_categories");
            edit_Pref.setOnPreferenceChangeListener(new Preference.OnPreferenceChangeListener() {

                @Override
                public boolean onPreferenceChange(Preference preference, Object newValue) {
                    String value = (String) newValue;
                    // put validation here..
                    if (value == null || value.isEmpty() || !value.startsWith("http"))
                        return false;
                    return true;
                }
            });
        }
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        switch (item.getItemId()) {
            case android.R.id.home:
                // app icon in action bar clicked; goto parent activity.
                this.finish();
                return true;
            default:
                return super.onOptionsItemSelected(item);
        }
    }
}

