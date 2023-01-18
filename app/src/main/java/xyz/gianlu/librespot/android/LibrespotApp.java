package xyz.gianlu.librespot.android;

import android.app.Application;
import android.os.Build;
import android.util.Log;

import xyz.gianlu.librespot.audio.decoders.Decoders;
import xyz.gianlu.librespot.audio.format.SuperAudioFormat;
import xyz.gianlu.librespot.player.decoders.AndroidNativeDecoder;
import xyz.gianlu.librespot.player.decoders.TremoloVorbisDecoder;

public final class LibrespotApp extends Application {
    private static final String TAG = LibrespotApp.class.getSimpleName();

    static {
        Decoders.registerDecoder(SuperAudioFormat.VORBIS, AndroidNativeDecoder.class);
        Decoders.registerDecoder(SuperAudioFormat.MP3, AndroidNativeDecoder.class);
    }
}
