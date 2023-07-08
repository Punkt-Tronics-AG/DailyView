package com.aphy.caldavsyncadapter.authenticator;

import android.annotation.SuppressLint;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager.NameNotFoundException;
import android.os.AsyncTask;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.os.Parcelable;
import android.os.PersistableBundle;
import android.support.annotation.NonNull;
import android.support.v7.widget.LinearLayoutManager;
import android.support.v7.widget.RecyclerView;
import android.text.InputType;
import android.text.TextUtils;
import android.util.Log;
import android.util.SparseArray;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.View;
import android.view.animation.AnimationUtils;
import android.view.ViewGroup;
import android.widget.LinearLayout;
import android.widget.TextSwitcher;
import android.widget.TextView;
import android.widget.Toast;
import android.widget.ViewSwitcher.ViewFactory;

import com.android.common.CenterLayoutManager;
import com.android.common.DailyViewListActivity;
import com.android.common.IListItemClick;
import com.android.common.LoginActivity;
import com.android.common.MenuItem;
import com.android.common.MyDecoration;
import com.aphy.caldavsyncadapter.Constants;
import com.aphy.caldavsyncadapter.android.entities.AccountName;
import com.aphy.caldavsyncadapter.caldav.entities.CalendarEvent;
import com.aphy.caldavsyncadapter.caldav.entities.CalendarList;
import com.aphy.caldavsyncadapter.caldav.entities.DavCalendar;

import org.apache.http.client.ClientProtocolException;
import org.apache.http.conn.HttpHostConnectException;

import com.aphy.caldavsyncadapter.android.entities.CalendarEventData;
import com.aphy.caldavsyncadapter.caldav.CaldavFacade;
import com.aphy.caldavsyncadapter.caldav.CaldavFacade.TestConnectionResult;
import com.aphy.caldavsyncadapter.caldav.CaldavProtocolException;
import com.aphy.provider.UserDBHelper;
import com.aphy.caldavsyncadapter.utils.TimeUtil;

import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.UnsupportedEncodingException;
import java.net.MalformedURLException;
import java.net.SocketException;
import java.net.URISyntaxException;
import java.net.UnknownHostException;
import java.util.ArrayList;
import java.util.List;

import javax.net.ssl.SSLException;
import javax.xml.parsers.ParserConfigurationException;

import ch.punkt.mp02.dailyview.App;
import ch.punkt.mp02.dailyview.R;

/**
 * Activity which displays a login screen to the user, offering registration as
 * well.
 */
public class AuthenticatorActivity extends Activity {

    public static final String USER_DATA_URL_KEY = "USER_DATA_URL_KEY";

    public static final String USER_DATA_USERNAME = "USER_DATA_USERNAME";


    public static final String USER_DATA_VERSION = "USER_DATA_VERSION";


    private static final String TAG = "AuthenticatorActivity";

    @Override
    public void onCreate(Bundle savedInstanceState, PersistableBundle persistentState) {
        super.onCreate(savedInstanceState, persistentState);
    }

    /**
     * Keep track of the login task to ensure we can cancel it if requested.
     */
    private UserLoginTask mAuthTask = null;

    // Values for email and password at the time of the login attempt.
    private String mUser = "";

    private String mPassword = "";

    private Context mContext;

    private String mURL = "";

    private List<CalendarEventData> mCalendarEventDataList;
    private List<CalendarEventData> mCalendarEventDataListFromDataBase;

    private RecyclerView mRcContent;
    private MyDecoration mDecoration;
    private View mCurrentView;
    private TextSwitcher mTextSwitcher;

    private MyAdaptor myAdaptor;
    private CenterLayoutManager manager;
    private ArrayList<String> mRecyclerViewList = new ArrayList<>();
    private ArrayList<String> mDateStampList = new ArrayList<>();

    private Handler mHandler = new Handler(Looper.getMainLooper());

    private int mCurrentIndex = 0;
    private int mLastPosition = 0;
    private int mCurrentIndexStopped = 0;

    private boolean mDataInited = false;

    private UserDBHelper userDBHelper;
    private AccountName mSyncAccountName;

    private boolean isSyncNow = false;
    private boolean isLoginView = true;
    private long mRespondTime;

    private Toast toastsync = null;

