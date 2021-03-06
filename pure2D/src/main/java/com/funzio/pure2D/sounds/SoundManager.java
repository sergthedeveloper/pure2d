/*******************************************************************************
 * Copyright (C) 2012-2014 GREE, Inc.
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in
 * all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN
 * THE SOFTWARE.
 ******************************************************************************/
package com.funzio.pure2D.sounds;

import android.annotation.SuppressLint;
import android.content.Context;
import android.media.AudioManager;
import android.media.MediaPlayer;
import android.media.MediaPlayer.OnErrorListener;
import android.media.MediaPlayer.OnPreparedListener;
import android.media.SoundPool;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Handler;
import android.os.Looper;
import android.os.Message;
import android.util.Log;
import android.util.SparseArray;
import android.util.SparseIntArray;

import java.io.IOException;

public class SoundManager extends Thread implements SoundPool.OnLoadCompleteListener, OnPreparedListener, OnErrorListener {
    protected static final String TAG = SoundManager.class.getSimpleName();

    protected static final float DEFAULT_MEDIA_VOLUME = 0.8f;

    // map keys to sounds, for caching
    protected SparseArray<Soundable> mSoundMap;

    protected final SoundPool mSoundPool;
    protected volatile boolean mSoundEnabled = true;
    protected volatile boolean mMediaEnabled = true;
    protected boolean mMediaPrepared;

    protected final Context mContext;
    protected final AudioManager mAudioManager;

    protected MediaPlayer mMediaPlayer;
    protected float mMediaVolume = DEFAULT_MEDIA_VOLUME;

    protected Handler mHandler;

    protected volatile SparseIntArray mStreamIds;

    public SoundManager(final Context context, final int maxStream) {
        mContext = context;
        mSoundMap = new SparseArray<Soundable>();
        mStreamIds = new SparseIntArray();

        mSoundPool = new SoundPool(maxStream, AudioManager.STREAM_MUSIC, 0);
        mSoundPool.setOnLoadCompleteListener(this);

        mAudioManager = (AudioManager) mContext.getSystemService(Context.AUDIO_SERVICE);

        start();
    }

    @Override
    public void run() {
        Looper.prepare();
        mHandler = new SoundHandler();
        Looper.loop();
    }

    public boolean isSoundEnabled() {
        return mSoundEnabled;
    }

    public void setSoundEnabled(final boolean enabled) {
        mSoundEnabled = enabled;
    }

    @SuppressLint("NewApi")
    public void load(final Soundable... sounds) {
        final AsyncLoader loader = new AsyncLoader();
        if (Build.VERSION.SDK_INT > Build.VERSION_CODES.HONEYCOMB) {
            loader.executeOnExecutor(AsyncTask.THREAD_POOL_EXECUTOR, sounds);
        } else {
            loader.execute(sounds);
        }
    }

    private class AsyncLoader extends AsyncTask<Soundable, Void, Void> {
        @Override
        protected Void doInBackground(final Soundable... params) {
            for (final Soundable sound : params) {
                final int soundID = sound.load(mSoundPool);

                // check and add to the map
                if (soundID > 0) {
                    synchronized (mSoundMap) {
                        mSoundMap.put(sound.getKey(), sound);
                    }
                }
            }

            return null;
        }
    }

