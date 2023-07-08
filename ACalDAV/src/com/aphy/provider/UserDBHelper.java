package com.aphy.provider;

import android.content.ContentValues;
import android.content.Context;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.support.annotation.Nullable;
import android.util.Log;

import com.aphy.caldavsyncadapter.android.entities.AccountName;
import com.aphy.caldavsyncadapter.android.entities.CalendarEventData;

import java.util.ArrayList;
import java.util.List;

public class UserDBHelper extends SQLiteOpenHelper {
    private static final String TAG = "UserDBHelper";

    public static UserDBHelper userDBHelper = null;

    private SQLiteDatabase db = null;

    public static final String DB_NAME = "calprovider.db";

    public static final String TABLE_NAME_CAL = "cal_info";
    public static final String TABLE_NAME_ACCOUNT = "account_info";

    public static int DB_VERSION = 1;

    public UserDBHelper(@Nullable Context context) {
        super(context, DB_NAME, null, DB_VERSION);
    }

    public UserDBHelper(@Nullable Context context, int version) {
        super(context, DB_NAME, null, version);
    }

    public static UserDBHelper getInstance(Context context, int version) {
        if (userDBHelper == null && version > 0) {
            userDBHelper = new UserDBHelper(context, version);
        } else if (userDBHelper == null) {
            userDBHelper = new UserDBHelper(context);
        }
        return userDBHelper;
    }

    public SQLiteDatabase openWriteLink() {
        if (db == null || !db.isOpen()) {
            db = userDBHelper.getWritableDatabase();
        }
        return db;
    }

    public SQLiteDatabase openReadLink() {
        if (db == null || !db.isOpen()) {
            db = userDBHelper.getReadableDatabase();
        }
        return db;
    }

