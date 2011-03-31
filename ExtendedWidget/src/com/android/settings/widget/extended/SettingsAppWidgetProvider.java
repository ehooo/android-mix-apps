/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.settings.widget.extended;

import java.io.IOException;
import java.lang.reflect.Method;
import android.Manifest.permission;
import android.app.PendingIntent;
import android.app.PendingIntent.CanceledException;
import android.app.UiModeManager;
import android.appwidget.AppWidgetManager;
import android.appwidget.AppWidgetProvider;
import android.content.ComponentName;
import android.content.ContentResolver;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.res.Configuration;
import android.hardware.Camera;
import android.location.LocationManager;
import android.net.ConnectivityManager;
import android.net.Uri;
import android.net.wifi.WifiConfiguration;
import android.net.wifi.WifiManager;
import android.os.AsyncTask;
import android.provider.Settings;
import android.provider.Settings.SettingNotFoundException;
import android.util.Log;
import android.widget.RemoteViews;

/**
 * Provides control of power-related settings from a widget.
 */
public class SettingsAppWidgetProvider extends AppWidgetProvider {
    static final String TAG = "extended.SettingsAppWidgetProvider";

    static final ComponentName THIS_APPWIDGET =
            new ComponentName("com.android.settings.widget.extended",
                    "com.android.settings.widget.extended.SettingsAppWidgetProvider");

    private static final int BUTTON_WIFI = 0;
    private static final int BUTTON_CAR = 1;
    private static final int BUTTON_CONNEXION = 2;
    private static final int BUTTON_FLASH = 3;
    private static final int BUTTON_ROTATE = 4;

    // This widget keeps track of two sets of states:
    // "3-state": STATE_DISABLED, STATE_ENABLED, STATE_INTERMEDIATE
    // "5-state": STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON, STATE_TURNING_OFF, STATE_UNKNOWN
    private static final int STATE_DISABLED = 0;
    private static final int STATE_ENABLED = 1;
    private static final int STATE_TURNING_ON = 2;
    private static final int STATE_TURNING_OFF = 3;
    private static final int STATE_UNKNOWN = 4;
    private static final int STATE_INTERMEDIATE = 5;

    private static final int POS_LEFT = 0;
    private static final int POS_CENTER = 1;
    private static final int POS_RIGHT = 2;

    private static final int[] IND_DRAWABLE_OFF = {
        R.drawable.appwidget_settings_ind_off_l,
        R.drawable.appwidget_settings_ind_off_c,
        R.drawable.appwidget_settings_ind_off_r
    };

    private static final int[] IND_DRAWABLE_MID = {
        R.drawable.appwidget_settings_ind_mid_l,
        R.drawable.appwidget_settings_ind_mid_c,
        R.drawable.appwidget_settings_ind_mid_r
    };

    private static final int[] IND_DRAWABLE_ON = {
        R.drawable.appwidget_settings_ind_on_l,
        R.drawable.appwidget_settings_ind_on_c,
        R.drawable.appwidget_settings_ind_on_r
    };

    private static final StateTracker sWifiState = new WifiStateTracker();
    private static final StateTracker sRotateState = new RotateStateTracker();
    private static final StateTracker sFlashState = new FlashStateTracker();
    private static final StateTracker sConnexionState = new ConnexionStateTracker();
    private static final StateTracker sCarState = new CarStateTracker();

    /**
     * The state machine for a setting's toggling, tracking reality
     * versus the user's intent.
     *
     * This is necessary because reality moves relatively slowly
     * (turning on &amp; off radio drivers), compared to user's
     * expectations.
     */
    private abstract static class StateTracker {
        // Is the state in the process of changing?
        private boolean mInTransition = false;
        private Boolean mActualState = null;  // initially not set
        private Boolean mIntendedState = null;  // initially not set

        // Did a toggle request arrive while a state update was
        // already in-flight?  If so, the mIntendedState needs to be
        // requested when the other one is done, unless we happened to
        // arrive at that state already.
        private boolean mDeferredStateChangeRequestNeeded = false;