    public void play(final int key) {
        // Log.v(TAG, "play(" + key + ")");

        final Soundable sound = mSoundMap.get(key);
        if (sound != null) {
            Message msg = new Message();
            msg.obj = sound;
            mHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "Unable to play sound: " + key);
        }
    }

    public void play(final Soundable sound) {
        if (sound != null) {
            Message msg = new Message();
            msg.obj = sound;
            mHandler.sendMessage(msg);
        } else {
            Log.e(TAG, "Unable to play sound: " + sound);
        }
    }

    public void playDelayed(final Soundable sound, final int msec) {
        if (sound != null) {
            Message msg = new Message();
            msg.arg1 = sound.getSoundID();
            msg.arg2 = sound.getLoop();
            mHandler.sendMessageDelayed(msg, msec);
        } else {
            Log.e(TAG, "Unable to play sound: " + sound);
        }
    }

    public int playDuration(final int key, final int loop, final int duration) {
        // Log.v(TAG, "playDuration(" + key + ")");

        final int streamID = forcePlay(key, loop);
        if (streamID != 0) {
            // stop later
            mHandler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    stopStream(streamID);
                }
            }, duration);
        }

        return streamID;
    }

    /**
     * Play synchronously instead of queueing. Not ideal for common usage.
     *
     * @param key
     * @param loop
     * @return stream ID
     */
    public int forcePlay(final int key, final int loop) {
        // Log.v(TAG, "forcePlay(" + sound + ")");

        final Soundable soundable = mSoundMap.get(key);
        if (soundable != null) {
            final int streamID = privatePlay(soundable, loop);
            if (streamID != 0) {
                mStreamIds.put(soundable.getSoundID(), streamID);
            }

            return streamID;
        } else {
            Log.e(TAG, "Unable to play sound: " + key);
            return -1;
        }
    }

    private int privatePlay(final Soundable sound) {
        // Log.v(TAG, "privatePlay(" + sound + ")");
        return privatePlay(sound, sound.getLoop());
    }

    private int privatePlay(final Soundable sound, final int loop) {
        // Log.v(TAG, "privatePlay(" + sound + ")");

        if (mSoundEnabled && sound.getSoundID() > 0) {
            final float volDefault = (float) mAudioManager.getStreamVolume(AudioManager.STREAM_MUSIC) / (float) mAudioManager.getStreamMaxVolume(AudioManager.STREAM_MUSIC);
            float volLeft = sound.getVolumeLeft();
            if (volLeft < 0) volLeft = volDefault;
            float volRight = sound.getVolumeRight();
            if (volRight < 0) volRight = volDefault;
            return mSoundPool.play(sound.getSoundID(), volLeft, volRight, sound.getPriority(), loop, sound.getRate());
        }

        return 0;
    }

    public void play(final Media media) throws IllegalStateException, IOException {
        // initialize
        if (mMediaPlayer == null) {
            mMediaPlayer = new MediaPlayer();
            mMediaPlayer.setOnErrorListener(this);
        }

        mMediaPlayer.reset(); // reset the mediaplayer state - IDLE
        mMediaPrepared = false;

        // load - Transitions to the INITIALIZED State
        if (media.load(mMediaPlayer, mContext) == 0) { // NOTE: Must be called before setting audio related stuff!
            return;
        }

        mMediaPlayer.setAudioStreamType(AudioManager.STREAM_MUSIC); // set type
        mMediaPlayer.setLooping(media.isLooping());
        mMediaPlayer.setOnPreparedListener(this);
        mMediaPlayer.setVolume(mMediaVolume, mMediaVolume);

        // this can also throw IOException
        mMediaPlayer.prepareAsync();
    }

    public void setMediaVolume(final float volume) {
        mMediaVolume = volume;
        if (mMediaPlayer != null) {
            mMediaPlayer.setVolume(mMediaVolume, mMediaVolume);
        }
    }

    public void stopMedia() {
        if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
            mMediaPlayer.stop();
            mMediaPrepared = false;
        }
    }

    public boolean isMediaPlaying() {
        return mMediaPlayer != null && mMediaPlayer.isPlaying();
    }

    public void releaseMedia() {
        if (mMediaPlayer != null) {
            mMediaPlayer.release();
            mMediaPlayer = null;
            mMediaPrepared = false;
        }
    }

    public void seekToMedia(final int msec) {
        if (mMediaPlayer != null) {
            mMediaPlayer.seekTo(msec);
        }
    }

    public boolean isMediaEnabled() {
        return mMediaEnabled;
    }

    public void setMediaEnabled(final boolean mediaEnabled) {
        mMediaEnabled = mediaEnabled;

        if (!mediaEnabled) {
            if (mMediaPlayer != null && mMediaPlayer.isPlaying()) {
                mMediaPlayer.pause();
            }
        } else if (mMediaPlayer != null) {
            // mMediaPlayer.prepareAsync();
            if (mMediaPrepared) {
                mMediaPlayer.start();
            }
        }
    }

    protected void playByID(final int soundID) {
        // Log.v(TAG, "playByID(" + soundID + ")");

        Message msg = new Message();
        msg.arg1 = soundID;
        msg.arg2 = 0;
        mHandler.sendMessage(msg);
    }

    public void stop(final int soundID) {
        synchronized (mStreamIds) {
            int streamID = mStreamIds.get(soundID, -1);

            if (streamID > 0) {
                mSoundPool.stop(streamID);
                mStreamIds.delete(soundID);
            }
        }
    }

    public void stopStream(final int streamID) {
        synchronized (mStreamIds) {
            mSoundPool.stop(streamID);

            final int index = mStreamIds.indexOfValue(streamID);
            if (index >= 0) {
                mStreamIds.removeAt(index);
            }
        }
    }

    public boolean unloadByID(final int soundID) {
        return unload(getSoundByID(soundID));
    }

    public boolean unloadByKey(final int key) {
        return unload(mSoundMap.get(key));
    }

    public boolean unload(final Soundable sound) {
        if (sound != null) {
            synchronized (mSoundMap) {
                // remove from map
                mSoundMap.remove(sound.getKey());
            }

            // unload from sound pool
            return mSoundPool.unload(sound.getSoundID());
        } else {
            return false;
        }
    }

    public int unloadAll() {
        synchronized (mSoundMap) {
            final int len = mSoundMap.size();
            for (int i = 0; i < len; i++) {
                final Soundable sound = mSoundMap.get(mSoundMap.keyAt(i));
                mSoundPool.unload(sound.getSoundID());
            }

            mSoundMap.clear();

            return len;
        }
    }

    /**
     * Find a sound by the sound ID
     *
     * @param soundID
     * @return
     * @see #getSound(int)
     */
    public Soundable getSoundByID(final int soundID) {
        synchronized (mSoundMap) {
            final int size = mSoundMap.size();
            Soundable sound;
            for (int i = 0; i < size; i++) {
                sound = mSoundMap.get(mSoundMap.keyAt(i));
                if (sound.getSoundID() == soundID) {
                    return sound;
                }
            }
        }

        return null;
    }

    /**
     * Find a sound by Key
     *
     * @param key
     * @return
     * @see #getSoundByID(int)
     */
    public Soundable getSound(final int key) {
        return mSoundMap.get(key);
    }

    public Context getContext() {
        return mContext;
    }

    public void dispose() {
        synchronized (mSoundMap) {
            mSoundMap.clear();
            mStreamIds.clear();
        }

        mSoundPool.release();

        releaseMedia();
    }

    public void onLoadComplete(final SoundPool soundPool, final int sampleId, final int status) {
        Log.v(TAG, "onLoadComplete(" + sampleId + ", " + status + ")");
    }

    /*
     * (non-Javadoc)
     * @see android.media.MediaPlayer.OnPreparedListener#onPrepared(android.media.MediaPlayer)
     */
    @Override
    public void onPrepared(final MediaPlayer mp) {
        // check first
        mMediaPrepared = true;
        if (mMediaEnabled) {
            // start the media now
            mp.start();
        }
    }

    /*
     * (non-Javadoc)
     * @see android.media.MediaPlayer.OnErrorListener#onError(android.media.MediaPlayer, int, int)
     */
    @Override
    public boolean onError(final MediaPlayer mp, final int what, final int extra) {
        if (mp != null) {
            mp.reset();
            mMediaPrepared = false;
        }

        return true;
    }

    private class SoundHandler extends Handler {

        @Override
        public void handleMessage(final Message msg) {
            final Soundable sound = (Soundable) msg.obj;
            int streamId = privatePlay(sound);
            if (streamId != 0) {
                mStreamIds.put(sound.getSoundID(), streamId);
            }
        }
    }

}
