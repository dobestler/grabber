package com.home.grabber;

import android.app.DownloadManager;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.os.Environment;
import android.os.ParcelFileDescriptor;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;

public class DownloadCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = DownloadCompleteReceiver.class.getSimpleName();

    public DownloadCompleteReceiver() {
    }

    @Override
    public void onReceive(Context context, Intent intent) {
        String action = intent.getAction();
        Log.d(TAG, "onReceive " + action);
        if (DownloadManager.ACTION_DOWNLOAD_COMPLETE.equals(action)) {
            long downloadId = intent.getLongExtra(DownloadManager.EXTRA_DOWNLOAD_ID, 0);

            DownloadManager manager = (DownloadManager) context.getSystemService(Context.DOWNLOAD_SERVICE);
            DownloadManager.Query query = new DownloadManager.Query();
            query.setFilterById(downloadId);
            Cursor c = manager.query(query);
            if (c.moveToFirst()) {

                int status = c.getInt(c.getColumnIndex(DownloadManager.COLUMN_STATUS));

                if (DownloadManager.STATUS_SUCCESSFUL == status) {
                    String uriString = c.getString(c.getColumnIndex(DownloadManager.COLUMN_LOCAL_URI));
                    String type = c.getString(c.getColumnIndex(DownloadManager.COLUMN_MEDIA_TYPE));
                    String title = c.getString(c.getColumnIndex(DownloadManager.COLUMN_TITLE));
                    Log.d(TAG, "Download successful. uri=" + uriString + ", type=" + type + ", title=" + title);
                    Log.d(TAG, "Download successful. extension=" + MimeTypeMap.getSingleton().getExtensionFromMimeType(type));

                    // rename file with proper file extension
                    addProperFileExtension(context, uriString, type, manager, downloadId);

                    // FIXME notification with VIEW intent
                } else {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Download failed");
                }
            } else {
                Toast.makeText(context, "Could not find downloaded file!", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Download failed");
            }

            if (c != null) {
                c.close();
            }
        }
    }

    private void addProperFileExtension(final Context context, final String uriString, final String type, final DownloadManager manager, long downloadId) {
    File f1 = new File(uriString);
        File f2 = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_DOWNLOADS) + "/" +
                f1.getName() + "." + MimeTypeMap.getSingleton().getExtensionFromMimeType(type));

        ParcelFileDescriptor f1Fd = null;
        InputStream f1InStream = null;
        OutputStream f2OutStream = null;
        try {
            f1Fd = manager.openDownloadedFile(downloadId);
            f1InStream = new FileInputStream(f1Fd.getFileDescriptor());
            f2OutStream = new FileOutputStream(f2);

            byte[] buffer = new byte[1024];
            int length;

            while ((length = f1InStream.read(buffer)) > 0) {
                f2OutStream.write(buffer, 0, length);
            }

            f2OutStream.flush();

            manager.remove(downloadId);

        } catch (IOException e) {
            Toast.makeText(context, "Renaming failed.", Toast.LENGTH_LONG).show();
            Log.e(TAG, "Renaming failed", e);
        } finally {
            try {
                if (f1InStream != null) {
                    f1InStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (f1Fd != null) {
                    f1Fd.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
            try {
                if (f2OutStream != null) {
                    f2OutStream.close();
                }
            } catch (IOException e) {
                e.printStackTrace();
            }
        }
    }
}
