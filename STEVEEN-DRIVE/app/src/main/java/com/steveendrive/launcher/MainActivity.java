
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

    private LocationManager locationManager;
    private boolean locationStarted = false;

    private MediaPlayer mediaPlayer;
    private Uri currentAudioUri;
    private boolean musicPrepared = false;

    private final Handler clockHandler =
            new Handler(Looper.getMainLooper());

    private final Runnable clockRunnable = new Runnable() {
        @Override
        public void run() {
            updateClock();
            clockHandler.postDelayed(this, 1000);
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

        GradientDrawable speedBackground = new GradientDrawable();
        speedBackground.setColor(Color.TRANSPARENT);
        speedBackground.setCornerRadius(18);
        speedText.setBackground(speedBackground);

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

        // MUSICA INTEGRADA
        addHotspot(0.735f, 0.505f, 0.255f, 0.265f, this::toggleMusic);

        // CONFIGURACION Y TELEFONO
        addHotspot(0.000f, 0.000f, 0.060f, 0.085f, this::openSettings);
        addHotspot(0.065f, 0.000f, 0.070f, 0.085f, this::openPhone);

        // ORDEN CORRECTO: WIFI PRIMERO
        addHotspot(0.785f, 0.000f, 0.055f, 0.090f, this::openWiFi);

        // BLUETOOTH DESPUES DEL WIFI
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

        // RELOJ MOVIDO A LA IZQUIERDA DEL WIFI
        addAt(panel, 0.635f, 0.015f, 0.135f, 0.075f);
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
            showToast("No se pudieron abrir los ajustes Wi-Fi");
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

    // REPRODUCTOR DE MUSICA INTEGRADO

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
                showToast("Música en pausa");

            } else {

                if (mediaPlayer.getCurrentPosition()
                        >= mediaPlayer.getDuration()) {
                    mediaPlayer.seekTo(0);
                }

                mediaPlayer.start();
                showToast("Reproduciendo música");
            }

        } catch (Exception e) {
            showToast("No se pudo controlar la reproducción");
        }
    }

    private void startInlineAudio(Uri uri) {

        releaseMediaPlayer();

        currentAudioUri = uri;
        musicPrepared = false;

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
                player.start();
                showToast("Reproduciendo música en STEVEEN DRIVE");
            });

            mediaPlayer.setOnCompletionListener(player -> {
                showToast("Canción finalizada");
            });

            mediaPlayer.setOnErrorListener((player, what, extra) -> {
                musicPrepared = false;
                showToast("No se pudo reproducir este archivo");
                releaseMediaPlayer();
                return true;
            });

            mediaPlayer.prepareAsync();

            showToast("Cargando canción...");

        } catch (Exception e) {
            releaseMediaPlayer();
            showToast("Error al cargar la canción");
        }
    }

    private void releaseMediaPlayer() {

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

        if (locationManager != null) {
            requestLocationIfNeeded();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        clockHandler.removeCallbacks(clockRunnable);

        stopGPS();

        // La música puede continuar mientras se abre otra aplicación.
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();

        clockHandler.removeCallbacks(clockRunnable);

        stopGPS();

        releaseMediaPlayer();
    }
}
