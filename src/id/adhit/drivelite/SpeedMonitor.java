package id.adhit.drivelite;

import android.Manifest;
import android.app.Activity;
import android.content.pm.PackageManager;
import android.location.*;
import android.os.*;
import java.util.Locale;

/** Standard GNSS speed, not an estimate from animation or unverified CAN broadcasts. */
final class SpeedMonitor implements LocationListener {
    interface Listener { void speed(String value, String status); }
    static final int PERMISSION = 42;
    private final Activity activity;
    private final LocationManager manager;
    private final Listener listener;
    private final Handler handler = new Handler(Looper.getMainLooper());
    private Location last;
    private boolean active, listening;
    SpeedMonitor(Activity a, Listener l) {
        activity=a; listener=l; manager=(LocationManager)a.getSystemService(Activity.LOCATION_SERVICE);
    }
    private final Runnable tick = new Runnable() {
        @Override public void run() { if (!active) return; render(); handler.postDelayed(this, 1000); }
    };
    boolean permitted() { return Build.VERSION.SDK_INT < 23 || activity.checkSelfPermission(Manifest.permission.ACCESS_FINE_LOCATION)==PackageManager.PERMISSION_GRANTED; }
    void requestPermission() {
        if (!permitted()) activity.requestPermissions(new String[]{Manifest.permission.ACCESS_FINE_LOCATION}, PERMISSION);
    }
    void start() {
        if (active) return;
        active=true;
        if (permitted() && manager != null) {
            try {
                if (manager.isProviderEnabled(LocationManager.GPS_PROVIDER)) {
                    manager.requestLocationUpdates(LocationManager.GPS_PROVIDER, 1000, 0, this, Looper.getMainLooper());
                    listening=true;
                    Location saved=manager.getLastKnownLocation(LocationManager.GPS_PROVIDER);
                    if (saved != null && !saved.isFromMockProvider()) last=saved;
                }
            } catch (RuntimeException failure) { android.util.Log.w("DriveLite","GPS unavailable",failure); }
        }
        tick.run();
    }
    void stop() {
        active=false; handler.removeCallbacks(tick);
        if (listening && manager != null) { try { manager.removeUpdates(this); } catch (RuntimeException ignored) {} }
        listening=false;
    }
    void restart() { stop(); start(); }
    private void render() {
        if (!permitted()) { listener.speed("—", "GPS · IZIN LOKASI"); return; }
        boolean enabled=false;
        try { enabled=manager!=null && manager.isProviderEnabled(LocationManager.GPS_PROVIDER); } catch (RuntimeException ignored) {}
        if (!enabled) { listener.speed("—", "GPS NONAKTIF"); return; }
        long age = last==null ? Long.MAX_VALUE : SystemClock.elapsedRealtimeNanos()-last.getElapsedRealtimeNanos();
        boolean usable=last!=null && age>=0 && age<10_000_000_000L && last.hasSpeed()
            && !last.isFromMockProvider() && (!last.hasAccuracy() || last.getAccuracy()<=50)
            && (Build.VERSION.SDK_INT<26 || !last.hasSpeedAccuracy() || last.getSpeedAccuracyMetersPerSecond()<=5)
            && last.getSpeed()>=0 && last.getSpeed()<100;
        if (!usable) { listener.speed("—", "MENCARI GPS"); return; }
        int kmh=displayedKmh(last.getSpeed());
        listener.speed(String.format(Locale.US, "%d", kmh), "GPS TERHUBUNG");
    }
    static int displayedKmh(float speedMps) {
        int kmh=Math.round(speedMps*3.6f);
        return kmh<=2?0:kmh; // Suppress the observed 1–2 km/h stationary GPS drift.
    }
    @Override public void onLocationChanged(Location l) { if (!l.isFromMockProvider()) { last=l; render(); } }
    @Override public void onProviderEnabled(String provider) { if (active) restart(); }
    @Override public void onProviderDisabled(String provider) { render(); }
    @Override public void onStatusChanged(String provider, int status, Bundle extras) { if (active) render(); }
}