        /**
         * User pressed a button to change the state.  Something
         * should immediately appear to the user afterwards, even if
         * we effectively do nothing.  Their press must be heard.
         */
        public final void toggleState(Context context) {
            int currentState = getTriState(context);
            boolean newState = false;
            switch (currentState) {
                case STATE_ENABLED:
                    newState = false;
                    break;
                case STATE_DISABLED:
                    newState = true;
                    break;
                case STATE_INTERMEDIATE:
                    if (mIntendedState != null) {
                        newState = !mIntendedState;
                    }
                    break;
            }
            mIntendedState = newState;
            if (mInTransition) {
                // We don't send off a transition request if we're
                // already transitioning.  Makes our state tracking
                // easier, and is probably nicer on lower levels.
                // (even though they should be able to take it...)
                mDeferredStateChangeRequestNeeded = true;
            } else {
                mInTransition = true;
                requestStateChange(context, newState);
            }
        }

        /**
         * Return the ID of the main large image button for the setting.
         */
        public abstract int getButtonId();

        /**
         * Returns the small indicator image ID underneath the setting.
         */
        public abstract int getIndicatorId();

        /**
         * Returns the resource ID of the image to show as a function of
         * the on-vs-off state.
         */
        public abstract int getButtonImageId(boolean on);

        /**
         * Returns the position in the button bar - either POS_LEFT, POS_RIGHT or POS_CENTER.
         */
        public int getPosition() { return POS_CENTER; }

