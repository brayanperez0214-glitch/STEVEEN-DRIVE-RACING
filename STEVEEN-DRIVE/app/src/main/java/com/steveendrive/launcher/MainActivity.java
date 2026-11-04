package com.steveendrive.launcher;

import android.Manifest;
import android.app.Activity;
import android.content.ActivityNotFoundException;
import android.content.Intent;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.graphics.Typeface;
import android.graphics.drawable.GradientDrawable;
import android.location.Location;
import android.location.LocationListener;
import android.location.LocationManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;
import android.provider.Settings;
import android.view.Gravity;
import android.view.View;
import android.view.WindowManager;
import android.widget.FrameLayout;
import android.widget.ImageView;
import android.widget.TextView;
import android.widget.Toast;

import java.util.Locale;

public class MainActivity extends Activity implements LocationListener {
    private static final int LOCATION_REQUEST = 410;
    private FrameLayout root;
    private TextView speedText;
    private LocationManager locationManager;
    private boolean locationStarted = false;

    @Override
    public void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_KEEP_SCREEN_ON);
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

        // Sustituye únicamente el número estático del velocímetro; conserva la gráfica original.
        speedText = new TextView(this);
        speedText.setText("0");
        speedText.setTextColor(Color.WHITE);
        speedText.setTextSize(43);
        speedText.setTypeface(Typeface.create("sans-serif-condensed", Typeface.BOLD));
        speedText.setGravity(Gravity.CENTER);
        speedText.setShadowLayer(5, 0, 0, Color.BLACK);
        speedText.setBackground(roundBackground(Color.rgb(5, 5, 7), 100));
        addAt(speedText, 0.155f, 0.372f, 0.075f, 0.105f);

        // Zonas táctiles transparentes alineadas con los botones de la imagen.
        addHotspot("Inicio", 0.00f, 0.80f, 0.112f, 0.19f, v -> showHome());
        addHotspot("Mapas", 0.115f, 0.80f, 0.105f, 0.19f, v -> openMaps());
        addHotspot("Bluetooth", 0.222f, 0.80f, 0.108f, 0.19f, v -> openBluetooth());
        addHotspot("WhatsApp", 0.330f, 0.80f, 0.125f, 0.19f, v -> openPackage("com.whatsapp"));
        addHotspot("Música", 0.560f, 0.80f, 0.120f, 0.19f, v -> openMusic());
        addHotspot("Video", 0.685f, 0.80f, 0.105f, 0.19f, v -> openVideo());
        addHotspot("Auto", 0.792f, 0.80f, 0.105f, 0.19f, v -> openBluetooth());
        addHotspot("Aplicaciones", 0.900f, 0.80f, 0.100f, 0.19f, v -> openApps());

        // Paneles de navegación y música; sus botones no sustituyen el fondo gráfico.
        addHotspot("Abrir navegación", 0.735f, 0.125f, 0.255f, 0.355f, v -> openMaps());
        addHotspot("Reproductor de música", 0.735f, 0.505f, 0.255f, 0.265f, v -> openMusic());
        addHotspot("Configuración", 0.000f, 0.000f, 0.060f, 0.105f, v -> openSettings());
        addHotspot("Teléfono", 0.065f, 0.000f, 0.070f, 0.105f, v -> openDialer());

        locationManager = (LocationManager) getSystemService(LOCATION_SERVICE);
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

    private void addAt(View view, float centerX, float centerY, float width, float height) {
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                Math.max(1, (int)(getResources().getDisplayMetrics().widthPixels * width)),
                Math.max(1, (int)(getResources().getDisplayMetrics().heightPixels * height)));
        p.leftMargin = (int)(getResources().getDisplayMetrics().widthPixels * centerX)
                - p.width / 2;
        p.topMargin = (int)(getResources().getDisplayMetrics().heightPixels * centerY)
                - p.height / 2;
        root.addView(view, p);
    }

    private void addHotspot(String description, float x, float y, float w, float h,
                            View.OnClickListener listener) {
        View v = new View(this);
        v.setContentDescription(description);
        v.setBackgroundColor(Color.TRANSPARENT);
        v.setOnClickListener(listener);
        FrameLayout.LayoutParams p = new FrameLayout.LayoutParams(
                Math.max(1, (int)(getResources().getDisplayMetrics().widthPixels * w)),
                Math.max(1, (int)(getResources().getDisplayMetrics().heightPixels * h)));
        p.leftMargin = (int)(getResources().getDisplayMetrics().widthPixels * x);
        p.topMargin = (int)(getResources().getDisplayMetrics().heightPixels * y);
        root.addView(v, p);
    }

    private void requestLocationIfNeeded() {
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) {
            startGps();
        } else {
            requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION,
                    Manifest.permission.ACCESS_COARSE_LOCATION}, LOCATION_REQUEST);
        }
    }

    private void startGps() {
        if (locationStarted || locationManager == null) return;
        try {
            if (!locationManager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                Toast.makeText(this, "Activa la ubicación/GPS para mostrar la velocidad real",
                        Toast.LENGTH_LONG).show();
                return;
            }
            locationManager.requestLocationUpdates(LocationManager.GPS_PROVIDER,
                    500, 0, this);
            locationStarted = true;
            Location last = locationManager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
            if (last != null) onLocationChanged(last);
        } catch (SecurityException e) {
            Toast.makeText(this, "Permiso GPS no concedido", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    public void onLocationChanged(Location location) {
        if (speedText == null || location == null) return;
        if (location.hasSpeed()) {
            int kmh = Math.max(0, Math.round(location.getSpeed() * 3.6f));
            speedText.setText(String.valueOf(kmh));
        }
    }

    @Override public void onStatusChanged(String provider, int status, Bundle extras) { }
    @Override public void onProviderEnabled(String provider) { }
    @Override public void onProviderDisabled(String provider) { }

    @Override
    public void onRequestPermissionsResult(int requestCode, String[] permissions, int[] results) {
        super.onRequestPermissionsResult(requestCode, permissions, results);
        if (requestCode == LOCATION_REQUEST && results.length > 0
                && results[0] == PackageManager.PERMISSION_GRANTED) startGps();
        else Toast.makeText(this, "Sin permiso GPS: el velocímetro no podrá leer velocidad",
                Toast.LENGTH_LONG).show();
    }

    private void showHome() {
        Toast.makeText(this, "STEVEEN-DRIVE | Inicio", Toast.LENGTH_SHORT).show();
    }

    private void openMaps() {
        try {
            Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse("google.navigation:q="));
            i.setPackage("com.google.android.apps.maps");
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("geo:0,0?q="))); }
            catch (Exception ignored) { openPackage("com.google.android.apps.maps"); }
        }
    }

    private void openBluetooth() {
        try { startActivity(new Intent(Settings.ACTION_BLUETOOTH_SETTINGS)); }
        catch (Exception e) { openSettings(); }
    }

    private void openMusic() {
        try {
            Intent i = new Intent("android.intent.action.MUSIC_PLAYER");
            i.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK);
            startActivity(i);
        } catch (Exception e) {
            try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_APP_MUSIC)); }
            catch (Exception ignored) { openApps(); }
        }
    }

    private void openVideo() {
        try { startActivity(new Intent(Intent.ACTION_MAIN).addCategory("android.intent.category.APP_VIDEO")); }
        catch (Exception e) { openApps(); }
    }

    private void openApps() {
        try { startActivity(new Intent(Settings.ACTION_MANAGE_APPLICATIONS_SETTINGS)); }
        catch (Exception e) { openSettings(); }
    }

    private void openSettings() {
        try { startActivity(new Intent(Settings.ACTION_SETTINGS)); }
        catch (Exception e) { Toast.makeText(this, "No se pudo abrir Configuración", Toast.LENGTH_SHORT).show(); }
    }

    private void openDialer() {
        try { startActivity(new Intent(Intent.ACTION_DIAL)); }
        catch (Exception e) { openApps(); }
    }

    private void openPackage(String packageName) {
        try {
            Intent i = getPackageManager().getLaunchIntentForPackage(packageName);
            if (i != null) startActivity(i);
            else Toast.makeText(this, "La aplicación no está instalada", Toast.LENGTH_SHORT).show();
        } catch (ActivityNotFoundException e) {
            Toast.makeText(this, "No se encontró la aplicación", Toast.LENGTH_SHORT).show();
        }
    }

    @Override
    protected void onResume() {
        super.onResume();
        hideSystemUI();
        if (checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)
                == PackageManager.PERMISSION_GRANTED) startGps();
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
