package com.yuanzhou.vlc.vlcplayer;

import android.annotation.SuppressLint;
import android.content.Context;
import android.graphics.Bitmap;
import android.graphics.SurfaceTexture;
import android.media.AudioManager;
import android.net.Uri;
import android.util.DisplayMetrics;
import android.util.Log;
import android.view.Surface;
import android.view.TextureView;
import android.view.View;

import com.facebook.react.bridge.Arguments;
import com.facebook.react.bridge.LifecycleEventListener;
import com.facebook.react.bridge.WritableMap;
import com.facebook.react.uimanager.ThemedReactContext;

import org.videolan.libvlc.IVLCVout;
import org.videolan.libvlc.LibVLC;
import org.videolan.libvlc.Media;
import org.videolan.libvlc.MediaPlayer;
import org.videolan.vlc.RecordEvent;
import org.videolan.vlc.util.VLCInstance;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.util.ArrayList;

@SuppressLint("ViewConstructor")
class ReactVlcPlayerView extends TextureView implements
        LifecycleEventListener,
        AudioManager.OnAudioFocusChangeListener{

    private static final String TAG = "ReactExoplayerView";

    private final VideoEventEmitter eventEmitter;
    private LibVLC libvlc;
    private MediaPlayer mMediaPlayer = null;
    private Surface surfaceVideo;//视频画布
    TextureView surfaceView;
    private boolean isSurfaceViewDestory;
    //资源路径
    private String src;
    //是否网络资源
    private  boolean netStrTag;
    private String[] initOptions;

    private int mVideoHeight = 0;
    private int mVideoWidth = 0;
    private int mVideoVisibleHeight = 0;
    private int mVideoVisibleWidth = 0;
    private int mSarNum = 0;
    private int mSarDen = 0;
    private int screenWidth = 0;
    private int screenHeight = 0;
    private boolean isPaused = true;
    private boolean isHostPaused = false;
    private int preVolume = 200;
    private boolean haEnabled = true;
    private boolean hasVideoOut = false;
    private boolean muted = false;
    private String aspectRatio = null;

    // React
    private final ThemedReactContext themedReactContext;
    private final AudioManager audioManager;

    public ReactVlcPlayerView(ThemedReactContext context) {
        super(context);
        this.eventEmitter = new VideoEventEmitter(context);
        this.themedReactContext = context;
        audioManager = (AudioManager) context.getSystemService(Context.AUDIO_SERVICE);
        themedReactContext.addLifecycleEventListener(this);
        DisplayMetrics dm = getResources().getDisplayMetrics();
        screenHeight = dm.heightPixels;
        screenWidth = dm.widthPixels;

        surfaceView = this;
        surfaceView.setSurfaceTextureListener(videoSurfaceListener);
        surfaceView.addOnLayoutChangeListener(onLayoutChangeListener);
    }


    @Override
    public void setId(int id) {
        super.setId(id);
        eventEmitter.setViewId(id);
    }

    @Override
    protected void onAttachedToWindow() {
        super.onAttachedToWindow();
        //createPlayer();
    }

    @Override
    protected void onDetachedFromWindow() {
        super.onDetachedFromWindow();
        stopPlayback();
    }

    // LifecycleEventListener implementation

    @Override
    public void onHostResume() {
        if(mMediaPlayer != null && isSurfaceViewDestory && isHostPaused){
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            if(!vlcOut.areViewsAttached()){
//                vlcOut.setVideoSurface(this.getHolder().getSurface(), this.getHolder());
//                vlcOut.attachViews(onNewVideoLayoutListener);
                attachViews();
                isSurfaceViewDestory = false;
                isPaused = false;
                this.setKeepScreenOn(true);
                mMediaPlayer.play();
            }
        }
    }


    @Override
    public void onHostPause() {
        if(!isPaused && mMediaPlayer != null){
            isPaused = true;
            isHostPaused = true;
            mMediaPlayer.pause();
            this.setKeepScreenOn(false);
            WritableMap map = Arguments.createMap();
            map.putString("type","Paused");
            eventEmitter.onVideoStateChange(map);
        }
        Log.i("onHostPause","---------onHostPause------------>");
    }



    @Override
    public void onHostDestroy() {
        stopPlayback();
    }


    // AudioManager.OnAudioFocusChangeListener implementation
    @Override
    public void onAudioFocusChange(int focusChange) {
    }


    /*************
     * Events  Listener
     *************/

    private View.OnLayoutChangeListener onLayoutChangeListener = new View.OnLayoutChangeListener(){

        @Override
        public void onLayoutChange(View view, int i, int i1, int i2, int i3, int i4, int i5, int i6, int i7) {
            mVideoWidth = view.getWidth(); // 获取宽度
            mVideoHeight = view.getHeight(); // 获取高度
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            vlcOut.setWindowSize(mVideoWidth,mVideoHeight);
        }
    };

    /**
     * 播放过程中的时间事件监听
     */
    private MediaPlayer.EventListener mPlayerListener = new MediaPlayer.EventListener(){
        long currentTime = 0;
        long totalLength = 0;
        @Override
        public void onEvent(MediaPlayer.Event event) {
            boolean isPlaying = mMediaPlayer.isPlaying();
            currentTime = mMediaPlayer.getTime();
            totalLength = mMediaPlayer.getLength();
            WritableMap map = Arguments.createMap();
            map.putBoolean("isPlaying",isPlaying);
            map.putBoolean("hasVideoOut",hasVideoOut);
            map.putDouble("currentTime",currentTime);
            map.putDouble("duration",totalLength);
            switch (event.type) {
                case MediaPlayer.Event.EndReached:
                    map.putString("type","Ended");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Playing:
                    map.putString("type","Playing");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Opening:
                    map.putString("type","Opening");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Paused:
                    map.putString("type","Paused");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Buffering:
                    if(muted){
                        mMediaPlayer.setAudioTrack(-1);
                    }
                    map.putDouble("bufferRate",event.getBuffering());
                    map.putString("type","Buffering");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.Stopped:
                    map.putString("type","Stopped");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.EncounteredError:
                    map.putString("type","Error");
                    eventEmitter.onVideoStateChange(map);
                    break;
                case MediaPlayer.Event.TimeChanged:
                    map.putString("type","TimeChanged");
                    eventEmitter.onVideoStateChange(map);
                    eventEmitter.progressChanged(currentTime, totalLength);
                    break;
                case MediaPlayer.Event.PositionChanged:
                    map.putString("type","PositionChanged");
                    eventEmitter.onVideoStateChange(map);
                    eventEmitter.progressChanged(currentTime, totalLength);
                    break;
                case MediaPlayer.Event.Vout:
//                    hasVideoOut = (event.getVoutCount() > 0);
                    map.putString("type","Vout");
                    map.putBoolean("hasVideoOut",hasVideoOut);
                    eventEmitter.onVideoStateChange(map);
                    break;
                default:
                    map.putString("type",event.type+"");
                    eventEmitter.onVideoStateChange(map);
                    break;
            }
            eventEmitter.isPlaying(mMediaPlayer.isPlaying());
        }
    };

    private IVLCVout.OnNewVideoLayoutListener onNewVideoLayoutListener = new IVLCVout.OnNewVideoLayoutListener(){
        @Override
        public void onNewVideoLayout(IVLCVout vout, int width, int height, int visibleWidth, int visibleHeight, int sarNum, int sarDen) {
            if (width * height == 0)
                return;
            // store video size
            mVideoWidth = width;
            mVideoHeight = height;
            mVideoVisibleWidth  = visibleWidth;
            mVideoVisibleHeight = visibleHeight;
            mSarNum = sarNum;
            mSarDen = sarDen;
        }
    };

    IVLCVout.Callback callback = new IVLCVout.Callback() {
        @Override
        public void onSurfacesCreated(IVLCVout ivlcVout) {
            isSurfaceViewDestory = false;
        }

        @Override
        public void onSurfacesDestroyed(IVLCVout ivlcVout) {
            isSurfaceViewDestory = true;
        }

    };



    /*************
     * MediaPlayer
     *************/


    private void stopPlayback() {
        onStopPlayback();
        releasePlayer();
    }

    private void onStopPlayback() {
        setKeepScreenOn(false);
        audioManager.abandonAudioFocus(this);
    }

    private void createPlayer(boolean autoplay) {
        releasePlayer();
        try {
            // Create LibVLC
            ArrayList<String> options = new ArrayList<String>(50);
            libvlc =  VLCInstance.get(getContext());
            //libvlc = new LibVLC(getContext(), options);
            // Create media player
            mMediaPlayer = new MediaPlayer(libvlc);
            mMediaPlayer.setEventListener(mPlayerListener);
            surfaceView = this;
//            surfaceView.addOnLayoutChangeListener(onLayoutChangeListener);
            this.setKeepScreenOn(true);
            IVLCVout vlcOut =  mMediaPlayer.getVLCVout();
            if(mVideoWidth > 0 && mVideoHeight > 0){
                vlcOut.setWindowSize(mVideoWidth,mVideoHeight);
            }
            if (!vlcOut.areViewsAttached()) {
                vlcOut.addCallback(callback);
//                vlcOut.setVideoView(surfaceView);
//                vlcOut.attachViews(onNewVideoLayoutListener);
                attachViews();
            }
            DisplayMetrics dm = getResources().getDisplayMetrics();
            Media m = null;
            if(netStrTag){
                Uri uri = Uri.parse(this.src);
                m = new Media(libvlc, uri);
            }else{
                m = new Media(libvlc, this.src);
            }

            if(this.haEnabled){
                m.setHWDecoderEnabled(true, false);
            }

            if(null != initOptions){
                for(int i = 0; i < initOptions.length; i++){
                    m.addOption(initOptions[i]);
                }
            }

/*            m.addOption(":rtsp-tcp");
            int cache = 200;
            m.addOption(":file-caching="+cache);
            m.addOption(":network-caching="+cache);
            m.addOption(":codec=mediacodec,iomx,all");
            m.addOption(":demux=h264");*/

            mMediaPlayer.setMedia(m);
            mMediaPlayer.setAspectRatio(aspectRatio);
            mMediaPlayer.setScale(0);
            if(autoplay){
                isPaused = false;
                mMediaPlayer.play();
            }
            eventEmitter.loadStart();
        } catch (Exception e) {
            //Toast.makeText(getContext(), "Error creating player!", Toast.LENGTH_LONG).show();
        }
    }

    private void releasePlayer() {
        this.hasVideoOut = false;
        if (libvlc == null)
            return;
        mMediaPlayer.stop();
        final IVLCVout vout = mMediaPlayer.getVLCVout();
        vout.removeCallback(callback);
        vout.detachViews();
//        surfaceView.removeOnLayoutChangeListener(onLayoutChangeListener);
        libvlc.release();
        libvlc = null;
    }

    /**
     *  视频进度调整
     * @param time
     */
    public void seekTo(long time) {
        if(mMediaPlayer != null){
            mMediaPlayer.setTime(time);
            mMediaPlayer.isSeekable();
        }
    }

    /**
     * 设置资源路径
     * @param uri
     * @param isNetStr
     */
    public void setSrc(String uri, boolean isNetStr, boolean autoplay) {
        this.src = uri;
        this.netStrTag = isNetStr;
        createPlayer(autoplay);
    }


    /**
     * 改变播放速率
     * @param rateModifier
     */
    public void setRateModifier(float rateModifier) {
        if(mMediaPlayer != null){
            mMediaPlayer.setRate(rateModifier);
        }
    }


    /**
     * 改变声音大小
     * @param volumeModifier
     */
    public void setVolumeModifier(int volumeModifier) {
        if(mMediaPlayer != null){
            mMediaPlayer.setVolume(volumeModifier);
        }
    }

    /**
     * 改变静音状态
     * @param muted
     */
    public void setMutedModifier(boolean muted) {
        this.muted = muted;
        if(mMediaPlayer != null){
            if(muted){
                mMediaPlayer.setAudioTrack(-1);
            }else{
                mMediaPlayer.setAudioTrack(1);
            }
        }
    }

    /**
     * 改变播放状态
     * @param paused
     */
    public void setPausedModifier(boolean paused){
        if(mMediaPlayer != null){
            if(paused){
                isPaused = true;
                mMediaPlayer.pause();
            }else{
                isPaused = false;
                mMediaPlayer.play();
            }
        }
    }


    /**
     * 截图
     * @param path
     */
    public int doSnapshot(String path){
        if(mMediaPlayer != null){
            boolean result = saveBitmap(path,this.getBitmap());
            return result?1:0;
            /*int result = new RecordEvent().takeSnapshot(mMediaPlayer,path,0,0);
            if(result == 0){
                eventEmitter.onSnapshot(1);
            }else{
                eventEmitter.onSnapshot(0);
            }
            return result;*/
        }
        return -1;
    }


    /**
     * 重新加载视频
     * @param autoplay
     */
    public void doResume(boolean autoplay){
        // createPlayer(autoplay);
        this.hasVideoOut = false;
        mMediaPlayer.stop();
        mMediaPlayer.play();
    }


    public void setRepeatModifier(boolean repeat){
    }


    /**
     * 改变宽高比
     * @param aspectRatio
     */
    public void setAspectRatio(String aspectRatio){
        this.aspectRatio = aspectRatio;
        if(mMediaPlayer != null){
            mMediaPlayer.setAspectRatio(aspectRatio);
        }
    }

    public void cleanUpResources() {
        stopPlayback();
    }

    public void setInitOptions(String[] initOptions) {
        this.initOptions = initOptions;
    }

    public void setHaEnabled(boolean enabled) {
        if(mMediaPlayer != null){
            mMediaPlayer.getMedia().setHWDecoderEnabled(haEnabled, false);
        }
        this.haEnabled = enabled;
    }

    public int takeSnapshot(String path){
        if(mMediaPlayer != null) {
            boolean result = saveBitmap(path,this.getBitmap());
            return result?0:-1;
//            return new RecordEvent().takeSnapshot(mMediaPlayer, path, 0, 0);
        }
        return -1;
    }


    public boolean startRecord(String fileDirectory){
        return new RecordEvent().startRecord(mMediaPlayer,fileDirectory);
    }

    public boolean stopRecord(){
        return new RecordEvent().stopRecord(mMediaPlayer);
    }

    public static boolean saveBitmap(String savePath, Bitmap mBitmap) {
        try {
            File filePic = new File(savePath);
            if (!filePic.exists()) {
                filePic.getParentFile().mkdirs();
                filePic.createNewFile();
            }
            FileOutputStream fos = new FileOutputStream(filePic);
            mBitmap.compress(Bitmap.CompressFormat.JPEG, 100, fos);
            fos.flush();
            fos.close();
        } catch (IOException e) {
            e.printStackTrace();
            return false;
        }

        return true;
    }

    private void attachViews(){
        IVLCVout vlcOut = mMediaPlayer.getVLCVout();
        if (!vlcOut.areViewsAttached() && null != surfaceVideo){
            vlcOut.attachSurfaceSlave(surfaceVideo,null,onNewVideoLayoutListener);
        }
    }

    private TextureView.SurfaceTextureListener videoSurfaceListener = new TextureView.SurfaceTextureListener() {
        @Override
        public void onSurfaceTextureAvailable(SurfaceTexture surface, int width, int height) {
//            videoMediaLogic.setWindowSize(width, height);
//            videoMediaLogic.setSurface(new Surface(surface), null);
            surfaceVideo = new Surface(surface);
            if (mMediaPlayer!=null){
                IVLCVout vlcOut = mMediaPlayer.getVLCVout();
                vlcOut.setWindowSize(width,height);
                attachViews();
            }
        }

        @Override
        public void onSurfaceTextureSizeChanged(SurfaceTexture surface, int width, int height) {
//            videoMediaLogic.setWindowSize(width, height);
            mMediaPlayer.getVLCVout().setWindowSize(width,height);
        }

        @Override
        public boolean onSurfaceTextureDestroyed(SurfaceTexture surface) {
//            videoMediaLogic.onSurfaceTextureDestroyedUI();
            mMediaPlayer.getVLCVout().detachViews();
            return true;//回收掉Surface
        }

        @Override
        public void onSurfaceTextureUpdated(SurfaceTexture surface) {
            hasVideoOut = true;
        }
    };
}
