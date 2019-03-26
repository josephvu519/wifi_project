package com.example.eric.wishare;

import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.database.Cursor;
import android.database.sqlite.SQLiteDatabase;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiInfo;
import android.net.wifi.WifiManager;
import android.preference.PreferenceManager;
import android.text.TextUtils;
import android.widget.Toast;

import com.afollestad.materialdialogs.MaterialDialog;
import com.example.eric.wishare.model.WiConfiguration;

import com.example.eric.wishare.model.WiConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;

import me.zhanghai.android.materialprogressbar.MaterialProgressBar;

import static android.content.Context.WIFI_SERVICE;

public class WiNetworkManager {
    private static WiNetworkManager sInstance;
    private static WifiManager sWifiManager;
    private static ArrayList<WiConfiguration> mConfiguredNetworks;
    private static ArrayList<WifiConfiguration> mNotConfiguredNetworks;

    private static HashMap<String, WifiConfiguration> mConfigured;
    private SharedPreferences.Editor mEditor;

    private WeakReference<Context> mContext;

    public WiNetworkManager(Context context) {
        mContext = new WeakReference<>(context.getApplicationContext());
        sWifiManager = (WifiManager) mContext.get().getApplicationContext().getSystemService(WIFI_SERVICE);
        mEditor = PreferenceManager.getDefaultSharedPreferences(context).edit();

        mConfiguredNetworks = new ArrayList<>();
        mNotConfiguredNetworks = new ArrayList<>();
        mConfigured = new HashMap<>();
        loadNetworks();
    }

    public static ArrayList<WiConfiguration> getConfiguredNetworks(Context context) {
        return mConfiguredNetworks;
    }

    public static ArrayList<WifiConfiguration> getUnConfiguredNetworks(Context context) {
        WifiManager wifiManager = (WifiManager) context.getApplicationContext().getSystemService(WIFI_SERVICE);
        List<WifiConfiguration> wifiList = wifiManager.getConfiguredNetworks();
        mNotConfiguredNetworks.addAll(wifiList);
        HashMap<String, WiConfiguration> configMap = new HashMap<>();
        for (WiConfiguration wConfig : mConfiguredNetworks){
            configMap.put(wConfig.getSSID(), wConfig);
        }
        for (Iterator<WifiConfiguration> it = mNotConfiguredNetworks.iterator(); it.hasNext();){
        //for(WifiConfiguration configuration : mNotConfiguredNetworks) {
            if(configMap.containsKey(it.next().SSID.replace("\"", ""))) {
                it.remove();
            }
        }
        return mNotConfiguredNetworks;
    }

    public void addConfiguredNetwork(final WiConfiguration config) {
        SQLiteDatabase db = WiSQLiteDatabase.getInstance(mContext.get()).getReadableDatabase();
        Cursor cur = db.rawQuery("SELECT * FROM WifiConfiguration WHERE network_id=(SELECT MAX(network_id) FROM WifiConfiguration)", null);
        cur.moveToFirst();
            WiConfiguration config2 = new WiConfiguration(
                    cur.getString(cur.getColumnIndex("SSID")),
                    cur.getString(cur.getColumnIndex("password")),
                    cur.getString(cur.getColumnIndex("network_id")));
        cur.close();

        mConfiguredNetworks.add(config2);

    }

    public void addNotConfiguredNetwork(WifiConfiguration config) {
        mNotConfiguredNetworks.add(config);
    }

    public void removeConfiguredNetwork(WiConfiguration config) {
        mConfiguredNetworks.remove(config);
    }

    public void removeNotConfiguredNetwork(WifiConfiguration config) {
        for(WifiConfiguration configuration : mNotConfiguredNetworks) {
            if(config.SSID.equals(configuration.SSID.replace("\"", ""))) {
                mNotConfiguredNetworks.remove(configuration);
                break;
            }
        }
    }

    public void addNetwork(WifiConfiguration config){
        sWifiManager.addNetwork(config);
    }

