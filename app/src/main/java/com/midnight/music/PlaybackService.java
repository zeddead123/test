package com.midnight.music;

import android.app.*;
import android.content.*;
import android.graphics.Color;
import android.media.*;
import android.media.session.MediaSession;
import android.media.session.PlaybackState;
import android.net.Uri;
import android.os.IBinder;
import org.json.*;
import java.util.*;

public class PlaybackService extends Service implements MediaPlayer.OnCompletionListener, MediaPlayer.OnPreparedListener, AudioManager.OnAudioFocusChangeListener {
    public static final String ACTION_STATE="com.midnight.music.STATE";
    public static final String PLAY="com.midnight.music.PLAY",TOGGLE="com.midnight.music.TOGGLE",NEXT="com.midnight.music.NEXT",PREV="com.midnight.music.PREV",STOP="com.midnight.music.STOP";
    private MediaPlayer player; private final ArrayList<Track> queue=new ArrayList<>(); private int index=-1; private String cookie=""; private MediaSession session; private AudioManager audio;
    public static class Track{String id,title,artist,url;Track(String i,String t,String a,String u){id=i;title=t;artist=a;url=u;}}
    @Override public void onCreate(){super.onCreate();createChannel();audio=(AudioManager)getSystemService(AUDIO_SERVICE);session=new MediaSession(this,"Midnight");session.setCallback(new MediaSession.Callback(){public void onPlay(){toggle();}public void onPause(){toggle();}public void onSkipToNext(){next();}public void onSkipToPrevious(){prev();}});session.setActive(true);}
    @Override public int onStartCommand(Intent i,int flags,int startId){String a=i==null?null:i.getAction();if(PLAY.equals(a)){loadQueue(i.getStringExtra("queue"),i.getIntExtra("index",0));cookie=i.getStringExtra("cookie");playCurrent();}else if(TOGGLE.equals(a))toggle();else if(NEXT.equals(a))next();else if(PREV.equals(a))prev();else if(STOP.equals(a)){stopSelf();}return START_STICKY;}
    private void loadQueue(String json,int idx){queue.clear();try{JSONArray arr=new JSONArray(json);for(int j=0;j<arr.length();j++){JSONObject o=arr.getJSONObject(j);queue.add(new Track(o.optString("id"),o.optString("title"),o.optString("artist"),o.optString("stream")));}index=Math.max(0,Math.min(idx,queue.size()-1));}catch(Exception e){index=-1;}}
    private void playCurrent(){if(index<0||index>=queue.size())return;releasePlayer();Track t=queue.get(index);player=new MediaPlayer();player.setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_MUSIC).build());player.setWakeMode(this,android.os.PowerManager.PARTIAL_WAKE_LOCK);player.setOnCompletionListener(this);player.setOnPreparedListener(this);try{Map<String,String> h=new HashMap<>();if(cookie!=null&&!cookie.isBlank())h.put("Cookie",cookie);player.setDataSource(this,Uri.parse(t.url),h);player.prepareAsync();startForeground(10,notification(t,false));sendState(false);}catch(Exception e){sendState(false);}}
    @Override public void onPrepared(MediaPlayer mp){int focus=audio.requestAudioFocus(this,AudioManager.STREAM_MUSIC,AudioManager.AUDIOFOCUS_GAIN);if(focus==AudioManager.AUDIOFOCUS_REQUEST_GRANTED){mp.start();update(true);}}
    private void toggle(){if(player==null){playCurrent();return;}if(player.isPlaying())player.pause();else player.start();update(player.isPlaying());}
    private void next(){if(queue.isEmpty())return;index=(index+1)%queue.size();playCurrent();}
    private void prev(){if(queue.isEmpty())return;if(player!=null&&player.getCurrentPosition()>4000){player.seekTo(0);return;}index=(index-1+queue.size())%queue.size();playCurrent();}
    @Override public void onCompletion(MediaPlayer mp){next();}
    private void update(boolean playing){Track t=index>=0&&index<queue.size()?queue.get(index):null;if(t==null)return;NotificationManager nm=getSystemService(NotificationManager.class);nm.notify(10,notification(t,playing));long actions=PlaybackState.ACTION_PLAY|PlaybackState.ACTION_PAUSE|PlaybackState.ACTION_SKIP_TO_NEXT|PlaybackState.ACTION_SKIP_TO_PREVIOUS;session.setPlaybackState(new PlaybackState.Builder().setActions(actions).setState(playing?PlaybackState.STATE_PLAYING:PlaybackState.STATE_PAUSED,player==null?0:player.getCurrentPosition(),1f).build());session.setMetadata(new android.media.MediaMetadata.Builder().putString(android.media.MediaMetadata.METADATA_KEY_TITLE,t.title).putString(android.media.MediaMetadata.METADATA_KEY_ARTIST,t.artist).build());sendState(playing);}
    private Notification notification(Track t,boolean playing){Intent open=new Intent(this,MainActivity.class);PendingIntent content=PendingIntent.getActivity(this,1,open,PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent pPrev=PendingIntent.getService(this,2,new Intent(this,PlaybackService.class).setAction(PREV),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent pToggle=PendingIntent.getService(this,3,new Intent(this,PlaybackService.class).setAction(TOGGLE),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);PendingIntent pNext=PendingIntent.getService(this,4,new Intent(this,PlaybackService.class).setAction(NEXT),PendingIntent.FLAG_IMMUTABLE|PendingIntent.FLAG_UPDATE_CURRENT);return new Notification.Builder(this,"midnight_playback").setSmallIcon(android.R.drawable.ic_media_play).setContentTitle(t.title).setContentText(t.artist).setContentIntent(content).setOngoing(playing).addAction(android.R.drawable.ic_media_previous,"Назад",pPrev).addAction(playing?android.R.drawable.ic_media_pause:android.R.drawable.ic_media_play,"Play",pToggle).addAction(android.R.drawable.ic_media_next,"Далее",pNext).setStyle(new Notification.MediaStyle().setMediaSession(session.getSessionToken()).setShowActionsInCompactView(0,1,2)).build();}
    private void sendState(boolean playing){Intent b=new Intent(ACTION_STATE).setPackage(getPackageName()).putExtra("playing",playing).putExtra("index",index);if(index>=0&&index<queue.size()){b.putExtra("title",queue.get(index).title);b.putExtra("artist",queue.get(index).artist);}sendBroadcast(b);}
    private void createChannel(){NotificationChannel c=new NotificationChannel("midnight_playback","Midnight playback",NotificationManager.IMPORTANCE_LOW);c.setDescription("Фоновое воспроизведение музыки");getSystemService(NotificationManager.class).createNotificationChannel(c);}
    private void releasePlayer(){if(player!=null){try{player.stop();}catch(Exception ignored){}player.release();player=null;}}
    @Override public void onAudioFocusChange(int f){if(player==null)return;if(f<=AudioManager.AUDIOFOCUS_LOSS_TRANSIENT&&player.isPlaying()){player.pause();update(false);}else if(f==AudioManager.AUDIOFOCUS_GAIN&&!player.isPlaying()){player.start();update(true);}}
    @Override public void onDestroy(){releasePlayer();if(session!=null){session.release();session=null;}audio.abandonAudioFocus(this);super.onDestroy();}
    @Override public IBinder onBind(Intent intent){return null;}
}
