/*
 * Copyright (c) 2016  Sebastian Paarmann
 * Licensed under the MIT license, see the LICENSE file
 */

package de.s_paarmann.homeworkapp.ui;

import android.app.Fragment;
import android.app.FragmentManager;
import android.app.FragmentTransaction;
import android.content.Intent;
import android.content.res.Configuration;
import android.os.Bundle;
import android.os.PersistableBundle;
import android.preference.PreferenceManager;
import android.support.design.widget.NavigationView;
import android.support.v4.view.GravityCompat;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.view.Menu;
import android.view.MenuItem;
import android.view.View;
import android.view.inputmethod.InputMethodManager;

import com.bugsnag.android.Bugsnag;

import de.s_paarmann.homeworkapp.Log;
import de.s_paarmann.homeworkapp.R;
import de.s_paarmann.homeworkapp.network.LoginManager;
import de.s_paarmann.homeworkapp.ui.login.LoginActivity;


public class MainActivity extends AppCompatActivity {
  public static final String TAG = "MainActivity";

  public static final String ACTION_REMIND = "de.s_paarmann.homeworkapp.action.REMIND";
  public static final String ACTION_UPDATEHOMEWORK = "de.s_paarmann.homeworkapp.action.UPDATEHOMEWORK";

  public static final String PREF_NAME = "de.s_paarmann.homeworkapp.sharedprefs";
  public static final String PREF_UPDATED = "de.s_paarmann.homeworkapp.updated";
  public static final String PREF_SETREMINDERS = "de.s_paarmann.homeworkapp.reminders";
  public static final String PREF_DELETEDREMINDERS = "de.s_paarmann.homeworkapp.deletedreminders";
  public static final String PREF_LASTUPDATED = "de.s_paarmann.homeworkapp.lastupdated";
  public static final String PREF_DONEITEMS = "de.s_paarmann.homeworkapp.doneitems";
  public static final String PREF_DONEITEMS2 = "de.s_paarmann.homeworkapp.doneitems2";
  public static final String PREF_FILTERSUBJECTS = "de.s_paarmann.homeworkapp.FilterSubjects";
  public static final String PREF_CHOSENSUBJECTS = "de.s_paarmann.homeworkapp.ChosenSubjects";
  public static final String PREF_HOMEWORKTODAY = "de.s_paarmann.homeworkapp.HomeworkToday";
  public static final String PREF_AUTOUPDATES = "de.s_paarmann.homeworkapp.AutomaticUpdates";
  public static final String PREF_AUTOREMINDERS = "de.s_paarmann.homeworkapp.AutomaticReminders";
  public static final String PREF_REMINDERTIME = "de.s_paarmann.homeworkapp.ReminderTime";
  public static final String PREF_REMINDERDAY = "de.s_paarmann.homeworkapp.ReminderDay";
  public static final String PREF_AUTOREMINDERSINSTANT = "de.s_paarmann.homeworkapp.InstantAutomaticReminders";
  public static final String PREF_MOBILEDATA = "de.s_paarmann.homeworkapp.UseMobileData";
  public static final String PREF_CRED_GROUP = "de.s_paarmann.homeworkapp.credgroup";
  public static final String PREF_CRED_USER = "de.s_paarmann.homeworkapp.creduser";
  public static final String PREF_CRED_AUTH = "de.s_paarmann.homeworkapp.credauth";
  public static final String PREF_CRED_TOKEN = "de.s_paarmann.homeworkapp.credtoken";
  public static final String PREF_PROVIDER = "de.s_paarmann.homeworkapp.provider";

  public static final String EXTRA_OPEN_VIEW = "de.s_paarmann.homeworkapp.extra.openview";

  private static final String STATE_VIEW = "view";

  public enum Views {
    MAIN, HOMEWORK_DETAIL, ADD_HOMEWORK, MANAGE_REMINDERS, SETTINGS
  }

  private Views currentView;

