package com.baidu.tts.util;

import android.content.Context;
import android.media.AudioManager;
import android.media.SoundPool;

import com.baidu.tts.R;


/**
 * Description:
 * Created on 2018/8/28 0028 14:32:05
 * author:Ahuangshang
 */
public class SoundPlayUtils {
    public static int soundID;
    // SoundPool对象
    private static SoundPool mSoundPlayer;
    private static SoundPlayUtils soundPlayUtils;

    private SoundPlayUtils() {
    }

    /**
     * 初始化
     */
    public static void init(Context context) {
        if (soundPlayUtils == null) {
            soundPlayUtils = new SoundPlayUtils();
        }
        if (mSoundPlayer == null) {
            mSoundPlayer = new SoundPool(10,
                    AudioManager.STREAM_SYSTEM, 5);
        }
        soundID = mSoundPlayer.load(context, R.raw.di, 1);// 1
    }

    public static void load(Context context, int resId) {
        if (mSoundPlayer == null) {
            mSoundPlayer = new SoundPool(10,
                    AudioManager.STREAM_SYSTEM, 5);
        }
        soundID = mSoundPlayer.load(context, resId, 1);// 1
    }

    /**
     * 播放声音
     */
    public static void play(int soundID) {
        if (mSoundPlayer != null) {
            mSoundPlayer.play(soundID, 1, 1, 0, 0, 1);
        }
    }

    public static void release() {
        if (mSoundPlayer != null) {
            mSoundPlayer.release();
        }
    }

    public static int getSoundID() {
        return soundID;
    }
}
