package com.shyamv.utilities.downloadmanager;

import android.webkit.URLUtil;

import java.net.HttpURLConnection;
import java.net.URL;

public class Download
{
    public int id;
    public String url;
    public String fileName;
    public boolean paused;
    public float progress;
    private String status;

    public Download(int id, String downlodURL)
    {
        fileName = URLUtil.guessFileName(url, null, null);
        /*if(fileName == null)
        {
            try
            {
                URL url = new URL(downlodURL);
                HttpURLConnection connection = (HttpURLConnection) url.openConnection();

                String header = connection.getHeaderField("Content-Disposition");
                if (header != null) {
                    String depoSplit[] = header.split("filename=");

                    if (depoSplit.length > 0 && depoSplit[1] != null)
                        fileName = depoSplit[1].replace("filename=", "").replace("\"", "").trim();
                }
            }
            catch(Exception e)
            {
                e.printStackTrace();
            }
        }*/
        set(id, url, fileName, false, 0);
    }

    public Download(int id, String url, String fileName, boolean paused, float progress)
    {
        set(id, url, fileName, paused, progress);
    }

    public void set(int id, String url, String fileName, boolean paused, float progress)
    {
        this.id = id;
        this.url = url;
        this.fileName = fileName;
        this.paused = paused;
        this.progress = progress;
        status = "";
    }

    public String getUrl() {
        return url;
    }

    public void setUrl(String url) {
        this.url = url;
    }

    public String getFileName() {
        return fileName;
    }

    public void setFileName(String fileName) {
        this.fileName = fileName;
    }

    public float getProgress() {
        return progress;
    }

    public void setProgress(float progress) {
        this.progress = progress;
    }

    public boolean isPaused() {
        return paused;
    }

    public void setPaused(boolean paused) {
        this.paused = paused;
    }

    public int getId() {
        return id;
    }

    public void setId(int id) {
        this.id = id;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }
}
