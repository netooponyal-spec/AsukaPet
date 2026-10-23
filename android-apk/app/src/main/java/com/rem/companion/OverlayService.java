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
  overlayRoot.setBackgroundColor(android.graphics.Color.argb(58, 35, 35, 48));
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
   "<style>html,body{margin:0;width:100%;height:100%;overflow:hidden;background:transparent}canvas{display:block;width:100%;height:100%;touch-action:none}#status{position:absolute;left:5px;top:5px;right:5px;color:white;background:rgba(0,0,0,.72);font:12px monospace;padding:5px;z-index:5;white-space:pre-wrap}</style></head><body><div id='status'>РЭМ WEBGL TEST\\nЗапуск…</div><canvas id='gl'></canvas>"+
   "<script>(function(){const st=document.getElementById('status'),c=document.getElementById('gl');function report(t){st.textContent='РЭМ — ТЕСТ WEBGL\\n'+t;console.log('REM_STATUS:'+t.replace(/\\n/g,' | '));}"+
   "try{const gl=c.getContext('webgl',{alpha:true,antialias:true,preserveDrawingBuffer:false})||c.getContext('experimental-webgl',{alpha:true,antialias:true});if(!gl){report('ОШИБКА: WebGL-контекст не создан');return;}"+
   "const vs='attribute vec3 p; uniform float a; void main(){float x=p.x*cos(a)-p.z*sin(a);float z=p.x*sin(a)+p.z*cos(a);gl_Position=vec4(x*0.78,p.y*0.78,z*0.25+0.35,1.0);}';"+
   "const fs='precision mediump float; uniform vec4 color; void main(){gl_FragColor=color;}';"+
   "function shader(type,src){const s=gl.createShader(type);gl.shaderSource(s,src);gl.compileShader(s);if(!gl.getShaderParameter(s,gl.COMPILE_STATUS))throw new Error(gl.getShaderInfoLog(s)||'shader compile failed');return s;}"+
   "const pr=gl.createProgram();gl.attachShader(pr,shader(gl.VERTEX_SHADER,vs));gl.attachShader(pr,shader(gl.FRAGMENT_SHADER,fs));gl.linkProgram(pr);if(!gl.getProgramParameter(pr,gl.LINK_STATUS))throw new Error(gl.getProgramInfoLog(pr)||'program link failed');gl.useProgram(pr);"+
   "const verts=new Float32Array([-0.5,-0.5,0.5,0.5,-0.5,0.5,0.5,0.5,0.5,-0.5,-0.5,0.5,0.5,0.5,0.5,-0.5,0.5,0.5,-0.5,-0.5,-0.5,-0.5,0.5,-0.5,0.5,0.5,-0.5,-0.5,-0.5,-0.5,0.5,0.5,-0.5,0.5,-0.5,-0.5,-0.5,-0.5,-0.5,-0.5,-0.5,0.5,-0.5,0.5,0.5,-0.5,-0.5,0.5,0.5,-0.5,0.5,0.5,0.5,0.5,-0.5,0.5,-0.5,-0.5,-0.5,-0.5,-0.5,0.5,0.5,-0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5,-0.5,0.5,0.5,0.5,-0.5,-0.5,-0.5,0.5,-0.5,0.5,0.5,0.5,0.5,0.5,-0.5,0.5]);"+
   "const buf=gl.createBuffer();gl.bindBuffer(gl.ARRAY_BUFFER,buf);gl.bufferData(gl.ARRAY_BUFFER,verts,gl.STATIC_DRAW);const pos=gl.getAttribLocation(pr,'p');gl.enableVertexAttribArray(pos);gl.vertexAttribPointer(pos,3,gl.FLOAT,false,0,0);const acol=gl.getUniformLocation(pr,'a'),ccol=gl.getUniformLocation(pr,'color');"+
   "function resize(){const d=Math.min(window.devicePixelRatio||1,2);c.width=Math.max(1,Math.floor(c.clientWidth*d));c.height=Math.max(1,Math.floor(c.clientHeight*d));gl.viewport(0,0,c.width,c.height);}window.addEventListener('resize',resize);resize();gl.enable(gl.DEPTH_TEST);gl.clearColor(0,0,0,0);"+
   "let a=0,frames=0;function draw(){resize();gl.clear(gl.COLOR_BUFFER_BIT|gl.DEPTH_BUFFER_BIT);gl.uniform1f(acol,a);gl.uniform4f(ccol,1,0,1,1);gl.drawArrays(gl.TRIANGLES,0,verts.length/3);a+=0.018;frames++;if(frames===1)report('WebGL-контекст: СОЗДАН\\nШейдеры: скомпилированы\\nКуб: рисуется напрямую без библиотек и интернета');requestAnimationFrame(draw);}draw();"+
   "}catch(e){report('ОШИБКА: '+(e.message||String(e)));console.error('Rem WebGL test error',e);}})();</script></body></html>";
 }
 @Override public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 @Override public void onDestroy(){running=false;webStatus="Сервис остановлен";if(wm!=null&&overlayRoot!=null)try{wm.removeView(overlayRoot);if(pet!=null)pet.destroy();}catch(Exception ignored){}wm=null;overlayRoot=null;pet=null;super.onDestroy();}
 @Override public IBinder onBind(Intent i){return null;}
}
