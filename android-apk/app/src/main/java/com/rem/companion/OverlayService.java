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
  lp=new WindowManager.LayoutParams(150,190,type,WindowManager.LayoutParams.FLAG_NOT_FOCUSABLE|WindowManager.LayoutParams.FLAG_LAYOUT_NO_LIMITS,PixelFormat.TRANSLUCENT);
  lp.gravity=Gravity.TOP|Gravity.START; lp.x=20;lp.y=180;
  try{wm.addView(pet,lp);}catch(Exception e){stopSelf();}
 }
 public int onStartCommand(Intent i,int f,int id){return START_STICKY;}
 public void onDestroy(){if(wm!=null&&pet!=null)try{wm.removeView(pet);}catch(Exception e){} super.onDestroy();}
 public IBinder onBind(Intent i){return null;}
 class Pet extends View{
  Paint p=new Paint(3); float dx,dy,sx,sy; boolean drag; long started=System.currentTimeMillis(),down;
  Pet(Context c){super(c);}
  protected void onDraw(Canvas c){float t=(System.currentTimeMillis()-started)/1000f,b=(float)Math.sin(t*2)*3,cx=getWidth()/2f,y=10+b;
   p.setColor(0x55303040);c.drawOval(cx-42,165,cx+42,180,p);
   p.setColor(Color.rgb(234,237,243));Path h=new Path();h.moveTo(cx-34,y+52);h.quadTo(cx-50,y+5,cx-8,y+7);h.quadTo(cx+30,y-2,cx+36,y+42);h.lineTo(cx+27,y+78);h.lineTo(cx-28,y+78);h.close();c.drawPath(h,p);
   p.setColor(Color.rgb(245,218,198));c.drawOval(cx-25,y+35,cx+25,y+91,p);
   p.setColor(Color.rgb(235,237,242));Path f=new Path();f.moveTo(cx-28,y+49);f.lineTo(cx-32,y+18);f.quadTo(cx,y-3,cx+29,y+20);f.lineTo(cx+13,y+47);f.lineTo(cx+3,y+35);f.lineTo(cx-9,y+51);f.lineTo(cx-17,y+37);f.close();c.drawPath(f,p);
   p.setColor(Color.rgb(50,80,137));c.drawOval(cx-18,y+57,cx-5,y+72,p);c.drawOval(cx+5,y+57,cx+18,y+72,p);
   p.setColor(Color.WHITE);c.drawCircle(cx-12,y+61,3,p);c.drawCircle(cx+12,y+61,3,p);
   p.setColor(Color.rgb(246,246,250));Path body=new Path();body.moveTo(cx-20,y+91);body.lineTo(cx+20,y+91);body.lineTo(cx+34,y+146);body.lineTo(cx-34,y+146);body.close();c.drawPath(body,p);
   p.setColor(Color.rgb(70,90,130));c.drawCircle(cx,y+107,6,p);p.setColor(Color.WHITE);p.setTextAlign(Paint.Align.CENTER);p.setTypeface(Typeface.DEFAULT_BOLD);p.setTextSize(13);c.drawText("РЭМ",cx,y+160,p);
   if(t%4.2f>3.98f){p.setColor(Color.rgb(245,218,198));c.drawRect(cx-20,y+57,cx-3,y+72,p);c.drawRect(cx+3,y+57,cx+20,y+72,p);p.setColor(Color.rgb(50,80,137));p.setStrokeWidth(2);c.drawLine(cx-17,y+65,cx-5,y+65,p);c.drawLine(cx+5,y+65,cx+17,y+65,p);}
   postInvalidateDelayed(40);
  }
  public boolean onTouchEvent(android.view.MotionEvent e){switch(e.getAction()){
   case MotionEvent.ACTION_DOWN:dx=e.getRawX();dy=e.getRawY();sx=lp.x;sy=lp.y;down=System.currentTimeMillis();drag=false;return true;
   case MotionEvent.ACTION_MOVE:float x=e.getRawX()-dx,y=e.getRawY()-dy;if(Math.abs(x)>5||Math.abs(y)>5)drag=true;if(drag){lp.x=(int)(sx+x);lp.y=(int)(sy+y);try{wm.updateViewLayout(this,lp);}catch(Exception ex){}}return true;
   case MotionEvent.ACTION_UP:if(!drag&&System.currentTimeMillis()-down<350)Toast.makeText(OverlayService.this,"Рэм: я здесь! Голосовой ИИ подключим следующим этапом.",Toast.LENGTH_SHORT).show();return true;
  }return true;}
 }
}