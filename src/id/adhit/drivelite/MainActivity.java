package id.adhit.drivelite;

import android.Manifest;
import android.app.*;
import android.content.*;
import android.content.pm.*;
import android.graphics.*;
import android.graphics.drawable.*;
import android.net.Uri;
import android.os.*;
import android.provider.Settings;
import android.text.*;
import android.view.*;
import android.view.inputmethod.InputMethodManager;
import android.widget.*;
import org.json.JSONArray;
import org.json.JSONObject;
import java.util.*;

/** Native offline launcher. No service, WebView, Internet permission, or video background. */
public final class MainActivity extends Activity {
    private static final int WHITE=0xFFF8FAFC, MUTED=0xFFB8C1C6;
    private final List<Entry> apps=new ArrayList<>(), favorites=new ArrayList<>();
    private final List<FrameLayout> favoriteCards=new ArrayList<>();
    private final Handler handler=new Handler(Looper.getMainLooper());
    private SharedPreferences prefs;
    private FrameLayout root, home, library, header;
    private HorizontalScrollView stripScroll;
    private LinearLayout strip;
    private GridLayout grid;
    private TextView selectedLabel, heroTitle, heroDescription, openButton, artTitle, speedNumber, speedStatus, motionLabel, libraryCount;
    private TextClock clock;
    private ImageView gridToggle;
    private View gpsDot;
    private Boolean gpsConnected;
    private FrameLayout heroImage;
    private ImageView motionIcon;
    private int heroArt=-2, motionMode=-1;
    private EditText search;
    private Artwork art;
    private Artwork.Wallpaper wallpaper;
    private SpeedMonitor speed;
    private Entry selected;
    private float scale=1;
    private boolean inLibrary, resumed, built, initialized, popupOpen, destroyed;
    private int activeArt=-1;
    private final Runnable slideshow=new Runnable() {
        @Override public void run() {
            if (!resumed || inLibrary || popupOpen || !prefs.getBoolean("slideshow",true)) return;
            activeArt=(wallpaper.index()+1)%Artwork.KEYS.length;
            wallpaper.select(activeArt,true); updateArtTitle(); scheduleSlideshow();
        }
    };
    private static final class Entry {
        String pkg, name, description, tag, glyph;
        Intent intent;
        Drawable icon;
        int color, art;
        boolean system;
    }

