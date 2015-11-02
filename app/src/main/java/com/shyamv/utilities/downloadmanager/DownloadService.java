package com.shyamv.utilities.downloadmanager;


import android.app.Notification;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.app.Service;
import android.content.Intent;
import android.content.Context;
import android.os.Bundle;
import android.os.Environment;
import android.os.IBinder;
import android.os.ResultReceiver;
import android.support.v7.app.NotificationCompat;
import android.util.Log;
import android.webkit.URLUtil;
import android.widget.RemoteViews;
import android.widget.Toast;
import java.io.BufferedInputStream;
import java.io.File;
import java.io.FileOutputStream;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.List;

public class DownloadService extends Service
{
    public static final int UPDATE_PROGRESS = 1111;
    public static String DownloadFolder = Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/";

    List<MyRunnable> taskList = new ArrayList<>();

    ResultReceiver mReceiver = null;

    DatabaseHandler mDbHandler;

    public DownloadService()
    {
        mDbHandler = DatabaseHandler.getInstance(this);
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId)
    {
        if(intent != null)
        {
            int request = intent.getIntExtra(MainActivity.ARG_REQUEST, -1);
            int requestId = intent.getIntExtra(MainActivity.ARG_REQUEST_ID, -1);

            ResultReceiver receiver = intent.getParcelableExtra("receiver");
            if(receiver != null)
                mReceiver = receiver;

            if(request == MainActivity.REQUEST_CONNECT_SERVICE)
            {
                ArrayList<Download> downloadList = new ArrayList<>();
                mDbHandler.loadAllItems(downloadList);

                for(int i = 0; i < downloadList.size(); i++)
                {
                    Download download = downloadList.get(i);
                    if(download.getProgress() < 100 && !download.paused)
                    {
                        MyRunnable newRunnable = new MyRunnable(download.getId());
                        newRunnable.start();

                        taskList.add(newRunnable);
                    }
                }
            }
            else if(request == MainActivity.REQUEST_ADD_THREAD)
            {
                MyRunnable newRunnable = new MyRunnable(requestId);
                newRunnable.start();

                taskList.add(newRunnable);
            }
            else if(request == MainActivity.REQUEST_CANCEL_THREAD)
            {
                MyRunnable r = getTask(requestId);
                if(r != null)
                    r.stop();
                else
                {
                    /*Download download = mDbHandler.getItem(requestId);
                    if(download != null && download.getProgress() < 100)
                    {
                        String filePath = DownloadFolder + download.getFileName();
                        File file = new File(filePath);
                        file.delete();
                    }*/
                }
            }
            else if(request == MainActivity.REQUEST_PAUSE_THREAD)
            {
                MyRunnable r = getTask(requestId);
                if(r != null)
                    r.pause();
            }
            else if(request == MainActivity.REQUEST_RESUME_THREAD)
            {
                MyRunnable r = getTask(requestId);
                if(r != null)
                    r.resume();
                else
                {
                    Download download = mDbHandler.getItem(requestId);
                    if(download != null && download.getProgress() < 100)
                    {
                        MyRunnable newRunnable = new MyRunnable(download.getId());
                        newRunnable.start();

                        taskList.add(newRunnable);
                    }
                }
            }
        }

        if(taskList.size() == 0)
            stopSelf();

        return START_STICKY;
    }

    private MyRunnable getTask(int id)
    {
        for(int i = 0; i < taskList.size(); i++)
            if(taskList.get(i).id == id)
                return taskList.get(i);

        return null;
    }

    @Override
    public IBinder onBind(Intent intent)
    {
        return null;
    }

    public class MyRunnable implements Runnable
    {
        private Thread thread;
        public int id;
        private boolean paused = false;
        private boolean stopped = false;
        private NotificationManager mNotificationManager;
        private NotificationCompat.Builder mBuilder;
        private Notification mNotification;
        private RemoteViews mNotificationView;

        public MyRunnable(int id)
        {
            this.id = id;

            mBuilder = new NotificationCompat.Builder(DownloadService.this);
            mNotification = mBuilder.build();

            mNotificationView = new RemoteViews(getPackageName(), R.layout.download_item);
        }

        public void start()
        {
            if (thread == null)
            {
                thread = new Thread (this);
                thread.start ();
            }
        }