        /**
         * Updates the remote views depending on the state (off, on,
         * turning off, turning on) of the setting.
         */
        public final void setImageViewResources(Context context, RemoteViews views) {
            int buttonId = getButtonId();
            int indicatorId = getIndicatorId();
            int pos = getPosition();
            switch (getTriState(context)) {
                case STATE_DISABLED:
                    views.setImageViewResource(buttonId, getButtonImageId(false));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_OFF[pos]);
                    break;
                case STATE_ENABLED:
                    views.setImageViewResource(buttonId, getButtonImageId(true));
                    views.setImageViewResource(
                        indicatorId, IND_DRAWABLE_ON[pos]);
                    break;
                case STATE_INTERMEDIATE:
                    // In the transitional state, the bottom green bar
                    // shows the tri-state (on, off, transitioning), but
                    // the top dark-gray-or-bright-white logo shows the
                    // user's intent.  This is much easier to see in
                    // sunlight.
                    if (isTurningOn()) {
                        views.setImageViewResource(buttonId, getButtonImageId(true));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_MID[pos]);
                    } else {
                        views.setImageViewResource(buttonId, getButtonImageId(false));
                        views.setImageViewResource(
                            indicatorId, IND_DRAWABLE_OFF[pos]);
                    }
                    break;
            }
        }

        /**
         * Update internal state from a broadcast state change.
         */
        public abstract void onActualStateChange(Context context, Intent intent);

        /**
         * Sets the value that we're now in.  To be called from onActualStateChange.
         *
         * @param newState one of STATE_DISABLED, STATE_ENABLED, STATE_TURNING_ON,
         *                 STATE_TURNING_OFF, STATE_UNKNOWN
         */
        protected final void setCurrentState(Context context, int newState) {
            final boolean wasInTransition = mInTransition;
            switch (newState) {
                case STATE_DISABLED:
                    mInTransition = false;
                    mActualState = false;
                    break;
                case STATE_ENABLED:
                    mInTransition = false;
                    mActualState = true;
                    break;
                case STATE_TURNING_ON:
                    mInTransition = true;
                    mActualState = false;
                    break;
                case STATE_TURNING_OFF:
                    mInTransition = true;
                    mActualState = true;
                    break;
            }

            if (wasInTransition && !mInTransition) {
                if (mDeferredStateChangeRequestNeeded) {
                    Log.v(TAG, "processing deferred state change");
                    if (mActualState != null && mIntendedState != null &&
                        mIntendedState.equals(mActualState)) {
                        Log.v(TAG, "... but intended state matches, so no changes.");
                    } else if (mIntendedState != null) {
                        mInTransition = true;
                        requestStateChange(context, mIntendedState);
                    }
                    mDeferredStateChangeRequestNeeded = false;
                }
            }
        }


        /**
         * If we're in a transition mode, this returns true if we're
         * transitioning towards being enabled.
         */
        public final boolean isTurningOn() {
            return mIntendedState != null && mIntendedState;
        }

        /**
         * Returns simplified 3-state value from underlying 5-state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, or STATE_INTERMEDIATE
         */
        public final int getTriState(Context context) {
            if (mInTransition) {
                // If we know we just got a toggle request recently
                // (which set mInTransition), don't even ask the
                // underlying interface for its state.  We know we're
                // changing.  This avoids blocking the UI thread
                // during UI refresh post-toggle if the underlying
                // service state accessor has coarse locking on its
                // state (to be fixed separately).
                return STATE_INTERMEDIATE;
            }
            switch (getActualState(context)) {
                case STATE_DISABLED:
                    return STATE_DISABLED;
                case STATE_ENABLED:
                    return STATE_ENABLED;
                default:
                    return STATE_INTERMEDIATE;
            }
        }

        /**
         * Gets underlying actual state.
         *
         * @param context
         * @return STATE_ENABLED, STATE_DISABLED, STATE_ENABLING, STATE_DISABLING,
         *         or or STATE_UNKNOWN.
         */
        public abstract int getActualState(Context context);

        /**
         * Actually make the desired change to the underlying radio
         * API.
         */
        protected abstract void requestStateChange(Context context, boolean desiredState);
    }


    private static final class CarStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_car; }
        public int getIndicatorId() { return R.id.ind_car; }
        public int getButtonImageId(boolean on) { return R.drawable.car; }
        public int getPosition() { return POS_RIGHT; }

        @Override
        public int getActualState(Context context) {
            ContentResolver resolver = context.getContentResolver();
            boolean on = Settings.Secure.isLocationProviderEnabled(
                resolver, LocationManager.GPS_PROVIDER);
            return on ? STATE_INTERMEDIATE : STATE_DISABLED;
        }

        public static final String PROVIDERS_CHANGED_ACTION = "android.location.PROVIDERS_CHANGED";
        @Override
        public void onActualStateChange(Context context, Intent unused) {
        	if (!PROVIDERS_CHANGED_ACTION.equals(unused.getAction())) {
                return;
            }
            // Note: the broadcast location providers changed intent
            // doesn't include an extras bundles saying what the new value is.
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                	if(desiredState){
                		Log.d(TAG, "Turn on GPS");
                		//Como nosotros no tenemos permiso para activar el GPS se lo mandamos al widget del sistema
            			Intent settings = new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS);
                		settings.setClassName("com.android.settings", "com.android.settings.widget.SettingsAppWidgetProvider");
                		settings.addCategory(Intent.CATEGORY_ALTERNATIVE);
                		settings.setData(Uri.parse("custom:3"));
            			//settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            			//context.startActivity(settings);
                		try {
							PendingIntent.getBroadcast(context, 0, settings, 0).send();
						} catch (CanceledException e) {
							e.printStackTrace();
							//No debe de dar error nunca
						}
					}

            		Log.d(TAG, "Turn on CARMODE");
                	UiModeManager carmode = (UiModeManager) context.getSystemService(Context.UI_MODE_SERVICE);
                	if(carmode.getCurrentModeType() != Configuration.UI_MODE_TYPE_CAR){
                		carmode.enableCarMode(0);
                	}

            		Log.d(TAG, "Turn on CARDOCK");
                	Intent cardock = new Intent(Intent.ACTION_MAIN);
                	cardock.setClassName("com.android.cardock", "com.android.cardock.CarDockActivity");
                	cardock.addCategory(Intent.CATEGORY_LAUNCHER);
                	//cardock.setClassName("com.google.android.carhome", "com.google.android.carhome.CarHome");
                	//cardock.addCategory(Intent.CATEGORY_CAR_DOCK);
					cardock.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
					cardock.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
					context.startActivity(cardock);

					return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                	//No se colorea nunca el estado
                    setCurrentState(context, STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    /**
     * Subclass of StateTracker to get/set Wifi state.
     */
    private static final class WifiStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_wifi; }
        public int getIndicatorId() { return R.id.ind_wifi; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_wifi_on
                    : R.drawable.ic_appwidget_settings_wifi_off;
        }

        @Override
        public int getPosition() { return POS_LEFT; }

        @Override
        public int getActualState(Context context) {
        	WifiManager wifiManager = (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            try {
    	    	Method method = wifiManager.getClass().getMethod("getWifiApState");
    	    	return wifiStateToFiveState((Integer) method.invoke(wifiManager));
    	    } catch (Exception e) {
            	e.printStackTrace();
    	    }
            return STATE_UNKNOWN;
        }

        @Override
        protected void requestStateChange(Context context, final boolean desiredState) {
            final WifiManager wifiManager =
                    (WifiManager) context.getSystemService(Context.WIFI_SERVICE);
            if (wifiManager == null) {
                Log.d(TAG, "No wifiManager.");
                return;
            }

            // Actually request the wifi change and persistent
            // settings write off the UI thread, as it can take a
            // user-noticeable amount of time, especially if there's
            // disk contention.
            new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                    /**
                     * Disable tethering if enabling Wifi
                     */
                	int wifiState = wifiManager.getWifiState();
                    if (desiredState && ((wifiState == WifiManager.WIFI_STATE_ENABLING) ||
                                         (wifiState == WifiManager.WIFI_STATE_ENABLED))) {
                    	wifiManager.setWifiEnabled(false);
                    }
                    try {
                        Method method = wifiManager.getClass().getMethod("setWifiApEnabled", WifiConfiguration.class, boolean.class);
                        method.invoke(wifiManager, null, desiredState);
                    } catch (Exception e) {
                    	e.printStackTrace();
                    }
                    return null;
                }
            }.execute();
        }

        public static final String WIFI_AP_STATE_CHANGED_ACTION = "android.net.wifi.WIFI_AP_STATE_CHANGED";

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            if (!WIFI_AP_STATE_CHANGED_ACTION.equals(intent.getAction())) {
                return;
            }
            int wifiState = intent.getIntExtra(WifiManager.EXTRA_WIFI_STATE, -1);
            setCurrentState(context, wifiStateToFiveState(wifiState));
        }

    	private static final int WIFI_AP_STATE_DISABLING = 0;
    	private static final int WIFI_AP_STATE_DISABLED = 1;
    	private static final int WIFI_AP_STATE_ENABLING = 2;
    	private static final int WIFI_AP_STATE_ENABLED = 3;

        /**
         * Converts WifiManager's state values into our
         * Wifi/Bluetooth-common state values.
         */
        private static int wifiStateToFiveState(int wifiState) {
            switch (wifiState) {
                case WIFI_AP_STATE_DISABLED:
                    return STATE_DISABLED;
                case WIFI_AP_STATE_ENABLED:
                    return STATE_ENABLED;
                case WIFI_AP_STATE_DISABLING:
                    return STATE_TURNING_OFF;
                case WIFI_AP_STATE_ENABLING:
                    return STATE_TURNING_ON;
                default:
                    return STATE_UNKNOWN;
            }
        }
    }

    private static final class RotateStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_rotate; }
        public int getIndicatorId() { return R.id.ind_rotate; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_rotate_on
                    : R.drawable.ic_appwidget_settings_rotate_off;
        }

        @Override
        public int getActualState(Context context) {
        	int rote;
			try {
				rote = Settings.System.getInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION);
				return rote>0 ? STATE_ENABLED : STATE_DISABLED;
			} catch (SettingNotFoundException e) { e.printStackTrace(); }
			return STATE_INTERMEDIATE;
        }

        @Override
        protected void requestStateChange(final Context context, final boolean desiredState) {
        	new AsyncTask<Void, Void, Void>() {
                @Override
                protected Void doInBackground(Void... args) {
                	Settings.System.putInt(context.getContentResolver(), Settings.System.ACCELEROMETER_ROTATION, desiredState? 1 : 0);
                    return null;
                }

                @Override
                protected void onPostExecute(Void result) {
                	setCurrentState(context, getActualState(context));
                    updateWidget(context);
                }
            }.execute();
        }

        @Override
        public void onActualStateChange(Context context, Intent intent) {
            setCurrentState(context, getActualState(context));
        }

    }

    private static final class FlashStateTracker extends StateTracker {
    	private Camera camara = null;

        public int getButtonId() { return R.id.img_flash; }
        public int getIndicatorId() { return R.id.ind_flash; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_flash_on
                    : R.drawable.ic_appwidget_settings_flash_off;
        }

        @Override
        public int getActualState(Context context) {
            return camara!=null ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                	if(camara!=null){
    					camara.release();
    		    		camara = null;
    				}else{
    					try {
	    					camara = Camera.open();
	    					Camera.Parameters params = camara.getParameters();
	    					params.setFlashMode(Camera.Parameters.FLASH_MODE_TORCH);
	    					camara.setParameters(params);
    						camara.setPreviewDisplay(null);
    					} catch (IOException e) {
    						e.printStackTrace();
    					} catch (RuntimeException e) {
    						e.printStackTrace();
    					}
    				}
                    return desiredState;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                	setCurrentState(context, result?STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    private static final class ConnexionStateTracker extends StateTracker {
        public int getButtonId() { return R.id.img_connexion; }
        public int getIndicatorId() { return R.id.ind_connexion; }
        public int getButtonImageId(boolean on) {
            return on ? R.drawable.ic_appwidget_settings_connexion_on
                    : R.drawable.ic_appwidget_settings_connexion_off;
        }

        @Override
        public int getActualState(Context context) {
        	ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

        	boolean on = connManager.getBackgroundDataSetting();
        	//*
			try {
				Method method = connManager.getClass().getMethod("getMobileDataEnabled");
	        	on = (Boolean) method.invoke(connManager);
			} catch (Exception e) {
				e.printStackTrace();
        		Log.e(TAG, "Error al obtener el estado de la red");
			}//*/
            return on ? STATE_ENABLED : STATE_DISABLED;
        }

        @Override
        public void onActualStateChange(Context context, Intent unused) {
        	if (!ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(unused.getAction()) &&
        		!ConnectivityManager.CONNECTIVITY_ACTION.equals(unused.getAction()) ) {
                return;
            }
            setCurrentState(context, getActualState(context));
        }

        @Override
        public void requestStateChange(final Context context, final boolean desiredState) {
            final ConnectivityManager connManager = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);

            new AsyncTask<Void, Void, Boolean>() {
                @Override
                protected Boolean doInBackground(Void... args) {
                	try {
                		int grand = context.getPackageManager().checkPermission(permission.WRITE_SECURE_SETTINGS, context.getPackageName());
                		if(grand == PackageManager.PERMISSION_GRANTED){
                			Method method = connManager.getClass().getMethod("setBackgroundDataSetting", boolean.class);
                            //Method method = connManager.getClass().getMethod("setMobileDataEnabled", boolean.class);
                            method.invoke(connManager, desiredState);
                		}else{
                			Log.e(TAG, "Error, no hemos elevado permisos");
                			Intent settings = new Intent(Intent.ACTION_VIEW);
                			settings.setClassName("com.android.phone", "com.android.phone.Settings");
                			//settings.addCategory(Intent.CATEGORY_DEFAULT);
                			settings.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
                			
                			context.startActivity(settings);
                		}
                		return desiredState;
                    } catch (Exception e) {
                    	e.printStackTrace();
                		Log.e(TAG, "Error al setear el estado de la red");
                    }
                    return false;
                }

                @Override
                protected void onPostExecute(Boolean result) {
                    setCurrentState(
                        context,
                        result ? STATE_ENABLED : STATE_DISABLED);
                    updateWidget(context);
                }
            }.execute();
        }
    }

    @Override
    public void onUpdate(Context context, AppWidgetManager appWidgetManager,
            int[] appWidgetIds) {
        // Update each requested appWidgetId
        RemoteViews view = buildUpdate(context, -1);

        for (int i = 0; i < appWidgetIds.length; i++) {
            appWidgetManager.updateAppWidget(appWidgetIds[i], view);
        }
    }

    @Override
    public void onEnabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings.widget.extended", ".SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_ENABLED,
                PackageManager.DONT_KILL_APP);
    }

    @Override
    public void onDisabled(Context context) {
        PackageManager pm = context.getPackageManager();
        pm.setComponentEnabledSetting(
                new ComponentName("com.android.settings.widget.extended", ".SettingsAppWidgetProvider"),
                PackageManager.COMPONENT_ENABLED_STATE_DISABLED,
                PackageManager.DONT_KILL_APP);
    }

    /**
     * Load image for given widget and build {@link RemoteViews} for it.
     */
    static RemoteViews buildUpdate(Context context, int appWidgetId) {
        RemoteViews views = new RemoteViews(context.getPackageName(),
                R.layout.widget);
        views.setOnClickPendingIntent(R.id.btn_wifi,
        		getLaunchPendingIntent(context, appWidgetId, BUTTON_WIFI));
        views.setOnClickPendingIntent(R.id.btn_car,
                getLaunchPendingIntent(context, appWidgetId, BUTTON_CAR));
        views.setOnClickPendingIntent(R.id.btn_connexion,
                getLaunchPendingIntent(context, appWidgetId, BUTTON_CONNEXION));
        views.setOnClickPendingIntent(R.id.btn_flash,
                getLaunchPendingIntent(context, appWidgetId, BUTTON_FLASH));
        views.setOnClickPendingIntent(R.id.btn_rotate,
                getLaunchPendingIntent(context, appWidgetId, BUTTON_ROTATE));

        updateButtons(views, context);
        return views;
    }

    /**
     * Updates the widget when something changes, or when a button is pushed.
     *
     * @param context
     */
    public static void updateWidget(Context context) {
        RemoteViews views = buildUpdate(context, -1);
        // Update specific list of appWidgetIds if given, otherwise default to all
        final AppWidgetManager gm = AppWidgetManager.getInstance(context);
        gm.updateAppWidget(THIS_APPWIDGET, views);
    }

    /**
     * Updates the buttons based on the underlying states of wifi, etc.
     *
     * @param views   The RemoteViews to update.
     * @param context
     */
    private static void updateButtons(RemoteViews views, Context context) {
        sWifiState.setImageViewResources(context, views);
        sRotateState.setImageViewResources(context, views);
        sFlashState.setImageViewResources(context, views);
        sConnexionState.setImageViewResources(context, views);
        sCarState.setImageViewResources(context, views);
    }

    /**
     * Creates PendingIntent to notify the widget of a button click.
     *
     * @param context
     * @param appWidgetId
     * @return
     */
    private static PendingIntent getLaunchPendingIntent(Context context, int appWidgetId,
            int buttonId) {
        Intent launchIntent = new Intent();
        launchIntent.setClass(context, SettingsAppWidgetProvider.class);
        launchIntent.addCategory(Intent.CATEGORY_ALTERNATIVE);
        launchIntent.setData(Uri.parse("custom:" + buttonId));
        PendingIntent pi = PendingIntent.getBroadcast(context, 0 /* no requestCode */,
                launchIntent, 0 /* no flags */);
        return pi;
    }

    /**
     * Receives and processes a button pressed intent or state change.
     *
     * @param context
     * @param intent  Indicates the pressed button.
     */
    @Override
    public void onReceive(Context context, Intent intent) {
        super.onReceive(context, intent);
        String action = intent.getAction();
        if (WifiStateTracker.WIFI_AP_STATE_CHANGED_ACTION.equals(action)) {
            sWifiState.onActualStateChange(context, intent);
        } else if (CarStateTracker.PROVIDERS_CHANGED_ACTION.equals(action)) {
            sCarState.onActualStateChange(context, intent);
        } else if (ConnectivityManager.ACTION_BACKGROUND_DATA_SETTING_CHANGED.equals(action) ||
        		   ConnectivityManager.CONNECTIVITY_ACTION.equals(action) ) {
            sConnexionState.onActualStateChange(context, intent);
        } else if (intent.hasCategory(Intent.CATEGORY_ALTERNATIVE)) {
            Uri data = intent.getData();
            int buttonId = Integer.parseInt(data.getSchemeSpecificPart());
            if (buttonId == BUTTON_WIFI) {
                sWifiState.toggleState(context);
            } else if (buttonId == BUTTON_CAR) {
            	sCarState.toggleState(context);
            } else if (buttonId == BUTTON_CONNEXION) {
                sConnexionState.toggleState(context);
            } else if (buttonId == BUTTON_FLASH) {
                sFlashState.toggleState(context);
            } else if (buttonId == BUTTON_ROTATE) {
                sRotateState.toggleState(context);
            }
        } else {
            // Don't fall-through to updating the widget.  The Intent
            // was something unrelated or that our super class took
            // care of.
            return;
        }

        // State changes fall through
        updateWidget(context);
    }

}
