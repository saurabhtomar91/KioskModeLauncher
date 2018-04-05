package cgr.launcher.mobile.android.kioskmode;

import android.app.Dialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.BroadcastReceiver;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.content.IntentFilter;
import android.content.SharedPreferences;
import android.content.pm.ActivityInfo;
import android.content.pm.PackageManager;
import android.content.pm.PackageManager.NameNotFoundException;
import android.content.pm.ResolveInfo;
import android.content.res.Resources;
import android.database.Cursor;
import android.graphics.Bitmap;
import android.graphics.Canvas;
import android.graphics.Paint;
import android.graphics.PaintFlagsDrawFilter;
import android.graphics.PixelFormat;
import android.graphics.Rect;
import android.graphics.drawable.BitmapDrawable;
import android.graphics.drawable.Drawable;
import android.graphics.drawable.PaintDrawable;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.view.Gravity;
import android.view.KeyEvent;
import android.view.LayoutInflater;
import android.view.Menu;
import android.view.MenuItem;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.view.Window;
import android.view.WindowManager;
import android.widget.AdapterView;
import android.widget.AdapterView.OnItemClickListener;
import android.widget.AdapterView.OnItemLongClickListener;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.GridView;
import android.widget.SlidingDrawer;
import android.widget.TextView;
import android.widget.Toast;

import com.cgr.android.home.AppConstants;
import com.cgr.android.home.ApplicationInfo;
import com.google.firebase.messaging.RemoteMessage;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import cgr.launcher.mobile.android.kioskmode.fcm.MyFirebaseMessagingService;

/**
 * Created by Saurabh on 11/3/2016.
 */

