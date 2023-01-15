package xyz.gianlu.librespot.android;

import android.app.Service;
import android.content.Intent;
import android.os.Build;
import android.os.Bundle;
import android.os.Handler;
import android.os.IBinder;
import android.os.Looper;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.Nullable;
import androidx.annotation.UiThread;
import androidx.appcompat.app.AppCompatActivity;

import com.spotify.connectstate.Connect;

import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Range;

import java.io.File;
import java.io.IOException;
import java.security.GeneralSecurityException;
import java.util.Locale;
import java.util.Timer;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

import xyz.gianlu.librespot.android.sink.AndroidSinkOutput;
import xyz.gianlu.librespot.audio.MetadataWrapper;
import xyz.gianlu.librespot.core.Session;
import xyz.gianlu.librespot.mercury.MercuryClient;
import xyz.gianlu.librespot.metadata.PlayableId;
import xyz.gianlu.librespot.player.Player;
import xyz.gianlu.librespot.player.PlayerConfiguration;

public final class MainService extends Service {
    private static final String TAG = MainActivity.class.getSimpleName();
    private final ExecutorService executorService = Executors.newSingleThreadExecutor();

    @Override
    public void  onDestroy() {
        super.onDestroy();
        LibrespotHolder.clear();
    }

    @Nullable
    @Override
    public IBinder onBind(Intent intent) {
        return null;
    }

    @Override
    public int onStartCommand(Intent intent, int flags, int startId) {

        File credentialsFile = Utils.getCredentialsFile(this);

        Runnable closingRunnable = () -> {
            executorService.execute(new SessionClosingRunnable(() -> {}));
        };

        Player.EventsListener playerListener = new Player.EventsListener() {
            private Runnable inactivityRunnable;
            private final Handler handler = new Handler();
            private final Timer timer = new Timer();

            @Override
            public void onContextChanged(@NotNull Player player, @NotNull String s) {
                Log.i(TAG, "Context changed");
            }

            @Override
            public void onTrackChanged(@NotNull Player player, @NotNull PlayableId playableId, @org.jetbrains.annotations.Nullable MetadataWrapper metadataWrapper, boolean b) {

            }

            @Override
            public void onPlaybackEnded(@NotNull Player player) {
                Log.i(TAG, "Playback ended");
            }

            @Override
            public void onPlaybackPaused(@NotNull Player player, long l) {
            }

            @Override
            public void onPlaybackResumed(@NotNull Player player, long l) {
                if (inactivityRunnable != null) {
                    handler.removeCallbacks(inactivityRunnable);
                    inactivityRunnable = null;
                }
            }

            @Override
            public void onTrackSeeked(@NotNull Player player, long l) {

            }


            @Override
            public void onMetadataAvailable(@NotNull Player player, @NotNull MetadataWrapper metadataWrapper) {

            }

            @Override
            public void onPlaybackHaltStateChanged(@NotNull Player player, boolean b, long l) {
                Log.i(TAG, "Playback halt state changed");

            }

            @Override
            public void onInactiveSession(@NotNull Player player, boolean b) {
                Log.i(TAG, "Inactive session");

                inactivityRunnable = closingRunnable;

                // Wait one minute before closing session
                handler.postDelayed(inactivityRunnable, 20000);
            }

            @Override
            public void onVolumeChanged(@NotNull Player player, @Range(from = 0L, to = 1L) float v) {
                Log.i(TAG, "Handle Volume changed from phone here: " + v);
            }

            @Override
            public void onPanicState(@NotNull Player player) {

            }

            @Override
            public void onStartedLoading(@NotNull Player player) {
                Log.i(TAG, "Started loading");

            }

            @Override
            public void onFinishedLoading(@NotNull Player player) {
                Log.i(TAG, "Finished loading");


            }
        };

        // Initialise Spotify Session
        executorService.submit(new SetupRunnable(credentialsFile, new SetupCallback() {
            @Override
            public void playerReady(@NotNull Player player, @NotNull String username) {
                Log.i(TAG, "Adding Event Listener for Player");
                player.addEventsListener(playerListener);
            }

            @Override
            public void notLoggedIn() {
            }

            @Override
            public void failedGettingReady(@NotNull Exception ex) {

            }
        }));

        // returns the status
        // of the program
        return START_STICKY;
    }

    @UiThread
    private interface SetupCallback {
        void playerReady(@NotNull Player player, @NotNull String username);

        void notLoggedIn();

        void failedGettingReady(@NotNull Exception ex);
    }

    private interface SimpleCallback {
        void done();
    }

    private static class SessionClosingRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        SessionClosingRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            LibrespotHolder.clear();