  public Fragment fragment;
  private DrawerLayout drawerLayout;
  private NavigationView navigationView;
  private ActionBarDrawerToggle drawerToggle;

  @Override
  protected void onCreate(Bundle savedInstanceState) {
    Bugsnag.init(this);

    super.onCreate(savedInstanceState);
    setContentView(R.layout.activity_main);

    Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
    setSupportActionBar(toolbar);
    getSupportActionBar().setDisplayHomeAsUpEnabled(true);
    getSupportActionBar().setHomeAsUpIndicator(R.drawable.ic_drawer);

    drawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
    navigationView = (NavigationView) findViewById(R.id.navigation);
    drawerLayout.setDrawerShadow(R.drawable.drawer_shadow, GravityCompat.START);

    navigationView.setNavigationItemSelectedListener(item -> {
      switch (item.getItemId()) {
        case R.id.nav_home:
          showView(Views.MAIN);
          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        case R.id.nav_add_homework:
          showView(Views.ADD_HOMEWORK);
          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        case R.id.nav_reminders:
          showView(Views.MANAGE_REMINDERS);
          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        case R.id.nav_logout:
          // TODO
          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        case R.id.nav_settings:
          showView(Views.SETTINGS);
          drawerLayout.closeDrawer(GravityCompat.START);
          return true;
        default:
          return false;
      }
    });

    // TODO
    drawerToggle = new ActionBarDrawerToggle(this, drawerLayout, toolbar,
      R.string.drawer_open, R.string.drawer_close) {
      public void onDrawerClosed(View view) {
        invalidateOptionsMenu();
      }
      public void onDrawerOpened(View drawerView) {
        invalidateOptionsMenu();
      }
    };
    drawerLayout.addDrawerListener(drawerToggle);

    getFragmentManager().addOnBackStackChangedListener(this::onBackStackChanged);

    if (getIntent().hasExtra(EXTRA_OPEN_VIEW)) {
      showView(Views.valueOf(getIntent().getStringExtra(EXTRA_OPEN_VIEW)));
    } else if (savedInstanceState != null && savedInstanceState.containsKey(STATE_VIEW)) {
      showView(Views.valueOf(savedInstanceState.getString(STATE_VIEW)));
    } else {
      showMainView();
    }

    PreferenceManager.setDefaultValues(this, R.xml.preferences, false);

    if (!LoginManager.loadCredentials(this)) {
      Intent intent = new Intent(this, LoginActivity.class);
      startActivity(intent);
    }
  }

  @Override
  protected void onSaveInstanceState(Bundle outState) {
    outState.putString(STATE_VIEW, currentView.name());

    super.onSaveInstanceState(outState);
  }

