package wepark.launcher.mobile.android.kioskmode;

import android.app.AlertDialog;
import android.app.Dialog;
import android.app.PendingIntent;
import android.app.admin.DevicePolicyManager;
import android.content.ComponentName;
import android.content.ContentUris;
import android.content.ContentValues;
import android.content.Context;
import android.content.DialogInterface;
import android.content.DialogInterface.OnCancelListener;
import android.content.Intent;
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
import android.os.Bundle;
import android.os.Handler;
import android.preference.PreferenceManager;
import android.support.v4.app.FragmentActivity;
import android.support.v4.app.LoaderManager.LoaderCallbacks;
import android.support.v4.content.CursorLoader;
import android.support.v4.content.Loader;
import android.support.v4.widget.CursorAdapter;
import android.util.Log;
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

import com.wepark.android.home.AppUtils;
import com.wepark.android.home.ApplicationInfo;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

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

    private static final String SHARED_PREF_NAME ="LOCKED";
    /**
     * Called when the activity is first created.
     */
    @Override
    public void onCreate(Bundle savedInstanceState) {
        //int flags = getIntent().getFlags();
        super.onCreate(savedInstanceState);

        //getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
        requestWindowFeature(Window.FEATURE_NO_TITLE);
        setRequestedOrientation(ActivityInfo.SCREEN_ORIENTATION_PORTRAIT);
        setContentView(R.layout.main);
        mGridView = (GridView) findViewById(R.id.grid);

        findViewById(R.id.lock).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                Toast.makeText(getApplicationContext(), "Please Enter Password to unclock", Toast.LENGTH_LONG).show();
                dialogPassword(KioskMode.this);
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

        String lockStatus = AppUtils.readUserPrefs(getApplicationContext(), SHARED_PREF_NAME);
        if (lockStatus != null) {
            if (lockStatus.equals("true")) {
                setLocked(true);
            } else {
                setLocked(false);
            }
        } else {
            updateLocked();
        }

        writeUserPrefs("PASSWORD", "KAMAL@321");

        preventStatusBarExpansion(this);
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

    public static void preventStatusBarExpansion(Context context) {
        WindowManager manager = ((WindowManager) context.getApplicationContext().getSystemService(Context.WINDOW_SERVICE));

        WindowManager.LayoutParams localLayoutParams = new WindowManager.LayoutParams();
        localLayoutParams.type = WindowManager.LayoutParams.TYPE_SYSTEM_ERROR;
        localLayoutParams.gravity = Gravity.TOP;
        localLayoutParams.flags = WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_NOT_TOUCH_MODAL|WindowManager.LayoutParams.FLAG_LAYOUT_IN_SCREEN;

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

    private void enableKioskMode(boolean enabled) {
        try {
            if (enabled) {
                if (mDpm.isLockTaskPermitted(this.getPackageName())) {
                    startLockTask();
                    mIsKioskEnabled = true;
                    //mButton.setText(getString(R.string.exit_kiosk_mode));
                } else {
                    Toast.makeText(this,"kiosk_not_permitted", Toast.LENGTH_SHORT).show();
                }
            } else {
                stopLockTask();
                mIsKioskEnabled = false;
                //mButton.setText(getString(R.string.enter_kiosk_mode));
            }
        } catch (Exception e) {
            // TODO: Log and handle appropriately
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
    public boolean onPrepareOptionsMenu(Menu menu) {
        super.onPrepareOptionsMenu(menu);

        menu.findItem(R.id.lock).setVisible(!mLocked);
        menu.findItem(R.id.unlock).setVisible(mLocked);
        menu.findItem(R.id.settings).setVisible(!mLocked);
        menu.findItem(R.id.preferences).setVisible(!mLocked);

        return true;
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        super.onCreateOptionsMenu(menu);
        getMenuInflater().inflate(R.menu.main, menu);
        return true;
    }

    @Override
    public void onBackPressed() {
        if (!mLocked) {
            super.onBackPressed();
            Toast.makeText(getApplicationContext(), "BackPressed click", Toast.LENGTH_LONG).show();
        }
    }

    @SuppressWarnings("deprecation")
    @Override
    public boolean onOptionsItemSelected(MenuItem item) {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }

        return super.onOptionsItemSelected(item);
    }

    private void setLocked(boolean locked) {
        //mPrefs.edit().putBoolean(Preferences.KEY_LOCKED, locked).commit();

        mLocked = locked;

        updateLocked();
    }

    private Handler mHandler = new Handler();

    private void updateLocked() {
        findViewById(R.id.slidingDrawer1).setVisibility(mLocked ? View.GONE : View.VISIBLE);
        if (mLocked) {
            getWindow().addFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);

            //Remove notification bar
            this.getWindow().setFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN, WindowManager.LayoutParams.FLAG_FULLSCREEN);

            mHandler.postDelayed(decor_view_settings, 500);
            //enableKioskMode(true);
            Toast.makeText(getApplicationContext(), "Launcher Locked", Toast.LENGTH_LONG).show();

        } else {
            getWindow().clearFlags(
                    WindowManager.LayoutParams.FLAG_FULLSCREEN
                            | WindowManager.LayoutParams.FLAG_SHOW_WHEN_LOCKED
                            | WindowManager.LayoutParams.FLAG_DISMISS_KEYGUARD);
            Toast.makeText(getApplicationContext(), "Launcher UnLocked", Toast.LENGTH_LONG).show();
            //enableKioskMode(false);
        }
    }

    private Runnable decor_view_settings = new Runnable() {
        public void run() {
            getWindow().getDecorView().setSystemUiVisibility(
                    View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_FULLSCREEN
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
        }
    };


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
                AppUtils.writeUserPrefs(getApplicationContext(), SHARED_PREF_NAME, "false");
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
                AppUtils.writeUserPrefs(getApplicationContext(), SHARED_PREF_NAME, "true");
                Toast.makeText(getApplicationContext(), "Application is locked", Toast.LENGTH_LONG).show();
                setLocked(true);
                return true;

            }
        }
        return false;
    }


    public void dialogPassword(final Context mContext) {
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

        final EditText edtPassword = (EditText) customDialog.findViewById(R.id.custom_number_plate);
        final Button btnOk = (Button) customDialog.findViewById(R.id.alert_ok_dialog);
        btnOk.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {

                String password = readUserPrefs("PASSWORD");
                if (edtPassword.getText().toString().equals(password)) {
                    setLocked(false);
                    customDialog.dismiss();
                }else{
                    Toast.makeText(KioskMode.this,"Incorrect Password, Conatct your owner",Toast.LENGTH_LONG).show();
                }
            }
        });
        final Button btnCancle = (Button) customDialog.findViewById(R.id.alert_cancel_dialog);
        btnCancle.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View v) {
                customDialog.dismiss();
            }
        });
    }


    public void writeUserPrefs(String key, String value) {
        try {
            SharedPreferences settings;
            settings = this.getSharedPreferences("wepark.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            SharedPreferences.Editor editor = settings.edit();
            editor.putString(key, value);
            editor.commit();
        } catch (Exception e) {
            //LogUtils.LOGD("writeUserPrefs", e.getMessage(), e);
        }
    }

    public String readUserPrefs(String key) {
        String value = "";
        try {
            SharedPreferences settings;
            settings = this.getSharedPreferences("wepark.launcher.mobile.android.kioskmode", Context.MODE_PRIVATE);
            value = settings.getString(key, "");

        } catch (Exception e) {
            //LogUtils.LOGD("readUserPrefs", e.getMessage(), e);
        }
        return value;
    }

    public void clearPreference() {
        SharedPreferences sharedPreferences = PreferenceManager.getDefaultSharedPreferences(this);
        sharedPreferences.edit().clear();
    }

}
