package io.github.danielt3131.fsm;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Environment;
import android.provider.Settings;
import android.widget.Toast;

import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;


public class Permissions {
    private Context context;
    private Activity activity;
    private final String[] mmsPermissionList = {Manifest.permission.READ_SMS, Manifest.permission.SEND_SMS, Manifest.permission.INTERNET,
            Manifest.permission.RECEIVE_MMS, Manifest.permission.READ_PHONE_STATE, Manifest.permission.READ_CONTACTS};
    private final String[] readWritePermissionList = {Manifest.permission.WRITE_EXTERNAL_STORAGE, Manifest.permission.READ_EXTERNAL_STORAGE};
    private final int READ_WRITE_PERM_REQ = 15;
    private final int MMS_PERM_REQ = 16;

    public Permissions(Context context, Activity activity) {
        this.context = context;
        this.activity = activity;
    }

    public boolean checkReadWritePermissions() {
        // Request all files permission for ANDROID 11+
        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.R) {
            if (Environment.isExternalStorageManager()) {
                 return true;
            } else {
                Toast toast = new Toast(context);
                toast.setText("Need all files permission to continue");
                toast.show();
                // Pull up settings
                try {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_APP_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(intent);
                } catch (Exception e) {
                    Intent intent = new Intent(Settings.ACTION_MANAGE_ALL_FILES_ACCESS_PERMISSION);
                    activity.startActivity(intent);
                }
            }
        } else {
            // For Android 10 and below
            boolean hasRW = true;
            for (int i = 0; i < readWritePermissionList.length; i++) {
                if (ContextCompat.checkSelfPermission(context, readWritePermissionList[i]) == PackageManager.PERMISSION_DENIED) {
                    hasRW = false;
                }
            }
            return hasRW;
        }
        //ActivityCompat.requestPermissions(context, permissionList, 22);
        return false;
    }

    public boolean checkMMSPermissions() {
        boolean gotPerms = true;
        for (int i = 0; i < mmsPermissionList.length; i++) {
            if (ContextCompat.checkSelfPermission(context, mmsPermissionList[i]) == PackageManager.PERMISSION_DENIED) {
                gotPerms = false;
            }
        }
        return gotPerms;
    }

    public boolean getReadWritePermissions() {
        if (!checkReadWritePermissions()) {
            ActivityCompat.requestPermissions(activity, readWritePermissionList, READ_WRITE_PERM_REQ);
            // Checking again
            return (checkReadWritePermissions());
        }
        return true;
    }

    public boolean getMMSPermissions() {
        if (!checkMMSPermissions()) {
            ActivityCompat.requestPermissions(activity, mmsPermissionList, MMS_PERM_REQ);
            return (checkMMSPermissions());
        }
        return true;
    }
}
