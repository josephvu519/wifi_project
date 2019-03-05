package com.example.eric.wishare;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.database.sqlite.SQLiteOpenHelper;
import android.os.AsyncTask;

public class WiSQLiteDatabase extends SQLiteOpenHelper {

    interface OnDBReadyListener {
        void onDBReady(SQLiteDatabase theDB);
    }

    private static final int mDATABASE_VERSION = 1;
    private static final String mDATABASE_NAME = "wishare.db";
    private static WiSQLiteDatabase mTheDb;


    //Table with variables
    //Common variable types include INT, FLOAT, DATE, TIME, varchar([max characters])    <-- string, BIT <-- boolean with 0 | 1
    private static final String mSQL_CREATE_ENTRIES =
            "CREATE TABLE synchronizedContacts (" +
                    "token varchar(255) NOT NULL PRIMARY KEY," +
                    "name varchar(255)," +
                    "phone varchar(255))";

    //Not sure if this will really be used
    private static final String mSQL_DELETE_ENTRIES =
            "DROP TABLE IF EXISTS synchronizedContacts";

    private WiSQLiteDatabase(Context context) {
        super(context.getApplicationContext(),mDATABASE_NAME,null,mDATABASE_VERSION);
    }

    public static synchronized WiSQLiteDatabase getInstance(Context context) {
        if (mTheDb == null) {
            mTheDb = new WiSQLiteDatabase(context);
        }
        return mTheDb;
    }

    @Override
    public void onCreate(SQLiteDatabase db) {
        db.execSQL(mSQL_CREATE_ENTRIES);
    }

    @Override
    public void onUpgrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        db.execSQL(mSQL_DELETE_ENTRIES);
        onCreate(db);
    }

    @Override
    public void onDowngrade(SQLiteDatabase db, int oldVersion, int newVersion) {
        onUpgrade(db, oldVersion, newVersion);
    }

    public void getWritableDatabase(OnDBReadyListener listener) {
        new OpenDbAsyncTask().execute(listener);
    }

    private static class OpenDbAsyncTask extends AsyncTask<OnDBReadyListener,Void,SQLiteDatabase> {
        OnDBReadyListener listener;

        @Override
        protected SQLiteDatabase doInBackground(OnDBReadyListener... params){
            listener = params[0];
            return WiSQLiteDatabase.mTheDb.getWritableDatabase();
        }

        @Override
        protected void onPostExecute(SQLiteDatabase db) {
            listener.onDBReady(db);
        }
    }
}