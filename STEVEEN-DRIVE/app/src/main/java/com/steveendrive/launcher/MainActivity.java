
package com.steveendrive.launcher;

import android.Manifest;
import android.app.Activity;
import android.app.AlertDialog;
import android.content.ActivityNotFoundException;
import android.content.ComponentName;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.content.pm.ResolveInfo;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Bundle;
import android.view.Gravity;
import android.view.View;
import android.view.ViewGroup;
import android.view.WindowManager;
import android.widget.BaseAdapter;
import android.widget.FrameLayout;
import android.widget.GridView;
import android.widget.ImageView;
import android.widget.LinearLayout;
import android.widget.TextView;
import android.widget.Toast;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.List;
import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {

    private static final int LOCATION_REQUEST = 410;
    private static final int AUDIO_REQUEST = 510;
    private static final int VIDEO_REQUEST = 511;

    private FrameLayout root;
    private TextView speedText;
    private LocationManager locationManager;
    private boolean locationStarted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);

        getWindow().addFlags(
                WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);

        hideSystemUI();

        root = new FrameLayout(this);
        root.setBackgroundColor(Color.BLACK);

        ImageView background = new ImageView(this);
        background.setImageResource(R.drawable.dashboard);
        background.setScaleType(ImageView.ScaleType.FIT_XY);

        root.addView(background, new FrameLayout.LayoutParams(
                FrameLayout.LayoutParams.MATCH_PARENT,
                FrameLayout.LayoutParams.MATCH_PARENT));

        setContentView(root);

        speedText = new TextView(this);
        speedText.setText("0");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(43);
        speedText.setTypeface(Typeface.create(
                "sans-serif-condensed", Typeface.BOLD));
        speedText.setGravity(Gravity.CENTER);
        speedText.setShadowLayer(5, 0, 0, Color.BLACK);
        speedText.setBackground(roundBackground(
                Color.rgb(5, 5, 7), 100));

        addAt(speedText, 0.155f, 0.372f, 0.075f, 0.105f);

        // BARRA INFERIOR

        addHotspot(0.00f, 0.80f, 0.112f, 0.19f,
                v -> showHome());

        addHotspot(0.115f, 0.80f, 0.105f, 0.19f,
                v -> openMaps());

        addHotspot(0.222f, 0.80f, 0.108f, 0.19f,
                v -> openBluetooth());

        addHotspot(0.330f, 0.80f, 0.125f, 0.19f,
                v -> openPackage("com.whatsapp"));

        addHotspot(0.560f, 0.80f, 0.120f, 0.19f,
                v -> openMusic());

        addHotspot(0.685f, 0.80f, 0.105f, 0.19f,
                v -> openVideo());

        // AUTO ABRE MI AUTO

        addHotspot(0.792f, 0.80f, 0.105f, 0.19f,
                v -> openMiAuto());

        // APLICACIONES

        addHotspot(0.900f, 0.80f, 0.100f, 0.19f,
                v -> showInstalledApps());

        // PANEL DERECHO

        addHotspot(0.735f, 0.125f, 0.255f, 0.355f,
                v -> openMaps());

        addHotspot(0.735f, 0.505f, 0.255f, 0.265f,
                v -> openMusic());

        // ACCESOS SUPERIORES

        addHotspot(0.000f, 0.000f, 0.060f, 0.105f,
                v -> openSettings());

        addHotspot(0.065f, 0.000f, 0.070f, 0.105f,
                v -> openDialer());

        locationManager = (LocationManager)
                getSystemService(LOCATION_SERVICE);

        requestLocationIfNeeded();
    }

    private void hideSystemUI() {
        getWindow().getDecorView().setSystemUiVisibility(
                View.SYSTEM_UI_FLAG_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY
                        | View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN
                        | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION
                        | View.SYSTEM_UI_FLAG_LAYOUT_STABLE);
    }

    private GradientDrawable roundBackground(int color, int radius) {
        GradientDrawable d = new GradientDrawable();
        d.setColor(color);
        d.setCornerRadius(radius);
        d.setStroke(1, Color.TRANSPARENT);
        return d;
    }

    private void addAt(View view, float centerX, float centerY,
                       float width, float height) {

        int screenWidth = getResources()
                .getDisplayMetrics().widthPixels;

        int screenHeight = getResources()
                .getDisplayMetrics().heightPixels;

        FrameLayout.LayoutParams p =
                new FrameLayout.LayoutParams(
                        Math.max(1, (int) (screenWidth * width)),
                        Math.max(1, (int) (screenHeight * height)));

        p.leftMargin = (int) (screenWidth * centerX) - p.width / 2;
        p.topMargin = (int) (screenHeight * centerY) - p.height / 2;

        root.addView(view, p);
    }

    private void addHotspot(float x, float y, float w, float h,
                            View.OnClickListener listener) {

        View v = new View(this);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setOnClickListener(listener);

        FrameLayout.LayoutParams p =
                new FrameLayout.LayoutParams(
                        Math.max(1, (int) (
                                getResources().getDisplayMetrics().widthPixels * w)),
                        Math.max(1, (int) (
                                getResources().getDisplayMetrics().heightPixels * h)));

        p.leftMargin = (int) (
                getResources().getDisplayMetrics().widthPixels * x);

        p.topMargin = (int) (
                getResources().getDisplayMetrics().heightPixels * y);

        root.addView(v, p);
    }

    // APLICACIONES EN CUADRICULA CON ICONOS

    private void showInstalledApps() {

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        PackageManager pm = getPackageManager();

        List<ResolveInfo> apps =
                pm.queryIntentActivities(launcherIntent, 0);

        List<ResolveInfo> availableApps = new ArrayList<>();

        for (ResolveInfo app : apps) {
            if (app.activityInfo == null) {
                continue;
            }

            if (app.activityInfo.packageName.equals(getPackageName())) {
                continue;
            }

            availableApps.add(app);
        }

        Collections.sort(availableApps, new Comparator<ResolveInfo>() {
            @Override
            public int compare(ResolveInfo a, ResolveInfo b) {
                return a.loadLabel(pm).toString()
                        .compareToIgnoreCase(
                                b.loadLabel(pm).toString());
            }
        });

        if (availableApps.isEmpty()) {
            Toast.makeText(this,
                    "No se encontraron aplicaciones",
                    Toast.LENGTH_SHORT).show();
            return;
        }

        GridView grid = new GridView(this);
        grid.setNumColumns(5);
        grid.setColumnWidth(dp(100));
        grid.setVerticalSpacing(dp(12));
        grid.setHorizontalSpacing(dp(8));
        grid.setStretchMode(GridView.STRETCH_COLUMN_WIDTH);
        grid.setPadding(dp(12), dp(12), dp(12), dp(12));
        grid.setBackgroundColor(Color.rgb(15, 15, 18));

        BaseAdapter adapter = new BaseAdapter() {

            @Override
            public int getCount() {
                return availableApps.size();
            }

            @Override
            public Object getItem(int position) {
                return availableApps.get(position);
            }

            @Override
            public long getItemId(int position) {
                return position;
            }

            @Override
            public View getView(int position, View convertView,
                                ViewGroup parent) {

                ResolveInfo app = availableApps.get(position);

                LinearLayout item = new LinearLayout(MainActivity.this);
                item.setOrientation(LinearLayout.VERTICAL);
                item.setGravity(Gravity.CENTER);
                item.setPadding(dp(5), dp(8), dp(5), dp(8));

                ImageView icon = new ImageView(MainActivity.this);
                icon.setImageDrawable(app.loadIcon(pm));
                icon.setScaleType(ImageView.ScaleType.FIT_CENTER);

                LinearLayout.LayoutParams iconParams =
                        new LinearLayout.LayoutParams(dp(54), dp(54));

                item.addView(icon, iconParams);

                TextView name = new TextView(MainActivity.this);
                name.setText(app.loadLabel(pm).toString());
                name.setTextColor(Color.WHITE);
                name.setTextSize(12);
                name.setGravity(Gravity.CENTER);
                name.setMaxLines(2);
                name.setPadding(0, dp(5), 0, 0);

                item.addView(name, new LinearLayout.LayoutParams(
                        ViewGroup.LayoutParams.MATCH_PARENT,
                        ViewGroup.LayoutParams.WRAP_CONTENT));

                return item;
            }
        };

        grid.setAdapter(adapter);

        AlertDialog dialog = new AlertDialog.Builder(this)
                .setTitle("STEVEEN DRIVE - APLICACIONES")
                .setView(grid)
                .setNegativeButton("CERRAR", null)
                .create();

        grid.setOnItemClickListener((parent, view, position, id) -> {
            ResolveInfo selected = availableApps.get(position);
            dialog.dismiss();
            launchApp(selected);
        });

        dialog.show();

        if (dialog.getWindow() != null) {
            dialog.getWindow().setLayout(
                    (int) (getResources().getDisplayMetrics().widthPixels * 0.90),
                    (int) (getResources().getDisplayMetrics().heightPixels * 0.85));
        }
    }

    private int dp(int value) {
        return (int) (value * getResources()
                .getDisplayMetrics().density + 0.5f);
    }

    private void launchApp(ResolveInfo selected) {
        try {
            Intent launch = new Intent(Intent.ACTION_MAIN);
            launch.addCategory(Intent.CATEGORY_LAUNCHER);

            launch.setComponent(new ComponentName(
                    selected.activityInfo.packageName,
                    selected.activityInfo.name));

            launch.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(launch);

        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir la aplicación",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // ABRIR MI AUTO

    private void openMiAuto() {

        Intent launcherIntent = new Intent(Intent.ACTION_MAIN);
        launcherIntent.addCategory(Intent.CATEGORY_LAUNCHER);

        List<ResolveInfo> apps =
                getPackageManager().queryIntentActivities(
                        launcherIntent, 0);

        ResolveInfo found = null;

        for (ResolveInfo app : apps) {
            if (app.activityInfo == null) {
                continue;
            }

            String name = app.loadLabel(getPackageManager())
                    .toString().trim().toLowerCase(Locale.ROOT);

            if (name.equals("mi auto")) {
                found = app;
                break;
            }

            if (name.contains("mi auto")) {
                found = app;
            }
        }

        if (found != null) {
            launchApp(found);
        } else {
            Toast.makeText(this,
                    "No se encontró la aplicación Mi Auto",
                    Toast.LENGTH_LONG).show();
        }
    }

    // MUSICA

    private void openMusic() {
        openMediaPicker("audio/*", AUDIO_REQUEST,
                "Selecciona una canción");
    }

    // VIDEO

    private void openVideo() {
        openMediaPicker("video/*", VIDEO_REQUEST,
                "Selecciona un video");
    }

    private void openMediaPicker(String type, int requestCode,
                                 String title) {

        try {
            Intent picker = new Intent(Intent.ACTION_GET_CONTENT);
            picker.setType(type);
            picker.addCategory(Intent.CATEGORY_OPENABLE);

            startActivityForResult(
                    Intent.createChooser(picker, title),
                    requestCode);

        } catch (ActivityNotFoundException e) {

            try {
                Intent picker = new Intent(Intent.ACTION_OPEN_DOCUMENT);
                picker.setType(type);
                picker.addCategory(Intent.CATEGORY_OPENABLE);

                startActivityForResult(picker, requestCode);

            } catch (Exception ex) {
                Toast.makeText(this,
                        "No hay explorador compatible para este archivo",
                        Toast.LENGTH_LONG).show();
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir el explorador de archivos",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode,
                                    Intent data) {

        super.onActivityResult(requestCode, resultCode, data);

        if (resultCode != RESULT_OK || data == null
                || data.getData() == null) {
            return;
        }

        if (requestCode == AUDIO_REQUEST) {
            playSelectedMedia(data.getData(), "audio/*");
        } else if (requestCode == VIDEO_REQUEST) {
            playSelectedMedia(data.getData(), "video/*");
        }
    }

    private void playSelectedMedia(Uri uri, String fallbackType) {

        try {
            String mime = getContentResolver().getType(uri);

            if (mime == null) {
                mime = fallbackType;
            }

            Intent play = new Intent(Intent.ACTION_VIEW);
            play.setDataAndType(uri, mime);
            play.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);

            startActivity(Intent.createChooser(
                    play, "Abrir con reproductor"));

        } catch (ActivityNotFoundException e) {
            Toast.makeText(this,
                    "No hay un reproductor compatible instalado",
                    Toast.LENGTH_LONG).show();

        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir el archivo",
                    Toast.LENGTH_SHORT).show();
        }
    }

    // MAPAS

    private void openMaps() {

        try {
            Intent i = new Intent(
                    Intent.ACTION_VIEW,
                    Uri.parse("google.navigation:q="));

            i.setPackage("com.google.android.apps.maps");
            startActivity(i);

        } catch (Exception e) {
            try {
                Intent geo = new Intent(
                        Intent.ACTION_VIEW,
                        Uri.parse("geo:0,0?q="));

                startActivity(Intent.createChooser(
                        geo, "Seleccionar navegación"));

            } catch (Exception ignored) {
                openPackage("com.google.android.apps.maps");
            }
        }
    }

    // BLUETOOTH

    private void openBluetooth() {
        try {
            startActivity(new Intent(
                    android.provider.Settings.ACTION_BLUETOOTH_SETTINGS));
        } catch (Exception e) {
            openSettings();
        }
    }

    // OTRAS APLICACIONES

    private void openPackage(String packageName) {

        try {
            Intent i = getPackageManager()
                    .getLaunchIntentForPackage(packageName);

            if (i != null) {
                startActivity(i);
            } else {
                Toast.makeText(this,
                        "La aplicación no está instalada",
                        Toast.LENGTH_SHORT).show();
            }

        } catch (Exception e) {
            Toast.makeText(this,
                    "No se pudo abrir la aplicación",
                    Toast.LENGTH_SHORT).show();
        }
    }

    private void showHome() {
        hideSystemUI();
        Toast.makeText(this,
                "STEVEEN DRIVE - Inicio",
                Toast.LENGTH_SHORT).show();
    }

    private void openSettings() {
        try {
            startActivity(new Intent(
                    android.provider.Settings.ACTION_SETTINGS));
        } catch (Exception e) {
            showHome();
        }
    }

    private void openDialer() {
        try {
            startActivity(new Intent(Intent.ACTION_DIAL));
        } catch (Exception e) {
            showInstalledApps();
        }
    }

    // GPS Y VELOCIMETRO

    private void requestLocationIfNeeded() {

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {

            startGps();

        } else {
            requestPermissions(
                    new String[]{
                            Manifest.permission.ACCESS_FINE_LOCATION,
                            Manifest.permission.ACCESS_COARSE_LOCATION
                    },
                    LOCATION_REQUEST);
        }
    }

    private void startGps() {

        if (locationStarted || locationManager == null) {
            return;
        }

        try {
            if (!locationManager.isProviderEnabled(
                    LocationManager.GPS_PROVIDER)) {

                Toast.makeText(this,
                        "Activa el GPS para mostrar la velocidad real",
                        Toast.LENGTH_LONG).show();
                return;
            }

            locationManager.requestLocationUpdates(
                    LocationManager.GPS_PROVIDER,
                    500,
                    0,
                    this);

            locationStarted = true;

            Location last = locationManager.getLastKnownLocation(
                    LocationManager.GPS_PROVIDER);

            if (last != null) {
                onLocationChanged(last);
            }

        } catch (SecurityException e) {
            Toast.makeText(this,
                    "Permiso GPS no concedido",
                    Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {

        if (speedText == null || location == null) {
            return;
        }

        if (location.hasSpeed()) {
            int kmh = Math.max(
                    0, Math.round(location.getSpeed() * 3.6f));

            speedText.setText(String.valueOf(kmh));
        }
    }

    @Override
    public void onStatusChanged(String provider, int status,
                                Bundle extras) {
    }

    @Override
    public void onProviderEnabled(String provider) {
    }

    @Override
    public void onProviderDisabled(String provider) {
    }

    @Override
    public void onRequestPermissionsResult(
            int requestCode, String[] permissions, int[] results) {

        super.onRequestPermissionsResult(
                requestCode, permissions, results);

        if (requestCode == LOCATION_REQUEST
                && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) {

            startGps();

        } else {
            Toast.makeText(this,
                    "Sin permiso GPS: velocímetro sin velocidad real",
                    Toast.LENGTH_LONG).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();

        if (checkSelfPermission(
                Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGps();
        }
    }

    @Override
    protected void onPause() {
        super.onPause();

        if (locationManager != null && locationStarted) {
            locationManager.removeUpdates(this);
            locationStarted = false;
        }
    }
}