    public boolean testConnection(String ssid){
        IntentFilter intentFilter = new IntentFilter();
        intentFilter.addAction(WifiManager.NETWORK_STATE_CHANGED_ACTION);
        mContext.get().registerReceiver(getWifiConnectedReceiver(), intentFilter);

        ConnectivityManager cm = (ConnectivityManager) mContext.get().getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo tmp = cm.getActiveNetworkInfo();

        if(tmp == null){
            System.out.println("No current network...");
            return false;
        }

        if(tmp.isConnected()){
            WifiInfo info = sWifiManager.getConnectionInfo();
            System.out.println("adding shit to prefs...");

            mEditor.putString("prev_network", info.getSSID());
            mEditor.putString("target_network", ssid);
            mEditor.putBoolean("test_connection", true);
            mEditor.commit(); // save now, blocking
        }

        System.out.println("Target: " + ssid);

        for(WifiConfiguration config : sWifiManager.getConfiguredNetworks()){
            System.out.println("Current: " + config.SSID.replace("\"", ""));

            if(config.SSID.replace("\"", "").equals(ssid)){
                System.out.println("Attempting to connect to " + ssid);
                return testConnection(config);
            }
        }

        return false;
    }

    public boolean testConnection(WifiConfiguration config){
        return sWifiManager.enableNetwork(config.networkId, true);
    }

    private void loadNetworks(){
        SQLiteDatabase db = WiSQLiteDatabase.getInstance(mContext.get()).getReadableDatabase();
        Cursor cur = db.rawQuery("select * from WifiConfiguration", null);

        if (cur != null && cur.moveToFirst()) {
            do {
                WiConfiguration wiConfiguration = new WiConfiguration(
                        cur.getString(cur.getColumnIndex("SSID")),
                        cur.getString(cur.getColumnIndex("password")),
                        cur.getString(cur.getColumnIndex("network_id")));
                mConfiguredNetworks.add(wiConfiguration);
            } while (cur.moveToNext());
        }
        cur.close();
    }

    public ArrayList<WifiConfiguration> getUnConfiguredNetworks() {
        return mNotConfiguredNetworks;
    }

    public ArrayList<WiConfiguration> getConfiguredNetworks() {
        return mConfiguredNetworks;
    }

    public synchronized static WiNetworkManager getInstance(Context context) {
        if(sInstance == null){
            sInstance = new WiNetworkManager(context.getApplicationContext());
        }
        return sInstance;
    }

    public interface OnTestConnectionCompleteListener{
        void onTestConnectionComplete(boolean success);
    }

    private OnTestConnectionCompleteListener mOnTestConnectionCompleteListener;

    public void setOnTestConnectionCompleteListener(OnTestConnectionCompleteListener listener){
        mOnTestConnectionCompleteListener = listener;
    }

    private BroadcastReceiver getWifiConnectedReceiver(){
        return new BroadcastReceiver() {
            @Override
            public void onReceive(Context context, Intent intent) {

                String action = intent.getAction();
                SharedPreferences prefs = PreferenceManager.getDefaultSharedPreferences(context);
                SharedPreferences.Editor editor = prefs.edit();

                String prevNetwork = prefs.getString("prev_network", "");
                String targetNetwork = prefs.getString("target_network", "");
                boolean testConnection = prefs.getBoolean("test_connection", false);

                System.out.println("Prev " + prevNetwork);
                System.out.println("Target " + targetNetwork);

                if(action.equals(WifiManager.NETWORK_STATE_CHANGED_ACTION) && testConnection){
                    System.out.println("Connected changed!!!");
                    NetworkInfo info = intent.getParcelableExtra(WifiManager.EXTRA_NETWORK_INFO);
                    System.out.println("Connected? " + info.isConnected());

                    if(info.isConnected()){
                        WifiManager manager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
                        String ssid = manager.getConnectionInfo().getSSID();

                        System.out.println("Connected!!!");

                        if(ssid.replace("\"","").equals(targetNetwork.replace("\"", ""))){
                            System.out.println("The connection was successful!");

                            //update shared prefs to avoid testing the connection more than once
                            editor.putString("prev_network", "");
                            editor.putString("target_network", "");
                            editor.putBoolean("test_connection", false);
                            editor.commit();

                            context.unregisterReceiver(this);
                            // if the user was previously connect to 'A' and just tested connection 'B', disconnect them from 'B'
                            // and connect them back to 'A'.
                            if(!prevNetwork.replace("\"", "").equals(targetNetwork.replace("\"", ""))){
                                manager.disconnect(); // succeeded, close this connection

                                // connect back to original network..
                                manager.enableNetwork(getPrevNetworkId(manager, prevNetwork), true);
                            }

                            mOnTestConnectionCompleteListener.onTestConnectionComplete(true);
                        }
                    }
                }
            }

            private int getPrevNetworkId(WifiManager manager, String ssid){
                for(WifiConfiguration config: manager.getConfiguredNetworks()) {
                    if (config.SSID.replace("\"", "").equals(ssid.replace("\"", ""))) {
                        return config.networkId;
                    }
                }
                return -1;
            }
        };
    }
}