        @Override
        public void run()
        {
            mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

            Intent notificationIntent = new Intent(DownloadService.this, MainActivity.class);
            notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
            PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, notificationIntent, 0);

            mBuilder.setContentTitle("Download Manager");
            mBuilder.setTicker("Download Manager");
            mBuilder.setContentText("Preparing");
            mBuilder.setSmallIcon(R.mipmap.ic_launcher);
            mBuilder.setContentIntent(pendingIntent);
            mBuilder.setOngoing(true);
            mBuilder.setProgress(100, 0, true);
            mNotification = mBuilder.build();

            //mNotification.contentView = mNotificationView;
            //mNotification.contentIntent = pendingIntent;
            //mNotification.flags |= Notification.FLAG_NO_CLEAR;

            startForeground(id, mNotification);

            boolean isDownloadFinished = false;

            try
            {
                Download d = mDbHandler.getItem(id);

                String urlToDownload = d.getUrl();
                URL url = new URL(urlToDownload);
                HttpURLConnection connection = (HttpURLConnection)url.openConnection();

                String filePath = DownloadFolder+d.getFileName();

                File file = new File(filePath);
                int downloaded = 0;
                if(file.exists())
                {
                    downloaded = (int) file.length();
                    connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
                }

                int fileLength = connection.getContentLength() + downloaded;

                InputStream input = new BufferedInputStream(connection.getInputStream());
                OutputStream output = new FileOutputStream(file, file.exists());

                byte data[] = new byte[1024];
                int total = downloaded;
                int progress = (total * 100 / fileLength);
                int count;

                Log.d("DownloadService", "fileLength: "+fileLength+" downloaded: "+total+" progress: "+progress);

                while ((count = input.read(data, 0, 1024)) != -1)
                {
                    total += count;
                    output.write(data, 0, count);

                    int newProgress = (total * 100 / fileLength);
                    if(newProgress != progress)
                    {
                        updateProgress(newProgress, false, "Downloading");
                        progress = newProgress;

                        Log.d("DownloadService", "fileLength: "+fileLength+" downloaded: "+total+" progress: "+progress);
                    }

                    if(paused)
                        updateProgress(progress, true, "Download paused");

                    if(paused || stopped)
                        break;
                }

                if(paused || stopped || total != fileLength)
                    isDownloadFinished = false;
                else
                    isDownloadFinished = true;

                if(!stopped)
                    output.flush();
                output.close();
                input.close();
                connection.disconnect();

                if(stopped)
                    file.delete();
            }
            catch (Exception e)
            {
                e.printStackTrace();
            }

            taskList.remove(this);

            if(isDownloadFinished)
            {
                updateProgress(100, false, "Download complete");

                Download d = mDbHandler.getItem(id);
                d.setProgress(100);

                mDbHandler.updateItem(d);
            }
            else if(stopped)
                mNotificationManager.cancel(id);

            if(taskList.size() == 0)
                stopSelf();
        }

        void updateProgress(float progress, boolean paused, String status)
        {
            Bundle resultData = new Bundle();
            resultData.putInt("id", id);
            resultData.putFloat("progress", progress);
            resultData.putBoolean("paused", paused);
            resultData.putString("status", status);

            if(mReceiver != null)
                mReceiver.send(UPDATE_PROGRESS, resultData);

            mBuilder.setProgress(100, (int) progress, false);

            if(progress == 100)
            {
                mBuilder.setOngoing(false);
                mBuilder.setContentText(status);
            }
            else
            {
                mBuilder.setContentText(status + ": " + (int)progress + "%");
            }

            mNotification = mBuilder.build();

            mNotificationManager.notify(id, mNotification);
        }

        void pause()
        {
            paused = true;
        }

        synchronized void resume()
        {
            paused = false;
            notify();
        }

        void stop()
        {
            stopped = true;

            resume();
        }
    }
}


