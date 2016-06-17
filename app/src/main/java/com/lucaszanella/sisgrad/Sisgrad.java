package com.lucaszanella.sisgrad;

import android.app.Application;
import android.content.Context;
import android.content.SharedPreferences;
import android.os.Bundle;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;

import com.lucaszanella.SisgradCrawler.SisgradCrawler;

/**
 * Created by lucaszanella on 5/15/16.
 */
public class Sisgrad extends Application {
    private static final String LOG_TAG = "Sisgrad.java";//tag for the Log.d function
    public SisgradCrawler login;
    boolean alreadyCreatedLoginObject = false;
    public void createLoginObject(String username, String password) {
        login = new SisgradCrawler(username, password);
        alreadyCreatedLoginObject = true;
    }
    public SisgradCrawler getLoginObject() {
        if (alreadyCreatedLoginObject) {
            return this.login;
        } else {
            Log.d(LOG_TAG, "didn't create login object");
            return null;
        }
    }
}