  @Override
  public void onPostCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
    super.onPostCreate(savedInstanceState, persistentState);
    drawerToggle.syncState();
  }

  @Override
  public boolean onCreateOptionsMenu(Menu menu) {
    // Inflate the menu; this adds items to the action bar if it is present.
    switch (currentView) {
      case MAIN:
        getMenuInflater().inflate(R.menu.main, menu);
        break;
      case HOMEWORK_DETAIL:
        getMenuInflater().inflate(R.menu.homework_detail, menu);
        break;
      case ADD_HOMEWORK:
      case MANAGE_REMINDERS:
      case SETTINGS:
        // No menu
        super.onCreateOptionsMenu(menu);
        break;
    }
    return true;
  }

  @Override
  public boolean onOptionsItemSelected(MenuItem item) {
    // Handle action bar item clicks here. The action bar will
    // automatically handle clicks on the Home/Up button, so long
    // as you specify a parent activity in AndroidManifest.xml.
    if (drawerToggle.onOptionsItemSelected(item)) {
      return true;
    }

    int id = item.getItemId();
    switch (id) {
      case R.id.action_update:
        if (currentView == Views.MAIN) {
          ((MainFragment) fragment).checkForUpdates(true);
        }
        return true;
      default:
        return super.onOptionsItemSelected(item);
    }
  }

  private void onBackStackChanged() {
    Fragment frag = getFragmentManager().findFragmentById(R.id.content_frame);

    if (frag instanceof MainFragment)
      currentView = Views.MAIN;
    else if (frag instanceof HomeworkDetailFragment)
      currentView = Views.HOMEWORK_DETAIL;
    else if (frag instanceof AddHomeworkFragment)
      currentView = Views.ADD_HOMEWORK;
    else if (frag instanceof ManagerRemindersFragment)
      currentView = Views.MANAGE_REMINDERS;
    else if (frag instanceof SettingsFragment)
      currentView = Views.SETTINGS;
    else
      Log.w(TAG, "Fragment displayed not recognized!");

    updateNavDrawerSelection(currentView);
  }

  private void showView(Fragment frag, Views view) {
    if (getCurrentFocus() != null) {
      InputMethodManager imm = (InputMethodManager) getSystemService(INPUT_METHOD_SERVICE);
      imm.hideSoftInputFromWindow(getCurrentFocus().getWindowToken(), 0);
    }

    fragment = frag;
    FragmentManager manager = getFragmentManager();
    FragmentTransaction
        transaction =
        manager.beginTransaction().replace(R.id.content_frame, fragment);
    if ((currentView == Views.MAIN
         || view == Views.ADD_HOMEWORK
         || view == Views.MANAGE_REMINDERS
         || view == Views.SETTINGS)
        && view != Views.MAIN
        && currentView != view) {
      transaction.addToBackStack(null);
    }
    transaction.commit();

    currentView = view;
    invalidateOptionsMenu();

    updateNavDrawerSelection(view);
  }

  private void updateNavDrawerSelection(Views view) {
    switch (view) {
      case MAIN:
        navigationView.setCheckedItem(R.id.nav_home);
        break;
      case HOMEWORK_DETAIL:
        navigationView.setCheckedItem(R.id.nav_home);
        break;
      case ADD_HOMEWORK:
        navigationView.setCheckedItem(R.id.nav_add_homework);
        break;
      case MANAGE_REMINDERS:
        navigationView.setCheckedItem(R.id.nav_reminders);
        break;
      case SETTINGS:
        navigationView.setCheckedItem(R.id.nav_settings);
        break;
    }
  }

  private void showView(Views view) {
    switch (view) {
      case MAIN:
        showMainView();
        break;
      case HOMEWORK_DETAIL:
        showHomeworkDetailView(getIntent().getStringExtra(HomeworkDetailFragment.EXTRA_DATE));
        break;
      case ADD_HOMEWORK:
        showAddHomeworkView();
        break;
      case MANAGE_REMINDERS:
        showRemindersView();
        break;
      case SETTINGS:
        showSettingsView();
        break;
    }
  }

  public void showMainView() {
    showView(new MainFragment(), Views.MAIN);
  }

  public void showHomeworkDetailView(String strDate) {
    Fragment frag = new HomeworkDetailFragment();
    Bundle args = new Bundle();
    if (strDate == null) {
      args.putString(HomeworkDetailFragment.EXTRA_DATE, "all");
    } else {
      args.putString(HomeworkDetailFragment.EXTRA_DATE, strDate);
    }
    frag.setArguments(args);

    showView(frag, Views.HOMEWORK_DETAIL);
  }

  public void showAddHomeworkView() {
    showView(new AddHomeworkFragment(), Views.ADD_HOMEWORK);
  }

  public void showRemindersView() {
    showView(new ManagerRemindersFragment(), Views.MANAGE_REMINDERS);
  }

  public void showSettingsView() {
    showView(new SettingsFragment(), Views.SETTINGS);
  }

  @Override
  public void onConfigurationChanged(Configuration newConfig) {
    super.onConfigurationChanged(newConfig);
    drawerToggle.onConfigurationChanged(newConfig);
  }

}
