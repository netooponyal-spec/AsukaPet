package com.rem.companion;
import android.app.*;
import android.content.*;
import android.graphics.*;
import android.os.*;
import android.provider.Settings;
import android.view.*;
import android.widget.Toast;

public class OverlayService extends Service {
 private WindowManager wm; private WindowManager.LayoutParams lp; private Pet pet;
 public void onCreate(){
  super.onCreate();
  if(Build.VERSION.SDK_INT>=26){((NotificationManager)getSystemService(NOTIFICATION_SERVICE)).createNotificationChannel(new NotificationChannel("rem","Рэм",NotificationManager.IMPORTANCE_LOW));}
  Notification.Builder nb=Build.VERSION.SDK_INT>=26?new Notification.Builder(this,"rem"):new Notification.Builder(this);
  Intent i=new Intent(this,MainActivity.class);
  PendingIntent pi=PendingIntent.getActivity(this,0,i,PendingIntent.FLAG_UPDATE_CURRENT|(Build.VERSION.SDK_INT>=23?PendingIntent.FLAG_IMMUTABLE:0));
  startForeground(101,nb.setContentTitle("Рэм активен").setContentText("Персонаж поверх приложений").setSmallIcon(android.R.drawable.ic_menu_view).setContentIntent(pi).build());
  if(!Settings.canDrawOverlays(this)){stopSelf();return;}
  wm=(WindowManager)getSystemService(WINDOW_SERVICE); pet=new Pet(this);
  int type=Build.VERSION.SDK_INT>=26?WindowManager.LayoutParams.TYPE_APPLICATION_OVERLAY:WindowManager.LayoutParams.TYPE_PHONE;
  lp=new WindowManager.LayoutParams(174,210,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.START; lp.x=20;lp.y=180;
  try{wm.addView(pet,lp);}catch(Exception e){stopSelf();}
 }
 public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 public void onDestroy(){if(wm!=null&&pet!=null)try{wm.removeView(pet);}catch(Exception e){} super.onDestroy();}
 public IBinder onBind(Intent i){return null;}

 class Pet extends View{
  Paint p=new Paint(3); float dx,dy,sx,sy; boolean drag; long started=System.currentTimeMillis(),down;
  Pet(Context c){super(c);}
  void fill(Canvas c,int color,Path path){p.setColor(color);c.drawPath(path,p);}
  protected void onDraw(Canvas c){
   float t=(System.currentTimeMillis()-started)/1000f,b=(float)Math.sin(t*2)*2,cx=getWidth()/2f,y=7+b;
   c.save(); c.scale(getWidth()/174f,getHeight()/210f);
   // Soft shadow
   p.setColor(0x44303040);c.drawOval(cx-42,190,cx+42,201,p);
   // Blue hair behind head, long side locks
   p.setColor(Color.rgb(70,190,232));
   Path hair=new Path();hair.moveTo(cx-39,y+52);hair.quadTo(cx-52,y+12,cx-24,y+8);hair.quadTo(cx,y-1,cx+25,y+9);hair.quadTo(cx+48,y+20,cx+38,y+57);hair.lineTo(cx+34,y+119);hair.lineTo(cx+22,y+130);hair.lineTo(cx+18,y+82);hair.lineTo(cx-18,y+82);hair.lineTo(cx-25,y+130);hair.lineTo(cx-38,y+116);hair.close();fill(c,Color.rgb(64,183,226),hair);
   // Face
   p.setColor(Color.rgb(255,224,207));c.drawOval(cx-27,y+27,cx+27,y+84,p);
   // White maid headband
   p.setColor(Color.rgb(248,248,252));c.drawRoundRect(cx-29,y+17,cx+29,y+29,7,7,p);
   p.setColor(Color.rgb(215,220,230));p.setStrokeWidth(2);c.drawLine(cx-23,y+22,cx+23,y+22,p);
   // Fringe with blue locks
   p.setColor(Color.rgb(70,190,232));Path fringe=new Path();fringe.moveTo(cx-29,y+40);fringe.lineTo(cx-31,y+16);fringe.quadTo(cx,y-2,cx+29,y+17);fringe.lineTo(cx+23,y+44);fringe.lineTo(cx+10,y+34);fringe.lineTo(cx+1,y+48);fringe.lineTo(cx-10,y+35);fringe.lineTo(cx-21,y+47);fringe.close();c.drawPath(fringe,p);
   // Pink flower accessory on the right side
   float fx=cx+27,fy=y+29;
   p.setColor(Color.rgb(255,160,190));for(int k=0;k<5;k++){double a=k*Math.PI*2/5;c.drawCircle(fx+(float)Math.cos(a)*5,fy+(float)Math.sin(a)*5,4.5f,p);}
   p.setColor(Color.rgb(255,221,105));c.drawCircle(fx,fy,3,p);
   // Eyes and highlights
   p.setColor(Color.rgb(48,92,143));c.drawOval(cx-19,y+48,cx-5,y+67,p);c.drawOval(cx+5,y+48,cx+19,y+67,p);
   p.setColor(Color.WHITE);c.drawCircle(cx-13,y+52,3.2f,p);c.drawCircle(cx+11,y+52,3.2f,p);
   p.setColor(Color.rgb(245,145,150));c.drawCircle(cx-24,y+68,3,p);c.drawCircle(cx+24,y+68,3,p);
   p.setColor(Color.rgb(180,90,100));p.setStyle(Paint.Style.STROKE);p.setStrokeWidth(1.6f);c.drawArc(cx-5,y+67,cx+6,y+75,10,160,false,p);p.setStyle(Paint.Style.FILL);
   // Black-and-white maid dress
   p.setColor(Color.rgb(40,43,57));Path dress=new Path();dress.moveTo(cx-19,y+82);dress.lineTo(cx+19,y+82);dress.lineTo(cx+35,y+151);dress.quadTo(cx,y+164,cx-35,y+151);dress.close();c.drawPath(dress,p);
   // White apron
   p.setColor(Color.rgb(250,250,252));Path apron=new Path();apron.moveTo(cx-12,y+91);apron.lineTo(cx+12,y+91);apron.lineTo(cx+25,y+147);apron.quadTo(cx,y+154,cx-25,y+147);apron.close();c.drawPath(apron,p);
   // Puff sleeves
   p.setColor(Color.rgb(248,248,252));c.drawCircle(cx-23,y+94,10,p);c.drawCircle(cx+23,y+94,10,p);
   // Bow and collar
   p.setColor(Color.rgb(65,180,225));Path bow=new Path();bow.moveTo(cx,y+89);bow.lineTo(cx-12,y+83);bow.lineTo(cx-10,y+96);bow.lineTo(cx,y+92);bow.lineTo(cx+10,y+96);bow.lineTo(cx+12,y+83);bow.close();c.drawPath(bow,p);
   p.setColor(Color.rgb(255,255,255));c.drawCircle(cx,y+91,3,p);
   // Legs and shoes
   p.setColor(Color.rgb(255,224,207));c.drawRoundRect(cx-18,y+151,cx-5,y+174,5,5,p);c.drawRoundRect(cx+5,y+151,cx+18,y+174,5,5,p);
   p.setColor(Color.rgb(35,38,49));c.drawRoundRect(cx-22,y+169,cx-3,y+180,5,5,p);c.drawRoundRect(cx+3,y+169,cx+22,y+180,5,5,p);
   // Small blue hair clips; no floating name label
   p.setColor(Color.rgb(245,245,250));c.drawCircle(cx-28,y+42,2,p);
   c.restore();
   postInvalidateDelayed(40);
  }
  public boolean onTouchEvent(android.view.MotionEvent e){switch(e.getAction()){
   case MotionEvent.ACTION_DOWN:dx=e.getRawX();dy=e.getRawY();sx=lp.x;sy=lp.y;down=System.currentTimeMillis();drag=false;return true;
   case MotionEvent.ACTION_MOVE:float x=e.getRawX()-dx,y=e.getRawY()-dy;if(Math.abs(x)>5||Math.abs(y)>5)drag=true;if(drag){lp.x=(int)(sx+x);lp.y=(int)(sy+y);try{wm.updateViewLayout(this,lp);}catch(Exception ex){}}return true;
   case MotionEvent.ACTION_UP:if(!drag&&System.currentTimeMillis()-down<350)Toast.makeText(OverlayService.this,"Рэм: я здесь! Голосовой ИИ подключим следующим этапом.",Toast.LENGTH_SHORT).show();return true;
  }return true;}
 }
}