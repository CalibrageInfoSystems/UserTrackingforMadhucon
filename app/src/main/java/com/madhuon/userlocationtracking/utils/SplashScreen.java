package com.madhuon.userlocationtracking.utils;

import static android.os.Build.VERSION.SDK_INT;

import android.Manifest;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.os.Handler;
import android.provider.Settings;
import android.text.TextUtils;
import android.util.Log;
import android.widget.Toast;

import androidx.activity.result.ActivityResult;
import androidx.activity.result.ActivityResultCallback;
import androidx.activity.result.ActivityResultLauncher;
import androidx.activity.result.contract.ActivityResultContracts;
import androidx.appcompat.app.AppCompatActivity;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;

import com.madhuon.userlocationtracking.ApplicationThread;
import com.madhuon.userlocationtracking.R;
import com.madhuon.userlocationtracking.common.CommonConstants;
import com.madhuon.userlocationtracking.common.CommonUtils;
import com.madhuon.userlocationtracking.common.ProgressBar;
import com.madhuon.userlocationtracking.database.DataAccessHandler;
import com.madhuon.userlocationtracking.database.Palm3FoilDatabase;
import com.madhuon.userlocationtracking.database.Queries;
import com.madhuon.userlocationtracking.datasync.helpers.DataSyncHelper;
import com.madhuon.userlocationtracking.helper.PrefUtil;

import java.util.ArrayList;
import java.util.List;


public class SplashScreen extends AppCompatActivity {

    public static final String LOG_TAG = SplashScreen.class.getName();
    private static int SPLASH_TIME_OUT = 3000;

    private static final int REQUEST_CODE_OPEN_DOCUMENT = 2;
    ActivityResultLauncher<Intent> mGetPermission;

