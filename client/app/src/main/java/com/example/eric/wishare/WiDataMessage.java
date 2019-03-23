package com.example.eric.wishare;

import android.util.Log;

import com.android.volley.DefaultRetryPolicy;
import com.android.volley.Request;
import com.android.volley.Response;
import com.android.volley.VolleyError;
import com.android.volley.toolbox.JsonObjectRequest;
import com.example.eric.wishare.model.WiInvitation;

import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import static com.example.eric.wishare.WiDataMessageController.BASE_URL;

public class WiDataMessage extends JSONObject {

    private final String TAG = "WiDataMessage";
    private String mUrl;

    public final static Integer MSG_ACKNOWLEDGE = 0;
    public final static Integer MSG_INVITATION = 1;
    public final static Integer MSG_CREDENTIALS = 2;

    private OnResponseListener mOnResponseListener;

    public WiDataMessage() {
        mUrl = BASE_URL;
    }

    public WiDataMessage(Integer msg_type){
        try {
            put("msg_type", msg_type);
        } catch (Exception e){

        }
    }

    public void setUrl(String url){
        mUrl = url;
    }

    public JsonObjectRequest build(){
        Log.d(TAG, "Building WiDataMessage...");
        JsonObjectRequest req = new JsonObjectRequest(Request.Method.POST, mUrl, this,
                new Response.Listener<JSONObject>() {
                    @Override
                    public void onResponse(JSONObject response) {
                        if(mOnResponseListener != null){
                            mOnResponseListener.onResponse(response);
                        }
                    }
                },
                new Response.ErrorListener() {
                    @Override
                    public void onErrorResponse(VolleyError error) {
                        if(mOnResponseListener != null){
                            if(error != null){
                                error.printStackTrace();
                            }
                            mOnResponseListener.onResponse(null);
                        }
                    }
                });


        req.setRetryPolicy(new DefaultRetryPolicy(20000,
                DefaultRetryPolicy.DEFAULT_MAX_RETRIES,
                DefaultRetryPolicy.DEFAULT_BACKOFF_MULT));

        return req;
    }

    public void setOnResponseListener(OnResponseListener listener){
        mOnResponseListener = listener;
    }

    public interface OnResponseListener {
        void onResponse(JSONObject response);
    }

    public void put(WiInvitation invitation) {
        try {
            if(!has("networks")) {
                put("networks", new JSONArray());
            }
            getJSONArray("networks").put(invitation.toJSON());

        } catch (JSONException e) {

        }
    }
}
