package com.shyamv.utilities.downloadmanager;

import android.app.DownloadManager;
import android.content.Intent;
import android.database.Cursor;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.ResultReceiver;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.view.Menu;
import android.view.MenuItem;
import android.app.Dialog;
import android.view.View;
import android.webkit.URLUtil;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.EditText;
import android.widget.ImageView;
import android.widget.ListView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.List;


public class MainActivity extends AppCompatActivity
{

    public final static String ARG_REQUEST = "request";
    public final static String ARG_REQUEST_ID = "requestId";

    public final static int REQUEST_ADD_THREAD = 1;
    public final static int REQUEST_CANCEL_THREAD = 2;
    public final static int REQUEST_PAUSE_THREAD = 3;
    public final static int REQUEST_RESUME_THREAD = 4;
    public final static int REQUEST_CONNECT_SERVICE = 5;

    ListView downloadListView;
    CustomAdapter listAdapter;
    ArrayList<Download> downloadList = new ArrayList<>();

    DownloadReceiver downloadReceiver = new DownloadReceiver(new Handler());

    DatabaseHandler mDbHandler;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);

        mDbHandler = DatabaseHandler.getInstance(this);

        mDbHandler.loadAllItems(downloadList);

        downloadListView = (ListView) findViewById(R.id.downloadList);
        listAdapter = new CustomAdapter(this, R.layout.download_item, downloadList);
        downloadListView.setAdapter(listAdapter);

        connectToService();
    }

    @Override
    public boolean onCreateOptionsMenu(Menu menu) {
        // Inflate the menu; this adds items to the action bar if it is present.
        getMenuInflater().inflate(R.menu.menu_main, menu);
        return true;
    }

    int getUniqueDownloadId()
    {
        if(downloadList.size() == 0)
            return 0;

        return downloadList.get(downloadList.size() - 1).id + 1;
    }

    Download getDownload(int id)
    {
        for(int i = 0; i < downloadList.size(); i++)
            if(downloadList.get(i).id == id)
                return downloadList.get(i);

        return null;
    }

    @Override
    public boolean onOptionsItemSelected(MenuItem item)
    {
        // Handle action bar item clicks here. The action bar will
        // automatically handle clicks on the Home/Up button, so long
        // as you specify a parent activity in AndroidManifest.xml.
        int id = item.getItemId();

        //noinspection SimplifiableIfStatement
        if (id == R.id.action_settings) {
            return true;
        }
        else if(id == R.id.action_add_download)
        {
            // Create custom dialog object
            final Dialog dialog = new Dialog(MainActivity.this);
            // Include dialog.xml file
            dialog.setContentView(R.layout.add_download_dialog);
            // Set dialog title
            dialog.setTitle(getString(R.string.action_add_download));

            dialog.show();

            Button downloadButton = (Button) dialog.findViewById(R.id.downloadBtn);
            final EditText urlTxt = (EditText) dialog.findViewById(R.id.downloadURLTxt);

            downloadButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v)
                {
                    String url = urlTxt.getText().toString();//"http://mirror.internode.on.net/pub/test/10meg.test";
                    if(URLUtil.isValidUrl(url))
                    {
                        addDownload(url);
                        dialog.dismiss();
                    }
                    else
                    {
                        Toast.makeText(MainActivity.this, "Invalid URL", Toast.LENGTH_LONG).show();
                    }
                }
            });

            // if cancel button is clicked, close the custom dialog
            Button cancelButton = (Button) dialog.findViewById(R.id.cancelBtn);
            cancelButton.setOnClickListener(new View.OnClickListener() {
                @Override
                public void onClick(View v) {
                    // Close dialog
                    dialog.dismiss();
                }
            });
        }

        return super.onOptionsItemSelected(item);
    }

    Intent getDownloadServiceIntent(int action, int id)
    {
        Intent intent = new Intent(MainActivity.this, DownloadService.class);
        intent.putExtra(ARG_REQUEST, action);
        intent.putExtra(ARG_REQUEST_ID, id);
        intent.putExtra("receiver", downloadReceiver);

        return intent;
    }

    public void connectToService()
    {
        Intent intent = getDownloadServiceIntent(REQUEST_CONNECT_SERVICE, 0);
        startService(intent);
    }

    public void addDownload(String url)
    {
        Download download = new Download(getUniqueDownloadId(), url);
        int id = mDbHandler.addItem(download);
        download.setId(id);

        downloadList.add(download);
        listAdapter.notifyDataSetChanged();

        Intent intent = getDownloadServiceIntent(REQUEST_ADD_THREAD, download.id);
        intent.putExtra("url", url);
        startService(intent);
    }

    public void pauseDownload(Download download)
    {
        Intent intent = getDownloadServiceIntent(REQUEST_PAUSE_THREAD, download.id);
        startService(intent);
    }

    public void resumeDownload(Download download)
    {
        Intent intent = getDownloadServiceIntent(REQUEST_RESUME_THREAD, download.id);
        startService(intent);
    }

    public void cancelDownload(Download download)
    {
        Intent intent = getDownloadServiceIntent(REQUEST_CANCEL_THREAD, download.id);
        startService(intent);

        mDbHandler.deleteItem(download);

        downloadList.remove(download);
        listAdapter.notifyDataSetChanged();
    }

    public void pauseButtonClickHandler(View v)
    {
        Download download = (Download) v.getTag();
        if(download != null)
        {
            if(download.paused)
                resumeDownload(download);
            else
                pauseDownload(download);
        }
    }

    public void cancelButtonClickHandler(View v)
    {
        Download download = (Download) v.getTag();
        if(download != null)
        {
            cancelDownload(download);
        }
    }

    private class DownloadReceiver extends ResultReceiver
    {
        public DownloadReceiver(Handler handler)
        {
            super(handler);
        }

        @Override
        protected void onReceiveResult(int resultCode, Bundle resultData) {
            super.onReceiveResult(resultCode, resultData);
            if (resultCode == DownloadService.UPDATE_PROGRESS)
            {
                int id = resultData.getInt("id");
                float progress = resultData.getFloat("progress");
                boolean paused = resultData.getBoolean("paused");
                String status = resultData.getString("status");

                Download download = getDownload(id);
                if(download != null)
                {
                    download.setPaused(paused);
                    download.setProgress(progress);
                    download.setStatus(status);
                    listAdapter.notifyDataSetChanged();

                    mDbHandler.updateItem(download);
                }
            }
        }
    }
}