            handler.post(callback::done);
        }
    }
    private static class SetupRunnable implements Runnable {
        private final File credentialsFile;
        private final SetupCallback callback;
        private final Handler handler;

        SetupRunnable(@NotNull File credentialsFile, @NotNull SetupCallback callback) {
            this.credentialsFile = credentialsFile;
            this.callback = callback;
            this.handler = new Handler(Looper.getMainLooper());
        }

        @Override
        public void run() {
            Session session;
            if (LibrespotHolder.hasSession()) {
                session = LibrespotHolder.getSession();
                if (session == null) throw new IllegalStateException();
            } else if (credentialsFile.exists() && credentialsFile.canRead()) {
                try {
                    Session.Configuration conf = new Session.Configuration.Builder()
                            .setStoreCredentials(true)
                            .setStoredCredentialsFile(credentialsFile)
                            .setCacheEnabled(false)
                            .build();

                    Session.Builder builder = new Session.Builder(conf)
                            .setPreferredLocale(Locale.getDefault().getLanguage())
                            .setDeviceType(Connect.DeviceType.AUTOMOBILE)
                            .setDeviceId(null).setDeviceName("XShoreOne");

                    session = builder.stored(credentialsFile).create();
                    Log.i(TAG, "Logged in as: " + session.username());

                    LibrespotHolder.set(session);
                } catch (IOException |
                        GeneralSecurityException |
                        Session.SpotifyAuthenticationException |
                        MercuryClient.MercuryException ex) {
                    Log.e(TAG, "Session creation failed!", ex);
                    handler.post(() -> callback.failedGettingReady(ex));
                    return;
                }
            } else if(!credentialsFile.exists() || !credentialsFile.canRead()) {
                try {
                    Session.Configuration conf = new Session.Configuration.Builder()
                            .setStoreCredentials(true)
                            .setStoredCredentialsFile(credentialsFile)
                            .setCacheEnabled(false)
                            .build();

                    Session.Builder builder = new Session.Builder(conf)
                            .setPreferredLocale(Locale.getDefault().getLanguage())
                            .setDeviceType(Connect.DeviceType.AUTOMOBILE)
                            .setDeviceId(null).setDeviceName("XShoreOne");

                    session = builder.userPass("software@xshore.com", "1AmaKomkommer4Lif3").create();
                    Log.i(TAG, "Logged in as: " + session.username());

                    LibrespotHolder.set(session);
                } catch (IOException |
                         GeneralSecurityException |
                         Session.SpotifyAuthenticationException |
                         MercuryClient.MercuryException ex) {
                    Log.e(TAG, "Session creation failed!", ex);
                    handler.post(() -> callback.failedGettingReady(ex));
                    return;
                }
            }else {
                handler.post(callback::notLoggedIn);
                return;
            }

            Player player;
            if (LibrespotHolder.hasPlayer()) {
                player = LibrespotHolder.getPlayer();
                if (player == null) throw new IllegalStateException();
            } else {
                PlayerConfiguration configuration = new PlayerConfiguration.Builder()
                        .setOutput(PlayerConfiguration.AudioOutput.CUSTOM)
                        .setOutputClass(AndroidSinkOutput.class.getName())
                        .build();

                player = new Player(configuration, session);
                LibrespotHolder.set(player);
            }

            if (Build.VERSION.SDK_INT <= Build.VERSION_CODES.M) {
                while (!player.isReady()) {
                    try {
                        //noinspection BusyWait
                        Thread.sleep(100);
                    } catch (InterruptedException ex) {
                        return;
                    }
                }
            } else {
                try {
                    player.waitReady();
                } catch (InterruptedException ex) {
                    LibrespotHolder.clear();
                    return;
                }
            }

            handler.post(() -> callback.playerReady(player, session.username()));
        }
    }

    private static class PlayRunnable implements Runnable {
        private final String playUri;
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        PlayRunnable(@NotNull String playUri, @NotNull SimpleCallback callback) {
            this.playUri = playUri;
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.load(playUri, true, false);
            handler.post(callback::done);
        }
    }

    private static class ResumeRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        ResumeRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.play();
            handler.post(callback::done);
        }
    }

    private static class PauseRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        PauseRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.pause();
            handler.post(callback::done);
        }
    }

    private static class PrevRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        PrevRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.previous();
            handler.post(callback::done);
        }
    }

    private static class NextRunnable implements Runnable {
        private final SimpleCallback callback;
        private final Handler handler = new Handler(Looper.getMainLooper());

        NextRunnable(@NotNull SimpleCallback callback) {
            this.callback = callback;
        }

        @Override
        public void run() {
            Player player = LibrespotHolder.getPlayer();
            if (player == null) return;

            player.next();
            handler.post(callback::done);
        }
    }
}
