package cgr.launcher.mobile.android.kioskmode;

import android.app.Activity;
import android.app.Application;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageInfo;
import android.content.res.Resources;
import android.net.ConnectivityManager;
import android.net.NetworkInfo;
import android.os.Build;
import android.os.StrictMode;
import android.preference.PreferenceManager;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import com.cgr.android.home.AppConstants;
import com.google.android.gms.common.ConnectionResult;
import com.google.android.gms.common.GoogleApiAvailability;
import com.google.firebase.analytics.FirebaseAnalytics;
import com.google.firebase.messaging.RemoteMessage;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;

import org.json.JSONObject;

import cgr.launcher.mobile.android.kioskmode.persistence.rest.ApiClientHttp;
import cz.msebera.android.httpclient.Header;

import static android.content.ContentValues.TAG;

/**
 * Created by saurabhtomar on 4/3/18.
 */

public class App extends Application {

    private static Context context;
    private static Resources resources;
    private static FirebaseAnalytics mFirebaseAnalytics;

    @Override
    public void onCreate() {
        super.onCreate();

        context = getApplicationContext();
        resources = context.getResources();

        //Firebase Analytics Init
        getDefaultFireBaseAnalytics();

        startKioskService();

    }

    public static Resources getResource() {
        return resources;
    }

    public static Context getContext() {
        return context;
    }


    synchronized public FirebaseAnalytics getDefaultFireBaseAnalytics() {
        try {
            if (!checkPlayServices(context)) {
                return null;
            }
            if (mFirebaseAnalytics == null) {
                mFirebaseAnalytics = FirebaseAnalytics.getInstance(this);
            }
        } catch (Exception e) {
            e.printStackTrace();
        }
        return mFirebaseAnalytics;
    }


    public static String getDeviceType() {
        return AppConstants.ANDROID;
    }

    private static final int PLAY_SERVICES_RESOLUTION_REQUEST = 9000;

    /**
     * Check the device to make sure it has the Google Play Services APK. If
     * it doesn't, display a dialog that allows users to download the APK from
     * the Google Play Store or enable it in the device's system settings.
     */
    public static boolean checkPlayServices(Context context) {
        GoogleApiAvailability apiAvailability = GoogleApiAvailability.getInstance();
        int resultCode = apiAvailability.isGooglePlayServicesAvailable(context);
        if (resultCode != ConnectionResult.SUCCESS) {
            if (apiAvailability.isUserResolvableError(resultCode)) {
                if (context != null && context instanceof Activity) {
                    apiAvailability.getErrorDialog((Activity) context, resultCode, PLAY_SERVICES_RESOLUTION_REQUEST)
                            .show();
                }
            } else {
                Log.i(TAG, "This device is not supported.");
            }
            return false;
        }
        return true;
    }

    public static void writeUserPrefs(String key, String value) {
        try {
            SharedPreferences settings;
            settings = context.getSharedPreferences("cgr.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
            Log.d("writeUserPrefs", e.getMessage(), e);
        }
    }

    public static String readUserPrefs(String key) {
        String value = "";
        try {
            SharedPreferences settings;
            settings = context.getSharedPreferences("cgr.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            value = settings.getString(key, "");

        } catch (Exception e) {
            Log.d("readUserPrefs", e.getMessage(), e);
        }
        return value;
    }

    public static void clearPreference() {
        SharedPreferences sharedPreferences = context.getSharedPreferences("cgr.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
        sharedPreferences.edit().clear().commit();
    }


    public static void showToast(Context context, String text) {
        Toast.makeText(context, text, Toast.LENGTH_SHORT).show();
    }

    public static boolean isNetworkConnected() {
        ConnectivityManager connMgr = (ConnectivityManager) context.getSystemService(Context.CONNECTIVITY_SERVICE);
        NetworkInfo networkInfo = connMgr.getActiveNetworkInfo();
        if (networkInfo != null && networkInfo.isConnected()) {
            return true;
        } else {
            return false;
        }
    }

    public static String getDeviceModel() {
        try {
            return Build.MODEL + Build.MANUFACTURER + Build.VERSION.RELEASE;
        } catch (Exception e) {
            Log.d("getDeviceModel", e.getMessage(), e);
        }
        return "NO-MODEL";
    }

    public static String getAppVersion() {
        try {
            PackageInfo pInfo = context.getPackageManager().getPackageInfo(context.getPackageName(), 0);
            String version = pInfo.versionName;
            int verCode = pInfo.versionCode;
            String versionStr = version + "-" + verCode;
            return versionStr;
        } catch (Exception e) {
            Log.d("getDeviceModel", e.getMessage(), e);

        }
        return "NO-VERSION";
    }

    public static String getDeviceId() {
        String deviceId = "";
        try {
            deviceId = Settings.Secure.getString(context.getContentResolver(), Settings.Secure.ANDROID_ID);
        } catch (Exception e) {
            Log.d("getDeviceId", e.getMessage(), e);
        }
        return deviceId;
    }

    public static boolean isvalidResponseCode(JSONObject response, Context mcontext) {
        boolean isvalid = false;
        String message = "";

        switch (response.optInt("responseCode")) {
            case 200:
                isvalid = true;
                message = response.optString("responseMessage");
                App.showToast(mcontext, message);
                break;
            case 403:
                isvalid = false;
                App.showToast(mcontext, mcontext.getString(R.string.unable_to_process));
                break;
            default:
                isvalid = false;
                message = response.optString("responseMessage");
                App.showToast(mcontext, message);
                break;
        }

        return isvalid;
    }

    //Set Locked implement in addDevice Token and check it by Type
    public static void addDeviceToken(final Context context, final String token, final String type) {
        if (!TextUtils.isEmpty(token)) {
            RequestParams requestParams = new RequestParams();
            requestParams.put("deviceToken", token);
            requestParams.put("deviceId", App.getDeviceId());
            requestParams.put("deviceType", AppConstants.ANDROID);
            requestParams.put("location", App.readUserPrefs(AppConstants.WAREHOUSENAME));
            ApiClientHttp.post(App.getContext(), "addDevice", requestParams, new JsonHttpResponseHandler() {
                @Override
                public void onSuccess(int statusCode, Header[] headers, JSONObject response) {
                    try {
                        if (App.isvalidResponseCode(response, App.getContext())) {
                            // Nothing to do
                            App.showToast(App.getContext(), App.getContext().getString(R.string.device_token_updated));
                            if(type.equals("ADD")) {
                                ((KioskMode) context).setLocked(true);
                            }
                        }
                    } catch (Exception e) {
                        e.printStackTrace();
                        //dismissProgress();
                    }
                }

                @Override
                public void onFailure(int statusCode, Header[] headers, Throwable throwable, JSONObject errorResponse) {
                    //dismissProgress();
                    if (throwable.getMessage() != null) {
                        App.showToast(App.getContext(), throwable.getMessage().toString());
                    }
                }
            });
        } else {
            App.showToast(App.getContext(), "Device token is missing");
        }
    }


    private void startKioskService() { // ... and this method
        startService(new Intent(this, KioskService.class));
    }

    private static final String PREF_KIOSK_MODE = "pref_kiosk_mode";


    public static boolean isKioskModeActive(final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        return sp.getBoolean(PREF_KIOSK_MODE, false);
    }

    public static void setKioskModeActive(final boolean active, final Context context) {
        SharedPreferences sp = PreferenceManager.getDefaultSharedPreferences(context);
        sp.edit().putBoolean(PREF_KIOSK_MODE, active).commit();
    }

}
