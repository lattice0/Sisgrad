package com.lucaszanella.sisgrad;
import com.lucaszanella.SisgradCrawler.SisgradCrawler;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.design.widget.TabLayout;
import android.support.v4.app.Fragment;
import android.support.v4.app.FragmentManager;
import android.support.v4.app.FragmentPagerAdapter;
import android.support.v4.view.ViewPager;
import android.support.v4.widget.DrawerLayout;
import android.support.v7.app.ActionBar;
import android.support.v7.app.ActionBarDrawerToggle;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.Gravity;
import android.view.Menu;
import android.view.MenuItem;
import android.widget.ListView;

public class MainActivity extends AppCompatActivity implements MessagesFragment.onItemSelected {
    private SisgradCrawler login;
    private static final String LOG_TAG = "MainActivity";//tag for the Log.d function

    ActionBar bar;
    String[] pageTitles = {"Início", "Mensagens", "Notas", "Biblioteca"};

    /*
     * Method to handle when a message is selected from MessagesFragment's list of messages
     */
    public void onMessageSelected(String id, String provisoryTitle) {
        /*
         * puts information about the message's title into the intent and starts the
         * ReadMessageActivity with a provisory title (real title will be loaded in the
         * activity itself).
         */
        Intent mainIntent = new Intent(MainActivity.this, ReadMessageActivity.class);
        mainIntent.putExtra("id", id);
        mainIntent.putExtra("provisoryTitle", provisoryTitle);
        startActivity(mainIntent);
        //MainActivity.this.finish();
    }
    public static class MainPageAdapter extends FragmentPagerAdapter {//Adapter for fragments
        private SisgradCrawler login;
        private static int NUM_ITEMS = 4;//Number of pages (tabs)

        public MainPageAdapter(FragmentManager fragmentManager) {
            super(fragmentManager);
            this.login = login;
        }

        // Returns total number of pages
        @Override
        public int getCount() {
            return NUM_ITEMS;
        }

        // Returns the fragment to display for each page
        @Override
        public Fragment getItem(int position) {
            switch (position) {
                case 0:
                    return BlankFragment.newInstance("", "");
                case 1:
                    return MessagesFragment.newInstance(0, "");
                case 2:
                    return BlankFragment.newInstance("", "");
                case 3:
                    return BlankFragment.newInstance("", "");
                default:
                    return null;
            }
        }

        // Returns the page title for the top indicator
        @Override
        public CharSequence getPageTitle(int position) {
            return null;//no title for tabs, just icons
        }
        //private int currentPage;

    }
    private class PageListener extends ViewPager.SimpleOnPageChangeListener {//called when a page is selected
        public void onPageSelected(int position) {
            Log.i(LOG_TAG, "page selected " + position);
            if (bar!=null) {
                bar.setTitle(pageTitles[position]);
            }
        }
    }
    private String[] options = {"Graduação", "Pós-Graduação", "Configurações"};//options of drawerLayout
    private DrawerLayout mDrawerLayout;
    private ListView mDrawerList;
    public void open()
    {
        mDrawerLayout.openDrawer(Gravity.LEFT);
    }


    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        Log.d(LOG_TAG, "entered main activity");

        //Manages toolbar
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);
        bar = getSupportActionBar();//will be used below in the code to set the title of the bar

        //Drawer layour is the left panel that appears when you swipe to the right
        mDrawerLayout = (DrawerLayout) findViewById(R.id.drawer_layout);
        ActionBarDrawerToggle mActionBarDrawerToggle = new ActionBarDrawerToggle(this,
                mDrawerLayout, toolbar, R.string.open, R.string.close);
        mActionBarDrawerToggle.syncState();
        mDrawerLayout.addDrawerListener(mActionBarDrawerToggle);//Verificar isso


        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("login", 0);

        String username = sharedPref.getString("username", "");
        String password = sharedPref.getString("password", "");
        Log.d(LOG_TAG, "username is " + username);
        Log.d(LOG_TAG, "password is " + password);

        Sisgrad app = ((Sisgrad)getApplicationContext());//gets the global login object
        app.createLoginObject(username, password);//register the username/login information in the login object
        login = app.getLoginObject();//gets the login object to be used here

        //View pager that handles tab swipes
        ViewPager thePager = (ViewPager) findViewById(R.id.fragment_container);
        MainPageAdapter adapterViewPager = new MainPageAdapter(getSupportFragmentManager());
        thePager.setAdapter(adapterViewPager);
        PageListener pageListener = new PageListener();
        thePager.addOnPageChangeListener(pageListener);
        TabLayout tabLayout = (TabLayout) findViewById(R.id.tabs);
        tabLayout.setupWithViewPager(thePager);
        tabLayout.getTabAt(0).setIcon(R.drawable.ic_home);
        tabLayout.getTabAt(1).setIcon(R.drawable.ic_email);
        tabLayout.getTabAt(2).setIcon(R.drawable.ic_school);
        tabLayout.getTabAt(3).setIcon(R.drawable.ic_chrome_reader_mode);

        thePager.setCurrentItem(1);
        //tabLayout.getTabAt(1).setIcon(R.drawable.ic_school);

    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_toolbar, menu);
        return true;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }
}