/*
//to test download UI with out internet
try
{
    Download d = mDbHandler.getItem(id);

    boolean resuming = false;

    for (int i = (int)d.getProgress(); i < 100; i++)
    {
        Log.d("MultiThreadDebug", "Thread id:" + id + " Progress:" + i);

        updateProgress(i, false, "Downloading");

        Thread.sleep(1000);

        synchronized(this)
        {
            while (paused)
            {
                updateProgress(i, true, "Download paused");

                wait();

                resuming = true;
            }
        }

        if(resuming)
        {
            updateProgress(i, true, "Download resuming");
            Thread.sleep(1000);
            resuming = false;
        }

        if(stopped)
            break;
    }
}
catch(InterruptedException e)
{
    e.printStackTrace();
}*/
/*
    @Override
    public void run()
    {
        mNotificationManager = (NotificationManager) getSystemService(Context.NOTIFICATION_SERVICE);

        Intent notificationIntent = new Intent(DownloadService.this, MainActivity.class);
        notificationIntent.setFlags(Intent.FLAG_ACTIVITY_CLEAR_TOP | Intent.FLAG_ACTIVITY_SINGLE_TOP);
        PendingIntent pendingIntent = PendingIntent.getActivity(DownloadService.this, 0, notificationIntent, 0);

        mBuilder.setContentTitle("Download Manager");
        mBuilder.setTicker("Download Manager");
        mBuilder.setContentText("Preparing");
        mBuilder.setSmallIcon(R.mipmap.ic_launcher);
        mBuilder.setContentIntent(pendingIntent);
        mBuilder.setOngoing(true);
        mBuilder.setProgress(100, 0, true);
        mNotification = mBuilder.build();


        //mNotification.contentView = mNotificationView;
        //mNotification.contentIntent = pendingIntent;
        //mNotification.flags |= Notification.FLAG_NO_CLEAR;

        startForeground(id, mNotification);

        try
        {
            Download d = mDbHandler.getItem(id);

            String urlToDownload = d.getUrl();
            URL url = new URL(urlToDownload);
            HttpURLConnection connection = (HttpURLConnection)url.openConnection();

            String filename = URLUtil.guessFileName(urlToDownload, null, null);
                /*String header = connection.getHeaderField("Content-Disposition");
                if(header != null)
                {
                    String depoSplit[] = header.split("filename=");

                    if (depoSplit.length > 0 && depoSplit[1] != null)
                        filename = depoSplit[1].replace("filename=", "").replace("\"", "").trim();
                }

                if(filename == null)
                    filename = URLUtil.guessFileName(urlToDownload, null, null);*/

          /*  String filePath = Environment.getExternalStorageDirectory()+"/"+Environment.DIRECTORY_DOWNLOADS+"/"+filename;

            File file = new File(filePath);
            int downloaded = 0;
            if(file.exists())
            {
                downloaded = (int) file.length();
                connection.setRequestProperty("Range", "bytes=" + downloaded + "-");
            }

            int fileLength = connection.getContentLength() + downloaded;

            // download the file
            InputStream input = new BufferedInputStream(connection.getInputStream());
            OutputStream output = new FileOutputStream(filePath, file.exists());

            byte data[] = new byte[1024];
            int total = downloaded;
            int progress = (total * 100 / fileLength);
            int count;
            boolean resuming = false;

            Log.d("DownloadService", "fileLength: "+fileLength+" downloaded: "+downloaded+" progress: "+progress);

            while ((count = input.read(data, 0, 1024)) != -1)
            {
                Log.d("DownloadService", "Count: "+count);

                total += count;
                output.write(data, 0, count);

                int newProgress = (total * 100 / fileLength);
                if(newProgress != progress)
                {
                    updateProgress(newProgress, false, "Downloading");
                    progress = newProgress;

                    Log.d("DownloadService", "fileLength: "+fileLength+" downloaded: "+downloaded+" progress: "+progress);
                }

                synchronized(this)
                {
                    while (paused)
                    {
                        updateProgress(progress, true, "Download paused");

                        wait();

                        resuming = true;
                    }
                }

                if(resuming)
                {
                    connection.connect();
                    updateProgress(progress, true, "Download resuming");
                    resuming = false;
                }

                if(stopped)
                    break;
            }

            output.flush();
            output.close();
            input.close();
            connection.disconnect();
        }
        catch (Exception e)
        {
            e.printStackTrace();
        }

        taskList.remove(this);

        if(stopped)
            mNotificationManager.cancel(id);
        else
        {
            updateProgress(100, false, "Download complete");

            Download d = mDbHandler.getItem(id);
            d.setProgress(100);

            mDbHandler.updateItem(d);
        }

        if(taskList.size() == 0)
            stopSelf();
    }*/