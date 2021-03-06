package com.example.eric.wishare.dialog;

import android.content.Context;
import android.support.annotation.NonNull;
import android.view.LayoutInflater;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import com.afollestad.materialdialogs.DialogAction;
import com.afollestad.materialdialogs.MaterialDialog;
import com.example.eric.wishare.R;
import com.example.eric.wishare.WiContactList;
import com.example.eric.wishare.WiDataMessageController;
import com.example.eric.wishare.WiInvitationList;
import com.example.eric.wishare.WiSQLiteDatabase;
import com.example.eric.wishare.model.WiContact;
import com.example.eric.wishare.model.WiInvitation;
import com.example.eric.wishare.model.messaging.WiInvitationDataMessage;

import org.json.JSONObject;

import java.util.Map;

import static android.content.Context.LAYOUT_INFLATER_SERVICE;

public class WiInvitationAcceptDeclineDialog extends WiDialog {
    private LinearLayout mCustomView;
    private WiInvitation mInvitation;

    public interface OnAcceptedListener {
        void onAccepted(WiInvitation invitation);
    }

    interface OnDeclinedListener{
        void onDeclined(WiInvitation invitation);
    }

    private OnAcceptedListener mAcceptListener;
    private OnDeclinedListener mDeclineListener;

    private WiContactList mContactList;

    public WiInvitationAcceptDeclineDialog(Context context, WiInvitation invitation) {
        super(context);
        LayoutInflater inflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);

        mContactList = WiContactList.getInstance(context);

        mCustomView = (LinearLayout) inflater.inflate(R.layout.layout_accept_decline_invitation_dialog, null);
        mInvitation = invitation;

        WiContact sender = mContactList.getContactByPhone(invitation.sender);
        String owner = (sender != null) ? sender.getName() : invitation.sender;

        ((TextView) mCustomView.findViewById(R.id.tv_time_limit)).setText(invitation.timeLimit);
        ((TextView) mCustomView.findViewById(R.id.tv_data_limit)).setText(invitation.dataLimit);
        ((TextView) mCustomView.findViewById(R.id.tv_invitation_expiration)).setText(invitation.expires);
        ((TextView) mCustomView.findViewById(R.id.tv_invitation_owner)).setText(owner);
    }

    @Override
    public MaterialDialog build() {
        return new MaterialDialog.Builder(context.get())
                .title(mInvitation.networkName)
                .customView(mCustomView, true)
                .positiveText("Accept")
                .onPositive(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if(mAcceptListener != null){
                            mAcceptListener.onAccepted(mInvitation);
                        }

                        WiInvitationDataMessage msg = new WiInvitationDataMessage(
                                mInvitation,
                                mInvitation.sender,
                                true // <- tells the host that we have accepted invite.
                        ) {
                            @Override
                            public void onResponse(JSONObject response) {

                            }
                        };

                        WiDataMessageController.getInstance(context.get()).send(msg);
                        WiInvitationList.getInstance(context.get()).remove(mInvitation);

                        Toast.makeText(context.get(), mInvitation.networkName + " has been accepted", Toast.LENGTH_LONG).show();
                    }
                })
                .negativeText("Decline")
                .onNegative(new MaterialDialog.SingleButtonCallback() {
                    @Override
                    public void onClick(@NonNull MaterialDialog dialog, @NonNull DialogAction which) {
                        if(mDeclineListener != null){
                            mDeclineListener.onDeclined(mInvitation);
                        }
                        WiSQLiteDatabase.getInstance(context.get()).delete(mInvitation);
                    }
                })
                .build();
    }

    public void setOnAcceptedListener(OnAcceptedListener listener){
        mAcceptListener = listener;
    }
    public void setOnDeclinedListener(OnDeclinedListener listener){
        mDeclineListener = listener;
    }
}
