package com.home.grabber;

import android.Manifest;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

/**
 * Basically acts like a video player by listening to VIEW intents containing video data but stores
 * the file instead of playing it. It uses the DownloadManager Service to download the file in the
 * background and stores it to Download folder (public external storage).
 */
public class GrabberActivity extends AppCompatActivity {

    public static final String DIRECTORY = Environment.DIRECTORY_PICTURES + "/Grabber";

    private static final String TAG = GrabberActivity.class.getSimpleName();

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        Intent intent = getIntent();
        if (intent == null || intent.getData() == null) {
            String versionName;
            try {
                versionName = getPackageManager().getPackageInfo(getPackageName(), 0).versionName;
            } catch (PackageManager.NameNotFoundException e) {
                Log.e(TAG, "Could not find versionName", e);
                versionName = "UNKNOWN";
            }
            showToast(this, "Hello. My version is " + versionName);
            finish();
            return;
        }

        AlertDialog.Builder builder = new AlertDialog.Builder(this);
        builder.setTitle(R.string.info_title);
        builder.setMessage(R.string.info_description);
        builder.setPositiveButton(R.string.ok, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                if (isStoragePermissionGranted()) {
                    download();
                }
            }
        });
        builder.setNegativeButton(R.string.cancel, new DialogInterface.OnClickListener() {
            @Override
            public void onClick(DialogInterface dialogInterface, int i) {
                finish();
            }
        });
        builder.show();
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, @NonNull String[] permissions, @NonNull int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            download();
        } else {
            showToast(this, "Missing permission");
        }
    }

    private boolean isStoragePermissionGranted() {
        if (Build.VERSION.SDK_INT >= 23) {
            if (checkSelfPermission(android.Manifest.permission.WRITE_EXTERNAL_STORAGE)
                    == PackageManager.PERMISSION_GRANTED) {
                Log.v(TAG, "Permission is granted");
                return true;
            } else {
                Log.v(TAG, "Permission is revoked");
                ActivityCompat.requestPermissions(this, new String[]{Manifest.permission.WRITE_EXTERNAL_STORAGE}, 1);
                return false;
            }
        } else { //permission is automatically granted on sdk<23 upon installation
            Log.v(TAG, "Permission is granted");
            return true;
        }
    }

    private void download() {
        Intent intent = getIntent();
        if (intent == null || intent.getData() == null || intent.getDataString().equals("")) {
            Log.w(TAG, "Ignored NULL intent or intent without data.");
            showToast(this, "I don't know what to grab. Please try again.");
            return;
        }

        String urlString = intent.getDataString();
        startService(GrabberService.buildStartIntent(this, urlString));
        finish();
    }

    public static void showToast(final Context context, final String message) {
        Toast.makeText(context, "Grabber: " + message, Toast.LENGTH_LONG).show();
    }

}
