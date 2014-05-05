package com.heimcontrol.mobile;

import android.app.ActionBar;
import android.app.Fragment;
import android.app.FragmentTransaction;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.preference.PreferenceManager;
import android.support.v13.app.FragmentPagerAdapter;
import android.support.v4.app.FragmentActivity;
import android.support.v4.view.ViewPager;
import android.view.Menu;
import android.view.MenuItem;

import com.loopj.android.http.JsonHttpResponseHandler;

import org.apache.http.Header;
import org.apache.http.entity.StringEntity;
import org.json.JSONException;
import org.json.JSONObject;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;

import de.keyboardsurfer.android.widget.crouton.Crouton;
import de.keyboardsurfer.android.widget.crouton.Style;


public class MainActivity extends FragmentActivity {

    ViewPager mViewPager;
    TabsAdapter mTabsAdapter;
    Context context;
    private String key;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        mViewPager = new ViewPager(this);
        mViewPager.setId(R.id.viewpager);
        setContentView(mViewPager);
        context = getApplicationContext();


        setKey(((Heimcontrol) context).user.getKey());

        final ActionBar bar = getActionBar();
        bar.setNavigationMode(ActionBar.NAVIGATION_MODE_TABS);
        bar.setDisplayOptions(0, ActionBar.DISPLAY_SHOW_TITLE);

        mTabsAdapter = new TabsAdapter(this, mViewPager);
        mTabsAdapter.addTab(bar.newTab().setText("GPIO"),
                Switches.class, null, getApplicationContext());
//        mTabsAdapter.addTab(bar.newTab().setText("RC"),
//                RCSwitches.class, null, getApplicationContext());

        if (savedInstanceState != null) {
            bar.setSelectedNavigationItem(savedInstanceState.getInt("tab", 0));
        }

        // Look out for conn changes
        ConnectivityChangeReceiver.enable(getApplicationContext());
        // Login to heimcontrol
        authenticate();

    }


/*
    public void logout() {
        ((Heimcontrol) getApplicationContext()).user.setKey("");
        Intent intent = new Intent(this, MainActivity.class);
        startActivity(intent);
        finish();
    }*/


    @Override
    protected void onSaveInstanceState(Bundle outState) {
        super.onSaveInstanceState(outState);
        outState.putInt("tab", getActionBar().getSelectedNavigationIndex());
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.switches, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        int id = item.getItemId();
        if (id == R.id.action_settings) {
            Intent intent = new Intent(this, SettingsActivity.class);
            startActivity(intent);
            return true;
        } else if (id == R.id.action_refresh) {
            for (int i = 0; i < mTabsAdapter.fragments.size(); i++) {
                RefreshInterface tab = (RefreshInterface) mTabsAdapter.fragments.get(i);
                tab.refresh();
            }
            return true;
        } else if (id == R.id.action_logout) {
            // this.logout();
        }
        return super.onOptionsItemSelected(item);
    }

    public String getKey() {
        return key;
    }

    public void setKey(String key) {
        this.key = key;
    }

    private void authenticate() {
        SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(this);
        String username = prefs.getString("pref_key_username", "");
        String passwordText = prefs.getString("pref_key_password", "");

        JSONObject params = new JSONObject();

        try {
            params.put("email", username);
            params.put("password", passwordText);
        } catch (JSONException e) {
            e.printStackTrace();
        }

        StringEntity entity = null;
        try {
            entity = new StringEntity(params.toString());
        } catch (UnsupportedEncodingException e) {
            e.printStackTrace();
        }

        RestClient.setBaseUrl(prefs.getString("home_url", ""));

        RestClient.postJSON(
                getApplicationContext(),
                "api/login",
                entity,
                new JsonHttpResponseHandler() {
                    @Override
                    public void onSuccess(JSONObject responseData) {
                        try {
                            String applicationKey = responseData.getString("token");
                            Heimcontrol.user.setKey(applicationKey);
                            //MainActivity.this.loggedIn();
                        } catch (JSONException e) {
                            // TODO Auto-generated catch block
                            e.printStackTrace();
                        }
                    }

                    @Override
                    public void onFailure(int statusCode, Header[] headers, byte[] responseBody, Throwable error) {
                        if (statusCode == 401) {
                            CharSequence text = statusCode + ": Wrong email or password";
                            Crouton.makeText(MainActivity.this, text, Style.ALERT).show();
                        } else {
                            CharSequence text = "Error " + statusCode + " while trying to log in";
                            Crouton.makeText(MainActivity.this, text, Style.ALERT).show();
                        }
                    }
                }
        );
    }

