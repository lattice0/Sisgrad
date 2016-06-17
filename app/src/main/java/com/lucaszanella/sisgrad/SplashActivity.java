package com.lucaszanella.sisgrad;

import android.content.Intent;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

/*
 * Activity that is loaded when the app is opened. It checks if the username/password is
 * not empty. If it is not empty, goes directly to MainActivity.java. If it is empty,
 * goes to the LoginActivity. This SplashActivity implements the Splash in the right way:
 * it has a background with the color and image to be displayed while the app is loading,
 * so there's no countdown to simulate an app loading, so the user's time is not wasted and
 * the splash screen is loaded imediattely when the app's icon is pressed.
 */

public class SplashActivity extends AppCompatActivity {
    private static final String LOG_TAG = "Splash";//tag for the Log.d function

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        //get sharedPreferences for "login"
        SharedPreferences sharedPref = getApplicationContext().getSharedPreferences("login", 0);
        //loads the username and password, or sets it to "" (empty) if they're not setted
        String username = sharedPref.getString("username", "");
        String password = sharedPref.getString("password", "");

        if (username.length()>2) { //if not empty...
            Log.d(LOG_TAG, "not empty, username is "+username);
            Intent mainIntent = new Intent(SplashActivity.this, MainActivity.class);
            SplashActivity.this.startActivity(mainIntent);
            SplashActivity.this.finish();
        } else {//if is empty...
            Log.d(LOG_TAG, "empty " + username + password);
            Intent mainIntent = new Intent(SplashActivity.this, LoginActivity.class);
            SplashActivity.this.startActivity(mainIntent);
            SplashActivity.this.finish();
        }
        finish();
    }

}