public class KioskMode extends FragmentActivity implements LoaderCallbacks<Cursor>,
        OnItemClickListener, OnItemLongClickListener {
    @SuppressWarnings("unused")
    private static final String TAG = KioskMode.class.getSimpleName();

    private GridView mGridView;
    private GridView mAllApps;

    private LauncherItemAdapter mAdapter;

    private static ArrayList<ApplicationInfo> mApplications;

    private boolean mLocked;
    private SlidingDrawer mDrawer;
    private PendingIntent intent;
    private DevicePolicyManager mDpm;
    private View mDecorView;
    private boolean mIsKioskEnabled;

    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);
        mGridView = (GridView) findViewById(R.id.grid);

        // every time someone enters the kiosk mode, set the flag true
        App.setKioskModeActive(true, getApplicationContext());

        findViewById(R.id.iv_warehouse_name).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                dialogWarehouseName(KioskMode.this);
            }
        });
        getSupportLoaderManager().initLoader(0, null, this);
        mAdapter = new LauncherItemAdapter(this, R.layout.launcher_item, null);
        mGridView.setAdapter(mAdapter);
        mGridView.setOnItemClickListener(this);
        mGridView.setOnItemLongClickListener(this);

        mAllApps = (GridView) findViewById(R.id.all_apps);

        loadApplications(true);

        mDrawer = (SlidingDrawer) findViewById(R.id.slidingDrawer1);

        mAllApps.setAdapter(new ApplicationsAdapter(this, mApplications));

        mAllApps.setOnItemClickListener(this);
        mAllApps.setOnItemLongClickListener(this);

        registerReceiver(myReceiver, new IntentFilter(MyFirebaseMessagingService.INTENT_FILTER));

        String lockstatus = App.readUserPrefs(AppConstants.LOCKSTATUS);

        // First time check whether it is empty or not
        if(!lockstatus.isEmpty()) {
            if(lockstatus.equals("true")){
                setLocked(true);
            }else if(lockstatus.equals("false")){
                setLocked(false);
            }
        }else{
            setLocked(false);
        }
    }

    /**
     * Loads the list of installed applications in mApplications.
     */
    private void loadApplications(boolean isLaunching) {
        if (isLaunching && mApplications != null) {
            return;
        }

        final PackageManager manager = getPackageManager();

        final Intent mainIntent = new Intent(Intent.ACTION_MAIN, null);
        mainIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        final List<ResolveInfo> apps = manager.queryIntentActivities(mainIntent, 0);
        Collections.sort(apps, new ResolveInfo.DisplayNameComparator(manager));

        if (apps != null) {
            final int count = apps.size();

            if (mApplications == null) {
                mApplications = new ArrayList<ApplicationInfo>(count);
            }
            mApplications.clear();

            for (int i = 0; i < count; i++) {
                final ApplicationInfo application = new ApplicationInfo();
                final ResolveInfo info = apps.get(i);

                application.title = info.loadLabel(manager);
                application.setActivity(new ComponentName(
                                info.activityInfo.applicationInfo.packageName, info.activityInfo.name),
                        Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
                application.icon = info.activityInfo.loadIcon(manager);

                mApplications.add(application);
            }
        }
    }

    public static class CustomViewGroup extends ViewGroup {
        public CustomViewGroup(Context context) {
            super(context);
        }

        @Override
        protected void onLayout(boolean changed, int l, int t, int r, int b) {
        }

        @Override
        public boolean onInterceptTouchEvent(MotionEvent ev) {
            // Intercepted touch!
            return true;
        }
    }

    private static class LauncherItemAdapter extends CursorAdapter {
        private final LayoutInflater mLayoutInflater;
        private final int mLayout;
        private final PackageManager mPackageManager;
        private final int mAppIconWidth;
        private final int mAppIconHeight;

        public LauncherItemAdapter(Context context, int layout, Cursor c) {
            super(context, c, 0);
            mLayout = layout;
            mLayoutInflater = (LayoutInflater) context.getSystemService(LAYOUT_INFLATER_SERVICE);
            mPackageManager = context.getPackageManager();

            final Resources resources = context.getResources();
            mAppIconWidth = (int) resources.getDimension(android.R.dimen.app_icon_size);
            mAppIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
        }

        private Drawable scaleDrawableToAppIconSize(Drawable icon) {
            int width = mAppIconWidth;
            int height = mAppIconHeight;

            final int iconWidth = icon.getIntrinsicWidth();
            final int iconHeight = icon.getIntrinsicHeight();

            if (width > 0 && height > 0 && (width < iconWidth || height < iconHeight)) {
                final float ratio = (float) iconWidth / iconHeight;

                if (iconWidth > iconHeight) {
                    height = (int) (width / ratio);
                } else if (iconHeight > iconWidth) {
                    width = (int) (height * ratio);
                }
            }
            icon.setBounds(0, 0, width, height);
            return icon;
        }

        @Override
        public void bindView(View v, Context context, Cursor c) {
            final String pkg = c.getString(c.getColumnIndex(LauncherItem.PACKAGE_NAME));
            final String cls = c.getString(c.getColumnIndex(LauncherItem.ACTIVITY_NAME));
            final ComponentName activity = new ComponentName(pkg, cls);
            try {
                final ActivityInfo i = mPackageManager.getActivityInfo(activity, 0);
                final android.content.pm.ApplicationInfo appInfo = mPackageManager
                        .getApplicationInfo(pkg, 0);
                final TextView label = (TextView) v.findViewById(R.id.label);
                final Drawable icon = i.loadIcon(mPackageManager);

                scaleDrawableToAppIconSize(icon);

                label.setCompoundDrawables(null, icon, null, null);

                label.setText(mPackageManager.getText(cls,
                        i.labelRes == 0 ? i.applicationInfo.labelRes : i.labelRes, appInfo));

            } catch (final NameNotFoundException e) {
                e.printStackTrace();
            }
        }

        @Override
        public View newView(Context context, Cursor cursor, ViewGroup parent) {
            final View v = mLayoutInflater.inflate(mLayout, parent, false);
            bindView(v, context, cursor);
            return v;
        }

    }

    @Override
    public Loader<Cursor> onCreateLoader(int arg0, Bundle arg1) {
        return new CursorLoader(this, LauncherItem.CONTENT_URI, null, null, null, null);
    }

    @Override
    public void onLoadFinished(Loader<Cursor> loader, Cursor c) {
        mAdapter.swapCursor(c);

    }

    @Override
    public void onLoaderReset(Loader<Cursor> arg0) {
        mAdapter.swapCursor(null);

    }

    @Override
    public void onBackPressed() {
//        if (!mLocked) {
//            super.onBackPressed();
//            Toast.makeText(getApplicationContext(), "BackPressed click", Toast.LENGTH_LONG).show();
//        }
    }

    // Set Locked function for loacking the launcher
    public void setLocked(boolean locked) {

        mLocked = locked;

        updateLocked();
        App.writeUserPrefs(AppConstants.LOCKSTATUS, mLocked + "");
    }

    private Handler mHandler = new Handler();

    private void updateLocked() {
        findViewById(R.id.slidingDrawer1).setVisibility(mLocked ? View.GONE : View.VISIBLE);
        //Lock if mLocked is true
        if (mLocked) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            //mHandler.postDelayed(decorViewDiable, 500);

            findViewById(R.id.iv_warehouse_name).setVisibility(View.GONE);

            //prevent to open from status bar expansion diable
            preventStatusBarExpansion(KioskMode.this);

            Toast.makeText(getApplicationContext(), "Launcher Locked", Toast.LENGTH_LONG).show();

        } else { //
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            Toast.makeText(getApplicationContext(), "Launcher UnLocked", Toast.LENGTH_LONG).show();


            //Enable status bar expansion
            enableStatusBarExpansion(KioskMode.this);

            findViewById(R.id.iv_warehouse_name).setVisibility(View.VISIBLE);
        }
    }

    //Disable StatusBarExpansion for user
    public void preventStatusBarExpansion(Context context) {
        WindowManager manager = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int result = 0;
        if (resId > 0) {
            result = context.getResources().getDimensionPixelSize(resId);
        } else {
            // Use Fallback size:
            result = 60; // 60px Fallback
        }

        localLayoutParams.height = result;
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        CustomViewGroup view = new CustomViewGroup(context);
        manager.addView(view, localLayoutParams);
    }


    //Enable StatusBarExpansion for user
    public void enableStatusBarExpansion(Context context) {
        WindowManager manager = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE | WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL | WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

        localLayoutParams.width = WindowManager.LayoutParams.MATCH_PARENT;

        int resId = context.getResources().getIdentifier("status_bar_height", "dimen", "android");
        int result = 0;
        localLayoutParams.height = result;
        localLayoutParams.format = PixelFormat.TRANSPARENT;

        CustomViewGroup view = new CustomViewGroup(context);
        manager.addView(view, localLayoutParams);
    }

    @Override
    public boolean onKeyDown(int keyCode, KeyEvent event) {
        if (mLocked && keyCode != KeyEvent.KEYCODE_MENU) {
            Toast.makeText(getApplicationContext(), "onKeyDown press Locked", Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(getApplicationContext(), "onKeyDown press unlocked", Toast.LENGTH_LONG).show();
            return super.onKeyDown(keyCode, event);
        }
    }

    @Override
    public boolean onKeyLongPress(int keyCode, KeyEvent event) {
        if (mLocked) {
            Toast.makeText(getApplicationContext(), "onKeyLong Press Locked", Toast.LENGTH_LONG).show();
            return true;
        } else {
            Toast.makeText(getApplicationContext(), "onKeyLong Press unlocked", Toast.LENGTH_LONG).show();
            return super.onKeyLongPress(keyCode, event);
        }
    }

    /**
     * GridView adapter to show the list of all installed applications.
     */
    private class ApplicationsAdapter extends ArrayAdapter<ApplicationInfo> {
        private final Rect mOldBounds = new Rect();
        private final int mAppIconHeight;
        private final int mAppIconWidth;

        public ApplicationsAdapter(Context context, ArrayList<ApplicationInfo> apps) {
            super(context, 0, apps);
            final Resources resources = getContext().getResources();
            mAppIconWidth = (int) resources.getDimension(android.R.dimen.app_icon_size);
            mAppIconHeight = (int) resources.getDimension(android.R.dimen.app_icon_size);
        }

        @Override
        public View getView(int position, View convertView, ViewGroup parent) {
            final ApplicationInfo info = mApplications.get(position);

            if (convertView == null) {
                final LayoutInflater inflater = getLayoutInflater();
                convertView = inflater.inflate(R.layout.launcher_item, parent, false);
            }

            Drawable icon = info.icon;

            if (!info.filtered) {
                int width = mAppIconWidth;
                int height = mAppIconHeight;

                final int iconWidth = icon.getIntrinsicWidth();
                final int iconHeight = icon.getIntrinsicHeight();

                if (icon instanceof PaintDrawable) {
                    final PaintDrawable painter = (PaintDrawable) icon;
                    painter.setIntrinsicWidth(width);
                    painter.setIntrinsicHeight(height);
                }

                if (width > 0 && height > 0 && (width < iconWidth || height < iconHeight)) {
                    final float ratio = (float) iconWidth / iconHeight;

                    if (iconWidth > iconHeight) {
                        height = (int) (width / ratio);
                    } else if (iconHeight > iconWidth) {
                        width = (int) (height * ratio);
                    }

                    final Bitmap.Config c = icon.getOpacity() != PixelFormat.OPAQUE ? Bitmap.Config.ARGB_8888
                            : Bitmap.Config.RGB_565;
                    final Bitmap thumb = Bitmap.createBitmap(width, height, c);
                    final Canvas canvas = new Canvas(thumb);
                    canvas.setDrawFilter(new PaintFlagsDrawFilter(Paint.DITHER_FLAG, 0));
                    // Copy the old bounds to restore them later
                    // If we were to do oldBounds = icon.getBounds(),
                    // the call to setBounds() that follows would
                    // change the same instance and we would lose the
                    // old bounds
                    mOldBounds.set(icon.getBounds());
                    icon.setBounds(0, 0, width, height);
                    icon.draw(canvas);
                    icon.setBounds(mOldBounds);
                    icon = info.icon = new BitmapDrawable(getResources(), thumb);
                    info.filtered = true;
                }
            }

            final TextView textView = (TextView) convertView.findViewById(R.id.label);
            textView.setCompoundDrawablesWithIntrinsicBounds(null, icon, null, null);
            textView.setText(info.title);

            return convertView;
        }
    }


    @Override
    public void onItemClick(AdapterView<?> adapter, View v, int position, long id) {
        switch (adapter.getId()) {
            case R.id.grid: {
                final Intent launch = new Intent();
                final Cursor c = mAdapter.getCursor();
                launch.setClassName(c.getString(c.getColumnIndex(LauncherItem.PACKAGE_NAME)),
                        c.getString(c.getColumnIndex(LauncherItem.ACTIVITY_NAME)));
                launch.addFlags(Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED
                        | Intent.FLAG_ACTIVITY_NEW_TASK);
                startActivity(launch);

                App.addDeviceToken(KioskMode.this , App.readUserPrefs(AppConstants.USER_FCM_KEY) , "UPDATE");
            }
            break;

            case R.id.all_apps: {
                final ApplicationInfo appInfo = (ApplicationInfo) mAllApps.getAdapter().getItem(
                        position);
                startActivity(appInfo.intent);
            }
            break;
        }

    }

    @Override
    public boolean onItemLongClick(AdapterView<?> adapter, View v, int position, long id) {
        switch (adapter.getId()) {
            case R.id.grid: {
                if (!mLocked) {
                    getContentResolver().delete(
                            ContentUris.withAppendedId(LauncherItem.CONTENT_URI, id), null, null);
                    Toast.makeText(getApplicationContext(), "Application Removed from the launcher", Toast.LENGTH_LONG).show();
                    return true;
                }
            }
            break;
            case R.id.all_apps: {
                final ApplicationInfo appInfo = (ApplicationInfo) mAllApps
                        .getItemAtPosition(position);
                final ContentValues cv = new ContentValues();
                final ComponentName c = appInfo.intent.getComponent();

                cv.put(LauncherItem.ACTIVITY_NAME, c.getClassName());
                //Toast.makeText(getApplicationContext(), c.getClassName(), Toast.LENGTH_LONG).show();
                cv.put(LauncherItem.PACKAGE_NAME, c.getPackageName());
                //Toast.makeText(getApplicationContext(), c.getPackageName(), Toast.LENGTH_LONG).show();
                getContentResolver().insert(LauncherItem.CONTENT_URI, cv);
                mDrawer.close();
                Toast.makeText(getApplicationContext(), "Application is locked", Toast.LENGTH_LONG).show();

                //setLocked(true); on long press lock disabled for now.
                return true;

            }
        }
        return false;
    }


    private BroadcastReceiver myReceiver = new BroadcastReceiver() {
        @Override
        public void onReceive(Context context, Intent intent) {
            Bundle mBundle = intent.getExtras();
            sendEvents(mBundle.getString("Body"));
        }
    };

    public void sendEvents(String event) {
        switch (event) {
            // Revoke the launcher open the menu box
            case AppConstants.REVOKE:
                setLocked(false);
                break;
            // Locked the launcher hide the menu box
            case AppConstants.LOCKED:
                setLocked(true);
                break;

        }
    }


    public void dialogWarehouseName(final Context mContext) {
        final Dialog customDialog = new Dialog(mContext);
        customDialog.requestWindowFeature(Window.FEATURE_NO_TITLE);
        customDialog.setContentView(R.layout.custom_alert_dialog);
        customDialog.setCanceledOnTouchOutside(true);
        customDialog.getWindow().getAttributes().windowAnimations = R.style.Dialog_No_Border;
        WindowManager.LayoutParams lp = new WindowManager.LayoutParams();
        Window window = customDialog.getWindow();
        lp.copyFrom(window.getAttributes());
        //This makes the dialog take up the full width
        lp.width = WindowManager.LayoutParams.MATCH_PARENT;
        lp.height = WindowManager.LayoutParams.WRAP_CONTENT;
        window.setAttributes(lp);
        customDialog.show();

        final EditText edtWHName = (EditText) customDialog.findViewById(R.id.edt_warehouse_name);
        final Button btnOk = (Button) customDialog.findViewById(R.id.alert_ok_dialog);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                if (!edtWHName.getText().toString().equals("")) {
                    try {
                        App.writeUserPrefs(AppConstants.WAREHOUSENAME, edtWHName.getText().toString());
                        findViewById(R.id.iv_warehouse_name).setVisibility(View.GONE);
                        App.addDeviceToken(KioskMode.this, App.readUserPrefs(AppConstants.USER_FCM_KEY) , "ADD");
                    } catch (Exception e) {
                        e.printStackTrace();
                    }
                    customDialog.dismiss();
                } else {
                    App.showToast(KioskMode.this, "Please enter Warehouse Name");
                }
            }
        });
    }

}
