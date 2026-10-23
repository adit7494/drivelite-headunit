package id.adhit.drivelite;

import android.animation.ValueAnimator;
import android.content.Context;
import android.graphics.*;
import android.os.Handler;
import android.os.Looper;
import android.view.View;
import android.view.animation.AccelerateDecelerateInterpolator;
import java.io.InputStream;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;

/** Bundled art only. Two full-screen bitmaps at most; tiny shared cover thumbnails. */
final class Artwork {
    static final String[] KEYS = {"witcher", "ghost", "horizon", "stray", "cyberpunk", "eldenring", "reddead", "godofwar", "ragnarok", "hogwarts", "spiderman", "milesmorales", "lastofus", "uncharted", "residentevil4", "residentevil2", "sekiro", "darksouls3", "doom", "gta5", "forza5", "nier", "hades", "hollowknight", "deathstranding"};
    static final String[] TITLES = {"The Witcher 3: Wild Hunt", "Ghost of Tsushima", "Horizon Forbidden West", "Stray", "Cyberpunk 2077", "ELDEN RING", "Red Dead Redemption 2", "God of War", "God of War Ragnarok", "Hogwarts Legacy", "Marvels Spider-Man Remastered", "Spider-Man Miles Morales", "The Last of Us Part I", "UNCHARTED Legacy of Thieves", "Resident Evil 4", "Resident Evil 2", "Sekiro Shadows Die Twice", "Dark Souls III", "DOOM Eternal", "Grand Theft Auto V", "Forza Horizon 5", "NieR Automata", "Hades", "Hollow Knight", "Death Stranding Directors Cut"};
    private final Context context;
    private final Handler main = new Handler(Looper.getMainLooper());
    private final ExecutorService worker = Executors.newSingleThreadExecutor();
    private final Bitmap[] covers = new Bitmap[KEYS.length];
    private boolean closed;
    Runnable coversReady;

    Artwork(Context c) { context = c.getApplicationContext(); }

    void loadCovers() {
        worker.execute(() -> {
            for (int i = 0; i < KEYS.length; i++) {
                final int index = i;
                final Bitmap b = decode("covers/" + KEYS[i] + ".jpg", 150, 225);
                main.post(() -> {
                    if (closed) { if (b != null) b.recycle(); return; }
                    covers[index] = b;
                    if (coversReady != null) coversReady.run();
                });
            }
        });
    }

    private Bitmap decode(String path, int width, int height) {
        try {
            BitmapFactory.Options o = new BitmapFactory.Options();
            o.inJustDecodeBounds = true;
            try (InputStream in = context.getAssets().open(path)) { BitmapFactory.decodeStream(in, null, o); }
            o.inSampleSize = 1;
            while (o.outWidth / (o.inSampleSize * 2) >= width && o.outHeight / (o.inSampleSize * 2) >= height) o.inSampleSize *= 2;
            o.inJustDecodeBounds = false;
            o.inPreferredConfig = Bitmap.Config.RGB_565;
            o.inDither = true;
            Bitmap decoded;
            try (InputStream in = context.getAssets().open(path)) { decoded = BitmapFactory.decodeStream(in, null, o); }
            if (decoded != null && (decoded.getWidth() > width || decoded.getHeight() > height)) {
                float ratio = Math.min(1f, Math.max(width / (float) decoded.getWidth(), height / (float) decoded.getHeight()));
                Bitmap resized = Bitmap.createScaledBitmap(decoded, Math.max(1, Math.round(decoded.getWidth()*ratio)), Math.max(1, Math.round(decoded.getHeight()*ratio)), true);
                if (resized != decoded) decoded.recycle();
                return resized;
            }
            return decoded;
        } catch (Exception failure) { android.util.Log.w("DriveLite", "Cannot decode " + path, failure); return null; }
    }

    private static void crop(Canvas c, Bitmap b, RectF area, Paint p) {
        float factor = Math.max(area.width()/b.getWidth(), area.height()/b.getHeight());
        float w = b.getWidth()*factor, h = b.getHeight()*factor;
        RectF to = new RectF(area.centerX()-w/2, area.centerY()-h/2, area.centerX()+w/2, area.centerY()+h/2);
        c.drawBitmap(b, null, to, p);
    }

