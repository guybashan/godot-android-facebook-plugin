package com.bashan.godot.facebook;

import android.content.Intent;
import android.os.Bundle;
import android.util.Log;

import androidx.annotation.NonNull;

import com.facebook.AccessToken;
import com.facebook.CallbackManager;
import com.facebook.FacebookCallback;
import com.facebook.FacebookException;
import com.facebook.FacebookSdk;
import com.facebook.FacebookSdkNotInitializedException;
import com.facebook.GraphRequest;
import com.facebook.GraphResponse;
import com.facebook.appevents.AppEventsLogger;
import com.facebook.login.LoginManager;
import com.facebook.login.LoginResult;
import com.facebook.share.model.GameRequestContent;
import com.facebook.share.widget.GameRequestDialog;

import org.godotengine.godot.Dictionary;
import org.godotengine.godot.Godot;
import org.godotengine.godot.GodotLib;
import org.godotengine.godot.plugin.GodotPlugin;
import org.json.JSONArray;
import org.json.JSONException;
import org.json.JSONObject;

import java.util.Arrays;
import java.util.List;

public class GodotFacebook extends GodotPlugin {

    private static final String TAG = "godot-facebook";

    private Godot activity = null;
    private Integer facebookCallbackId = 0;
    private GameRequestDialog requestDialog;
    private CallbackManager callbackManager;
    private AppEventsLogger fbLogger;

    public GodotFacebook(Godot godot) {
        super(godot);
        activity = godot;
    }

