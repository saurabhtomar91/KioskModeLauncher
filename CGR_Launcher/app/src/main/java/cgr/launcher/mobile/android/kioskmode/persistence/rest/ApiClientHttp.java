package cgr.launcher.mobile.android.kioskmode.persistence.rest;

import android.content.Context;

import com.loopj.android.http.AsyncHttpClient;
import com.loopj.android.http.AsyncHttpResponseHandler;
import com.loopj.android.http.JsonHttpResponseHandler;
import com.loopj.android.http.RequestParams;
import com.loopj.android.http.SyncHttpClient;

import cgr.launcher.mobile.android.kioskmode.App;
import cgr.launcher.mobile.android.kioskmode.R;

public class ApiClientHttp {

    private static final String BASE_URL = App.getResource().getString(R.string.cgr_url) + "user/";


    private static AsyncHttpClient client = new AsyncHttpClient();

    public static AsyncHttpClient getClient() {
        return client;
    }

    public static void get(Context context , String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        if (App.isNetworkConnected()) {
            client.get(getAbsoluteUrl(url), params, responseHandler);
        }else{
            App.showToast(context , context.getString(R.string.please_connect_to_internet));
        }
    }

    public static void get(Context context , String url, RequestParams params, JsonHttpResponseHandler responseHandler) {
        if (App.isNetworkConnected()) {
            client.get(getAbsoluteUrl(url), params, responseHandler);
        }else {
            App.showToast(context , context.getString(R.string.please_connect_to_internet));
        }
    }

    //Called this method before logging and registration
    public static void post(Context context , String url, RequestParams params, JsonHttpResponseHandler responseHandler) {
        if (App.isNetworkConnected()) {
            client.post(getAbsoluteUrl(url), params, responseHandler);
        }else {
            App.showToast(context , context.getString(R.string.please_connect_to_internet));
        }
    }

    public static void getByUrl(Context context , String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.get(url, params, responseHandler);
    }

    public static void postByUrl(Context context , String url, RequestParams params, AsyncHttpResponseHandler responseHandler) {
        client.post(url, params, responseHandler);
    }

    private static String getAbsoluteUrl(String relativeUrl) {
        return BASE_URL + relativeUrl;
    }
}