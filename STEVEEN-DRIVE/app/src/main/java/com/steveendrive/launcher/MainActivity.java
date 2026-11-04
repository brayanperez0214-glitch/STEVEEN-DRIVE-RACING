
package com.steveendrive.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.media.AudioAttributes;
import android.media.MediaMetadataRetriever;
import android.media.MediaPlayer;
import android.net.Uri;
import android.os.Bundle;
import android.os.Handler;
import android.os.Looper;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.SeekBar;
import android.widget.TextView;
import android.widget.Toast;

import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.Date;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final int LOCATION_REQUEST = 410;
    private static final int AUDIO_REQUEST = 510;
    private static final int VIDEO_REQUEST = 511;

    private FrameLayout root;

    private TextView speedText;
    private TextView clockText;
    private TextView dateText;

    private TextView songTitle;
    private TextView songArtist;
    private TextView playButton;
    private TextView currentTimeText;
    private TextView durationText;
    private SeekBar musicSeekBar;

    private LocationManager locationManager;
    private boolean locationStarted = false;

    private MediaPlayer mediaPlayer;
    private Uri currentAudioUri;
    private boolean musicPrepared = false;
    private boolean userSeeking = false;

    private final Handler clockHandler =
            new Handler(Looper.getMainLooper());

    private final Handler musicHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000);
        }
    };

    private final Runnable musicProgressRunnable = new Runnable() {
        @Override
        public void run() {
            updateMusicProgress();

            if (musicPrepared && mediaPlayer != null) {
                musicHandler.postDelayed(this, 500);
            }
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON
        );

        hideSystemUI();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);
        setContentView(root);

        // IMAGEN PRINCIPAL

        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.dashboard);
        background.setScaleType(ImageView.ScaleType.FIT_XY);

        root.addView(
                background,
                new FrameLayout.LayoutParams(
                        FrameLayout.LayoutParams.MATCH_PARENT,
                        FrameLayout.LayoutParams.MATCH_PARENT
                )
        );

        // VELOCIMETRO GPS

        speedText = new TextView(this);
        speedText.setText("0");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(43);
        speedText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        speedText.setGravity(Gravity.CENTER);

        addAt(speedText, 0.155f, 0.372f, 0.075f, 0.105f);

        // RELOJ Y FECHA

        createClockPanel();

        // BARRA INFERIOR

        addHotspot(0.010f, 0.755f, 0.100f, 0.145f, this::showHome);
        addHotspot(0.118f, 0.755f, 0.100f, 0.145f, this::openAndroidAuto);
        addHotspot(0.224f, 0.755f, 0.100f, 0.145f, this::showHome);
        addHotspot(0.330f, 0.755f, 0.122f, 0.145f, this::openMusic);
        addHotspot(0.460f, 0.755f, 0.120f, 0.145f, this::openVideo);
        addHotspot(0.565f, 0.755f, 0.115f, 0.145f, this::openMiAuto);
        addHotspot(0.690f, 0.755f, 0.100f, 0.145f, this::showInstalledApps);
        addHotspot(0.795f, 0.755f, 0.100f, 0.145f,
                () -> openAppByName("xuper hydra"));
        addHotspot(0.900f, 0.755f, 0.095f, 0.145f, this::openYouTubeWeb);

        // MAPAS

        addHotspot(0.735f, 0.125f, 0.255f, 0.355f, this::openMaps);

        // REPRODUCTOR INTEGRADO

        createMusicPanel();

        // CONFIGURACION Y TELEFONO

        addHotspot(0.000f, 0.000f, 0.060f, 0.085f, this::openSettings);
        addHotspot(0.065f, 0.000f, 0.070f, 0.085f, this::openPhone);

        // WIFI: SENSOR DESPLAZADO A LA DERECHA

        addHotspot(0.835f, 0.000f, 0.055f, 0.110f, this::openWiFi);

        // BLUETOOTH: ACCESO INDEPENDIENTE

        addHotspot(0.945f, 0.000f, 0.055f, 0.090f, this::openBluetooth);

        // GPS

        locationManager =
                (LocationManager) getSystemService(LOCATION_SERVICE);

        updateClock();
        requestLocationIfNeeded();
    }

    // PANTALLA COMPLETA

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE
        );
    }

    // POSICIONAMIENTO

    private void addAt(
            View view,
            float x,
            float y,
            float width,
            float height
    ) {
        int screenWidth =
                getResources().getDisplayMetrics().widthPixels;

        int screenHeight =
                getResources().getDisplayMetrics().heightPixels;

        FrameLayout.LayoutParams params =
                new FrameLayout.LayoutParams(
                        (int) (width * screenWidth),
                        (int) (height * screenHeight)
                );

        params.leftMargin = (int) (x * screenWidth);
        params.topMargin = (int) (y * screenHeight);

        root.addView(view, params);
    }

    private void addHotspot(
            float x,
            float y,
            float width,
            float height,
            Runnable action
    ) {
        View hotspot = new View(this);
        hotspot.setBackgroundColor(Color.TRANSPARENT);

        hotspot.setOnClickListener(v -> {
            if (action != null) {
                action.run();
            }
        });

        addAt(hotspot, x, y, width, height);
    }

    // RELOJ Y FECHA

    private void createClockPanel() {

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER);
        panel.setBackgroundColor(Color.TRANSPARENT);

        clockText = new TextView(this);
        clockText.setTextColor(Color.WHITE);
        clockText.setTextSize(15);
        clockText.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        clockText.setGravity(Gravity.CENTER);
        clockText.setSingleLine(true);

        dateText = new TextView(this);
        dateText.setTextColor(Color.WHITE);
        dateText.setTextSize(8);
        dateText.setGravity(Gravity.CENTER);
        dateText.setSingleLine(true);

        panel.addView(
                clockText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        panel.addView(
                dateText,
                new LinearLayout.LayoutParams(
                        LinearLayout.LayoutParams.MATCH_PARENT,
                        0,
                        1
                )
        );

        // HORA DESPLAZADA A LA DERECHA

        addAt(panel, 0.700f, 0.008f, 0.105f, 0.080f);
    }

    private void updateClock() {

        if (clockText == null || dateText == null) {
            return;
        }

        Date now = new Date();

        Locale locale = new Locale("es", "CO");

        SimpleDateFormat timeFormat =
                new SimpleDateFormat("h:mm a", locale);

        SimpleDateFormat dateFormat =
                new SimpleDateFormat("EEE d MMM", locale);

        clockText.setText(timeFormat.format(now));

        String date = dateFormat.format(now);

        if (!date.isEmpty()) {
            date = date.substring(0, 1).toUpperCase(locale)
                    + date.substring(1);
        }

        dateText.setText(date);
    }

    // WIFI

    private void openWiFi() {

        try {

            Intent intent =
                    new Intent(Settings.ACTION_WIFI_SETTINGS);

            startActivity(intent);

        } catch (Exception e) {

            try {

                Intent fallback =
                        new Intent(Settings.ACTION_WIRELESS_SETTINGS);

                startActivity(fallback);

            } catch (Exception ex) {

                showToast("No se pudieron abrir los ajustes Wi-Fi");
            }
        }
    }

    // BLUETOOTH

    private void openBluetooth() {

        try {

            Intent intent =
                    new Intent(Settings.ACTION_BLUETOOTH_SETTINGS);

            startActivity(intent);

        } catch (Exception e) {

            showToast("No se pudieron abrir los ajustes Bluetooth");
        }
    }

    // TELEFONO Y CONFIGURACION

    private void openPhone() {
        try {
            Intent intent = new Intent(Intent.ACTION_DIAL);
            startActivity(intent);
        } catch (Exception e) {
            showToast("No se encontró la aplicación telefónica");
        }
    }

    private void openSettings() {
        try {
            Intent intent =
                    new Intent(Settings.ACTION_SETTINGS);

            startActivity(intent);
        } catch (Exception e) {
            showToast("No se pudieron abrir los ajustes");
        }
    }

    // ANDROID AUTO

    private void openAndroidAuto() {

        String packageName =
                "com.google.android.projection.gearhead";

        try {
            Intent intent = getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            if (intent != null) {
                startActivity(intent);
            } else {
                showToast("Android Auto no está instalado");
            }

        } catch (Exception e) {
            showToast("No se pudo abrir Android Auto");
        }
    }

    // YOUTUBE WEB

    private void openYouTubeWeb() {
        try {
            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("https://www.youtube.com")
            );

            startActivity(intent);

        } catch (Exception e) {
            showToast("No se pudo abrir YouTube");
        }
    }

    // XUPER HYDRA Y APLICACIONES

    private void openAppByName(String searchName) {

        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps =
                pm.queryIntentActivities(intent, 0);

        for (ResolveInfo app : apps) {

            String label = app.loadLabel(pm).toString();

            if (label.toLowerCase(Locale.ROOT)
                    .contains(searchName.toLowerCase(Locale.ROOT))) {

                Intent launchIntent =
                        pm.getLaunchIntentForPackage(
                                app.activityInfo.packageName
                        );

                if (launchIntent != null) {
                    startActivity(launchIntent);
                    return;
                }
            }
        }

        showToast("No se encontró: " + searchName);
    }

    private void openMiAuto() {
        openAppByName("mi auto");
    }

    private void showInstalledApps() {

        PackageManager pm = getPackageManager();

        Intent intent = new Intent(Intent.ACTION_MAIN);
        intent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> results =
                pm.queryIntentActivities(intent, 0);

        ArrayList<ResolveInfo> apps = new ArrayList<>();

        for (ResolveInfo info : results) {
            if (!info.activityInfo.packageName.equals(getPackageName())) {
                apps.add(info);
            }
        }

        Collections.sort(apps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString()
                        .compareToIgnoreCase(b.loadLabel(pm).toString());
            }
        });

        GridView grid = new GridView(this);
        grid.setNumColumns(4);
        grid.setPadding(12, 12, 12, 12);
        grid.setVerticalSpacing(12);
        grid.setHorizontalSpacing(12);

        grid.setAdapter(new BaseAdapter() {

            @Override
            public int getCount() {
                return apps.size();
            }

            @Override
            public Object getItem(int position) {
                return apps.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(
                    int position,
                    View convertView,
                    android.view.ViewGroup parent
            ) {

                LinearLayout item =
                        new LinearLayout(MainActivity.this);

                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(5, 10, 5, 10);

                ImageView icon = new ImageView(MainActivity.this);
                icon.setImageDrawable(apps.get(position).loadIcon(pm));

                LinearLayout.LayoutParams iconParams =
                        new LinearLayout.LayoutParams(65, 65);

                item.addView(icon, iconParams);

                TextView name = new TextView(MainActivity.this);
                name.setText(apps.get(position).loadLabel(pm));
                name.setTextColor(Color.WHITE);
                name.setTextSize(11);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);

                item.addView(name);

                return item;
            }
        });

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("APLICACIONES")
                .setView(grid)
                .setNegativeButton("CERRAR", null)
                .create();

        grid.setOnItemClickListener((parent, view, position, id) -> {

            ResolveInfo selected = apps.get(position);

            Intent launchIntent = pm.getLaunchIntentForPackage(
                    selected.activityInfo.packageName
            );

            if (launchIntent != null) {
                startActivity(launchIntent);
                dialog.dismiss();
            }
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (
                            getResources().getDisplayMetrics().widthPixels
                                    * 0.85
                    ),
                    (int) (
                            getResources().getDisplayMetrics().heightPixels
                                    * 0.80
                    )
            );
        }
    }

    // INICIO

    private void showHome() {
        showToast("STEVEEN DRIVE");
    }

    // PANEL VISUAL DE MUSICA

    private void createMusicPanel() {

        LinearLayout panel = new LinearLayout(this);
        panel.setOrientation(LinearLayout.VERTICAL);
        panel.setGravity(Gravity.CENTER_VERTICAL);
        panel.setPadding(9, 5, 9, 5);

        // FONDO NEGRO COMPLETAMENTE OPACO

        GradientDrawable background = new GradientDrawable();
        background.setColor(Color.BLACK);
        background.setAlpha(255);
        background.setCornerRadius(12);
        background.setStroke(2, Color.rgb(210, 35, 35));

        panel.setBackground(background);

        TextView heading = new TextView(this);
        heading.setText("REPRODUCTOR");
        heading.setTextColor(Color.rgb(255, 65, 65));
        heading.setTextSize(10);
        heading.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        heading.setGravity(Gravity.CENTER);

        panel.addView(heading);

        songTitle = new TextView(this);
        songTitle.setText("Selecciona una canción");
        songTitle.setTextColor(Color.WHITE);
        songTitle.setTextSize(11);
        songTitle.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        songTitle.setSingleLine(true);
        songTitle.setEllipsize(android.text.TextUtils.TruncateAt.END);

        panel.addView(songTitle);

        songArtist = new TextView(this);
        songArtist.setText("Ninguna canción");
        songArtist.setTextColor(Color.LTGRAY);
        songArtist.setTextSize(9);
        songArtist.setSingleLine(true);
        songArtist.setEllipsize(android.text.TextUtils.TruncateAt.END);

        panel.addView(songArtist);

        musicSeekBar = new SeekBar(this);
        musicSeekBar.setMax(1000);
        musicSeekBar.setProgress(0);
        musicSeekBar.setPadding(0, 0, 0, 0);

        musicSeekBar.setOnSeekBarChangeListener(
                new SeekBar.OnSeekBarChangeListener() {

                    @Override
                    public void onProgressChanged(
                            SeekBar seekBar,
                            int progress,
                            boolean fromUser
                    ) {
                        if (fromUser && musicPrepared
                                && mediaPlayer != null) {

                            int duration = mediaPlayer.getDuration();

                            currentTimeText.setText(
                                    formatTime(duration * progress / 1000)
                            );
                        }
                    }

                    @Override
                    public void onStartTrackingTouch(SeekBar seekBar) {
                        userSeeking = true;
                    }

                    @Override
                    public void onStopTrackingTouch(SeekBar seekBar) {

                        if (musicPrepared && mediaPlayer != null) {

                            int position =
                                    mediaPlayer.getDuration()
                                            * seekBar.getProgress() / 1000;

                            mediaPlayer.seekTo(position);
                        }

                        userSeeking = false;
                    }
                }
        );

        panel.addView(musicSeekBar);

        LinearLayout controls = new LinearLayout(this);
        controls.setGravity(Gravity.CENTER_VERTICAL);
        controls.setOrientation(LinearLayout.HORIZONTAL);

        currentTimeText = new TextView(this);
        currentTimeText.setText("00:00");
        currentTimeText.setTextColor(Color.WHITE);
        currentTimeText.setTextSize(8);

        controls.addView(
                currentTimeText,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        playButton = createMusicButton("▶", 36);
        playButton.setOnClickListener(v -> toggleMusic());

        controls.addView(playButton);

        TextView selectButton = createMusicButton("+", 32);
        selectButton.setOnClickListener(v -> openMusic());

        controls.addView(selectButton);

        durationText = new TextView(this);
        durationText.setText("00:00");
        durationText.setTextColor(Color.WHITE);
        durationText.setTextSize(8);
        durationText.setGravity(Gravity.RIGHT);

        controls.addView(
                durationText,
                new LinearLayout.LayoutParams(
                        0,
                        LinearLayout.LayoutParams.WRAP_CONTENT,
                        1
                )
        );

        panel.addView(controls);

        // ZONA DE MUSICA DEBAJO DEL NAVEGADOR

        addAt(panel, 0.735f, 0.505f, 0.255f, 0.265f);
    }

    private TextView createMusicButton(String symbol, int size) {

        TextView button = new TextView(this);

        button.setText(symbol);
        button.setTextColor(Color.WHITE);
        button.setTextSize(17);
        button.setTypeface(Typeface.DEFAULT, Typeface.BOLD);
        button.setGravity(Gravity.CENTER);

        button.setBackgroundColor(Color.TRANSPARENT);

        button.setLayoutParams(
                new LinearLayout.LayoutParams(
                        size,
                        34
                )
        );

        return button;
    }

    private void updateMusicInfo(Uri uri) {

        String title = "Canción seleccionada";
        String artist = "Artista desconocido";

        MediaMetadataRetriever retriever =
                new MediaMetadataRetriever();

        try {

            retriever.setDataSource(this, uri);

            String metadataTitle =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_TITLE
                    );

            String metadataArtist =
                    retriever.extractMetadata(
                            MediaMetadataRetriever.METADATA_KEY_ARTIST
                    );

            if (metadataTitle != null && !metadataTitle.trim().isEmpty()) {
                title = metadataTitle;
            }

            if (metadataArtist != null && !metadataArtist.trim().isEmpty()) {
                artist = metadataArtist;
            }

        } catch (Exception ignored) {

        } finally {

            try {
                retriever.release();
            } catch (Exception ignored) {
            }
        }

        songTitle.setText(title);
        songArtist.setText(artist);
    }

    // SELECCION DE MUSICA

    private void openMusic() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("audio/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
        intent.addFlags(Intent.FLAG_GRANT_PERSISTABLE_URI_PERMISSION);

        try {
            startActivityForResult(intent, AUDIO_REQUEST);
        } catch (Exception e) {
            showToast("No se pudo abrir el selector de música");
        }
    }

    // REPRODUCIR Y PAUSAR

    private void toggleMusic() {

        if (mediaPlayer == null || currentAudioUri == null) {
            openMusic();
            return;
        }

        if (!musicPrepared) {
            showToast("La canción se está preparando");
            return;
        }

        try {

            if (mediaPlayer.isPlaying()) {

                mediaPlayer.pause();
                playButton.setText("▶");
                showToast("Música en pausa");

            } else {

                if (mediaPlayer.getCurrentPosition()
                        >= mediaPlayer.getDuration()) {
                    mediaPlayer.seekTo(0);
                }

                mediaPlayer.start();
                playButton.setText("Ⅱ");

                musicHandler.removeCallbacks(musicProgressRunnable);
                musicHandler.post(musicProgressRunnable);

                showToast("Reproduciendo música");
            }

        } catch (Exception e) {
            showToast("No se pudo controlar la reproducción");
        }
    }

    // CARGAR AUDIO

    private void startInlineAudio(Uri uri) {

        releaseMediaPlayer();

        currentAudioUri = uri;
        musicPrepared = false;

        musicSeekBar.setProgress(0);
        currentTimeText.setText("00:00");
        durationText.setText("00:00");
        playButton.setText("…");

        updateMusicInfo(uri);

        try {

            try {
                getContentResolver().takePersistableUriPermission(
                        uri,
                        Intent.FLAG_GRANT_READ_URI_PERMISSION
                );
            } catch (Exception ignored) {
            }

            mediaPlayer = new MediaPlayer();

            mediaPlayer.setAudioAttributes(
                    new AudioAttributes.Builder()
                            .setUsage(AudioAttributes.USAGE_MEDIA)
                            .setContentType(
                                    AudioAttributes.CONTENT_TYPE_MUSIC
                            )
                            .build()
            );

            mediaPlayer.setDataSource(this, uri);

            mediaPlayer.setOnPreparedListener(player -> {

                musicPrepared = true;

                int duration = player.getDuration();

                musicSeekBar.setMax(1000);
                durationText.setText(formatTime(duration));

                player.start();

                playButton.setText("Ⅱ");

                musicHandler.removeCallbacks(musicProgressRunnable);
                musicHandler.post(musicProgressRunnable);

                showToast("Reproduciendo música en STEVEEN DRIVE");
            });

            mediaPlayer.setOnCompletionListener(player -> {

                playButton.setText("▶");
                musicSeekBar.setProgress(0);
                currentTimeText.setText("00:00");

                musicHandler.removeCallbacks(musicProgressRunnable);

                showToast("Canción finalizada");
            });

            mediaPlayer.setOnErrorListener((player, what, extra) -> {

                musicPrepared = false;
                playButton.setText("▶");

                showToast("No se pudo reproducir este archivo");

                releaseMediaPlayer();

                return true;
            });

            mediaPlayer.prepareAsync();

            showToast("Cargando canción...");

        } catch (Exception e) {

            releaseMediaPlayer();

            songTitle.setText("Error al cargar canción");
            songArtist.setText("Selecciona otro archivo");

            showToast("Error al cargar la canción");
        }
    }

    // ACTUALIZAR PROGRESO

    private void updateMusicProgress() {

        if (!musicPrepared || mediaPlayer == null) {
            return;
        }

        try {

            int duration = mediaPlayer.getDuration();
            int position = mediaPlayer.getCurrentPosition();

            if (!userSeeking && duration > 0) {

                int progress = (int) (
                        position * 1000L / duration
                );

                musicSeekBar.setProgress(progress);

                currentTimeText.setText(formatTime(position));
            }

        } catch (Exception ignored) {
        }
    }

    private String formatTime(int milliseconds) {

        int totalSeconds = Math.max(0, milliseconds / 1000);

        int minutes = totalSeconds / 60;
        int seconds = totalSeconds % 60;

        return String.format(
                Locale.getDefault(),
                "%02d:%02d",
                minutes,
                seconds
        );
    }

    // LIBERAR REPRODUCTOR

    private void releaseMediaPlayer() {

        musicHandler.removeCallbacks(musicProgressRunnable);

        musicPrepared = false;

        if (mediaPlayer != null) {

            try {
                mediaPlayer.stop();
            } catch (Exception ignored) {
            }

            try {
                mediaPlayer.release();
            } catch (Exception ignored) {
            }

            mediaPlayer = null;
        }

        if (playButton != null) {
            playButton.setText("▶");
        }
    }

    // VIDEO

    private void openVideo() {

        Intent intent =
                new Intent(Intent.ACTION_OPEN_DOCUMENT);

        intent.setType("video/*");
        intent.addCategory(Intent.CATEGORY_OPENABLE);
        intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

        try {
            startActivityForResult(intent, VIDEO_REQUEST);
        } catch (Exception e) {
            showToast("No se pudo abrir el selector de videos");
        }
    }

    private void playSelectedVideo(Uri uri) {

        try {

            Intent intent = new Intent(Intent.ACTION_VIEW);
            intent.setDataAndType(uri, "video/*");
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(intent);

        } catch (ActivityNotFoundException e) {
            showToast("No hay reproductor de video instalado");
        } catch (Exception e) {
            showToast("No se pudo reproducir el video");
        }
    }

    // MAPAS

    private void openMaps() {

        try {

            Intent intent = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q=")
            );

            intent.setPackage("com.google.android.apps.maps");

            startActivity(intent);

        } catch (Exception e) {

            try {

                Intent fallback = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q=")
                );

                startActivity(fallback);

            } catch (Exception ex) {
                showToast("No se encontró una aplicación de mapas");
            }
        }
    }

    // RESULTADOS DE SELECTORES

    @Override
    protected void onActivityResult(
            int requestCode,
            int resultCode,
            Intent data
    ) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK
                || data == null
                || data.getData() == null) {
            return;
        }

        Uri uri = data.getData();

        if (requestCode == AUDIO_REQUEST) {
            startInlineAudio(uri);
        }

        if (requestCode == VIDEO_REQUEST) {
            playSelectedVideo(uri);
        }
    }

    // GPS Y VELOCIMETRO

    private void requestLocationIfNeeded() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION
        ) != PackageManager.PERMISSION_GRANTED) {

            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST
            );

            return;
        }

        startGPS();
    }

    private void startGPS() {

        if (locationManager == null || locationStarted) {
            return;
        }

        try {

            if (checkSelfPermission(
                    Manifest.permission.ACCESS_FINE_LOCATION
            ) != PackageManager.PERMISSION_GRANTED) {
                return;
            }

            if (!locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER
            )) {
                showToast("Activa la ubicación GPS del vehículo");
                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500,
                    0,
                    this,
                    Looper.getMainLooper()
            );

            Location lastLocation =
                    locationManager.getLastKnownLocation(
                            LocationManager.GPS_PROVIDER
                    );

            if (lastLocation != null) {
                updateSpeed(lastLocation);
            }

            locationStarted = true;

        } catch (SecurityException e) {
            showToast("Permiso GPS no disponible");
        } catch (Exception e) {
            showToast("No se pudo iniciar el GPS");
        }
    }

    private void stopGPS() {

        if (locationManager != null && locationStarted) {

            try {
                locationManager.removeUpdates(this);
            } catch (Exception ignored) {
            }

            locationStarted = false;
        }
    }

    private void updateSpeed(Location location) {

        if (location == null || speedText == null) {
            return;
        }

        float speedKmh = location.getSpeed() * 3.6f;
        int speed = Math.round(speedKmh);

        speedText.setText(String.valueOf(Math.max(0, speed)));
    }

    @Override
    public void onLocationChanged(Location location) {
        updateSpeed(location);
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {

        if (speedText != null) {
            speedText.setText("0");
        }
    }

    @Override
    public void onStatusChanged(
            String provider,
            int status,
            Bundle extras
    ) {
    }

    // PERMISOS

    @Override
    public void onRequestPermissionsResult(
            int requestCode,
            String[] permissions,
            int[] grantResults
    ) {

        super.onRequestPermissionsResult(
                requestCode,
                permissions,
                grantResults
        );

        if (requestCode == LOCATION_REQUEST) {

            if (grantResults.length > 0
                    && grantResults[0]
                    == PackageManager.PERMISSION_GRANTED) {

                startGPS();

            } else {

                showToast(
                        "Se necesita permiso de ubicación para el velocímetro"
                );
            }
        }
    }

    // MENSAJES

    private void showToast(String message) {

        Toast.makeText(
                this,
                message,
                Toast.LENGTH_SHORT
        ).show();
    }

    // CICLO DE VIDA

    @Override
    protected void onResume() {

        super.onResume();

        hideSystemUI();

        clockHandler.removeCallbacks(clockRunnable);
        clockHandler.post(clockRunnable);

        if (musicPrepared && mediaPlayer != null) {

            musicHandler.removeCallbacks(musicProgressRunnable);
            musicHandler.post(musicProgressRunnable);
        }

        if (locationManager != null) {
            requestLocationIfNeeded();
        }
    }

    @Override
    protected void onPause() {

        super.onPause();

        clockHandler.removeCallbacks(clockRunnable);

        musicHandler.removeCallbacks(musicProgressRunnable);

        stopGPS();

        // LA MUSICA PUEDE CONTINUAR MIENTRAS SE ABRE OTRA APLICACION.
    }

    @Override
    protected void onDestroy() {

        super.onDestroy();

        clockHandler.removeCallbacks(clockRunnable);
        musicHandler.removeCallbacks(musicProgressRunnable);

        stopGPS();

        releaseMediaPlayer();
    }
}
