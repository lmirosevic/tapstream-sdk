package com.tapstream.phonegap;

import android.util.Log;
import com.tapstream.sdk.*;
import java.lang.reflect.Method;
import java.util.*;
import org.apache.cordova.CordovaPlugin;
import org.apache.cordova.CallbackContext;
import org.json.*;

public class TapstreamPlugin extends CordovaPlugin {

    @Override
    public boolean execute(String action, JSONArray args, CallbackContext callbackContext) throws JSONException {
        if(action.equals("create")) {
            final String accountName = args.getString(0);
            final String developerSecret = args.getString(1);
            final JSONObject config = args.optJSONObject(2);

            final TapstreamPlugin self = this;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        self.create(accountName, developerSecret, config);
                    } catch(JSONException ex) {
                        Log.e(self.getClass().getSimpleName(), "Tapstream.create failed: " + ex.toString());
                    }
                }
            });
            return true;
        } else if(action.equals("fireEvent")) {
            final String eventName = args.getString(0);
            final boolean oneTimeOnly = args.getBoolean(1);
            final JSONObject params = args.optJSONObject(2);

            final TapstreamPlugin self = this;
            cordova.getThreadPool().execute(new Runnable() {
                public void run() {
                    try {
                        self.fireEvent(eventName, oneTimeOnly, params);
                    } catch(JSONException ex) {
                        Log.e(self.getClass().getSimpleName(), "Tapstream.fireEvent failed: " + ex.toString());
                    }
                }
            });
            return true;
        }
        return false;
    }

    private Method lookupMethod(String propertyName, Class argType) {
        String methodName = propertyName;
        if(methodName.length() > 0) {
            methodName = Character.toUpperCase(methodName.charAt(0)) + methodName.substring(1);
        }
        methodName = "set" + methodName;
        
        Method method = null;
        try {
            method = Config.class.getMethod(methodName, argType);
        } catch (NoSuchMethodException e) {
            Log.i(getClass().getSimpleName(), "Ignoring config field named '" + propertyName + "', probably not meant for this platform.");
        } catch(Exception e) {
            Log.e(getClass().getSimpleName(), "Error getting Config setter method: " + e.getMessage());
        }
        return method;
    }

    private void create(String accountName, String developerSecret, JSONObject configVals) throws JSONException {
        Config config = new Config();

        if(configVals != null) {
            Iterator<?> iter = configVals.keys();
            while(iter.hasNext()) {
                String key = (String)iter.next();

                if(key.equals("globalEventParams")) {
                    JSONObject globalEventParams = configVals.getJSONObject(key);
                    if(globalEventParams != null) {
                        Iterator<?> paramsIter = globalEventParams.keys();
                        while(paramsIter.hasNext()) {
                            String paramKey = (String)paramsIter.next();
                            Object paramValue = globalEventParams.get(paramKey);
                            config.globalEventParams.put(paramKey, paramValue);
                        }
                    }
                } else {
                    Object value = configVals.get(key);
                    if(value != null) {
                        try {
                            if(value instanceof String) {
                                Method method = lookupMethod(key, String.class);
                                if(method != null) {
                                    method.invoke(config, (String)value);
                                }
                            } else if(value instanceof Boolean) {
                                Method method = lookupMethod(key, boolean.class);
                                if(method != null) {
                                    method.invoke(config, (Boolean)value);
                                }
                            } else if(value instanceof Integer) {
                                Method method = lookupMethod(key, int.class);
                                if(method != null) {
                                    method.invoke(config, (Integer)value);
                                }
                            } else if(value instanceof Float) {
                                Method method = lookupMethod(key, float.class);
                                if(method != null) {
                                    method.invoke(config, (Float)value);
                                }
                            } else {
                                Log.e(getClass().getSimpleName(), "Config object will not accept type: " + value.getClass().toString());
                            }
                        } catch(Exception e) {
                            Log.e(getClass().getSimpleName(), "Error setting field on config object (key=" + key + "). " + e.getMessage());
                        }
                    }
                }
            }
        }

        Tapstream.create(this.cordova.getActivity().getApplication(), accountName, developerSecret, config);
    }

    private void fireEvent(String eventName, boolean oneTimeOnly, JSONObject params) throws JSONException {
        Event e = new Event(eventName, oneTimeOnly);
        if(params != null) {
            Iterator<?> iter = params.keys();
            while(iter.hasNext()) {
                String key = (String)iter.next();
                e.addPair(key, params.get(key));
            }
        }
        Tapstream.getInstance().fireEvent(e);
    }

}