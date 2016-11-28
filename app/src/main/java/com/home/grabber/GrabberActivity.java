package com.home.grabber;

import android.Manifest;
import android.app.DownloadManager;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.os.Environment;
import android.support.design.widget.FloatingActionButton;
import android.support.design.widget.Snackbar;
import android.support.v4.app.ActivityCompat;
import android.support.v7.app.AppCompatActivity;
import android.support.v7.widget.Toolbar;
import android.util.Log;
import android.view.View;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

/**
 * TODO
 * 2) Define UI flow
 * 3) Refactor
 * 4) Update icon
 */
public class GrabberActivity extends AppCompatActivity {

    private static final String TAG = GrabberActivity.class.getSimpleName();

    private DownloadManager mDownloadManager;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        Toolbar toolbar = (Toolbar) findViewById(R.id.toolbar);
        setSupportActionBar(toolbar);

        // Get DownloadManager instance
        mDownloadManager = (DownloadManager) this.getSystemService(Context.DOWNLOAD_SERVICE);


        FloatingActionButton fab = (FloatingActionButton) findViewById(R.id.fab);
        fab.setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                Snackbar.make(view, "Download to Gallery", Snackbar.LENGTH_LONG)
                        .setAction("Action", new View.OnClickListener() {
                            @Override
                            public void onClick(View view) {
                                if (isStoragePermissionGranted()) {
                                    download();
                                }
                            }
                        }).show();
            }
        });
    }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] grantResults) {
        super.onRequestPermissionsResult(requestCode, permissions, grantResults);
        if (grantResults[0] == PackageManager.PERMISSION_GRANTED) {
            Log.v(TAG, "Permission: " + permissions[0] + "was " + grantResults[0]);
            download();
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
            return;
        }

        String urlString = intent.getData().toString();
        if (!urlString.equals("")) {
            try {
                String queryString = urlString.substring(urlString.lastIndexOf("?") + 1);
                Map<String, String> queryMap = getQueryMap(queryString);
                String fileName = queryMap.containsKey("id") ? queryMap.get("id") : "video_" + new Random().nextLong();
                DownloadManager.Request request = new DownloadManager.Request(Uri.parse((urlString)));
                request.setNotificationVisibility(DownloadManager.Request.VISIBILITY_VISIBLE);
                request.setVisibleInDownloadsUi(false); //if your download is visible in history of downloads
                request.allowScanningByMediaScanner();
                String description = fileName + " from " + urlString;
                Log.d(TAG, description);
                request.setDescription(description);
                request.setTitle("Grabbing");
                request.setDestinationInExternalPublicDir(Environment.DIRECTORY_DOWNLOADS, fileName);
//                request.setDestinationUri(Uri.parse("file://" + Environment.getExternalStoragePublicDirectory(
//                        Environment.DIRECTORY_DOWNLOADS) + "/" + fileName));
                mDownloadManager.enqueue(request);

            } catch (Exception e) {
                Log.e(TAG, "Requesting to download the file failed.", e);
            }

        }


        File storageDir = new File(
                Environment.getExternalStoragePublicDirectory(
                        Environment.DIRECTORY_PICTURES
                ),
                "Grabbed" // FIXME
        );
    }


    public static Map<String, String> getQueryMap(String query) {
        String[] params = query.split("&");
        Map<String, String> map = new HashMap<String, String>();
        for (String param : params) {
            String name = param.split("=")[0];
            String value = param.split("=")[1];
            map.put(name, value);
        }
        return map;
    }

    private Intent createShareIntent(final String uriString) {
        Intent shareIntent = new Intent(Intent.ACTION_SEND);
        //shareIntent.addFlags(Intent.FLAG_ACTIVITY_CLEAR_WHEN_TASK_RESET);
        shareIntent.setType("video/*");

        // For a file in shared storage.  For data in private storage, use a ContentProvider.
        Uri uri = Uri.fromFile(getFileStreamPath(uriString));
        shareIntent.putExtra(Intent.EXTRA_STREAM, uri);
        return shareIntent;
    }

    public static void main(String[] args) {

    }
}
