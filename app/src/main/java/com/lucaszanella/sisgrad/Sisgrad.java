package com.lucaszanella.sisgrad;

import android.app.Application;
import android.util.Log;

import com.lucaszanella.SisgradCrawler.SisgradCrawler;

import java.util.Date;

/**
 * Created by lucaszanella on 5/15/16.
 * Implementation of the following technique: http://stackoverflow.com/questions/1944656/android-global-variable
 * This is a global object intended to stay accessible by any activity.
 */
public class Sisgrad extends Application {
    private static final String LOG_TAG = "Sisgrad.java";//tag for the Log.d function

    public SisgradCrawler login;
    boolean alreadyCreatedLoginObject = false;
    public long lastLoginSuccess = 0;

    public static int LOGIN_TIMEOUT = TimeAgo.ONE_MINUTE*20;//will try to login if last login was 20 minutes ago or more

    public static final int OK = 0;
    public static final int WRONG_EMAIL = 1;
    public static final int WRONG_PASSWORD = 2;
    public static final int PAGE_ERROR = 3;
    public static final int TIMEOUT = 4;
    public static final int NOT_FOUND = 5;
    public static final int GENERIC_ERROR = 6;
    public static final int NOT_CONNECTED = 7;
    public static final int RESUME = 8;


    public void createLoginObject(String username, String password) {
        this.login = new SisgradCrawler(username, password);
        this.alreadyCreatedLoginObject = true;
    }
    public SisgradCrawler getLoginObject() {
        if (alreadyCreatedLoginObject) {
            return this.login;
        } else {
            Log.d(LOG_TAG, "didn't create login object yet");
            return null;
        }
    }

    public synchronized Integer DoOrResumeLogin() throws Exception {
        //TODO: identify if never logged in, or if login is about to timeout. Differentiate between timedout, about to time out, and session open
        Log.d(LOG_TAG, "DoOrResumeLogin called");
        Long currentUnix = new Date().getTime()/1000;
        if (this.alreadyCreatedLoginObject && ((currentUnix-this.lastLoginSuccess)>=LOGIN_TIMEOUT || this.lastLoginSuccess==0)) {//lastLoginSuccess==0 means it was just created, never assigned a value
            SisgradCrawler.SentinelaLoginObject loginObject = this.login.loginToSentinela();//logs in
            if (loginObject.loginError != null) {
                Log.d(LOG_TAG, "something wrong with login information:");
                if (loginObject.loginError.wrongEmail) {
                    Log.d(LOG_TAG, "wrong email");
                    return WRONG_EMAIL;
                }
                if (loginObject.loginError.wrongPassword) {
                    Log.d(LOG_TAG, "wrong password");
                    return WRONG_PASSWORD;
                }
            } else if (loginObject.pageError != null) {
                System.out.println("error with the page loading, code is: "
                        + loginObject.pageError.errorCode + " message is " +
                        loginObject.pageError.errorMessage
                );
                return PAGE_ERROR;
            } else {
                this.lastLoginSuccess = new Date().getTime()/1000;//current unix time
                return OK;
            }

        } else if (this.alreadyCreatedLoginObject && (currentUnix-this.lastLoginSuccess)<LOGIN_TIMEOUT){
            //Didn't timeout, let's just resume it

            return RESUME;
        } else if (!alreadyCreatedLoginObject) {
            return NOT_CONNECTED;
        }
        return null;
    }
}