//    public void loggedIn() {
//        String key = ((Heimcontrol) getApplicationContext()).user.getKey();
//        if (key == "") {
//            CharSequence text = "Error!";
//            Crouton.makeText(MainActivity.this, text, Style.ALERT).show();
//        }
//    }

    /**
     * This is a helper class that implements the management of tabs and all
     * details of connecting a ViewPager with associated TabHost.  It relies on a
     * trick.  Normally a tab host has a simple API for supplying a View or
     * Intent that each tab will show.  This is not sufficient for switching
     * between pages.  So instead we make the content part of the tab host
     * 0dp high (it is not shown) and the TabsAdapter supplies its own dummy
     * view to show as the tab content.  It listens to changes in tabs, and takes
     * care of switch to the correct paged in the ViewPager whenever the selected
     * tab changes.
     */
    public static class TabsAdapter extends FragmentPagerAdapter
            implements ActionBar.TabListener, ViewPager.OnPageChangeListener {
        private final Context mContext;
        private final ActionBar mActionBar;
        private final ViewPager mViewPager;
        private final ArrayList<TabInfo> mTabs = new ArrayList<TabInfo>();
        private final ArrayList<Fragment> fragments = new ArrayList<android.app.Fragment>();


        public TabsAdapter(FragmentActivity activity, ViewPager pager) {
            super(activity.getFragmentManager());
            mContext = activity;
            mActionBar = activity.getActionBar();
            mViewPager = pager;
            mViewPager.setAdapter(this);
            mViewPager.setOnPageChangeListener(this);
        }

        @Override
        public android.app.Fragment getItem(int i) {
            return fragments.get(i);
        }

        public void addTab(ActionBar.Tab tab, Class<?> clss, Bundle args, Context context) {
            TabInfo info = new TabInfo(clss, args);
            fragments.add(android.app.Fragment.instantiate(context, clss.getName()));
            tab.setTag(info);
            tab.setTabListener(this);
            mTabs.add(info);
            mActionBar.addTab(tab);
            notifyDataSetChanged();
        }

        @Override
        public int getCount() {
            return mTabs.size();
        }

        @Override
        public void onPageScrolled(int position, float positionOffset, int positionOffsetPixels) {
        }

        @Override
        public void onPageSelected(int position) {
            mActionBar.setSelectedNavigationItem(position);
        }

        @Override
        public void onPageScrollStateChanged(int state) {
        }

        @Override
        public void onTabSelected(ActionBar.Tab tab, FragmentTransaction ft) {
            Object tag = tab.getTag();
            for (int i = 0; i < mTabs.size(); i++) {
                if (mTabs.get(i) == tag) {
                    mViewPager.setCurrentItem(i);
                }
            }
        }

        @Override
        public void onTabUnselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        @Override
        public void onTabReselected(ActionBar.Tab tab, FragmentTransaction ft) {
        }

        static final class TabInfo {
            private final Class<?> clss;
            private final Bundle args;

            TabInfo(Class<?> _class, Bundle _args) {
                clss = _class;
                args = _args;
            }
        }
    }

    @Override
    protected void onStop() {
        super.onStop();
        // Stop looking out for conn changes
        ConnectivityChangeReceiver.disable(getApplicationContext());
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

    }
}