    private Bitmap prepareWallpaper(int index) {
        Bitmap source=decode("wallpapers/"+KEYS[index]+".jpg",1024,600);
        if(source==null) return null;
        Bitmap result=Bitmap.createBitmap(1024,600,Bitmap.Config.ARGB_8888);
        Canvas canvas=new Canvas(result);
        Paint p=new Paint(Paint.ANTI_ALIAS_FLAG|Paint.FILTER_BITMAP_FLAG|Paint.DITHER_FLAG);
        RectF bounds=new RectF(0,0,1024,600);
        crop(canvas,source,bounds,p);
        source.recycle();
        p.setShader(new LinearGradient(0,0,1024,0,new int[]{0xB808100F,0x8008100F,0x1608100F},new float[]{0,.42f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,p);
        p.setShader(new LinearGradient(0,0,0,600,new int[]{0x7204080B,0x1004080B,0x2604080B,0xD404080B},new float[]{0,.28f,.6f,1},Shader.TileMode.CLAMP));
        canvas.drawRect(bounds,p);
        return result;
    }

    final class Cover extends View {
        final int index;
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private final Path clip = new Path();
        Cover(Context c, int i) { super(c); index = i; setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        @Override protected void onSizeChanged(int w, int h, int ow, int oh) {
            clip.reset(); clip.addRoundRect(new RectF(0, 0, w, h), w*.10f, w*.10f, Path.Direction.CW);
        }
        @Override protected void onDraw(Canvas c) {
            c.save(); c.clipPath(clip); c.drawColor(0xFF23323A);
            Bitmap b = covers[index];
            if (b != null) crop(c, b, new RectF(0, 0, getWidth(), getHeight()), paint);
            c.restore();
        }
    }

    final class Wallpaper extends View {
        private final Paint paint = new Paint(Paint.ANTI_ALIAS_FLAG | Paint.FILTER_BITMAP_FLAG);
        private Bitmap current, incoming;
        private int currentIndex = -1, requestedIndex = -1, generation;
        private float progress;
        private ValueAnimator animator;
        private boolean library;
        Wallpaper(Context c) { super(c); setImportantForAccessibility(IMPORTANT_FOR_ACCESSIBILITY_NO); }
        int index() { return requestedIndex < 0 ? 0 : requestedIndex; }
        void library(boolean shown) { library = shown; invalidate(); }
        void select(int index, boolean animate) {
            if (closed || (requestedIndex == index && current != null)) return;
            requestedIndex = index;
            final int ticket = ++generation;
            finishTransition();
            worker.execute(() -> {
                if (ticket != generation || closed) return;
                // Crop and bake the scrims off-thread. Only two 1024x600 surfaces are blended.
                final Bitmap b = prepareWallpaper(index);
                main.post(() -> {
                    if (closed || ticket != generation) { if (b != null) b.recycle(); return; }
                    if (b == null) return;
                    if (current == null || !animate || !isShown()) {
                        release(current); current = b; currentIndex = index; invalidate(); return;
                    }
                    incoming = b; progress = 0;
                    animator = ValueAnimator.ofFloat(0f, 1f);
                    animator.setDuration(900);
                    animator.setInterpolator(new AccelerateDecelerateInterpolator());
                    animator.addUpdateListener(a -> {
                        progress = (float)a.getAnimatedValue();
                        // Transfer once; near-final interpolated frames may occur more than once.
                        if (progress >= 1f && incoming != null) { release(current); current = incoming; incoming = null; currentIndex = index; progress = 0; }
                        invalidate();
                    });
                    animator.start();
                });
            });
        }
        void finishTransition() {
            if (animator != null) { animator.cancel(); animator = null; }
            if (incoming != null) { release(current); current = incoming; incoming = null; currentIndex = requestedIndex; }
            progress = 0; invalidate();
        }
        @Override protected void onDraw(Canvas c) {
            paint.setShader(null); paint.setAlpha(255);
            RectF bounds = new RectF(0,0,getWidth(),getHeight());
            if (current != null) c.drawBitmap(current,null,bounds,paint); else c.drawColor(0xFF101A1E);
            if (incoming != null) { paint.setAlpha(Math.round(progress*255)); c.drawBitmap(incoming,null,bounds,paint); paint.setAlpha(255); }
            if (library) c.drawColor(0x680B111B);
        }
        void dispose() { generation++; finishTransition(); release(current); current = null; }
        private void release(Bitmap b) { if (b != null && !b.isRecycled()) b.recycle(); }
    }

    void close() {
        closed = true; coversReady = null; worker.shutdownNow();
        for (int i=0; i<covers.length; i++) { if (covers[i] != null) covers[i].recycle(); covers[i] = null; }
    }
}