    public AuthenticatorActivity() {
        super();
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        mContext = getBaseContext();
        App.setContext(mContext);
        setContentView(R.layout.activity_main);
        mRcContent = findViewById(R.id.rv_content);
        mTextSwitcher = findViewById(R.id.textSwitcher);
        mTextSwitcher.setFactory(mFactory);
        isSyncNow = false;
//        initList();
        MenuItem.init(this);
        myAdaptor = new MyAdaptor();
        myAdaptor.setIListItemClick(iListItemClickListener);
        manager = new CenterLayoutManager(this, LinearLayoutManager.VERTICAL, false);
        mRcContent.setLayoutManager(manager);
        mDecoration = new MyDecoration(this);
        mRcContent.addItemDecoration(mDecoration);
        mRcContent.setAdapter(myAdaptor);
        initUserDB();
    }

    private void initUserDB() {
        userDBHelper = UserDBHelper.getInstance(this, 1);
        userDBHelper.openWriteLink();

        getAccountInfo();

        if (mSyncAccountName != null && !"".equals(mSyncAccountName.getAccountName()) && !"".equals(mSyncAccountName.getPassword())) {
            initList();
            mUser = mSyncAccountName.getAccountName();
            mPassword = mSyncAccountName.getPassword();
            mURL = mSyncAccountName.getServerURL();
            mRecyclerViewList.set(0, getString(R.string.logout_label));
            mRecyclerViewList.set(1, getString(R.string.sync_calendar));
            mTextSwitcher.setText(mRecyclerViewList.get(0));
            mCalendarEventDataListFromDataBase = userDBHelper.queryCal(
                    "DtStart between " + TimeUtil.getTodayStamp() + " and " + TimeUtil.getFourDaysStamp() + " order by DtStart asc");
            isLoginView = false;
            if (mCalendarEventDataListFromDataBase != null) {
                updateCalendarList();
            }
        } else
            initLoginItems();
    }

    private void getAccountInfo() {
        List<AccountName> accountNameList = userDBHelper.queryAccount("1=1");
        if (accountNameList.size() != 0) {
            for (int i = 0; i < accountNameList.size(); i++) {
                mSyncAccountName = new AccountName();
                mSyncAccountName.setAccountName(accountNameList.get(i).getAccountName());
                mSyncAccountName.setPassword(accountNameList.get(i).getPassword());
                mSyncAccountName.setServerURL(accountNameList.get(i).getServerURL());
            }
        }
    }

    @Override
    protected void onStart() {
        super.onStart();
    }

    @Override
    protected void onStop() {
        super.onStop();
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        if (userDBHelper != null) {
            userDBHelper.closeLink();
        }
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        return true;
    }

    private void initList() {
        mRecyclerViewList.add("");
        mRecyclerViewList.add("");
        mRecyclerViewList.add("");
        mRecyclerViewList.add("");
    }

    private final ViewFactory mFactory = new ViewFactory() {

        @Override
        public View makeView() {
            // Create a new TextView
            TextView t = new TextView(AuthenticatorActivity.this);
            t.setTextAppearance(R.style.mp02TextViewFocusStyle);
            t.setEllipsize(TextUtils.TruncateAt.MARQUEE);
            t.setSingleLine();
            t.setSelected(true);
            t.requestFocus();
            return t;
        }
    };

