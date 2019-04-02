package com.example.eric.wishare.dialog;

import android.content.Context;
import android.database.sqlite.SQLiteDatabase;
import android.net.wifi.WifiConfiguration;
import android.support.annotation.NonNull;
import android.text.InputType;
import android.util.Log;
import android.view.View;
import android.widget.Button;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.eric.wishare.WiNetworkManager;
import com.example.eric.wishare.WiSQLiteDatabase;
import com.example.eric.wishare.model.WiConfiguration;

import java.lang.ref.WeakReference;
import java.util.ArrayList;
import java.util.List;

public class WiAddNetworkDialog extends WiDialog {
    private WiNetworkManager mManager;
    private ArrayList<String> mNetworks;
    private OnPasswordEnteredListener mListener;
    private WeakReference<Context> mContext;

    public interface OnPasswordEnteredListener {
        void OnPasswordEntered(WiConfiguration config);
    }

    public void setOnPasswordEnteredListener(OnPasswordEnteredListener listener) {
        mListener = listener;
    }

    @Override
    public MaterialDialog build() {
        updateNetworks();
        return new MaterialDialog.Builder(context.get())
                .title("Select a Network")
                .items(mNetworks)
                .itemsCallback(onNetWorkSelect())
                .negativeText("Cancel")
                .build();
    }

    private void updateNetworks() {
        ArrayList<String> temp = new ArrayList<>();

        for(WifiConfiguration config : mManager.getUnConfiguredNetworks()) {
            temp.add(config.SSID.replace("\"", ""));
        }

        mNetworks = temp;
    }

    public WiAddNetworkDialog(Context context, Button btnAddNetwork){
        super(context);
        mContext = new WeakReference<>(context.getApplicationContext());
        mManager = WiNetworkManager.getInstance(context.getApplicationContext());
        List<WifiConfiguration> wifiList = mManager.getUnConfiguredNetworks();

        mNetworks = new ArrayList<>();

        for(WifiConfiguration config : wifiList) {
            mNetworks.add(config.SSID.replace("\"", ""));
        }

        btnAddNetwork.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                WiAddNetworkDialog.this.show();
            }
        });

        build();
    }

    private MaterialDialog.ListCallback onNetWorkSelect() {
        return new MaterialDialog.ListCallback() {
            @Override
            public void onSelection(MaterialDialog dialog, View itemView, int position, final CharSequence wifiName) {
                new MaterialDialog.Builder(context.get())
                        .title("Enter Password")
                        .inputType(InputType.TYPE_CLASS_TEXT | InputType.TYPE_TEXT_VARIATION_PASSWORD)
                        .input("Password", "", false, new MaterialDialog.InputCallback() {
                            @Override
                            public void onInput(@NonNull MaterialDialog dialog, final CharSequence password) {
                                WiConfiguration config = new WiConfiguration(wifiName.toString(), password.toString());
                                mListener.OnPasswordEntered(config);

                                WiSQLiteDatabase.getInstance(mContext.get()).insert(config);
                                mManager.configureNetwork(config);
                                Toast.makeText(mContext.get(), "Wifi name " + wifiName, Toast.LENGTH_LONG).show();
                            }})
                        .neutralText("Test Connection")
                        .onNeutral(new MaterialDialog.SingleButtonCallback() {
                            @Override
                            public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                                WiNetworkManager mNetworkManager = WiNetworkManager.getInstance(mContext.get());
                                mNetworkManager.testConnection("eduroam");

                                final MaterialDialog d2 = new MaterialDialog.Builder(mContext.get()).progress(true, 100).content("Testing connection...").show();

                                System.out.println("About to test...");

                                mNetworkManager.setOnTestConnectionCompleteListener(new WiNetworkManager.OnTestConnectionCompleteListener() {
                                    @Override
                                    public void onTestConnectionComplete(boolean success) {
                                        d2.dismiss();
                                        new MaterialDialog.Builder(mContext.get()).title("Connection successful!").positiveText("Ok").show();
                                    }
                                });
                            }
                        })
                        .show();
            }
        };
    }
}
