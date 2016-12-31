package com.home.grabber;

import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Environment;
import android.os.IBinder;
import android.text.format.Formatter;
import android.util.Log;

import com.liulishuo.filedownloader.BaseDownloadTask;
import com.liulishuo.filedownloader.FileDownloadListener;
import com.liulishuo.filedownloader.FileDownloader;

import java.io.File;
import java.util.HashMap;
import java.util.Map;
import java.util.Random;

public class GrabberService extends Service {

    private static final String TAG = GrabberService.class.getSimpleName();

    private static final int NOTIFICATION_ID = 99887766;
    private static final File PATH = new File(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES), "Grabber");

    private static final String ACTION_START = GrabberService.class.getCanonicalName() + ".action.start";
    private static final String ACTION_CANCEL = GrabberService.class.getCanonicalName() + ".action.cancel";

    private static final String EXTRA_FETCH_URL = GrabberService.class.getCanonicalName() + ".extra.fetch_url";
    private static final String EXTRA_TASK_ID = GrabberService.class.getCanonicalName() + ".extra.task_id";

    private NotificationManager notificationManager;
    private StateStorage stateStorage;

    public static Intent buildStartIntent(final Context context, final String fetchUrl) {
        Intent i = new Intent(context, GrabberService.class);
        i.setAction(ACTION_START);
        i.putExtra(EXTRA_FETCH_URL, fetchUrl);
        return i;

    }

    public static Intent buildCancelIntent(final Context context, final int taskId) {
        Intent i = new Intent(context, GrabberService.class);
        i.setAction(ACTION_CANCEL);
        i.putExtra(EXTRA_TASK_ID, taskId);
        return i;
    }

    @Override
    public IBinder onBind(Intent intent) {
        throw new UnsupportedOperationException("Not implemented");
    }

    @Override
    public void onCreate() {
        super.onCreate();
        Log.d(TAG, "onCreate");
        notificationManager = (NotificationManager) getSystemService(NOTIFICATION_SERVICE);
        stateStorage = new StateStorage();
    }

    @Override
    public void onDestroy() {
        super.onDestroy();
        Log.d(TAG, "onDestroy");
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {
        if (intent == null) {
            Log.w(TAG, "Ignoring null intent...");
            return START_REDELIVER_INTENT; // important for screen off case
        }

        Log.d(TAG, "Ensuring service is bound"); // ugly workaround for complicated FileDownloader lib
        FileDownloader.getImpl().insureServiceBind();

        String action = intent.getAction();
        Log.d(TAG, "Received " + action);
        if (ACTION_START.equals(action)) {

            String url = intent.getStringExtra(EXTRA_FETCH_URL);
            Log.d(TAG, "Requesting " + url);

            BaseDownloadTask task = FileDownloader.getImpl().create(url)
                    .setPath(new File(PATH, extractFileName(url)).getAbsolutePath())
                    .setListener(new MyFileDownloadListener());
            int downloadId = task.start();
            Log.d(TAG, "Requested to start downloadId = " + downloadId);

        } else if (ACTION_CANCEL.equals(action)) {
            int downloadId = intent.getIntExtra(EXTRA_TASK_ID, -1);
            FileDownloader.getImpl().pause(downloadId);
            Log.d(TAG, "Requested to pause downloadId = " + downloadId);

        }

        return START_REDELIVER_INTENT; // important for screen off case
    }

    private class MyFileDownloadListener extends FileDownloadListener {
        @Override
        protected void pending(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": pending");
            createOrUpdateRunningNotification("Pending", task, soFarBytes, totalBytes);
        }

        @Override
        protected void started(BaseDownloadTask task) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": started");

            addRunningTaskAndStartForegroundIfFirstTask(task);

            createOrUpdateRunningNotification("Started", task, 0, 0);
        }

        @Override
        protected void connected(BaseDownloadTask task, String etag, boolean isContinue, int soFarBytes, int totalBytes) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": connected");
            createOrUpdateRunningNotification("Connected", task, soFarBytes, totalBytes);
        }

        @Override
        protected void progress(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": progress - " + soFarBytes + "/" + totalBytes);
            createOrUpdateRunningNotification("Downloading", task, soFarBytes, totalBytes);
        }

        @Override
        protected void blockComplete(BaseDownloadTask task) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": blockComplete");
            // ignore, wait for completed to be called
        }

        @Override
        protected void retry(final BaseDownloadTask task, final Throwable ex, final int retryingTimes, final int soFarBytes) {
            Log.w(TAG, "DownloadId = " + task.getId() + ": retry", ex);
        }

        @Override
        protected void completed(BaseDownloadTask task) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": completed");

            removeRunningTaskAndStopForegroundIfLastTask(task);

            createCompletedNotification(task);
        }

        @Override
        protected void paused(BaseDownloadTask task, int soFarBytes, int totalBytes) {
            Log.d(TAG, "DownloadId = " + task.getId() + ": paused");

            removeRunningTaskAndStopForegroundIfLastTask(task);
        }

        @Override
        protected void error(BaseDownloadTask task, Throwable e) {
            Log.e(TAG, "DownloadId = " + task.getId() + ": error", e);

            removeRunningTaskAndStopForegroundIfLastTask(task);

            createErrorNotification(task, e);
        }

        @Override
        protected void warn(BaseDownloadTask task) {
            Log.w(TAG, "DownloadId = " + task.getId() + ": warn (duplicate task already running)");
        }
    }

    private void addRunningTaskAndStartForegroundIfFirstTask(BaseDownloadTask task) {
        if (stateStorage.getRunningTasks(this) == 0) {
            Log.d(TAG, "First task, starting foreground service ...");
            FileDownloader.getImpl().startForeground(NOTIFICATION_ID, createNotification());
        }
        stateStorage.putRunningTask(this, task.getId());
    }

    private void removeRunningTaskAndStopForegroundIfLastTask(BaseDownloadTask task) {
        stateStorage.removeRunningTask(this, task.getId());
        notificationManager.cancel(task.getId());
        if (stateStorage.getRunningTasks(this) == 0) {
            Log.d(TAG, "Last task completed, stopping foreground service ...");
            FileDownloader.getImpl().stopForeground(true);
            stopSelf();
        }
    }

    private Notification createNotification() {
        PendingIntent pIntent = PendingIntent.getService(this, (int) System.currentTimeMillis(), new Intent(), 0);
        return new Notification.Builder(this)
                .setContentTitle("Background Download Service")
                .setContentText("Running")
                .setSmallIcon(R.mipmap.ic_stat_teddy_bear_machine_white)
                .setContentIntent(pIntent).build();
    }

    private void createOrUpdateRunningNotification(String title, BaseDownloadTask task, int soFarBytes, int totalBytes) {
        PendingIntent cancelPendingIntent = PendingIntent.getService(this, (int) System.currentTimeMillis(), buildCancelIntent(this, task.getId()), 0);
        String summary = getPercent(soFarBytes, totalBytes) + " of " + Formatter.formatShortFileSize(this, totalBytes);
        Notification n = new Notification.Builder(this)
                .setContentTitle(title)
                .setContentText(summary)
                .setSmallIcon(R.mipmap.ic_stat_teddy_bear_machine_white)
                .addAction(android.R.drawable.ic_menu_close_clear_cancel, getString(R.string.notification_action_cancel), cancelPendingIntent)
                .setOngoing(true)
                .setAutoCancel(false).build();

        notificationManager.notify(task.getId(), n);
    }

    private String getPercent(int soFarBytes, int totalBytes) {
        int percent = (int) ((soFarBytes * 100.0f) / totalBytes);
        return String.valueOf(percent) + "%";
    }

    private void createCompletedNotification(BaseDownloadTask task) {
        // Open folder
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Grabber").getPath());
        intent.setDataAndType(uri, "resource/folder");

        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        Notification n = new Notification.Builder(this)
                .setContentTitle("Download successful")
                .setContentText("Open Download directory")
                .setStyle(new Notification.BigTextStyle().bigText("Open Download directory" +
                        "\nFilename: " + task.getFilename()))
                .setSmallIcon(R.mipmap.ic_stat_teddy_bear_machine_white)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        notificationManager.notify(task.getId(), n);
    }

    private void createErrorNotification(BaseDownloadTask task, Throwable e) {
        // Open folder
        Intent intent = new Intent(Intent.ACTION_VIEW);
        Uri uri = Uri.parse(Environment.getExternalStoragePublicDirectory(Environment.DIRECTORY_PICTURES + "/Grabber").getPath());
        intent.setDataAndType(uri, "resource/folder");

        PendingIntent pIntent = PendingIntent.getActivity(this, (int) System.currentTimeMillis(), intent, 0);
        Notification n = new Notification.Builder(this)
                .setContentTitle("Download failed")
                .setContentText("Exception: " + e.getMessage())
                .setStyle(new Notification.BigTextStyle().bigText("Failed file: " + task.getFilename() +
                        "\nException: " + e.toString()))
                .setSmallIcon(R.mipmap.ic_stat_teddy_bear_machine_white)
                .setContentIntent(pIntent)
                .setAutoCancel(true).build();

        notificationManager.notify(task.getId(), n);
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

}
