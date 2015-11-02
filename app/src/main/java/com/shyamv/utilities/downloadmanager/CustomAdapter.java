package com.shyamv.utilities.downloadmanager;

import android.annotation.TargetApi;
import android.content.Context;
import android.content.res.ColorStateList;
import android.graphics.Color;
import android.graphics.drawable.Drawable;
import android.support.v4.content.ContextCompat;
import android.support.v4.graphics.drawable.DrawableCompat;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.ArrayAdapter;
import android.widget.Button;
import android.widget.ImageButton;
import android.widget.ProgressBar;
import android.widget.TextView;

import java.util.List;

public class CustomAdapter extends ArrayAdapter<Download>
{
    public CustomAdapter(Context context, int resource, List<Download> objects)
    {
        super(context, resource, objects);
    }

    @TargetApi(16)
    @Override
    public View getView(int position, View convertView, ViewGroup parent)
    {
        Download download = getItem(position);

        if (convertView == null)
            convertView = LayoutInflater.from(getContext()).inflate(R.layout.download_item, parent, false);

        TextView fileName = (TextView) convertView.findViewById(R.id.downloadFileName);
        TextView progress = (TextView) convertView.findViewById(R.id.progressTxt);
        ImageButton pauseBtn = (ImageButton) convertView.findViewById(R.id.pauseBtn);
        ImageButton cancelBtn = (ImageButton) convertView.findViewById(R.id.cancelBtn);
        ProgressBar progressBar = (ProgressBar) convertView.findViewById(R.id.progressBar);

        fileName.setText(download.getFileName());

        if(download.progress > 0)
        {
            progressBar.setIndeterminate(false);
            progressBar.setProgress((int)download.progress);
        }

        String status = download.getStatus();

        if(status.isEmpty())
        {
            if(download.progress == 100)
                status = "Download complete";
            else
            {
                if (download.paused)
                    status = "Downloading paused";
                else
                    status = "Downloading";
            }
        }

        if(download.progress == 100)
            progress.setText(status);
        else
        {
            progress.setText(status + ": " + download.progress + "%");

            if(download.paused)
                pauseBtn.setBackgroundResource(R.drawable.ic_play_circle_outline_black_36dp);
            else
                pauseBtn.setBackgroundResource(R.drawable.ic_pause_circle_outline_black_36dp);

            /*Drawable d = ContextCompat.getDrawable(getContext(), R.drawable.ic_play_circle_outline_black_36dp);
            d = DrawableCompat.wrap(d);
            DrawableCompat.setTint(d, Color.RED);

            final int sdk = android.os.Build.VERSION.SDK_INT;
            if(sdk < android.os.Build.VERSION_CODES.JELLY_BEAN)
            {
                pauseBtn.setBackgroundDrawable( d );
            }
            else
            {
                pauseBtn.setBackground( d);
            }*/

            //pauseBtn.set
            //pauseBtn.setBackgroundTintList(new ColorStateList());
            //pauseBtn.setColorFilter(Color.BLUE);
        }

        /*pauseBtn.setOnTouchListener(new View.OnTouchListener() {
            @Override
            public boolean onTouch(View v, MotionEvent event)
            {
                if(event.getActionMasked() == MotionEvent.ACTION_DOWN)
                    ((ImageButton)v).setColorFilter(Color.BLUE);
                else if(event.getActionMasked() == MotionEvent.ACTION_UP)
                    ((ImageButton)v).setColorFilter(Color.BLACK);
                return false;
            }
        });*/

        pauseBtn.setTag(download);
        cancelBtn.setTag(download);

        return convertView;
    }
}