    @Override public void onCreate(Bundle state) {
        super.onCreate(state);
        getWindow().addFlags(WindowManager.LayoutParams.FLAG_FULLSCREEN);
        prefs=getSharedPreferences("home",MODE_PRIVATE);
        art=new Artwork(this);
        speed=new SpeedMonitor(this,(value,status)-> {
            if (speedNumber!=null) {
                if (!value.contentEquals(speedNumber.getText())) speedNumber.setText(value);
                if (!status.contentEquals(speedStatus.getText())) speedStatus.setText(status);
                boolean connected="GPS TERHUBUNG".equals(status);
                if(gpsConnected==null || gpsConnected!=connected) {
                    gpsConnected=connected;
                    gpsDot.setBackground(rounded(connected?0xFF67E4A3:0xFFFF6868,6,0,0));
                    gpsDot.setContentDescription(connected?"GPS terhubung, indikator hijau":"GPS tidak terhubung, indikator merah");
                }
                int kmh="—".equals(value)?-1:Integer.parseInt(value);
                int mode=kmh<0?0:kmh==0?1:kmh<5?2:3;
                if(mode!=motionMode) {
                    motionMode=mode;
                    motionLabel.setText(mode==1?"Mobil berhenti":mode==2?"Bergerak pelan":mode==3?"Mobil berjalan":"Menunggu GPS");
                    motionIcon.setImageDrawable(new Glyph(mode==1?"park":mode>=2?"driving":"search",mode==1?0xFFBFEBD5:mode==2?0xFFF0D2A1:mode==3?0xFFB1DAFF:0xFFD4DDE2));
                }
            }
        });
        root=new FrameLayout(this); root.setBackgroundColor(0xFF101A1E);
        setContentView(root); immersive(); loadApps();
        root.getViewTreeObserver().addOnGlobalLayoutListener(new ViewTreeObserver.OnGlobalLayoutListener() {
          @Override public void onGlobalLayout() {
            if (destroyed || root.getWidth()<=0 || root.getHeight()<=0) return;
            root.getViewTreeObserver().removeOnGlobalLayoutListener(this);
            scale=Math.min(root.getWidth()/1024f,root.getHeight()/600f);
            buildInterface(); built=true; initialized=true; showHome();
            art.coversReady=()-> { if (!destroyed) { invalidateCovers(home); if (library!=null) invalidateCovers(library); } };
            art.loadCovers();
            if (!speed.permitted() && !prefs.getBoolean("locationAsked",false)) {
                prefs.edit().putBoolean("locationAsked",true).apply(); speed.requestPermission();
            }
          }
        });
    }
    @Override protected void onResume() {
        super.onResume(); resumed=true;
        if (built) { loadApps(); rebuildFavorites(); if (inLibrary) fillGrid(); else showHome(); }
        immersive();
    }
    @Override protected void onPause() {
        resumed=false; handler.removeCallbacks(slideshow); speed.stop();
        if (wallpaper!=null) wallpaper.finishTransition();
        super.onPause();
    }
    @Override protected void onDestroy() {
        destroyed=true; handler.removeCallbacksAndMessages(null); speed.stop();
        if (wallpaper!=null) wallpaper.dispose(); art.close(); super.onDestroy();
    }
    @Override protected void onNewIntent(Intent intent) { super.onNewIntent(intent); setIntent(intent); if (built) showHome(); }
    @Override public void onWindowFocusChanged(boolean focused) { super.onWindowFocusChanged(focused); if (focused) immersive(); }
    @Override public void onRequestPermissionsResult(int request,String[] permissions,int[] result) {
        super.onRequestPermissionsResult(request,permissions,result);
        if (request==SpeedMonitor.PERMISSION && resumed && !inLibrary) speed.restart();
    }
    private void immersive() {
        getWindow().getDecorView().setSystemUiVisibility(View.SYSTEM_UI_FLAG_FULLSCREEN|View.SYSTEM_UI_FLAG_HIDE_NAVIGATION|
            View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY|View.SYSTEM_UI_FLAG_LAYOUT_STABLE|View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN|View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION);
    }
    private void loadApps() {
        String last=selected==null?prefs.getString("selected","com.tw.radio"):selected.pkg;
        Map<String,Entry> unique=new LinkedHashMap<>();
        PackageManager pm=getPackageManager();
        for (ResolveInfo info:pm.queryIntentActivities(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER),0)) {
            String pkg=info.activityInfo.packageName;
            if (pkg.equals(getPackageName()) || pkg.equals("com.dofun.variety") || unique.containsKey(pkg) || !info.activityInfo.enabled || !info.activityInfo.exported) continue;
            Entry e=new Entry(); e.pkg=pkg; e.name=info.loadLabel(pm).toString().trim();
            e.system=(info.activityInfo.applicationInfo.flags & (ApplicationInfo.FLAG_SYSTEM|ApplicationInfo.FLAG_UPDATED_SYSTEM_APP))!=0;
            e.intent=pm.getLaunchIntentForPackage(pkg);
            if (e.intent==null) e.intent=new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_LAUNCHER).setComponent(new ComponentName(pkg,info.activityInfo.name));
            e.intent.addFlags(Intent.FLAG_ACTIVITY_NEW_TASK|Intent.FLAG_ACTIVITY_RESET_TASK_IF_NEEDED);
            e.art=(pkg.hashCode() & 0x7fffffff)%Artwork.KEYS.length;
            e.color=0xFF42647B; e.tag="APLIKASI"; e.description="Buka "+e.name+" di head unit Anda."; customize(e); unique.put(pkg,e);
        }
        apps.clear(); apps.addAll(unique.values()); Collections.sort(apps,(a,b)->a.name.compareToIgnoreCase(b.name));
        assignArtwork();
        favorites.clear();
        if (!prefs.contains("favorites_v2")) {
            String[] defaults={"com.tw.radio","com.tw.bt","com.tw.media","com.tw.auxin","com.tw.eq","com.tw.twfileexplore","com.dofun.carsetting","com.android.chrome","com.android.settings"};
            for(String pkg:defaults) if(unique.containsKey(pkg)) favorites.add(unique.get(pkg));
            saveFavorites();
        } else {
            try {
                JSONArray saved=new JSONArray(prefs.getString("favorites_v2","[]"));
                for(int i=0;i<saved.length();i++) { Entry e=unique.get(saved.getString(i)); if(e!=null && !favorites.contains(e)) favorites.add(e); }
            } catch(Exception invalid) { android.util.Log.w("DriveLite","Invalid favorite list",invalid); }
        }
        selected=favorites.isEmpty()?null:favorites.get(0);
        for(Entry e:favorites) if(e.pkg.equals(last)) selected=e;
    }
    private void assignArtwork() {
        String key="art_assignments_v22", encoded=prefs.getString(key,"{}");
        Map<String,Integer> saved=new LinkedHashMap<>(), seeds=new LinkedHashMap<>();
        List<String> packages=new ArrayList<>();
        try { JSONObject old=new JSONObject(encoded); for(Entry e:apps) if(old.has(e.pkg)) saved.put(e.pkg,old.getInt(e.pkg)); }
        catch(Exception invalid) { android.util.Log.w("DriveLite","Reset invalid art assignments",invalid); }
        seeds.put("com.tw.radio",0); seeds.put("com.tw.bt",1); seeds.put("com.tw.media",2);
        seeds.put("com.tw.twfileexplore",3); seeds.put("com.android.chrome",4);
        for(Entry e:apps) packages.add(e.pkg);
        Map<String,Integer> assigned=CoverAssignments.assign(packages,saved,seeds,Artwork.KEYS.length);
        JSONObject result=new JSONObject();
        for(Entry e:apps) { e.art=assigned.get(e.pkg); try { result.put(e.pkg,e.art); } catch(Exception ignored) {} }
        if(!encoded.equals(result.toString())) prefs.edit().putString(key,result.toString()).apply();
    }
    private void customize(Entry e) {
        switch(e.pkg) {
            case "com.tw.radio": describe(e,"Radio","AUDIO","Stasiun favorit Anda, langsung dari head unit.","radio",0xFF3766BC,0); break;
            case "com.tw.bt": describe(e,"Bluetooth","KONEKSI","Musik dan panggilan dari ponsel Anda.","bluetooth",0xFF6750A0,1); break;
            case "com.tw.media": describe(e,"Musik","MEDIA","Koleksi musik dan video untuk perjalanan Anda.","music",0xFF9C4D79,2); break;
            case "com.tw.auxin": describe(e,"AUX","AUDIO","Audio dari perangkat yang terhubung.","aux",0xFF387B79,3); break;
            case "com.tw.eq": describe(e,"Equalizer","SUARA","Atur karakter suara sesuai selera Anda.","eq",0xFF947041,4); break;
            case "com.tw.twfileexplore": describe(e,"Berkas","KOLEKSI","Jelajahi penyimpanan dan flashdisk.","folder",0xFF3A778C,3); break;
            case "com.dofun.carsetting": describe(e,"Setelan mobil","KENDARAAN","Pengaturan layar, suara, dan fungsi kendaraan.","car",0xFF486D91,1); break;
            case "com.android.chrome": describe(e,"Browser","JELAJAHI","Buka situs favorit Anda.","globe",0xFF457571,4); break;
            case "com.android.settings": describe(e,"Pengaturan","PERANGKAT","Wi-Fi, aplikasi, dan pengaturan Android.","settings",0xFF596884,2); break;
        }
    }
    private void describe(Entry e,String name,String tag,String desc,String glyph,int color,int backdrop) {
        e.name=name; e.tag=tag; e.description=desc; e.glyph=glyph; e.color=color; e.art=backdrop;
    }
    private boolean isFavorite(Entry e) { for(Entry f:favorites) if(f.pkg.equals(e.pkg)) return true; return false; }
    private void saveFavorites() {
        JSONArray a=new JSONArray(); for(Entry e:favorites) a.put(e.pkg); prefs.edit().putString("favorites_v2",a.toString()).apply();
    }
    private void toggleFavorite(Entry e) {
        boolean remove=isFavorite(e);
        if(remove) { for(Iterator<Entry> i=favorites.iterator();i.hasNext();) if(i.next().pkg.equals(e.pkg)) i.remove(); }
        else favorites.add(e);
        saveFavorites();
        if(selected!=null && !isFavorite(selected)) selected=favorites.isEmpty()?null:favorites.get(0);
        if(selected==null && !favorites.isEmpty()) selected=favorites.get(0);
        activeArt=selected==null?0:selected.art;
        rebuildFavorites(); updateSelected(false); if(inLibrary) fillGrid();
        Toast.makeText(this,e.name+(remove?" dihapus dari favorit":" ditambahkan ke favorit"),Toast.LENGTH_SHORT).show();
    }
    private int px(float n) { return Math.round(n*scale); }
    private void place(FrameLayout parent,View view,int w,int h,int x,int y) {
        FrameLayout.LayoutParams lp=new FrameLayout.LayoutParams(px(w),px(h)); lp.leftMargin=px(x); lp.topMargin=px(y); parent.addView(view,lp);
    }
    private TextView text(String value,float size,int color,boolean medium) {
        TextView t=new TextView(this); t.setText(value); t.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(size)); t.setTextColor(color);
        t.setTypeface(Typeface.create(medium?"sans-serif-medium":"sans-serif-light",Typeface.NORMAL)); t.setIncludeFontPadding(false); t.setGravity(Gravity.CENTER_VERTICAL);
        return t;
    }
    private GradientDrawable rounded(int color,float radius,int stroke,float width) {
        GradientDrawable d=new GradientDrawable(); d.setColor(color); d.setCornerRadius(px(radius)); if(width>0) d.setStroke(Math.max(1,px(width)),stroke); return d;
    }
    private TextView button(String label,int size,Runnable click) {
        TextView b=text(label,size,WHITE,true); b.setGravity(Gravity.CENTER); b.setBackground(rounded(0x36FFFFFF,28,0x15FFFFFF,1));
        b.setFocusable(true); b.setOnClickListener(v->click.run());
        b.setOnFocusChangeListener((v,focus)->b.setBackground(rounded(focus?0x64FFFFFF:0x36FFFFFF,28,focus?WHITE:0x15FFFFFF,focus?2:1)));
        return b;
    }
    private ImageView iconButton(String glyph,String desc,Runnable action) {
        ImageView image=new ImageView(this); image.setImageDrawable(new Glyph(glyph,WHITE)); image.setPadding(px(10),px(10),px(10),px(10));
        image.setContentDescription(desc); image.setFocusable(true); image.setBackground(rounded(0x08000000,24,0,0));
        image.setOnClickListener(v->action.run()); image.setOnFocusChangeListener((v,f)->image.setBackground(rounded(f?0x42FFFFFF:0x08000000,24,WHITE,f?1:0)));
        return image;
    }
    private void buildInterface() {
        root.removeAllViews(); wallpaper=art.new Wallpaper(this);
        root.addView(wallpaper,new FrameLayout.LayoutParams(-1,-1));
        home=new FrameLayout(this); root.addView(home,new FrameLayout.LayoutParams(-1,-1));
        stripScroll=new HorizontalScrollView(this); stripScroll.setHorizontalScrollBarEnabled(false); stripScroll.setOverScrollMode(View.OVER_SCROLL_NEVER);
        stripScroll.setClipToPadding(false); place(home,stripScroll,944,171,30,75);
        strip=new LinearLayout(this); strip.setOrientation(LinearLayout.HORIZONTAL); strip.setPadding(px(2),px(4),px(18),0);
        stripScroll.addView(strip,new HorizontalScrollView.LayoutParams(-2,-1));
        selectedLabel=text("",19,WHITE,false); selectedLabel.setSingleLine(true); selectedLabel.setEllipsize(TextUtils.TruncateAt.END);
        place(home,selectedLabel,752,30,221,213);
        heroImage=new FrameLayout(this); place(home,heroImage,65,87,62,401);
        heroTitle=text("",20,WHITE,false); place(home,heroTitle,310,30,145,402);
        heroDescription=text("",12,0xFFD3DADC,false); heroDescription.setMaxLines(2); place(home,heroDescription,280,36,146,437);
        openButton=button("Buka",14,()-> { if(selected!=null) launch(selected); else showLibrary(false); }); place(home,openButton,155,38,145,486);
        TextView more=button("•••",16,()-> { if(selected!=null) showActions(selected); else showLibrary(false); }); more.setContentDescription("Opsi aplikasi terpilih"); place(home,more,38,38,310,486);
        artTitle=text("",10,0xBBD7DEDF,false); place(home,artTitle,354,22,46,565);
        FrameLayout navigation=new FrameLayout(this); navigation.setBackground(rounded(0x30071017,29,0x18FFFFFF,1)); place(home,navigation,184,52,420,532);
        TextView previous=button("‹",32,()->moveSelection(-1)); previous.setContentDescription("Aplikasi sebelumnya"); place(navigation,previous,44,44,4,4);
        ImageView select=iconButton("enter","Buka aplikasi terpilih",()-> { if(selected!=null) launch(selected); else showLibrary(false); }); select.setBackground(rounded(0x38FFFFFF,25,0x30FFFFFF,1)); place(navigation,select,64,44,60,4);
        TextView next=button("›",32,()->moveSelection(1)); next.setContentDescription("Aplikasi berikutnya"); place(navigation,next,44,44,136,4);
        FrameLayout speedBox=new FrameLayout(this); speedBox.setBackground(rounded(0x3807101B,20,0x20FFFFFF,1)); place(home,speedBox,234,157,762,418); speedBox.setContentDescription("Kecepatan mobil dari GPS");
        motionIcon=new ImageView(this); motionIcon.setImageDrawable(new Glyph("search",0xFFD4DDE2)); place(speedBox,motionIcon,46,46,20,22);
        speedNumber=text("—",50,WHITE,false); speedNumber.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); place(speedBox,speedNumber,104,64,72,6);
        TextView unit=text("km/h",16,MUTED,false); place(speedBox,unit,48,30,183,34);
        motionLabel=text("Menunggu GPS",18,WHITE,true); place(speedBox,motionLabel,202,29,20,85);
        gpsDot=new View(this); gpsDot.setBackground(rounded(0xFFFF6868,6,0,0)); gpsDot.setContentDescription("GPS tidak terhubung, indikator merah"); place(speedBox,gpsDot,9,9,20,126);
        speedStatus=text("MENCARI GPS",11,0xFFD1DBDF,true); place(speedBox,speedStatus,185,21,37,120);
        speedBox.setFocusable(true); speedBox.setOnClickListener(v-> { if(!speed.permitted()) speed.requestPermission(); else startSafely(new Intent(Settings.ACTION_LOCATION_SOURCE_SETTINGS)); });
        header=new FrameLayout(this); place(root,header,1024,73,0,0);
        gridToggle=iconButton("grid","Buka semua aplikasi",()-> { if(inLibrary) showHome(); else showLibrary(false); }); place(header,gridToggle,48,48,32,14);
        place(header,iconButton("search","Cari aplikasi",()->showLibrary(true)),44,44,774,16);
        place(header,iconButton("settings","Pengaturan launcher",this::launcherSettings),44,44,829,16);
        clock=new TextClock(this); clock.setFormat24Hour("HH:mm"); clock.setFormat12Hour("HH:mm"); clock.setTextColor(WHITE); clock.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(25)); clock.setTypeface(Typeface.create("sans-serif-light",Typeface.NORMAL)); clock.setGravity(Gravity.RIGHT|Gravity.CENTER_VERTICAL); clock.setIncludeFontPadding(false); clock.setContentDescription("Jam head unit"); place(header,clock,103,48,886,14);
        rebuildFavorites();
    }
    private void rebuildFavorites() {
        if(strip==null) return;
        int oldScroll=stripScroll.getScrollX(); strip.removeAllViews(); favoriteCards.clear();
        Space lead=new Space(this); strip.addView(lead,new LinearLayout.LayoutParams(px(76),px(1)));
        for(Entry e:favorites) {
            FrameLayout card=favoriteCard(e); favoriteCards.add(card);
            LinearLayout.LayoutParams lp=new LinearLayout.LayoutParams(px(82),px(165)); lp.rightMargin=px(5); strip.addView(card,lp);
        }
        if(favorites.isEmpty()) {
            TextView empty=text("Belum ada favorit\nTekan tahan aplikasi di Semua aplikasi untuk menambahkannya.",17,MUTED,false); empty.setMaxLines(3);
            strip.addView(empty,new LinearLayout.LayoutParams(px(650),px(104)));
        }
        arrangeFavorites(); stripScroll.post(()->stripScroll.scrollTo(oldScroll,0));
    }
    private FrameLayout favoriteCard(Entry e) {
        FrameLayout card=new FrameLayout(this); card.setTag(e); card.setFocusable(true); card.setClickable(true); card.setContentDescription("Pilih "+e.name);
        Artwork.Cover cover=art.new Cover(this,e.art); place(card,cover,72,94,4,4);
        FrameLayout badge=new FrameLayout(this); badge.setBackground(rounded(0xD90B1017,10,0x44FFFFFF,1)); place(card,badge,24,24,47,68);
        ImageView symbol=new ImageView(this); symbol.setImageDrawable(icon(e)); place(badge,symbol,16,16,4,4);
        TextView label=text(e.name,11.5f,MUTED,true); label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER); place(card,label,80,24,0,104);
        card.setOnClickListener(v-> { selected=e; activeArt=e.art; updateSelected(true); });
        card.setOnFocusChangeListener((v,focus)-> { if(focus && initialized && !inLibrary) { selected=e; activeArt=e.art; updateSelected(true); } });
        card.setOnLongClickListener(v-> { showActions(e); return true; });
        return card;
    }
    private void arrangeFavorites() {
        for(FrameLayout card:favoriteCards) {
            Entry e=(Entry)card.getTag(); boolean active=selected!=null && selected.pkg.equals(e.pkg);
            LinearLayout.LayoutParams lp=(LinearLayout.LayoutParams)card.getLayoutParams(); lp.width=px(active?110:82); card.setLayoutParams(lp);
            View cover=card.getChildAt(0); FrameLayout.LayoutParams cp=(FrameLayout.LayoutParams)cover.getLayoutParams(); cp.width=px(active?98:72); cp.height=px(active?131:94); cp.leftMargin=px(5); cp.topMargin=px(4); cover.setLayoutParams(cp);
            View badge=card.getChildAt(1); FrameLayout.LayoutParams bp=(FrameLayout.LayoutParams)badge.getLayoutParams(); bp.leftMargin=px(active?73:48); bp.topMargin=px(active?107:70); badge.setLayoutParams(bp);
            TextView label=(TextView)card.getChildAt(2); FrameLayout.LayoutParams tp=(FrameLayout.LayoutParams)label.getLayoutParams(); tp.width=px(active?108:82); tp.topMargin=px(active?137:103); label.setLayoutParams(tp); label.setTextColor(active?WHITE:MUTED); label.setVisibility(active?View.INVISIBLE:View.VISIBLE);
            card.setBackground(null);
            if(Build.VERSION.SDK_INT>=23) cover.setForeground(active?rounded(0x00FFFFFF,10,WHITE,2):null);
            else card.setBackground(active?rounded(0x00FFFFFF,10,WHITE,1):null);
        }
    }
    private Drawable icon(Entry e) {
        if(e.glyph!=null) return new Glyph(e.glyph,WHITE);
        if(e.icon==null) { try { e.icon=getPackageManager().getApplicationIcon(e.pkg); } catch(Exception missing) { e.icon=getPackageManager().getDefaultActivityIcon(); } }
        return e.icon;
    }
    private void updateSelected(boolean animate) {
        if(heroTitle==null) return;
        arrangeFavorites();
        if(selected==null) {
            selectedLabel.setText("Favorit Anda"); heroTitle.setText("Buat beranda Anda"); heroDescription.setText("Tekan tahan aplikasi di Semua aplikasi, lalu pilih Tambah favorit.");
            setHeroCover(-1); openButton.setText("Semua aplikasi");
        } else {
            selectedLabel.setText(selected.name+"  |  "+selected.tag); heroTitle.setText(selected.name); heroDescription.setText(selected.description);
            setHeroCover(selected.art); openButton.setText("Buka "+selected.name);
            prefs.edit().putString("selected",selected.pkg).apply();
        }
        if(activeArt<0) activeArt=selected==null?0:selected.art;
        wallpaper.select(activeArt,animate); updateArtTitle(); scheduleSlideshow();
    }
    private void setHeroCover(int index) {
        if(heroArt==index) return;
        heroArt=index; heroImage.removeAllViews();
        View picture;
        if(index>=0) picture=art.new Cover(this,index);
        else { ImageView empty=new ImageView(this); empty.setImageDrawable(new Glyph("grid",WHITE)); picture=empty; }
        heroImage.addView(picture,new FrameLayout.LayoutParams(-1,-1));
        heroImage.setForeground(rounded(0x00FFFFFF,8,0x88FFFFFF,1));
    }
    private void moveSelection(int direction) {
        if(favorites.isEmpty()) { showLibrary(false); return; }
        int index=0;
        for(int i=0;i<favorites.size();i++) if(selected!=null && favorites.get(i).pkg.equals(selected.pkg)) index=i;
        index=(index+direction+favorites.size())%favorites.size();
        selected=favorites.get(index); activeArt=selected.art; updateSelected(true);
        final FrameLayout card=favoriteCards.get(index);
        card.post(()->stripScroll.smoothScrollTo(Math.max(0,card.getLeft()-px(300)),0));
    }
    private void updateArtTitle() { if(artTitle!=null) artTitle.setText((art.isDemo()?"DriveLite Demo":Artwork.TITLES[activeArt<0?0:activeArt])+"  •  "+((activeArt<0?0:activeArt)+1)+" / "+Artwork.KEYS.length); }
    private void scheduleSlideshow() {
        handler.removeCallbacks(slideshow);
        if(resumed && !inLibrary && !popupOpen && prefs.getBoolean("slideshow",true)) handler.postDelayed(slideshow,25000);
    }
    private void showHome() {
        inLibrary=false; hideKeyboard();
        if(library!=null) library.setVisibility(View.GONE); home.setVisibility(View.VISIBLE); header.bringToFront(); wallpaper.library(false);
        gridToggle.setContentDescription("Buka semua aplikasi"); gridToggle.setSelected(false);
        activeArt=selected==null?0:selected.art; updateSelected(true);
        if(resumed) speed.start();
    }
    private void showLibrary(boolean focusSearch) {
        speed.stop(); handler.removeCallbacks(slideshow); wallpaper.finishTransition(); loadApps(); rebuildFavorites();
        if(library==null) buildLibrary();
        inLibrary=true; home.setVisibility(View.GONE); library.setVisibility(View.VISIBLE); header.bringToFront(); wallpaper.library(true);
        gridToggle.setContentDescription("Kembali ke beranda"); gridToggle.setSelected(true); fillGrid();
        if(focusSearch) { search.requestFocus(); search.postDelayed(()->((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).showSoftInput(search,InputMethodManager.SHOW_IMPLICIT),150); }
    }
    private void buildLibrary() {
        library=new FrameLayout(this); root.addView(library,new FrameLayout.LayoutParams(-1,-1));
        search=new EditText(this); search.setSingleLine(true); search.setTextColor(WHITE); search.setHintTextColor(0xFF9AAAB8); search.setHint("Cari aplikasi");
        search.setTextSize(android.util.TypedValue.COMPLEX_UNIT_PX,px(18)); search.setPadding(px(18),0,px(18),0);
        search.setBackground(rounded(0x18FFFFFF,23,0x20FFFFFF,1)); place(library,search,440,44,46,91);
        libraryCount=text("",12,MUTED,false); place(library,libraryCount,450,25,520,101);
        ScrollView scroll=new ScrollView(this); scroll.setOverScrollMode(View.OVER_SCROLL_NEVER); place(library,scroll,944,393,40,162);
        grid=new GridLayout(this); grid.setColumnCount(7); grid.setPadding(px(4),0,0,px(12)); scroll.addView(grid,new ScrollView.LayoutParams(-1,-2));
        TextView tip=text("Tekan tahan aplikasi untuk menambah favorit atau menghapus aplikasi",12,0xFFA4B0BD,false); place(library,tip,850,27,46,565);
        search.addTextChangedListener(new TextWatcher() { public void beforeTextChanged(CharSequence s,int st,int count,int after) {} public void onTextChanged(CharSequence s,int st,int before,int count) { fillGrid(); } public void afterTextChanged(Editable e) {} });
    }
    private void fillGrid() {
        if(grid==null) return;
        grid.removeAllViews(); String query=search.getText().toString().toLowerCase(Locale.ROOT).trim(); int count=0;
        for(Entry e:apps) {
            if(!query.isEmpty() && !e.name.toLowerCase(Locale.ROOT).contains(query) && !e.pkg.toLowerCase(Locale.ROOT).contains(query)) continue;
            FrameLayout card=new FrameLayout(this); card.setFocusable(true); card.setContentDescription("Buka "+e.name);
            FrameLayout tile=new FrameLayout(this); place(card,tile,88,116,21,2);
            Artwork.Cover image=art.new Cover(this,e.art); tile.addView(image,new FrameLayout.LayoutParams(-1,-1));
            FrameLayout badge=new FrameLayout(this); badge.setBackground(rounded(0xD90B1017,10,0x44FFFFFF,1)); place(tile,badge,24,24,61,88);
            ImageView symbol=new ImageView(this); symbol.setImageDrawable(icon(e)); place(badge,symbol,16,16,4,4);
            if(isFavorite(e)) { TextView star=text("★",13,0xFFF5DD9C,true); star.setGravity(Gravity.CENTER); star.setBackground(rounded(0xD90B1017,12,0,0)); place(tile,star,24,24,62,1); }
            TextView label=text(e.name,13,WHITE,true); label.setShadowLayer(px(2),0,px(1),0xFF081018); label.setSingleLine(true); label.setEllipsize(TextUtils.TruncateAt.END); label.setGravity(Gravity.CENTER); place(card,label,130,28,0,122);
            card.setOnClickListener(v->launch(e)); card.setOnLongClickListener(v-> { showActions(e); return true; });
            card.setOnFocusChangeListener((v,f)->card.setBackground(f?rounded(0x20FFFFFF,18,WHITE,1):null));
            GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=px(130); lp.height=px(160); lp.rightMargin=px(4); grid.addView(card,lp); count++;
        }
        libraryCount.setText(count+" aplikasi  •  "+favorites.size()+" favorit");
        if(count==0) { TextView empty=text("Tidak ada aplikasi yang cocok.",18,MUTED,false); GridLayout.LayoutParams lp=new GridLayout.LayoutParams(); lp.width=px(640); lp.height=px(65); lp.columnSpec=GridLayout.spec(0,7); grid.addView(empty,lp); }
    }
    private void showActions(Entry e) {
        popupOpen=true; handler.removeCallbacks(slideshow);
        AlertDialog dialog=new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle(e.name)
            .setItems(new String[]{isFavorite(e)?"★  Hapus dari favorit":"☆  Tambah favorit","Hapus aplikasi","Info aplikasi"},(d,which)-> {
                if(which==0) toggleFavorite(e);
                else if(which==1) uninstall(e);
                else startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+e.pkg)));
            }).create();
        dialog.setOnDismissListener(d-> { popupOpen=false; scheduleSlideshow(); }); dialog.show(); styleDialog(dialog);
    }
    private void uninstall(Entry e) {
        if(e.system) {
            AlertDialog dialog=new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("Aplikasi bawaan")
                .setMessage(e.name+" adalah aplikasi sistem. Penghapusan dari launcher tidak tersedia untuk aplikasi bawaan. Anda tetap dapat menghapusnya dari favorit.")
                .setPositiveButton("Mengerti",null).setNeutralButton("Info aplikasi",(d,w)->startSafely(new Intent(Settings.ACTION_APPLICATION_DETAILS_SETTINGS,Uri.parse("package:"+e.pkg)))).show(); styleDialog(dialog); return;
        }
        try { startActivityForResult(new Intent(Intent.ACTION_UNINSTALL_PACKAGE,Uri.parse("package:"+e.pkg)).putExtra(Intent.EXTRA_RETURN_RESULT,true),71); }
        catch(RuntimeException unavailable) { Toast.makeText(this,"Buka Info aplikasi untuk menghapus aplikasi ini.",Toast.LENGTH_LONG).show(); }
    }
    private void launcherSettings() {
        boolean auto=prefs.getBoolean("slideshow",true);
        AlertDialog dialog=new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("DriveLite")
            .setItems(new String[]{"Pengaturan Android","Wallpaper berikutnya",auto?"Matikan pergantian otomatis (25 detik)":"Aktifkan pergantian otomatis (25 detik)","Tentang launcher"},(d,which)-> {
                if(which==0) startSafely(new Intent(Settings.ACTION_SETTINGS));
                else if(which==1) { activeArt=(wallpaper.index()+1)%Artwork.KEYS.length; wallpaper.select(activeArt,true); updateArtTitle(); scheduleSlideshow(); }
                else if(which==2) { prefs.edit().putBoolean("slideshow",!auto).apply(); scheduleSlideshow(); }
                else about();
            }).show(); styleDialog(dialog);
    }
    private void about() {
        AlertDialog dialog=new AlertDialog.Builder(this,android.R.style.Theme_Material_Dialog_Alert).setTitle("DriveLite 2.2")
            .setMessage("Launcher native untuk head unit Anda.\n\nKecepatan berasal dari GPS dan tersedia setelah sinyal terkunci. Sentuh angka kecepatan untuk membuka pengaturan lokasi.\n\nSentuh kartu untuk memilih, lalu Buka untuk menjalankan. Tekan tahan aplikasi untuk mengatur favorit.\n\n"+(art.isDemo()?"Build demo dengan artwork geometris buatan proyek; gambar game tidak disertakan.":"Artwork game tersimpan offline. Sumber: PlayStation dan aset penerbit di Steam. Hak gambar milik pemilik masing-masing."))
            .setPositiveButton("Tutup",null).show(); styleDialog(dialog);
    }
    private void styleDialog(AlertDialog d) { if(d.getWindow()!=null) { d.getWindow().setBackgroundDrawable(rounded(0xFF17212C,18,0x30B2C5DD,1)); d.getWindow().setDimAmount(.65f); } }
    private void launch(Entry e) { hideKeyboard(); try { startActivity(e.intent); overridePendingTransition(0,0); } catch(RuntimeException failure) { Toast.makeText(this,"Aplikasi tidak dapat dibuka: "+e.name,Toast.LENGTH_SHORT).show(); } }
    private void startSafely(Intent i) { try { startActivity(i); } catch(RuntimeException unavailable) { Toast.makeText(this,"Menu ini tidak tersedia di perangkat.",Toast.LENGTH_SHORT).show(); } }
    private void hideKeyboard() { ((InputMethodManager)getSystemService(INPUT_METHOD_SERVICE)).hideSoftInputFromWindow(root.getWindowToken(),0); if(search!=null) search.clearFocus(); }
    private void invalidateCovers(View view) { if(view instanceof Artwork.Cover) view.invalidate(); if(view instanceof ViewGroup) for(int i=0;i<((ViewGroup)view).getChildCount();i++) invalidateCovers(((ViewGroup)view).getChildAt(i)); }
    @Override public void onBackPressed() {
        if(inLibrary) { showHome(); return; }
        ResolveInfo current=getPackageManager().resolveActivity(new Intent(Intent.ACTION_MAIN).addCategory(Intent.CATEGORY_HOME),PackageManager.MATCH_DEFAULT_ONLY);
        if(current==null || !getPackageName().equals(current.activityInfo.packageName)) super.onBackPressed();
    }

    private static final class Glyph extends Drawable {
        private final String kind;
        private final Paint p = new Paint(Paint.ANTI_ALIAS_FLAG);
        private final Path path = new Path();
        Glyph(String kind, int color) { this.kind = kind; p.setColor(color); p.setStrokeWidth(3); p.setStrokeCap(Paint.Cap.ROUND); p.setStrokeJoin(Paint.Join.ROUND); }
        @Override public void draw(Canvas c) {
            c.save(); c.translate(getBounds().left, getBounds().top); c.scale(getBounds().width()/64f, getBounds().height()/64f);
            p.setStyle(Paint.Style.STROKE); path.reset();
            switch (kind) {
                case "enter":
                    path.moveTo(49,15); path.lineTo(49,37); path.lineTo(16,37); path.moveTo(28,25); path.lineTo(16,37); path.lineTo(28,49); c.drawPath(path,p); break;
                case "play":
                    path.moveTo(23,12); path.lineTo(49,32); path.lineTo(23,52); path.close(); c.drawPath(path,p); break;
                case "search":
                    c.drawCircle(27,27,15,p); c.drawLine(39,39,54,54,p); break;
                case "grid":
                    for(int y=12;y<=40;y+=14) for(int x=12;x<=40;x+=14) c.drawRoundRect(new RectF(x,y,x+9,y+9),1,1,p); break;
                case "radio":
                    c.drawRoundRect(new RectF(9, 24, 55, 52), 5, 5, p); c.drawLine(14, 24, 49, 12, p);
                    c.drawCircle(23, 38, 7, p); c.drawLine(39, 34, 47, 34, p); c.drawLine(39, 42, 47, 42, p); break;
                case "bluetooth":
                    path.moveTo(29, 7); path.lineTo(43, 20); path.lineTo(21, 43); path.moveTo(29, 7);
                    path.lineTo(29, 57); path.lineTo(44, 43); path.lineTo(20, 20); c.drawPath(path, p); break;
                case "music":
                    c.drawLine(25, 42, 25, 17, p); c.drawLine(25, 17, 48, 11, p); c.drawLine(48, 11, 48, 36, p);
                    c.drawOval(new RectF(11, 39, 25, 51), p); c.drawOval(new RectF(34, 33, 48, 45), p); break;
                case "eq":
                    for (int x : new int[]{16, 32, 48}) c.drawLine(x, 11, x, 53, p);
                    c.drawRoundRect(new RectF(10, 21, 22, 29), 2, 2, p);
                    c.drawRoundRect(new RectF(26, 37, 38, 45), 2, 2, p);
                    c.drawRoundRect(new RectF(42, 16, 54, 24), 2, 2, p); break;
                case "folder":
                    path.moveTo(8, 22); path.lineTo(8, 15); path.lineTo(27, 15); path.lineTo(33, 22); path.lineTo(55, 22);
                    path.lineTo(55, 49); path.lineTo(8, 49); path.close(); c.drawPath(path, p); c.drawLine(8, 25, 55, 25, p); break;
                case "car": case "park": case "driving":
                    path.moveTo(13, 30); path.lineTo(19, 17); path.lineTo(46, 17); path.lineTo(52, 30); c.drawPath(path, p);
                    c.drawRoundRect(new RectF(9, 29, 55, 47), 5, 5, p); c.drawLine(15, 47, 15, 53, p); c.drawLine(49, 47, 49, 53, p);
                    c.drawLine(16, 37, 22, 37, p); c.drawLine(42, 37, 48, 37, p);
                    if(kind.equals("park")) { c.drawLine(29,21,29,27,p); c.drawRoundRect(new RectF(29,20,36,24),1,1,p); }
                    if(kind.equals("driving")) { c.drawLine(4,10,15,10,p); c.drawLine(49,10,60,10,p); c.drawLine(1,19,9,19,p); c.drawLine(55,19,63,19,p); }
                    break;
                case "globe":
                    c.drawCircle(32, 32, 22, p); c.drawOval(new RectF(22, 10, 42, 54), p); c.drawLine(10, 32, 54, 32, p);
                    c.drawOval(new RectF(12, 22, 52, 42), p); break;
                case "aux":
                    c.drawRoundRect(new RectF(21, 22, 43, 42), 4, 4, p); c.drawLine(26, 11, 26, 22, p);
                    c.drawLine(38, 11, 38, 22, p); c.drawLine(32, 42, 32, 54, p); break;
                default:
                    c.drawCircle(32, 32, 14, p); c.drawCircle(32, 32, 5, p);
                    for (int i=0; i<8; i++) { c.save(); c.rotate(i*45,32,32); c.drawLine(32,9,32,16,p); c.restore(); } break;
            }
            c.restore();
        }
        @Override public void setAlpha(int a) { p.setAlpha(a); }
        @Override public void setColorFilter(ColorFilter f) { p.setColorFilter(f); }
        @Override public int getOpacity() { return PixelFormat.TRANSLUCENT; }
    }
}
