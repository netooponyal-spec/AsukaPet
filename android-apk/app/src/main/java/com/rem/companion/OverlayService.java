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
 private static volatile int overlayWidth=300,overlayHeight=380,overlayX=20,overlayY=160;

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
   "<style>html,body,#v{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}#v{position:absolute;inset:0;background:rgba(20,20,28,.22)}#v canvas{position:absolute;inset:0;display:block;width:100%;height:100%;z-index:1}#s{display:none}</style></head><body><div id='v'></div><div id='s'>ОТЛАДКА РЭМ\\nСтатус: запускаю 3D…</div>"+
   "<script type='module'>"+
   "const status=document.getElementById('s');const say=t=>{status.textContent='ОТЛАДКА РЭМ\\n'+t;console.log('REM_STATUS:'+t.replace(/\\n/g,' | '));};"+
   "window.addEventListener('error',e=>say('Ошибка JS: '+(e.message||'неизвестно')));"+
   "window.addEventListener('unhandledrejection',e=>say('Ошибка загрузки: '+String(e.reason).slice(0,120)));"+
   "async function run(){try{say('Статус: загружаю 3D-движок…');const THREE=await import('https://esm.sh/three@0.170.0');const {GLTFLoader}=await import('https://esm.sh/three@0.170.0/examples/jsm/loaders/GLTFLoader.js');"+
   "const scene=new THREE.Scene();const camera=new THREE.PerspectiveCamera(35,Math.max(1,innerWidth)/Math.max(1,innerHeight),0.01,1000);const renderer=new THREE.WebGLRenderer({antialias:true,alpha:true,powerPreference:'high-performance'});renderer.setPixelRatio(Math.min(devicePixelRatio||1,2));renderer.setSize(Math.max(1,innerWidth),Math.max(1,innerHeight));renderer.setClearColor(0x000000,0);renderer.outputColorSpace=THREE.SRGBColorSpace;renderer.toneMapping=THREE.NoToneMapping;renderer.domElement.style.background='transparent';document.getElementById('v').appendChild(renderer.domElement);say('WebGL: работает\\nСтатус: готовлю сцену…');"+
   "scene.add(new THREE.HemisphereLight(0xffffff,0x666677,2));const light=new THREE.DirectionalLight(0xffffff,3);light.position.set(4,7,6);scene.add(light);const fill=new THREE.DirectionalLight(0x9fd8ff,1.5);fill.position.set(-4,3,-4);scene.add(fill);"+
   "say('Статус: загружаю модель Рэм (около 110 МБ)…');const url='https://netooponyal-spec.github.io/AsukaPet/models/rem-textured.glb';const gltf=await new GLTFLoader().loadAsync(url,undefined,e=>{if(e.total)say('Статус: загрузка модели '+Math.round(e.loaded/e.total*100)+'%\\nПолучено: '+Math.round(e.loaded/1048576)+' МБ из '+Math.round(e.total/1048576)+' МБ');});const model=gltf.scene;scene.add(model);let count=0;model.traverse(o=>{if(o.isMesh){count++;o.frustumCulled=false;if(o.material){const mats=Array.isArray(o.material)?o.material:[o.material];for(const m of mats){m.side=THREE.DoubleSide;m.visible=true;m.needsUpdate=true;}}}});if(!count)throw new Error('GLB загружен, но 3D-геометрия не найдена');let box=new THREE.Box3().setFromObject(model);if(box.isEmpty())throw new Error('GLB загружен, но габариты 3D-геометрии пустые');const size=box.getSize(new THREE.Vector3());const center=box.getCenter(new THREE.Vector3());const max=Math.max(size.x,size.y,size.z);model.scale.setScalar(2.8/max);model.position.set(-center.x*model.scale.x,-center.y*model.scale.y,-center.z*model.scale.z);box.setFromObject(model);const sphere=box.getBoundingSphere(new THREE.Sphere());camera.aspect=Math.max(1,innerWidth)/Math.max(1,innerHeight);camera.near=Math.max(.001,sphere.radius/1000);camera.far=Math.max(1000,sphere.radius*100);const vFov=THREE.MathUtils.degToRad(camera.fov);const hFov=2*Math.atan(Math.tan(vFov/2)*camera.aspect);const fitAngle=Math.max(.05,Math.min(vFov,hFov));const distance=Math.max(.5,sphere.radius/Math.sin(fitAngle/2)*1.35);camera.position.set(sphere.center.x,sphere.center.y+sphere.radius*.08,sphere.center.z+distance);camera.lookAt(sphere.center);camera.updateProjectionMatrix();model.traverse(o=>{if(o.isMesh){o.visible=true;o.frustumCulled=false;if(o.material){const mats=Array.isArray(o.material)?o.material:[o.material];for(const m of mats){m.visible=true;m.side=THREE.DoubleSide;if(m.opacity===0)m.opacity=1;m.needsUpdate=true;}}}});console.log('Rem model fit',JSON.stringify({meshes:count,bounds:{x:size.x,y:size.y,z:size.z},radius:sphere.radius,center:{x:sphere.center.x,y:sphere.center.y,z:sphere.center.z},cameraZ:camera.position.z,aspect:camera.aspect}));say('3D: загружено ✓\\nОбъектов: '+count+'\\nСтатус: отрисовка запускается…');"+
   "addEventListener('resize',()=>{camera.aspect=Math.max(1,innerWidth)/Math.max(1,innerHeight);camera.updateProjectionMatrix();renderer.setSize(Math.max(1,innerWidth),Math.max(1,innerHeight));});let frames=0;function animate(){requestAnimationFrame(animate);frames++;model.rotation.y+=.003;renderer.render(scene,camera);if(frames===30)say('3D: ОТРИСОВКА РАБОТАЕТ ✓\\nОбъектов: '+count+'\\nАнимация: работает');}animate();"+
   "}catch(e){say('ОШИБКА 3D: '+(e&&e.message?e.message:String(e)).slice(0,180)+'\\nПроверь интернет и доступ к модели.');console.error('Rem 3D error',e)}}run();</script></body></html>";
 }

 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public void onDestroy(){running=false;webStatus="Сервис остановлен";if(wm!=null&&overlayRoot!=null)try{wm.removeView(overlayRoot);if(pet!=null)pet.destroy();}catch(Exception ignored){}wm=null;overlayRoot=null;pet=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
