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
  pet.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onConsoleMessage(ConsoleMessage m){
    android.util.Log.e("Rem3D","JS "+m.message()+" ("+m.sourceId()+":"+m.lineNumber()+")");
    return true;
   }
  });
  pet.setWebViewClient(new WebViewClient(){
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){
    android.util.Log.e("Rem3D","Web error "+req.getUrl()+" : "+err.getDescription());
    if(req.isForMainFrame())Toast.makeText(OverlayService.this,"Ошибка 3D-сцены: "+err.getDescription(),Toast.LENGTH_LONG).show();
   }
   @Override public void onReceivedHttpError(WebView view,WebResourceRequest req,WebResourceResponse res){
    android.util.Log.e("Rem3D","HTTP "+res.getStatusCode()+" "+req.getUrl());
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
  return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
   "<style>html,body,#v{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}#s{position:fixed;left:4px;right:4px;bottom:4px;color:#fff;background:#222d;padding:5px;border-radius:6px;font:11px sans-serif;text-align:center;pointer-events:none}</style></head><body><div id='v'></div><div id='s'>Запускаю 3D…</div>"+
   "<script type='module'>"+
   "const status=document.getElementById('s');const say=t=>status.textContent=t;"+
   "window.addEventListener('error',e=>say('Ошибка JS: '+(e.message||'неизвестно')));"+
   "window.addEventListener('unhandledrejection',e=>say('Ошибка загрузки: '+String(e.reason).slice(0,120)));"+
   "async function run(){try{say('Загружаю 3D-движок…');const THREE=await import('https://esm.sh/three@0.170.0');const {GLTFLoader}=await import('https://esm.sh/three@0.170.0/examples/jsm/loaders/GLTFLoader.js');"+
   "const scene=new THREE.Scene();const camera=new THREE.PerspectiveCamera(35,innerWidth/innerHeight,0.001,10000);const renderer=new THREE.WebGLRenderer({antialias:true,alpha:true});renderer.setPixelRatio(Math.min(devicePixelRatio||1,2));renderer.setSize(innerWidth,innerHeight);renderer.setClearColor(0,0);renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.ACESFilmicToneMapping;renderer.toneMappingExposure=1.2;document.getElementById('v').appendChild(renderer.domElement);"+
   "scene.add(new THREE.HemisphereLight(0xffffff,0x666677,2));const light=new THREE.DirectionalLight(0xffffff,3);light.position.set(4,7,6);scene.add(light);const fill=new THREE.DirectionalLight(0x9fd8ff,1.5);fill.position.set(-4,3,-4);scene.add(fill);"+
   "say('Загружаю модель Рэм (около 110 МБ)…');const url='https://netooponyal-spec.github.io/AsukaPet/models/rem-textured.glb';const gltf=await new GLTFLoader().loadAsync(url,undefined,e=>{if(e.total)say('Загружаю модель: '+Math.round(e.loaded/e.total*100)+'%');});const model=gltf.scene;scene.add(model);let count=0;const box=new THREE.Box3().setFromObject(model);model.traverse(o=>{if(o.isMesh){count++;o.frustumCulled=false;if(o.geometry&&!o.geometry.boundingBox)o.geometry.computeBoundingBox();}});if(!count||box.isEmpty())throw new Error('GLB загружен, но 3D-геометрия не найдена');const size=box.getSize(new THREE.Vector3());const center=box.getCenter(new THREE.Vector3());const max=Math.max(size.x,size.y,size.z);model.position.sub(center);model.scale.setScalar(3/max);camera.position.set(0,0.75,3/2/Math.tan(THREE.MathUtils.degToRad(camera.fov/2))*1.35);camera.lookAt(0,0.45,0);say('Рэм загружена ✓');setTimeout(()=>status.style.display='none',2500);"+
   "addEventListener('resize',()=>{camera.aspect=innerWidth/innerHeight;camera.updateProjectionMatrix();renderer.setSize(innerWidth,innerHeight)});function animate(){requestAnimationFrame(animate);model.rotation.y+=0.003;renderer.render(scene,camera)}animate();"+
   "}catch(e){say('3D не загрузилась: '+(e&&e.message?e.message:String(e)).slice(0,160));console.error('Rem 3D error',e)}}run();</script></body></html>";
 }

 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public void onDestroy(){if(wm!=null&&pet!=null)try{wm.removeView(pet);pet.destroy();}catch(Exception ignored){}super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}