    public void init(final String key) {
        Log.e(TAG, "Initializing Facebook plugin with token: " + key);
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                try {
                    FacebookSdk.setApplicationId(key);
                    FacebookSdk.sdkInitialize(activity.getActivity().getApplicationContext());

                    callbackManager = CallbackManager.Factory.create();
                    fbLogger = AppEventsLogger.newLogger(activity.getActivity().getApplicationContext(), key);
                    requestDialog = new GameRequestDialog(activity.getActivity());
                    requestDialog.registerCallback(callbackManager, new FacebookCallback<GameRequestDialog.Result>() {
                        public void onSuccess(GameRequestDialog.Result result) {
                            String id = result.getRequestId();
                            //result.getRequestRecipients()
                            Log.i(TAG, "Facebook game request finished: " + id);
                            Dictionary map;
                            try {
                                JSONObject object = new JSONObject();
                                object.put("requestId", result.getRequestId());
                                object.put("recipientsIds", new JSONArray(result.getRequestRecipients()));
                                map = JsonHelper.toMap(object);
                            } catch (JSONException e) {
                                map = new Dictionary();
                            }
                            GodotLib.calldeferred(facebookCallbackId, "request_success", new Object[]{map});
                        }

                        public void onCancel() {
                            Log.w(TAG, "Facebook game request cancelled");
                            GodotLib.calldeferred(facebookCallbackId, "request_cancelled", new Object[]{});
                        }

                        public void onError(FacebookException error) {
                            Log.e(TAG, "Failed to send facebook game request: " + error.getMessage());
                            GodotLib.calldeferred(facebookCallbackId, "request_failed", new Object[]{error.toString()});
                        }
                    });

                    LoginManager.getInstance().registerCallback(callbackManager, new FacebookCallback<LoginResult>() {
                        @Override
                        public void onSuccess(LoginResult loginResult) {
                            AccessToken at = loginResult.getAccessToken();
                            GodotLib.calldeferred(facebookCallbackId, "login_success", new Object[]{at.getToken()});
                        }

                        @Override
                        public void onCancel() {
                            GodotLib.calldeferred(facebookCallbackId, "login_cancelled", new Object[]{});
                        }

                        @Override
                        public void onError(FacebookException exception) {
                            GodotLib.calldeferred(facebookCallbackId, "login_failed", new Object[]{exception.toString()});
                        }
                    });

                } catch (FacebookSdkNotInitializedException e) {
                    Log.e(TAG, "Failed to initialize FacebookSdk: " + e.getMessage());
                } catch (Exception e) {
                    Log.e(TAG, "Exception: " + e.getMessage());
                }
            }
        });
    }

    public void setFacebookCallbackId(int facebookCallbackId) {
        this.facebookCallbackId = facebookCallbackId;
    }

    public int getFacebookCallbackId() {
        return facebookCallbackId;
    }

    public void gameRequest(final String message, final String recipient, final String objectId) {
        Log.i(TAG, "Facebook gameRequest");
        activity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                if (FacebookSdk.isInitialized()) {
                    GameRequestContent.Builder builder = new GameRequestContent.Builder();
                    builder.setMessage(message);
                    if (recipient != null && recipient.length() > 0)
                        builder.setTo(recipient);
                    if (objectId != null && objectId.length() > 0) {
                        builder.setActionType(GameRequestContent.ActionType.SEND);
                        builder.setObjectId(objectId);
                    }
                    GameRequestContent content = builder.build();
                    requestDialog.show(content);
                } else {
                    Log.d(TAG, "Facebook sdk not initialized");
                }
            }
        });
    }

    public void login(String[] permissions) {
        Log.i(TAG, "Facebook login");
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            GodotLib.calldeferred(facebookCallbackId, "login_success", new Object[]{accessToken.getToken()});
        } else {
            List<String> perm = Arrays.asList(permissions);
            LoginManager.getInstance().logInWithReadPermissions(activity, perm);
        }
    }

    public void logout() {
        Log.i(TAG, "Facebook logout");
        LoginManager.getInstance().logOut();
    }

    public boolean isLoggedIn() {
        Log.i(TAG, "Facebook isLoggedIn");
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken == null || accessToken.isExpired()) {
            //GodotLib.calldeferred(facebookCallbackId, "login_failed", new Object[]{"No token"});
            return false;
        } else {
            //GodotLib.calldeferred(facebookCallbackId, "login_success", new Object[]{accessToken.getToken()});
            return true;
        }
    }

    public void userProfile(final int callbackObject, final String callbackMethod) {
        Log.i(TAG, "Facebook userProfile");
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            GraphRequest gr = GraphRequest.newMeRequest(accessToken, new GraphRequest.GraphJSONObjectCallback() {
                @Override
                public void onCompleted(JSONObject object, GraphResponse response) {
                    if (object == null) {
                        Log.e(TAG, "Facebook graph request error: " + response.toString());
                        GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{"Error"});
                    } else {
                        Log.i(TAG, "Facebook graph response: " + object.toString());
                        try {
                            Dictionary map = JsonHelper.toMap(object);
                            //String res = object.toString();
                            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{map});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{"JSON Error"});
                        }
                    }
                }
            });
            gr.executeAsync();
        } else {
            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{"No token"});
        }
    }

    public void callApi(final String path, final Dictionary properties, final int callbackObject, final String callbackMethod) {
        Log.i(TAG, "Facebook callApi");
        AccessToken accessToken = AccessToken.getCurrentAccessToken();
        if (accessToken != null && !accessToken.isExpired()) {
            GraphRequest gr = GraphRequest.newGraphPathRequest(accessToken, path, new GraphRequest.Callback() {
                @Override
                public void onCompleted(GraphResponse response) {
                    JSONObject object = response.getJSONObject();
                    if (object == null || response.getError() != null) {
                        String err = response.getError().toString();
                        Log.e(TAG, "Facebook graph request error: " + response.toString());
                        GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{err});
                    } else {
                        Log.i(TAG, "Facebook graph response: " + object.toString());
                        try {
                            Dictionary map = JsonHelper.toMap(object);
                            Log.i(TAG, "Api result: " + map.toString());
                            //String res = object.toString();
                            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{map});
                        } catch (JSONException e) {
                            e.printStackTrace();
                            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{e.toString()});
                        }
                    }
                }
            });
            Bundle params = gr.getParameters();
            for (String key : properties.get_keys()) {
                params.putString(key, properties.get(key).toString());
            }
            gr.setParameters(params);
            gr.executeAsync();
        } else {
            GodotLib.calldeferred(callbackObject, callbackMethod, new Object[]{"No token"});
        }

    }

    public void set_push_token(final String token) {
        Log.i(TAG, "Facebook set_push_token");
        if (fbLogger == null) {
            Log.w(TAG, "Facebook logger doesn't inited yet!");
            return;
        }
        fbLogger.setPushNotificationsRegistrationId(token);
    }

    public void log_event(final String event) {
        Log.i(TAG, "Facebook log_event");
        if (fbLogger == null) {
            Log.w(TAG, "Facebook logger doesn't inited yet!");
            return;
        }
        fbLogger.logEvent(event);
    }

    public void log_event_value(final String event, double value) {
        Log.i(TAG, "Facebook log_event_value");
        if (fbLogger == null) {
            Log.w(TAG, "Facebook logger doesn't inited yet!");
            return;
        }
        fbLogger.logEvent(event, value);
    }

    public void log_event_params(final String event, final Dictionary params) {
        Log.i(TAG, "Facebook log_event_params");
        if (fbLogger == null) {
            Log.w(TAG, "Facebook logger doesn't inited yet!");
            return;
        }
        Bundle parameters = new Bundle();
        for (String key : params.get_keys()) {
            parameters.putString(key, params.get(key).toString());
        }
        fbLogger.logEvent(event, parameters);
    }

    public void log_event_value_params(final String event, double value, final Dictionary params) {
        Log.i(TAG, "Facebook log_event_value_params");
        if (fbLogger == null) {
            Log.w(TAG, "Facebook logger doesn't inited yet!");
            return;
        }
        Bundle parameters = new Bundle();
        for (String key : params.get_keys()) {
            if (params.get(key) != null)
                parameters.putString(key, params.get(key).toString());
        }
        fbLogger.logEvent(event, value, parameters);
    }

    // Internal methods

    public void callbackSuccess(String ticket, String signature, String sku) {
        //GodotLib.callobject(facebookCallbackId, "purchase_success", new Object[]{ticket, signature, sku});
        //GodotLib.calldeferred(purchaseCallbackId, "consume_fail", new Object[]{});
    }

    @Override
    public void onMainActivityResult(int requestCode, int resultCode, Intent data) {
        callbackManager.onActivityResult(requestCode, resultCode, data);
    }

    @NonNull
    @Override
    public String getPluginName() {
        return "GodotFacebook";
    }

    @NonNull
    @Override
    public List<String> getPluginMethods() {
        return Arrays.asList(
                "init",
                "setFacebookCallbackId",
                "getFacebookCallbackId",
                "gameRequest",
                "login",
                "logout",
                "isLoggedIn",
                "userProfile",
                "callApi",
                "set_push_token",
                "log_event",
                "log_event_value",
                "log_event_params",
                "log_event_value_params"
        );
    }
}
