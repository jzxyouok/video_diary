package com.mti.videodialy.adapter;

import android.app.Activity;
import android.app.ActivityOptions;
import android.content.ContentResolver;
import android.content.ContentValues;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.graphics.drawable.BitmapDrawable;
import android.net.Uri;
import android.os.Bundle;
import android.provider.MediaStore;
import android.support.v4.app.ActivityOptionsCompat;
import android.support.v4.content.LocalBroadcastManager;
import android.support.v7.widget.CardView;
import android.support.v7.widget.RecyclerView;
import android.view.LayoutInflater;
import android.view.MotionEvent;
import android.view.View;
import android.view.ViewGroup;
import android.widget.EditText;
import android.widget.FrameLayout;
import android.widget.ImageView;

import com.daimajia.swipe.SimpleSwipeListener;
import com.daimajia.swipe.SwipeLayout;
import com.daimajia.swipe.adapters.RecyclerSwipeAdapter;
import com.mti.videodialy.activity.BaseActivity;
import com.mti.videodialy.activity.CreateVideoNoteActivity;
import com.mti.videodialy.activity.MenuActivity;
import com.mti.videodialy.data.DataBaseManager;
import com.mti.videodialy.data.dao.Video;
import com.mti.videodialy.fragment.VideoFragment;
import com.nostra13.universalimageloader.core.ImageLoader;
import com.nostra13.universalimageloader.core.assist.FailReason;
import com.nostra13.universalimageloader.core.listener.SimpleImageLoadingListener;

import java.io.File;
import java.util.List;

import mti.com.videodiary.R;

/**
 * Created by Taras Matolinets on 24.02.15.
 */
public class VideoAdapter extends RecyclerSwipeAdapter<VideoAdapter.ViewHolder> implements View.OnClickListener {

    private static final String FILE = "file:///";
    public static final String KEY_POSITION = "com.mti.position.key";
    private Context mContext;
    private List<Video> mListVideos;
    private View view;

    public VideoAdapter(Context context, List<Video> listVideos) {
        mContext = context;
        mListVideos = listVideos;
    }

    @Override
    public ViewHolder onCreateViewHolder(ViewGroup parent, int viewType) {
        view = LayoutInflater.from(parent.getContext()).inflate(R.layout.elem_view_video, parent, false);

        ViewHolder vh = new ViewHolder(view);

        return vh;
    }

    @Override
    public void onBindViewHolder(final ViewHolder holder, final int position) {
        Video video = mListVideos.get(position);

        holder.delete.setTag(position);
        holder.cardView.setTag(position);
        holder.share.setTag(position);

        holder.tvDescription.clearFocus();
        holder.tvTitle.clearFocus();

        holder.tvDescription.setText(video.getDescription());
        holder.tvTitle.setText(video.getTitle());

        ImageLoader.getInstance().displayImage(FILE + video.getImageUrl(), holder.imIcon, new SimpleImageLoadingListener() {
            @Override
            public void onLoadingStarted(String imageUri, View view) {

            }

            @Override
            public void onLoadingFailed(String imageUri, View view, FailReason failReason) {

            }

            @Override
            public void onLoadingComplete(String imageUri, View view, Bitmap loadedImage) {
                holder.imIcon.setImageBitmap(loadedImage);

            }
        });
    }

    @Override
    public int getItemCount() {
        return mListVideos.size();
    }

    public void setListVideos(List<Video> list) {
        mListVideos = list;
    }

    @Override
    public void onClick(View v) {
        int position = (int) v.getTag();

        switch (v.getId()) {
            case R.id.trash:
                Video video = mListVideos.get(position);

                File videoFile = new File(video.getVideoName());
                File fileImage = new File(video.getImageUrl());

                deleteFile(videoFile);
                deleteFile(fileImage);
                DataBaseManager.getInstance().deleteVideoById(video.getId());

                mListVideos.remove(video);
                notifyItemRemoved(position);
                notifyItemRangeChanged(position, mListVideos.size());

                Intent intent = new Intent(VideoFragment.UPDATE_ADAPTER);
                LocalBroadcastManager.getInstance(mContext).sendBroadcast(intent);

                break;
            case R.id.cardViewCreateVideo:
                int[] screenLocation = new int[2];
                v.getLocationOnScreen(screenLocation);
                int orientation = mContext.getResources().getConfiguration().orientation;

                Intent activityIntent = new Intent(mContext, CreateVideoNoteActivity.class);

                activityIntent.putExtra(KEY_POSITION, position).
                        putExtra(BaseActivity.PACKAGE + ".orientation", orientation).
                        putExtra(BaseActivity.PACKAGE + ".left", screenLocation[0]).
                        putExtra(BaseActivity.PACKAGE + ".top", screenLocation[1]).
                        putExtra(BaseActivity.PACKAGE + ".width", v.getWidth()).
                        putExtra(BaseActivity.PACKAGE + ".height", v.getHeight());

                ((MenuActivity) mContext).startActivityForResult(activityIntent, MenuActivity.UPDATE_VIDEO_ADAPTER);

                ((Activity) mContext).overridePendingTransition(0, 0);
                break;
            case R.id.ivShare:
                Video videoForShare = mListVideos.get(position);

                ContentValues content = new ContentValues(4);
                content.put(MediaStore.Video.VideoColumns.TITLE, videoForShare.getTitle());
                content.put(MediaStore.Video.VideoColumns.DATE_ADDED,
                        System.currentTimeMillis() / 1000);
                content.put(MediaStore.Video.Media.MIME_TYPE, "video/mp4");
                content.put(MediaStore.Video.Media.DATA, videoForShare.getVideoName());
                ContentResolver resolver = mContext.getContentResolver();
                Uri uri = resolver.insert(MediaStore.Video.Media.EXTERNAL_CONTENT_URI, content);

                Intent sharingIntent = new Intent(android.content.Intent.ACTION_SEND);
                sharingIntent.setType("video/*");
                sharingIntent.putExtra(android.content.Intent.EXTRA_SUBJECT, videoForShare.getTitle());
                sharingIntent.putExtra(android.content.Intent.EXTRA_STREAM, uri);
                mContext.startActivity(Intent.createChooser(sharingIntent, "share:"));

                break;
        }
    }

    private void deleteFile(File file) {
        if (file.exists())
            file.delete();
    }


    @Override
    public int getSwipeLayoutResourceId(int i) {
        return 0;
    }

    public class ViewHolder extends RecyclerView.ViewHolder {

        public EditText tvTitle;
        public EditText tvDescription;
        public ImageView imIcon;
        public ImageView delete;
        public ImageView share;
        public CardView cardView;
        public FrameLayout flMain;
        public SwipeLayout swipe;

        public ViewHolder(View itemLayoutView) {
            super(itemLayoutView);

            tvDescription = (EditText) itemLayoutView.findViewById(R.id.etDescription);
            swipe = (SwipeLayout) itemLayoutView.findViewById(R.id.swipe);
            tvTitle = (EditText) itemLayoutView.findViewById(R.id.etTitle);
            imIcon = (ImageView) itemLayoutView.findViewById(R.id.ivVideoThumbnail);
            share = (ImageView) itemLayoutView.findViewById(R.id.ivShare);
            delete = (ImageView) itemLayoutView.findViewById(R.id.trash);
            cardView = (CardView) itemLayoutView.findViewById(R.id.cardViewCreateVideo);
            flMain = (FrameLayout) itemLayoutView.findViewById(R.id.flMain);

            cardView.setOnClickListener(VideoAdapter.this);
            delete.setOnClickListener(VideoAdapter.this);
            share.setOnClickListener(VideoAdapter.this);
        }
    }
}
