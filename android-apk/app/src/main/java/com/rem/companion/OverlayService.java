package com.rem.companion;

import android.app.*;
import android.content.*;
import android.graphics.PixelFormat;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.webkit.*;
import android.widget.Toast;

public class OverlayService extends Service {
 private WindowManager wm;
 private WindowManager.LayoutParams lp;
 private WebView pet;

 @Override public void onCreate() {
  super.onCreate();
  if(Build.VERSION.SDK_INT>=26) {
   ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("rem","Рэм",NotificationManager.IMPORTANCE_LOW));
  }
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"rem"):new Notification.Builder(this);
  Intent i=new Intent(this,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
  startForeground(101,nb.setContentTitle("Рэм активен").setContentText("3D-модель поверх приложений").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).build());
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}

  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  pet=new WebView(this);
  pet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
  pet.setLayerType(View.LAYER_TYPE_HARDWARE,null);
  WebSettings s=pet.getSettings();
  s.setJavaScriptEnabled(true);
  s.setDomStorageEnabled(true);
  s.setLoadsImagesAutomatically(true);
  s.setAllowFileAccess(false);
  s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  pet.setWebChromeClient(new WebChromeClient());
  pet.setWebViewClient(new WebViewClient(){
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){
    if(req.isForMainFrame())Toast.makeText(OverlayService.this,"Не удалось открыть 3D-сцену. Проверь интернет.",Toast.LENGTH_LONG).show();
   }
  });
  int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  lp=new WindowManager.LayoutParams(220,280,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.START;lp.x=20;lp.y=160;

  pet.setOnTouchListener(new View.OnTouchListener(){
   float downX,downY,startX,startY;boolean moved;
   @Override public boolean onTouch(View v,MotionEvent e){
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=lp.x;startY=lp.y;moved=false;return true;
     case MotionEvent.ACTION_MOVE:
      float dx=e.getRawX()-downX,dy=e.getRawY()-downY;
      if(Math.abs(dx)>6||Math.abs(dy)>6)moved=true;
      if(moved){lp.x=(int)(startX+dx);lp.y=(int)(startY+dy);try{wm.updateViewLayout(pet,lp);}catch(Exception ignored){}}
      return true;
     case MotionEvent.ACTION_UP:if(!moved)Toast.makeText(OverlayService.this,"Рэм: 3D-модель загружается. Подожди немного при первом запуске.",Toast.LENGTH_SHORT).show();return true;
    }
    return true;
   }
  });
  try {
   wm.addView(pet,lp);
   pet.loadDataWithBaseURL("https://netooponyal-spec.github.io/AsukaPet/",html(),"text/html","UTF-8",null);
  }catch(Exception e){stopSelf();}
 }

 private String html(){
  return "<!doctype html><html><head><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
   "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}model-viewer{width:100%;height:100%;background:transparent;--poster-color:transparent;outline:none}</style>"+
   "<script type='module' src='https://unpkg.com/@google/model-viewer/dist/model-viewer.min.js'></script></head><body>"+
   "<model-viewer src='https://github.com/netooponyal-spec/AsukaPet/releases/download/Rem-textured1/rem-textured.glb' alt='Rem 3D model' "+
   "camera-orbit='0deg 75deg 2.4m' field-of-view='30deg' camera-target='0m 0.9m 0m' auto-rotate auto-rotate-delay='0' "+
   "rotation-per-second='12deg' interaction-prompt='none' shadow-intensity='0.6' exposure='1' loading='eager'></model-viewer>"+
   "</body></html>";
 }

 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public void onDestroy(){if(wm!=null&&pet!=null)try{wm.removeView(pet);pet.destroy();}catch(Exception ignored){}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}