    @Override
    public boolean dispatchKeyEvent(KeyEvent event) {
        int action = event.getAction();
        if (action == KeyEvent.ACTION_DOWN) {
            if (System.currentTimeMillis() - mRespondTime < 400) {
                return true;
            }
            mRespondTime = System.currentTimeMillis();
            switch (event.getKeyCode()) {
                case KeyEvent.KEYCODE_DPAD_UP:
                    if (mRecyclerViewList == null) return true;
                    mCurrentIndex--;
                    if (mCurrentIndex < 0) {
                        mCurrentIndex = 0;
                        return true;
                    }
                    manager.smoothScrollToPosition(mRcContent, null, mLastPosition, mCurrentIndex);
                    if (myAdaptor != null && myAdaptor.mSparseArray != null) {
                        MyHolder mUpHolder = myAdaptor.mSparseArray.get(mCurrentIndex);
                        if (mUpHolder != null) {
                            mUpHolder.mView.requestFocus();
//                            MenuItem.animate(mUpHolder.mView, MenuItem.Direction.UP);
                            setTextSwitcher(MenuItem.Direction.UP);
                        }
                    }
                    return true;
                case KeyEvent.KEYCODE_DPAD_DOWN:
                    if (mRecyclerViewList == null) return true;
                    mCurrentIndex++;
                    if (mCurrentIndex > mRecyclerViewList.size() - 1) {
                        mCurrentIndex = mRecyclerViewList.size() - 1;
                        return true;
                    }
                    manager.smoothScrollToPosition(mRcContent, null, mLastPosition, mCurrentIndex);
                    if (myAdaptor != null && myAdaptor.mSparseArray != null) {
                        MyHolder mDownHolder = myAdaptor.mSparseArray.get(mCurrentIndex);
                        if (mDownHolder != null) {
                            mDownHolder.mView.requestFocus();
//                            MenuItem.animate(mDownHolder.mView, MenuItem.Direction.DOWN);
                            setTextSwitcher(MenuItem.Direction.DOWN);
                        }
                    }
                    return true;
                case KeyEvent.KEYCODE_ENTER:
                case KeyEvent.KEYCODE_DPAD_CENTER:
                    if (mCurrentView != null) mCurrentView.performClick();
                    return true;
                case KeyEvent.KEYCODE_BACK:
                    if (!isSyncNow) {
                        finish();
                    }
                    return true;
            }
        }
        if (mLastPosition != mCurrentIndex) {
            mLastPosition = mCurrentIndex;
        }
        return super.dispatchKeyEvent(event);
    }