    public void closeLink() {
        if (db != null && db.isOpen()) {
            db.close();
            db = null;
        }
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        String drop_sql = "DROP TABLE IF EXISTS " + TABLE_NAME_CAL + ";";
        db.execSQL(drop_sql);
        String create_sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_CAL + "("
                + "DtStart LONG,"
                + "TimeZone VARCHAR,"
                + "DtEnd LONG,"
                + "EndTimeZone VARCHAR,"
                + "Duration VARCHAR,"
                + "AllDay INTEGER,"
                + "Title VARCHAR,"
                + "SyncID VARCHAR,"
                + "ETag VARCHAR,"
                + "Description VARCHAR,"
                + "Location VARCHAR,"
                + "AccessLevel LONG,"
                + "Status INTEGER,"
                + "Rdate VARCHAR,"
                + "Rrule VARCHAR,"
                + "ExRule VARCHAR,"
                + "ExDate VARCHAR,"
                + "UID VARCHAR UNIQUE"
                + ");";
        db.execSQL(create_sql);

        String account_sql = "CREATE TABLE IF NOT EXISTS " + TABLE_NAME_ACCOUNT + "("
                + "AccountName VARCHAR,"
                + "Password VARCHAR,"
                + "ServerUrl VARCHAR"
                + ");";
        db.execSQL(account_sql);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {

    }

    public int delete(String condition, String tablename ) {
        return db.delete(tablename, condition, null);
    }

    public int deleteAll(String tablename) {
        return db.delete(tablename, "1=1", null);
    }

    public long insert(CalendarEventData calendarEventData) {
        List<CalendarEventData> infoList = new ArrayList<>();
        infoList.add(calendarEventData);
        return insertCal(infoList);
    }

    public long insertCal(List<CalendarEventData> infoList) {
        long result = -1;
        for (int i = 0; i < infoList.size(); i++) {
            CalendarEventData calendarEventData = infoList.get(i);
            ContentValues cv = getCalContentValues(calendarEventData);

            result = db.insert(TABLE_NAME_CAL, "", cv);
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    public int update(CalendarEventData calendarEventData, String condition) {
        ContentValues cv = getCalContentValues(calendarEventData);
        return db.update(TABLE_NAME_CAL, cv, condition, null);
    }

    public List<CalendarEventData> queryCal(String condition) {
        String sql = String.format("select * " +
                " from %s where %s;", TABLE_NAME_CAL, condition);
        List<CalendarEventData> infoList = new ArrayList<>();

        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            CalendarEventData calendarEventData = new CalendarEventData();

            calendarEventData.setDtStart(cursor.getLong(cursor.getColumnIndex(CalendarEventData.DtStart)));
            calendarEventData.setTimeZone(cursor.getString(cursor.getColumnIndex(CalendarEventData.TimeZone)));
            calendarEventData.setDtEnd(cursor.getLong(cursor.getColumnIndex(CalendarEventData.DtEnd)));
            calendarEventData.setEndTimeZone(cursor.getString(cursor.getColumnIndex(CalendarEventData.EndTimeZone)));
            calendarEventData.setDuration(cursor.getString(cursor.getColumnIndex(CalendarEventData.Duration)));

            calendarEventData.setAllDay(cursor.getInt(cursor.getColumnIndex(CalendarEventData.AllDay)));
            calendarEventData.setTitle(cursor.getString(cursor.getColumnIndex(CalendarEventData.Title)));
            calendarEventData.setSyncID(cursor.getString(cursor.getColumnIndex(CalendarEventData.SyncID)));
            calendarEventData.setETag(cursor.getString(cursor.getColumnIndex(CalendarEventData.ETag)));

            calendarEventData.setDescription(cursor.getString(cursor.getColumnIndex(CalendarEventData.Description)));
            calendarEventData.setLocation(cursor.getString(cursor.getColumnIndex(CalendarEventData.Location)));
            calendarEventData.setAccessLevel(cursor.getLong(cursor.getColumnIndex(CalendarEventData.AccessLevel)));
            calendarEventData.setStatus(cursor.getInt(cursor.getColumnIndex(CalendarEventData.Status)));
            calendarEventData.setRdate(cursor.getString(cursor.getColumnIndex(CalendarEventData.Rdate)));
            calendarEventData.setRrule(cursor.getString(cursor.getColumnIndex(CalendarEventData.Rrule)));
            calendarEventData.setExRule(cursor.getString(cursor.getColumnIndex(CalendarEventData.ExRule)));
            calendarEventData.setExDate(cursor.getString(cursor.getColumnIndex(CalendarEventData.ExDate)));
            calendarEventData.setUID(cursor.getString(cursor.getColumnIndex(CalendarEventData.UID)));
//            calendarEventData.setRawData(cursor.getString(cursor.getColumnIndex(CalendarEventData.RawData)));

            infoList.add(calendarEventData);
        }
        cursor.close();
        return infoList;
    }

    public ContentValues getCalContentValues(CalendarEventData calendarEventData) {
        ContentValues cv = new ContentValues();
        cv.put(CalendarEventData.DtStart, calendarEventData.getDtStart());
        cv.put(CalendarEventData.TimeZone, calendarEventData.getTimeZone());
        cv.put(CalendarEventData.DtEnd, calendarEventData.getDtEnd());
        cv.put(CalendarEventData.EndTimeZone, calendarEventData.getEndTimeZone());
        cv.put(CalendarEventData.Duration, calendarEventData.getDuration());

        cv.put(CalendarEventData.AllDay, calendarEventData.getAllDay());
        cv.put(CalendarEventData.Title, calendarEventData.getTitle());
        cv.put(CalendarEventData.SyncID, calendarEventData.getSyncID());
        cv.put(CalendarEventData.ETag, calendarEventData.getETag());

        cv.put(CalendarEventData.Description, calendarEventData.getDescription());
        cv.put(CalendarEventData.Location, calendarEventData.getLocation());
        cv.put(CalendarEventData.AccessLevel, calendarEventData.getAccessLevel());
        cv.put(CalendarEventData.Status, calendarEventData.getStatus());
        cv.put(CalendarEventData.Rdate, calendarEventData.getRdate());
        cv.put(CalendarEventData.Rrule, calendarEventData.getRrule());
        cv.put(CalendarEventData.ExRule, calendarEventData.getExRule());
        cv.put(CalendarEventData.ExDate, calendarEventData.getExDate());
        cv.put(CalendarEventData.UID, calendarEventData.getUID());
//        cv.put(CalendarEventData.RawData, calendarEventData.getRawData());
        return cv;
    }


    public int update(CalendarEventData calendarEventData) {
        return update(calendarEventData, "1");
    }

    public long insertAccount(AccountName accountName) {
        List<AccountName> infoList = new ArrayList<>();
        infoList.add(accountName);
        return insertCalAccount(infoList);
    }

    public long insertCalAccount(List<AccountName> infoList) {
        long result = -1;
        for (int i = 0; i < infoList.size(); i++) {
            AccountName accountName = infoList.get(i);
            List<CalendarEventData> tempList = new ArrayList<>();

            ContentValues cv = getAccountContentValues(accountName);

            result = db.insert(TABLE_NAME_ACCOUNT, "", cv);
            if (result == -1) {
                return result;
            }
        }
        return result;
    }

    public ContentValues getAccountContentValues(AccountName accountName) {
        ContentValues cv = new ContentValues();
        cv.put(AccountName.AccountName, accountName.getAccountName());
        cv.put(AccountName.Password, accountName.getPassword());
        cv.put(AccountName.ServerURL, accountName.getServerURL());
        return cv;
    }

    public List<AccountName> queryAccount(String condition) {
        String sql = String.format("select * " +
                " from %s where %s;", TABLE_NAME_ACCOUNT, condition);
        List<AccountName> infoList = new ArrayList<>();

        Cursor cursor = db.rawQuery(sql, null);
        while (cursor.moveToNext()) {
            AccountName accountName = new AccountName();

            accountName.setAccountName(cursor.getString(cursor.getColumnIndex(AccountName.AccountName)));
            accountName.setPassword(cursor.getString(cursor.getColumnIndex(AccountName.Password)));
            accountName.setServerURL(cursor.getString(cursor.getColumnIndex(AccountName.ServerURL)));
            infoList.add(accountName);
        }
        cursor.close();
        return infoList;
    }
}
