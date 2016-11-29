package com.home.grabber;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.DialogInterface;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.annotation.NonNull;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.util.Log;
import android.widget.Toast;

import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * Basically acts like a video player by listening to VIEW intents containing video data but stores
 * the file instead of playing it. It uses the DownloadManager Service to download the file in the
 * background and stores it to Download folder (public external storage).
 */
public class GrabberActivity extends AppCompatActivity {

    public static final String DIRECTORY = Environment.DIRECTORY_PICTURES + "/Grabber";

    private static final String TAG = GrabberActivity.class.getSimpleName();

    private DownloadManager mDownloadManager;

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
            showToast(this, "Hello. My version is " + versionName + "\nPlease choose me to play a video file.");
            finish();
            return;
        }

        mDownloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);

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
        if (intent == null || intent.getData() == null) {
            Log.w(TAG, "Ignored NULL intent or intent without data.");
            showToast(this, "I don't know what to grab. Please try again.");
            return;
        }

        String urlString = intent.getData().toString();
        if (!urlString.equals("")) {
            try {

                DownloadManager.Request request = new DownloadManager.Request(Uri.parse((urlString)));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setVisibleInDownloadsUi(true); //if your download is visible in history of downloads
                request.allowScanningByMediaScanner();

                String fileName = extractFileName(urlString);
                String description = fileName + " from " + urlString;
                Log.d(TAG, description);
                request.setDescription(description);
                request.setTitle("Grabbing");
                request.setDestinationInExternalPublicDir(DIRECTORY, fileName);
                mDownloadManager.enqueue(request);
                showToast(this, "Grabbing...\nYou'll be notified once I'm done.");
                finish();
            } catch (Exception e) {
                Log.e(TAG, "Requesting to download the file failed.", e);
                showToast(this, "Could not request to download.");
            }

        }
    }

    private static String extractFileName(String urlString) {
        String queryString = urlString.substring(urlString.lastIndexOf("?") + 1);
        Map<String, String> queryMap = getQueryMap(queryString);
        return queryMap.containsKey("id") ? queryMap.get("id") : "video_" + new Random().nextLong();
    }

    private static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }


    public static void showToast(final Context context, final String message) {
        Toast.makeText(context, "Grabber: " + message, Toast.LENGTH_LONG).show();
    }

}
