package com.aphy.caldavsyncadapter.android.entities;

import com.aphy.caldavsyncadapter.Constants;

import java.util.Objects;

public class AccountName {
    public static String AccountName = "AccountName";
    public static String Password = "Password";

    public static String ServerURL = "ServerUrl";

    private String mAccountName;
    private String mPassword;

    private String mServerURL;

    public String getAccountName() {
        return mAccountName;
    }

    public void setAccountName(String accountName) {
        this.mAccountName = accountName;
    }

    public String getPassword() {
        return mPassword;
    }

    public String getServerURL() {
        return mServerURL;
    }

    public void setPassword(String password) {
        this.mPassword = password;
    }

    public void setServerURL(String serverURL) {
        this.mServerURL = serverURL;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        AccountName that = (AccountName) o;
        return Objects.equals(mAccountName, that.mAccountName) && Objects.equals(mPassword, that.mPassword);
    }

    @Override
    public int hashCode() {
        return Objects.hash(mAccountName, mPassword, mServerURL);
    }

    @Override
    public String toString() {
        return "AccountName{" +
                "mAccountName='" + mAccountName + '\'' +
                ", mPassword='" + mPassword + '\'' +
                ", mServerURL='" + mServerURL + '\'' +
                '}';
    }

    public void clearAccountInfo() {
        mAccountName = "";
        mPassword = "";
        mServerURL = "";
    }
}