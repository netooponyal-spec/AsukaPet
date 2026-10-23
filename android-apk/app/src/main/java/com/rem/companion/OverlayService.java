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
 private static WindowManager wm;
 private static WindowManager.LayoutParams lp;
 private static WebView pet;
 private static android.widget.FrameLayout overlayRoot;
 private static volatile boolean running=false;
 private static volatile String webStatus="Не запущено";
 private static volatile String lastError="Нет";
 private static volatile int overlayWidth=750,overlayHeight=950,overlayX=20,overlayY=160;

 public static boolean isRunning(){return running;}
 public static String getDiagnostics(Context c){
  StringBuilder b=new StringBuilder();
  android.util.DisplayMetrics dm=c.getResources().getDisplayMetrics();
  b.append("СОСТОЯНИЕ «ПОВЕРХ ПРИЛОЖЕНИЙ»\n\n");
  b.append("Разрешение поверх других приложений: ").append(Settings.canDrawOverlays(c)?"ДА ✓":"НЕТ ✗").append("\n");
  b.append("Сервис OverlayService: ").append(running?"РАБОТАЕТ ✓":"НЕ ЗАПУЩЕН ✗").append("\n");
  b.append("WindowManager: ").append(wm!=null?"создан ✓":"нет ✗").append("\n");
  b.append("Overlay-контейнер: ").append(overlayRoot!=null?"создан ✓":"нет ✗");
  if(overlayRoot!=null)b.append(" / attached=").append(overlayRoot.isAttachedToWindow()?"ДА":"НЕТ");
  b.append("\n");
  b.append("WebView 3D: ").append(pet!=null?"создан ✓":"нет ✗");
  if(pet!=null){
   b.append(" / visible=").append(pet.getVisibility()==View.VISIBLE?"ДА":"НЕТ");
   b.append(" / shown=").append(pet.isShown()?"ДА":"НЕТ");
   b.append(" / attached=").append(pet.isAttachedToWindow()?"ДА":"НЕТ");
   b.append(" / size=").append(pet.getWidth()).append("×").append(pet.getHeight());
   b.append(" / alpha=").append(pet.getAlpha());
  }
  b.append("\n");
  b.append("WebView URL: ").append(pet!=null&&pet.getUrl()!=null?pet.getUrl():"нет").append("\n");
  b.append("WebView WebGL/модель: ").append(webStatus).append("\n");
  b.append("Размер окна: ").append(overlayWidth).append(" × ").append(overlayHeight).append(" px\n");
  b.append("Позиция: X=").append(overlayX).append(", Y=").append(overlayY).append("\n");
  b.append("Экран: ").append(dm.widthPixels).append(" × ").append(dm.heightPixels).append(" px\n");
  b.append("Плотность: ").append(dm.density).append("\n");
  b.append("Тип окна: ").append(Build.VERSION.SDK_INT>=26?"TYPE_APPLICATION_OVERLAY":"TYPE_PHONE").append("\n");
  b.append("Android: ").append(Build.VERSION.RELEASE).append(" (SDK ").append(Build.VERSION.SDK_INT).append(")\n");
  b.append("Устройство: ").append(Build.MANUFACTURER).append(" ").append(Build.MODEL).append("\n");
  b.append("Аппаратное ускорение WebView: включено ✓\n");
  b.append("Последняя ошибка: ").append(lastError);
  return b.toString();
 }

 @Override public void onCreate() {
  super.onCreate();
  running=true;
  webStatus="Запуск WebView…";
  lastError="Нет";
  if(Build.VERSION.SDK_INT>=26) ((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("rem","Рэм",NotificationManager.IMPORTANCE_LOW));
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"rem"):new Notification.Builder(this);
  Intent i=new Intent(this,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
  startForeground(101,nb.setContentTitle("Рэм активен").setContentText("3D-модель поверх приложений").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).build());
  if(!Settings.canDrawOverlays(this)){lastError="Нет разрешения SYSTEM_ALERT_WINDOW";stopSelf();return;}
  wm=(WindowManager)getSystemService(WINDOW_SERVICE);
  overlayRoot=new android.widget.FrameLayout(this);
  overlayRoot.setBackgroundColor(android.graphics.Color.TRANSPARENT);
  pet=new WebView(this);
  pet.setBackgroundColor(android.graphics.Color.TRANSPARENT);
  pet.setLayerType(View.LAYER_TYPE_HARDWARE,null);
  WebSettings s=pet.getSettings();
  s.setJavaScriptEnabled(true);s.setDomStorageEnabled(true);s.setLoadsImagesAutomatically(true);s.setAllowFileAccess(false);s.setMixedContentMode(WebSettings.MIXED_CONTENT_NEVER_ALLOW);
  pet.setWebChromeClient(new WebChromeClient(){
   @Override public boolean onConsoleMessage(ConsoleMessage m){
    String msg=m.message();
    android.util.Log.e("Rem3D","JS "+msg+" ("+m.sourceId()+":"+m.lineNumber()+")");
    if(msg.startsWith("REM_STATUS:")){webStatus=msg.substring(11).replace("\\n"," | ");}
    if(msg.startsWith("Rem 3D error")||msg.startsWith("Uncaught")||msg.startsWith("Error"))lastError=msg;
    return true;
   }
  });
  pet.setWebViewClient(new WebViewClient(){
   @Override public void onPageFinished(WebView view,String url){webStatus="HTML-сцена загружена; запускаю 3D…";}
   @Override public void onReceivedError(WebView view,WebResourceRequest req,WebResourceError err){lastError="Web error: "+err.getDescription();android.util.Log.e("Rem3D",lastError);if(req.isForMainFrame())Toast.makeText(OverlayService.this,"Ошибка 3D-сцены: "+err.getDescription(),Toast.LENGTH_LONG).show();}
   @Override public void onReceivedHttpError(WebView view,WebResourceRequest req,WebResourceResponse res){lastError="HTTP "+res.getStatusCode()+" "+req.getUrl();android.util.Log.e("Rem3D",lastError);}
  });
  int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  lp=new WindowManager.LayoutParams(overlayWidth,overlayHeight,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS|WindowManager.LayoutParams.FLAG_HARDWARE_ACCELERATED,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.START;lp.x=overlayX;lp.y=overlayY;
  overlayRoot.addView(pet,new android.widget.FrameLayout.LayoutParams(-1,-1));
  pet.setOnTouchListener(new View.OnTouchListener(){
   float downX,downY,startX,startY;boolean moved;
   @Override public boolean onTouch(View v,MotionEvent e){
    switch(e.getAction()){
     case MotionEvent.ACTION_DOWN:downX=e.getRawX();downY=e.getRawY();startX=lp.x;startY=lp.y;moved=false;return true;
     case MotionEvent.ACTION_MOVE:
      float dx=e.getRawX()-downX,dy=e.getRawY()-downY;if(Math.abs(dx)>6||Math.abs(dy)>6)moved=true;
      if(moved){lp.x=(int)(startX+dx);lp.y=(int)(startY+dy);overlayX=lp.x;overlayY=lp.y;try{wm.updateViewLayout(overlayRoot,lp);}catch(Exception ex){lastError="updateViewLayout: "+ex.getMessage();}}
      return true;
     case MotionEvent.ACTION_UP:
      if(!moved) Toast.makeText(OverlayService.this,"Рэм: "+webStatus,Toast.LENGTH_SHORT).show();
      return true;
    }
    return true;
   }
  });
  try{wm.addView(overlayRoot,lp);webStatus="Окно поверх приложений создано ✓";pet.loadDataWithBaseURL("https://netooponyal-spec.github.io/AsukaPet/",html(),"text/html","UTF-8",null);}catch(Exception e){lastError="addView/load: "+e.getMessage();running=false;stopSelf();}
 }

 private String html(){
  return "<!doctype html><html><head><meta charset='utf-8'><meta name='viewport' content='width=device-width,initial-scale=1,maximum-scale=1,user-scalable=no'>"+
   "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}canvas{display:block;width:100%;height:100%;touch-action:none}</style></head><body><canvas id='model'></canvas>"+
   "<script type='module'>const say=t=>console.log('REM_STATUS:'+t.replace(/\\n/g,' | '));async function loadRem(){try{say('Загружаю Three.js…');const THREE=await import('https://esm.sh/three@0.170.0');const {GLTFLoader}=await import('https://esm.sh/three@0.170.0/examples/jsm/loaders/GLTFLoader.js');const c=document.getElementById('model');const renderer=new THREE.WebGLRenderer({canvas:c,alpha:true,antialias:true,powerPreference:'high-performance'});renderer.setPixelRatio(Math.min(devicePixelRatio||1,2));renderer.setSize(Math.max(1,c.clientWidth),Math.max(1,c.clientHeight),false);renderer.setClearColor(0x000000,0);renderer.outputColorSpace=THREE.SRGBColorSpace;const scene=new THREE.Scene();const camera=new THREE.PerspectiveCamera(35,Math.max(1,c.clientWidth)/Math.max(1,c.clientHeight),0.01,1000);scene.add(new THREE.HemisphereLight(0xffffff,0x666677,2));const light=new THREE.DirectionalLight(0xffffff,3);light.position.set(4,7,6);scene.add(light);const fill=new THREE.DirectionalLight(0x9fd8ff,1.5);fill.position.set(-4,3,-4);scene.add(fill);say('Скачиваю GLB…');const url='https://netooponyal-spec.github.io/AsukaPet/models/rem-textured.glb';const gltf=await new GLTFLoader().loadAsync(url,undefined,e=>{if(e.total)say('Модель: '+Math.round(e.loaded/e.total*100)+'%');});const model=gltf.scene;scene.add(model);let meshes=0,missingMaps=[];model.traverse(o=>{if(o.isMesh){meshes++;o.frustumCulled=false;if(o.material){const ms=Array.isArray(o.material)?o.material:[o.material];ms.forEach(m=>{m.side=THREE.DoubleSide;if(!m.map)missingMaps.push(m);m.needsUpdate=true;});}}});if(!meshes)throw new Error('GLB загружен, но сеток не найдено');if(missingMaps.length){try{say('Подключаю текстуру…');const tex=await new THREE.TextureLoader().loadAsync('https://netooponyal-spec.github.io/AsukaPet/Rem_ALBShadowAlt.png');tex.colorSpace=THREE.SRGBColorSpace;tex.flipY=false;tex.needsUpdate=true;for(const m of missingMaps){m.map=tex;if(m.color)m.color.set(0xffffff);m.needsUpdate=true;}say('Текстура загружена');}catch(texErr){console.error('Rem albedo texture error',texErr);say('Текстура не скачалась: '+(texErr.message||String(texErr)).slice(0,90));}}model.updateMatrixWorld(true);let box=new THREE.Box3().setFromObject(model);let size=box.getSize(new THREE.Vector3()),center=box.getCenter(new THREE.Vector3()),max=Math.max(size.x,size.y,size.z);if(!Number.isFinite(max)||max<1e-6){let minX=Infinity,minY=Infinity,minZ=Infinity,maxX=-Infinity,maxY=-Infinity,maxZ=-Infinity;model.traverse(o=>{if(!o.isMesh||!o.geometry)return;const g=o.geometry;if(!g.boundingBox)g.computeBoundingBox();const b=g.boundingBox;if(!b||b.isEmpty())return;const corners=[new THREE.Vector3(b.min.x,b.min.y,b.min.z),new THREE.Vector3(b.min.x,b.min.y,b.max.z),new THREE.Vector3(b.min.x,b.max.y,b.min.z),new THREE.Vector3(b.min.x,b.max.y,b.max.z),new THREE.Vector3(b.max.x,b.min.y,b.min.z),new THREE.Vector3(b.max.x,b.min.y,b.max.z),new THREE.Vector3(b.max.x,b.max.y,b.min.z),new THREE.Vector3(b.max.x,b.max.y,b.max.z)];for(const v of corners){v.applyMatrix4(o.matrixWorld);if(!Number.isFinite(v.x)||!Number.isFinite(v.y)||!Number.isFinite(v.z))continue;minX=Math.min(minX,v.x);minY=Math.min(minY,v.y);minZ=Math.min(minZ,v.z);maxX=Math.max(maxX,v.x);maxY=Math.max(maxY,v.y);maxZ=Math.max(maxZ,v.z);}});if(Number.isFinite(minX)&&Number.isFinite(maxX)){box.min.set(minX,minY,minZ);box.max.set(maxX,maxY,maxZ);size=box.getSize(new THREE.Vector3());center=box.getCenter(new THREE.Vector3());max=Math.max(size.x,size.y,size.z);}}if(!Number.isFinite(max)||max<1e-6){say('Габариты не читаются; показываю без масштабирования');model.scale.setScalar(1);model.position.set(0,0,0);camera.position.set(0,0,5);camera.lookAt(0,0,0);camera.near=.001;camera.far=100000;camera.updateProjectionMatrix();}else{model.scale.setScalar(2.6/max);model.position.set(-center.x*model.scale.x,-center.y*model.scale.y,-center.z*model.scale.z);}model.updateMatrixWorld(true);let fitBox=new THREE.Box3().setFromObject(model),sphere=fitBox.getBoundingSphere(new THREE.Sphere());if(!Number.isFinite(sphere.radius)||sphere.radius<1e-6){sphere.center.set(0,0,0);sphere.radius=1;}camera.aspect=Math.max(1,c.clientWidth)/Math.max(1,c.clientHeight);const fov=THREE.MathUtils.degToRad(camera.fov);const hFov=2*Math.atan(Math.tan(fov/2)*camera.aspect);const dist=Math.max(.5,sphere.radius/Math.sin(Math.min(fov,hFov)/2)*1.35);camera.position.set(sphere.center.x,sphere.center.y+sphere.radius*.08,sphere.center.z+dist);camera.lookAt(sphere.center);camera.near=Math.max(.001,sphere.radius/1000);camera.far=Math.max(1000,sphere.radius*100);camera.updateProjectionMatrix();say('Рэм загружен ✓; сеток: '+meshes);function resize(){const w=Math.max(1,c.clientWidth),h=Math.max(1,c.clientHeight);renderer.setSize(w,h,false);camera.aspect=w/h;camera.updateProjectionMatrix();}window.addEventListener('resize',resize);function animate(){requestAnimationFrame(animate);renderer.render(scene,camera);}animate();}catch(e){say('Модель не загрузилась: '+(e.message||String(e)).slice(0,130));console.error('Rem model load error',e);}}loadRem();</script></body></html>";
 }
 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public void onDestroy(){running=false;webStatus="Сервис остановлен";if(wm!=null&&overlayRoot!=null)try{wm.removeView(overlayRoot);if(pet!=null)pet.destroy();}catch(Exception ignored){}wm=null;overlayRoot=null;pet=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
