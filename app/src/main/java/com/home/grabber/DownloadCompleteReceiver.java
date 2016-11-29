package com.home.grabber;

import android.app.DownloadManager;
import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.BroadcastReceiver;
import android.content.Context;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.util.Log;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import java.io.File;

import static android.content.Context.NOTIFICATION_SERVICE;

public class DownloadCompleteReceiver extends BroadcastReceiver {

    private static final String TAG = DownloadCompleteReceiver.class.getSimpleName();
    private static final int NOTIFICATION_ID = 980000053;

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
                    File f1 = new File(uriString);
                    String extension = MimeTypeMap.getSingleton().getExtensionFromMimeType(type);
                    File f2 = new File(f1.getPath(), f1.getName() + "." + extension);

//                    boolean success = renameFile(f1.getName(), f2.getName());
//                    File file;
//                    if (success) {
//                        file = f2;
//                    } else {
//                        GrabberActivity.showToast(context, "Failed to rename the downloaded file to " + f2.getName());
//                        file = f1;
//                    }
//                    triggerMediaScan(context, file);
                    File file = f1;
                    showNotification(context, file.getName());
                } else {
                    Toast.makeText(context, "Download failed", Toast.LENGTH_LONG).show();
                    Log.w(TAG, "Download failed");
                }
            } else {
                Toast.makeText(context, "Could not find downloaded file!", Toast.LENGTH_LONG).show();
                Log.w(TAG, "Download failed");
            }

            c.close();
        }
    }

    private void triggerMediaScan(final Context context, final File file) {
        //Broadcast the Media Scanner Intent to trigger it
//        context.sendBroadcast(new Intent(Intent.ACTION_MEDIA_MOUNTED,
//                Uri.parse("file://" + Environment.getExternalStorageDirectory())));
        Intent intent = new Intent(Intent.ACTION_MEDIA_SCANNER_SCAN_FILE);
        intent.setData(Uri.fromFile(file));
        context.sendBroadcast(intent);
    }

    private void showNotification(final Context context, final String filename) {
        Log.d(TAG, "showNotification | filename = " + filename);

        // Open folder
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(GrabberActivity.DIRECTORY).getPath());
        intent.setDataAndType(uri, "resource/folder");

        PendingIntent pIntent = PendingIntent.getActivity(context, (int) System.currentTimeMillis(), intent, 0);
        Notification n = new Notification.Builder(context)
                .setContentTitle("Download successful")
                .setContentText("Open Download directory")
                .setStyle(new Notification.BigTextStyle().bigText("Open Download directory\nFilename: " + filename))
                .setSmallIcon(R.mipmap.ic_stat_teddy_bear_machine_white)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        NotificationManager notificationManager =
                (NotificationManager) context.getSystemService(NOTIFICATION_SERVICE);

        notificationManager.notify(NOTIFICATION_ID, n);
    }

    private static boolean renameFile(String oldName, String newName) {
        File dir = Environment.getExternalStoragePublicDirectory(GrabberActivity.DIRECTORY);
        if (dir.exists()) {
            File from = new File(dir, oldName);
            File to = new File(dir, newName);
            Log.d(TAG, "renameFile " + from + " -> " + to);
            if (from.exists()) {
                return from.renameTo(to);
            }
        }
        return false;
    }
}