    private Palm3FoilDatabase palm3FoilDatabase;
    private String[] PERMISSIONS_REQUIRED = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.READ_PHONE_STATE,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_COARSE_LOCATION,
            Manifest.permission.ACCESS_FINE_LOCATION,
            Manifest.permission.CAMERA,
            Manifest.permission.MANAGE_EXTERNAL_STORAGE,
            Manifest.permission.FOREGROUND_SERVICE
    };
    private SharedPreferences sharedPreferences;
    private static final int STORAGE_PERMISSION_REQUEST_CODE = 100;
    //Creating DB and Master Sync
    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash_screen);
        sharedPreferences = getSharedPreferences("appprefs", MODE_PRIVATE);
        mGetPermission = registerForActivityResult(new ActivityResultContracts.StartActivityForResult(), new ActivityResultCallback<ActivityResult>() {
            @Override
            public void onActivityResult(ActivityResult result) {
                if (result.getResultCode() == SplashScreen.RESULT_OK) {
                    Toast.makeText(SplashScreen.this, "Android 11 permission ok", Toast.LENGTH_SHORT).show();
                }
            }
        });
        requestPermission();

        if (!CommonUtils.isNetworkAvailable(this)) {
            UiUtils.showCustomToastMessage("Please check your network connection", SplashScreen.this, 1);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.M && !CommonUtils.areAllPermissionsAllowedNew(this, PERMISSIONS_REQUIRED)) {
            Log.d("one", "areAllPermissionsAllowedNew");
            ActivityCompat.requestPermissions(this, PERMISSIONS_REQUIRED, CommonUtils.PERMISSION_CODE);
        } else {
            // Check if you have permission to access external storage
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
                if (Environment.isExternalStorageManager()) {
                    Log.d("one", "Environment");
                    // You have permission, proceed with your app logic
                    initializeApp();
                } else {
                    // You don't have permission, open app settings
                    Log.d("one", "openAppSettings");
                    openAppSettings();
                }
            }
        }



    }

    public void requestPermission() {
        if (SDK_INT >= Build.VERSION_CODES.R) {
            try {
                Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                intent.addCategory("android.intent.category.DEFAULT");
                intent.setData(Uri.parse(String.format("package:%s", getApplicationContext().getPackageName())));
                mGetPermission.launch(intent);
                checkAllPermissions();
                // startActivityForResult(intent, 2296);
            } catch (Exception e) {
                e.printStackTrace();
//                Intent intent = new Intent();
//                intent.setAction(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
//                startActivityForResult(intent, 2296);
            }
        } else {
            checkAllPermissions();
        }
    }

    //Request Permissions Result
    @Override
    public void onRequestPermissionsResult(int requestCode, String permissions[], int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        switch (requestCode) {
            case CommonUtils.PERMISSION_CODE:
                if (grantResults.length > 0 && grantResults[0] == PackageManager.PERMISSION_GRANTED) {
                    Log.v(LOG_TAG, "permission granted");
                    try {
                        Log.d("PermissionGrantedddd", "YES");
                        palm3FoilDatabase = Palm3FoilDatabase.getPalm3FoilDatabase(this);
                        palm3FoilDatabase.createDataBase();
                        dbUpgradeCall();
                    } catch (Exception e) {
                        Log.d("PermissionGrantedddd", "NO");
                        Log.e(LOG_TAG, "@@@ Error while getting master data " + e.getMessage());
                    }
                    // startMasterSync();
                    Log.d("PermissionGrantedddd", "???????");
                }
                break;
        }
    }

    //Perform Master Sync
    public void startMasterSync() {

        if (CommonUtils.isNetworkAvailable(this)) {
            DataSyncHelper.performMasterSync(this, PrefUtil.getBool(this, CommonConstants.IS_MASTER_SYNC_SUCCESS), new ApplicationThread.OnComplete() {
                @Override
                public void execute(boolean success, Object result, String msg) {
                    ProgressBar.hideProgressBar();
                    if (success) {
                        Log.d("MasterSyncSuccess", "true");
                        UiUtils.showCustomToastMessage("Master Sync Success", SplashScreen.this, 0);
                        sharedPreferences.edit().putBoolean(CommonConstants.IS_MASTER_SYNC_SUCCESS, true).apply();
                        // startActivity(new Intent(SplashScreen.this, MainLoginScreen.class));
                        finish();
                    } else {
                        Log.d("MasterSyncSuccess", "false");
                        Log.v(LOG_TAG, "@@@ Master sync failed " + msg);
                        ApplicationThread.uiPost(LOG_TAG, "master sync message", new Runnable() {
                            @Override
                            public void run() {
                                UiUtils.showCustomToastMessage("Data syncing failed", SplashScreen.this, 1);
                                // startActivity(new Intent(SplashScreen.this, MainLoginScreen.class));
                                finish();
                            }
                        });
                    }
                }
            });
        } else {
            // startActivity(new Intent(SplashScreen.this, MainLoginScreen.class));
            finish();
        }
    }

    //Db Upgrade Method
    public void dbUpgradeCall() {
        DataAccessHandler dataAccessHandler = new DataAccessHandler(SplashScreen.this, false);
        String count = dataAccessHandler.getCountValue(Queries.getInstance().UpgradeCount());
        if (TextUtils.isEmpty(count) || Integer.parseInt(count) == 0) {
            SharedPreferences sharedPreferences = getSharedPreferences("appprefs", MODE_PRIVATE);
            sharedPreferences.edit().putBoolean(CommonConstants.IS_FRESH_INSTALL, true).apply();
        }
    }

    private void setViews() {

        new Handler().postDelayed(new Runnable() {
            @Override
            public void run() {

//                    Intent intent = new Intent(SplashScreen.this, MainLoginScreen.class);
//                    intent.setFlags(Intent.FLAG_ACTIVITY_NEW_TASK | Intent.FLAG_ACTIVITY_CLEAR_TOP);
//                    startActivity(intent);
//                    finish();
            }

        }, SPLASH_TIME_OUT);
    }

    private void initializeApp() {
        try {
            palm3FoilDatabase = Palm3FoilDatabase.getPalm3FoilDatabase(this);
            palm3FoilDatabase.createDataBase();
            dbUpgradeCall();
            startMasterSync();
        } catch (Exception e) {
            Log.e(LOG_TAG, "Error during initialization: " + e.getMessage());
        }
    }

    private void openAppSettings() {
        Intent intent = new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS);
        Uri uri = Uri.fromParts("package", getPackageName(), null);
        intent.setData(uri);
        startActivity(intent);
    }

    private boolean checkAllPermissions() {
        try {
            // TODO: Check which permissions are granted
            List<String> listPermissionsNeeded = new ArrayList<>();
            for (String permission : PERMISSIONS_REQUIRED) {
                if (ContextCompat.checkSelfPermission(this, permission) != PackageManager.PERMISSION_GRANTED) {
                    listPermissionsNeeded.add(permission);
//                    palm3FoilDatabase = Palm3FoilDatabase.getPalm3FoilDatabase(this);
//                    palm3FoilDatabase.createDataBase();
                }
            }

            // TODO: Ask for non granted permissions
            if (!listPermissionsNeeded.isEmpty()) {
                ActivityCompat.requestPermissions(this, listPermissionsNeeded.toArray(new String[listPermissionsNeeded.size()]), SPLASH_TIME_OUT);
            }
            palm3FoilDatabase = Palm3FoilDatabase.getPalm3FoilDatabase(this);
            palm3FoilDatabase.createDataBase();
        } catch (Exception ex) {
            ex.printStackTrace();
        }
        return true;
    }

}