    private void setTextSwitcher(MenuItem.Direction direction) {
        switch (direction) {
            case DOWN:
                mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.upandvisible));
                mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.upandgone));
                break;
            case UP:
                mTextSwitcher.setInAnimation(AnimationUtils.loadAnimation(this, R.anim.downandvisible));
                mTextSwitcher.setOutAnimation(AnimationUtils.loadAnimation(this, R.anim.downandgone));
                break;
        }
        mTextSwitcher.setText(mRecyclerViewList.get(mCurrentIndex));
    }

    private void requestFocus(TextView textView) {
//        textView.setTextAppearance(R.style.mp02TextViewFocusStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 54);
        textView.setLayoutParams(lp);
    }

    private void requestUnfocus(TextView textView) {
//        textView.setTextAppearance(R.style.mp02TextViewUnfocusStyle);
        LinearLayout.LayoutParams lp = new LinearLayout.LayoutParams(LinearLayout.LayoutParams.MATCH_PARENT, 32);
        textView.setLayoutParams(lp);
    }

    class MyHolder extends RecyclerView.ViewHolder {
        View mView;

        MyHolder(View itemView) {
            super(itemView);
            mView = itemView;
        }
    }

    class MyAdaptor extends RecyclerView.Adapter<MyHolder> {

        IListItemClick mIListItemClick;
        SparseArray<MyHolder> mSparseArray = new SparseArray<>();

        public void setIListItemClick(IListItemClick mIListItemClick) {
            this.mIListItemClick = mIListItemClick;
        }

        @NonNull
        @Override
        public MyHolder onCreateViewHolder(@NonNull ViewGroup parent, int viewType) {
            View view = LayoutInflater.from(parent.getContext()).inflate(R.layout.item_view_layout, parent, false);
            return new MyHolder(view);
        }

        @Override
        public void onBindViewHolder(@NonNull final MyHolder holder, final int position) {
            final String s = mRecyclerViewList.get(position);
            if (!TextUtils.isEmpty(s)) {
                holder.mView.setOnFocusChangeListener(new View.OnFocusChangeListener() {
                    @Override
                    public void onFocusChange(View v, boolean hasFocus) {
                        if (hasFocus) {
                            mCurrentView = v;
                            holder.mView.setSelected(true);
                            requestFocus((TextView) holder.mView.findViewById(R.id.tv_item));
                        } else {
                            holder.mView.setSelected(false);
                            requestUnfocus((TextView) holder.mView.findViewById(R.id.tv_item));
                        }
                    }
                });

                holder.mView.setTag(position);
                holder.mView.setOnClickListener(new View.OnClickListener() {
                    @Override
                    public void onClick(View v) {
                        if (mIListItemClick != null) {
                            mIListItemClick.itemClick(v, position);
                        }
                    }
                });

                TextView item = holder.mView.findViewById(R.id.tv_item);
                item.setText(s);
                if (position == 1 && !getString(R.string.password_label).equals(s) && !getString(R.string.sync_calendar).equals(s)) {
                    item.setInputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD);
                } else {
                    item.setInputType(InputType.TYPE_CLASS_TEXT);
                }

                mSparseArray.put(position, holder);

                if (!mDataInited && position == 0) {
                    mHandler.postDelayed(new Runnable() {
                        @Override
                        public void run() {
                            mDataInited = true;
                            mCurrentIndex = position;
                            manager.smoothScrollToPosition(mRcContent, null, mLastPosition, position);
                            holder.mView.requestFocus();
                            if (mLastPosition != mCurrentIndex) {
                                mLastPosition = mCurrentIndex;
                            }
                            mTextSwitcher.setText(mRecyclerViewList.get(0));
                        }
                    }, 100);
                }

                if (mCurrentIndexStopped != 0) {
                    mCurrentIndex = mCurrentIndexStopped;
                    manager.smoothScrollToPosition(mRcContent, null, mLastPosition, mCurrentIndexStopped);
                    holder.mView.requestFocus();
                    if (mLastPosition != mCurrentIndex) {
                        mLastPosition = mCurrentIndex;
                    }
                    mCurrentIndexStopped = 0;
                }
            }
        }

        @Override
        public int getItemCount() {
            return mRecyclerViewList == null ? 0 : mRecyclerViewList.size();
        }
    }

    private IListItemClick iListItemClickListener = new IListItemClick() {
        @Override
        public void itemClick(View view, int position) {
            String itemStr = getSelectedItemLabel(position);

            if (isLoginView && !getString(R.string.login_btn_label).equals(itemStr)) {
                Log.i(TAG, "isSyncNow = " + isSyncNow);
                if (!isSyncNow) {
                    Intent intent = new Intent();
                    intent.setClass(AuthenticatorActivity.this, LoginActivity.class);
                    intent.putExtra("position", position);
                    intent.putExtra("name", itemStr);

                    if (mSyncAccountName == null) getAccountInfo();
                    if (mSyncAccountName != null) {
                        intent.putExtra("value", getSelectedItemValue(position));
                    }
                    mCurrentIndexStopped = mCurrentIndex;
                    startActivityForResult(intent, 0);
                }
            } else if (getString(R.string.logout_label).equals(itemStr)) {
                logout();
            } else if (getString(R.string.sync_calendar).equals(itemStr) || getString(R.string.login_btn_label).equals(itemStr)) {
                attemptLogin();
            } else {
                String datestamp = mDateStampList.get(position - 2);

                Intent intent = new Intent();
                intent.putExtra("week", mRecyclerViewList.get(position));
                intent.putExtra("dateStamp", datestamp);

                long daystart = TimeUtil.getdailyStamp(datestamp, true);
                long dayend = TimeUtil.getdailyStamp(datestamp, false);

                List<CalendarEventData> mCalendarEventDataListDailyView = new ArrayList<CalendarEventData>();

                for (int i = 0; i < mCalendarEventDataListFromDataBase.size(); i++) {
                    CalendarEventData calendarEventData = mCalendarEventDataListFromDataBase.get(i);
                    long dtstart = calendarEventData.getDtStart();
                    if (dtstart >= daystart && dtstart <= dayend) {
                        mCalendarEventDataListDailyView.add(calendarEventData);
                    }
                }
                intent.putParcelableArrayListExtra("calendarDataList", (ArrayList<? extends Parcelable>) mCalendarEventDataListDailyView);
                intent.setClass(AuthenticatorActivity.this, DailyViewListActivity.class);
                startActivity(intent);
            }
        }
    };


    private String getSelectedItemLabel(int position) {
        return mRecyclerViewList.get(position);
    }

    private String getSelectedItemValue(int position) {
        String item = getSelectedItemLabel(position);
        if (getString(R.string.email_label).equals(item)) {
            return mSyncAccountName.getAccountName();
        } else if (getString(R.string.password_label).equals(item)) {
            return mSyncAccountName.getPassword();
        } else if (getString(R.string.custom_server_label).equals(item)) {
            return mSyncAccountName.getServerURL();
        }

        return "";
    }

    // TODO: Improve login form validation
    private boolean validateForm() {

        return true;

//        String errorText = "";
//
//        if (mURL.equals("")) {
//            if (mUser.equals("")) {
//                errorText = getString(R.string.error_invalid_email);
//            } else if (mPassword.equals("")) {
//                errorText = getString(R.string.error_invalid_password);
//            } else {
//                return true;
//            }
//        } else {
//            return true;
//        }
//
//        toastsync = Toast
//                .makeText(getApplicationContext(), errorText, Toast.LENGTH_SHORT);
//        toastsync.show();
//
//        return false;

    }

    private void initLoginItems() {
        mRecyclerViewList.clear();

        if (!Constants.DEBUG) {
            mRecyclerViewList.add(getString(R.string.email_label));
            mRecyclerViewList.add(getString(R.string.password_label));
            mRecyclerViewList.add(getString(R.string.custom_server_label));
            mURL = Constants.APOSTROPHY_DAV_URL;
        } else {
            mRecyclerViewList.add(0, Constants.DEBUG_USER);
            mRecyclerViewList.add(1, Constants.DEBUG_PASSWORD);
            mRecyclerViewList.add(2, Constants.DEBUG_SERVER);
            mUser = Constants.DEBUG_USER;
            mPassword = Constants.DEBUG_PASSWORD;
            mURL = Constants.DEBUG_SERVER;
        }

        mRecyclerViewList.add(getString(R.string.login_btn_label));
        mTextSwitcher.setText(mRecyclerViewList.get(0));

    }


    private void logout() {
        Log.d(TAG, "logout");
        mUser = "";
        mPassword = "";
        mURL = "";
        if (mSyncAccountName != null) {
            mSyncAccountName.clearAccountInfo();
        }
        userDBHelper.deleteAll(UserDBHelper.TABLE_NAME_ACCOUNT);
        initLoginItems();
        myAdaptor.notifyDataSetChanged();
        mCurrentIndex = 0;
        mLastPosition = 0;
        mCurrentIndexStopped = 0;
        isLoginView = true;
    }

    @SuppressLint("NotifyDataSetChanged")
    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        super.onActivityResult(requestCode, resultCode, data);
        if (data != null && resultCode == 0) {
            String value = data.getExtras().getString("Save");
            String name = data.getExtras().getString("Name");

            if (getString(R.string.email_label).equals(name)) {
                mUser = value;
                if (!"".equals(value)) {
                    mRecyclerViewList.set(0, value);
                    myAdaptor.notifyDataSetChanged();
                }
            }
            if (getString(R.string.password_label).equals(name)) {
                mPassword = value;
                if (!"".equals(value)) {
                    int length = value.length();
                    value = "";
                    for (int i = 0; i < length; i++) {
                        value += "•";
                    }
                    mRecyclerViewList.set(1, value);
                    myAdaptor.notifyDataSetChanged();
                }
            }

            if (getString(R.string.custom_server_label).equals(name)) {
                mURL = value;
                if (!"".equals(value)) {
                    mRecyclerViewList.set(2, value);
                    myAdaptor.notifyDataSetChanged();
                }
            }


            mTextSwitcher.setText(value);
        }
    }

    private void notifyLoginSuccess() {
        mRecyclerViewList.clear();
        mRecyclerViewList.add(getString(R.string.logout_label));
        mRecyclerViewList.add(getString(R.string.sync_calendar));
        mTextSwitcher.setText(getString(R.string.logout_label));
        mDataInited = false;
        mRcContent.removeItemDecoration(mDecoration);
        mDecoration = null;
        mDecoration = new MyDecoration(this);
        mRcContent.addItemDecoration(mDecoration);
        myAdaptor.notifyDataSetChanged();
        mCurrentIndex = 0;
        mLastPosition = 0;
        isLoginView = false;
        updateCalendarList();
    }

    private void updateCalendarList() {
        mDateStampList.clear();
        for (CalendarEventData calendarEventData : mCalendarEventDataListFromDataBase) {
            if (calendarEventData != null) {
                String week = TimeUtil.getWeek(this, calendarEventData.getDtStart());
                if (!mRecyclerViewList.contains(week)) {
                    mRecyclerViewList.add(week);
                    mDateStampList.add(TimeUtil.getDate(calendarEventData.getDtStart()));
                }
            }
            myAdaptor.notifyDataSetChanged();
        }
    }

    /**
     * Attempts to sign in or register the account specified by the login form.
     * If there are form errors (invalid email, missing fields, etc.), the
     * errors are presented and no actual login attempt is made.
     */
    public void attemptLogin() {
        if (mAuthTask != null) {
            return;
        }

        if (!validateForm()) {
            return;
        }

        Log.i(TAG, "attemptLogin");

        boolean cancel = false;
        View focusView = null;


        if (cancel) {
            focusView.requestFocus();
        } else {
            mAuthTask = new UserLoginTask();
            mAuthTask.setActivity(this);
            mAuthTask.execute((Void) null);
        }
    }

    protected enum LoginResult {
        MalformedURLException,
        GeneralSecurityException,
        UnkonwnException,
        WrongCredentials,
        InvalidResponse,
        WrongUrl,
        ConnectionRefused,
        Success_Calendar,
        Success_Collection,
        UNTRUSTED_CERT,
        Account_Already_In_Use,
        UnknownHostException
    }

    /**
     * Represents an asynchronous login/registration task used to authenticate
     * the user.
     */
    public class UserLoginTask extends AsyncTask<Void, Void, LoginResult> {
        private Activity activity;

        @Override
        protected LoginResult doInBackground(Void... params) {
            isSyncNow = true;
            Log.i(TAG, "doInBackground isSyncNow = " + isSyncNow);

            activity.runOnUiThread(new Runnable() {
                @Override
                public void run() {
                    toastsync = Toast
                            .makeText(getApplicationContext(), R.string.login_progress_signing_in, Toast.LENGTH_SHORT);
                    toastsync.show();
                }
            });

            TestConnectionResult result = null;
            try {
                CaldavFacade facade = new CaldavFacade(mUser, mPassword, mURL, "false");
                String version = "";
                try {
                    version = mContext.getPackageManager()
                            .getPackageInfo(mContext.getPackageName(), 0).versionName;
                } catch (NameNotFoundException e) {
                    version = "unknown";
                    e.printStackTrace();
                }
                facade.setVersion(version);
//                result = facade.testConnection();
                CalendarList serverCalList;
                try {
                    serverCalList = facade.getCalendarList(getApplicationContext());
//                    Log.i(TAG, "serverCalList = " + serverCalList.getCalendarList().size());
                    for (DavCalendar serverCalendar : serverCalList.getCalendarList()) {
                        mCalendarEventDataList = new ArrayList<CalendarEventData>();
//                        Log.i(TAG, "Detected calendar name=" + serverCalendar.getCalendarDisplayName()
//                                + " URI=" + serverCalendar
//                                .getURI());
                        if (serverCalendar.readCalendarEvents(facade)) {
//                            Log.i(TAG, "serverCalendar.getCalendarEvents() size = " + serverCalendar.getCalendarEvents().size());
                            for (CalendarEvent calendarEvent : serverCalendar.getCalendarEvents()) {
                                CalendarEventData calendarEventData = calendarEvent.createmp02Event();
                                if (calendarEventData != null) {
                                    mCalendarEventDataList.add(calendarEventData);
                                }
                            }
                        } else {
                            Log.d(TAG, "unable to read events from server calendar");
                        }
                    }

                    userDBHelper.deleteAll(UserDBHelper.TABLE_NAME_CAL);
                    userDBHelper.insertCal(mCalendarEventDataList);
//                    Log.i(TAG, "mCalendarEventDataList = " + mCalendarEventDataList.size());

                    mCalendarEventDataListFromDataBase = userDBHelper.queryCal(
                            "DtStart between " + TimeUtil.getTodayStamp() + " and " + TimeUtil.getFourDaysStamp() + " order by DtStart asc");

                    AccountName accountName = new AccountName();
                    accountName.setAccountName(mUser);
                    accountName.setPassword(mPassword);
                    accountName.setServerURL(mURL);

                    userDBHelper.insertAccount(accountName);

                    result = TestConnectionResult.SUCCESS;
                    Log.i(TAG, "sync end");
                } catch (SSLException e) {
                    e.printStackTrace();
                    Log.w(TAG, "SSLException = " + e);
                    result = TestConnectionResult.SSL_ERROR;
                } catch (SocketException e) {
                    e.printStackTrace();
                    Log.w(TAG, "SocketException = " + e);
                    result = TestConnectionResult.WRONG_URL;
                } catch (ClientProtocolException e) {
                    e.printStackTrace();
                    Log.w(TAG, "ClientProtocolException = " + e);
                    result = TestConnectionResult.WRONG_SERVER_STATUS;
                } catch (CaldavProtocolException e) {
                    e.printStackTrace();
                    Log.w(TAG, "CaldavProtocolException = " + e);
                    result = TestConnectionResult.WRONG_ANSWER;
                } catch (FileNotFoundException e) {
                    e.printStackTrace();
                    Log.w(TAG, "FileNotFoundException = " + e);
                    result = TestConnectionResult.WRONG_URL;
                }

            } catch (HttpHostConnectException e) {
                e.printStackTrace();
                Log.w(TAG, "HttpHostConnectException = " + e);
                return LoginResult.ConnectionRefused;
            } catch (MalformedURLException e) {
                e.printStackTrace();
                Log.w(TAG, "MalformedURLException = " + e);
                return LoginResult.MalformedURLException;
            } catch (UnsupportedEncodingException e) {
                e.printStackTrace();
                Log.w(TAG, "UnsupportedEncodingException = " + e);
                return LoginResult.UnkonwnException;
            } catch (ParserConfigurationException e) {
                e.printStackTrace();
                Log.w(TAG, "ParserConfigurationException = " + e);
                return LoginResult.UnkonwnException;
            } catch (UnknownHostException e) {
                e.printStackTrace();
                Log.w(TAG, "UnknownHostException = " + e);
                return LoginResult.UnknownHostException;
            } catch (IOException e) {
                e.printStackTrace();
                Log.w(TAG, "IOException = " + e);
                return LoginResult.UnkonwnException;
            } catch (URISyntaxException e) {
                e.printStackTrace();
                Log.w(TAG, "URISyntaxException = " + e);
                return LoginResult.MalformedURLException;
            }

            if (result == null) {
                return LoginResult.UnkonwnException;
            }

            switch (result) {

                case SSL_ERROR:
                    return LoginResult.UNTRUSTED_CERT;
                case SUCCESS:
                    return LoginResult.Success_Calendar;

                case WRONG_CREDENTIAL:
                    return LoginResult.WrongCredentials;

                case WRONG_SERVER_STATUS:
                    return LoginResult.InvalidResponse;

                case WRONG_URL:
                    return LoginResult.WrongUrl;

                case WRONG_ANSWER:
                    return LoginResult.InvalidResponse;

                default:
                    return LoginResult.UnkonwnException;

            }
        }

        @Override
        protected void onPostExecute(final LoginResult result) {
            isSyncNow = false;
            Log.i(TAG, "onPostExecute isSyncNow = " + isSyncNow);
            mAuthTask = null;
            if (toastsync != null) {
                toastsync.cancel();
            }
            int duration = Toast.LENGTH_LONG;
            Toast toast = null;

            Log.i(TAG, "result = " + result);
            switch (result) {
                case Success_Calendar:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.success_calendar, duration);
                    toast.show();
                    notifyLoginSuccess();
                    break;

                case Success_Collection:
                    toast = Toast.makeText(getApplicationContext(), R.string.success_collection,
                            duration);
                    toast.show();
                    break;

                case MalformedURLException:

                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_incorrect_url_format,
                                    duration);
                    toast.show();
                    break;
                case InvalidResponse:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_invalid_server_answer,
                                    duration);
                    toast.show();
                    break;
                case WrongUrl:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_wrong_url, duration);
                    toast.show();
                    break;

                case GeneralSecurityException:
                    break;
                case UnkonwnException:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_invalid_email_or_password, duration);
                    toast.show();
                    break;
                case UnknownHostException:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_no_net, duration);
                    toast.show();
                    break;
                case WrongCredentials:
                    break;

                case ConnectionRefused:
                    toast = Toast
                            .makeText(getApplicationContext(), R.string.error_connection_refused,
                                    duration);
                    toast.show();
                    break;
                case UNTRUSTED_CERT:
                    toast = Toast.makeText(getApplicationContext(),
                            getString(R.string.error_untrusted_certificate), duration);
                    toast.show();
                    break;
                case Account_Already_In_Use:
                    toast = Toast.makeText(getApplicationContext(),
                            R.string.error_account_already_in_use, duration);
                    toast.show();
                    break;
                default:
                    toast = Toast.makeText(getApplicationContext(), R.string.error_unkown_error,
                            duration);
                    toast.show();
                    break;
            }


        }

        @Override
        protected void onCancelled() {
            mAuthTask = null;
        }

        public void setActivity(Activity activity) {
            this.activity = activity;
        }
    }
}