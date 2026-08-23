package org.vietnom.app;

import android.app.Activity;
import android.content.res.AssetManager;
import android.media.*;
import android.os.Bundle;
import android.os.Build;
import android.graphics.Insets;
import android.view.WindowInsets;
import android.webkit.*;
import android.widget.Toast;
import android.speech.tts.TextToSpeech;
import com.k2fsa.sherpa.onnx.*;
import java.io.*;
import java.util.Locale;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicBoolean;
import org.json.*;
import android.speech.tts.UtteranceProgressListener;

public class MainActivity extends Activity {
  private static final String DIR="vits-piper-vi_VN-vais1000-medium";
  private WebView webView;
  private volatile OfflineTts tts;
  private volatile boolean loading;
  private AudioTrack track;
  private TextToSpeech systemTts;
  private volatile boolean systemTtsReady;
  private final ConcurrentHashMap<String,CountDownLatch> speechWaiters=new ConcurrentHashMap<>();
  private final ExecutorService speechExecutor=Executors.newSingleThreadExecutor();
  private final AtomicBoolean speaking=new AtomicBoolean(false);

  @Override public void onCreate(Bundle state){
    super.onCreate(state);
    webView=new WebView(this); setContentView(webView);
    webView.setOnApplyWindowInsetsListener((view,insets)->{
      int top,bottom;
      if(Build.VERSION.SDK_INT>=30){Insets bars=insets.getInsets(WindowInsets.Type.systemBars()|WindowInsets.Type.displayCutout());top=bars.top;bottom=bars.bottom;}
      else{top=insets.getSystemWindowInsetTop();bottom=insets.getSystemWindowInsetBottom();}
      view.setPadding(0,top,0,bottom);return insets;
    });
    WebSettings s=webView.getSettings(); s.setJavaScriptEnabled(true); s.setDomStorageEnabled(true); s.setAllowFileAccess(true);
    webView.setWebViewClient(new WebViewClient()); webView.setWebChromeClient(new WebChromeClient());
    webView.addJavascriptInterface(new SpeechBridge(),"AndroidTTS");
    webView.loadUrl("file:///android_asset/viet-nom.html");
    systemTts=new TextToSpeech(this,status->{if(status==TextToSpeech.SUCCESS){int result=systemTts.setLanguage(Locale.ENGLISH);if(result==TextToSpeech.LANG_MISSING_DATA||result==TextToSpeech.LANG_NOT_SUPPORTED)systemTts.setLanguage(Locale.getDefault());systemTts.setSpeechRate(.9f);systemTts.setOnUtteranceProgressListener(new UtteranceProgressListener(){public void onStart(String id){}public void onDone(String id){CountDownLatch l=speechWaiters.remove(id);if(l!=null)l.countDown();}public void onError(String id){onDone(id);}});systemTtsReady=true;}});
  }

  private void toast(String text){runOnUiThread(()->Toast.makeText(this,text,Toast.LENGTH_LONG).show());}

  private void copyAssets(String assetPath, File target) throws IOException {
    AssetManager am=getAssets(); String[] children=am.list(assetPath);
    if(children!=null && children.length>0){
      if(!target.exists()&&!target.mkdirs())throw new IOException("mkdir failed");
      for(String child:children)copyAssets(assetPath+"/"+child,new File(target,child));
    }else{
      if(target.exists()&&target.length()>0)return;
      File parent=target.getParentFile(); if(parent!=null&&!parent.exists())parent.mkdirs();
      try(InputStream in=am.open(assetPath,AssetManager.ACCESS_STREAMING);OutputStream out=new BufferedOutputStream(new FileOutputStream(target))){byte[] buffer=new byte[1024*256];int n;while((n=in.read(buffer))>0)out.write(buffer,0,n);}
    }
  }

  private synchronized boolean ensureTts(){
    if(tts!=null)return true; if(loading)return false; loading=true;
    try{
      File root=new File(getFilesDir(),DIR); copyAssets(DIR,root);
      OfflineTtsVitsModelConfig v=new OfflineTtsVitsModelConfig();
      v.setModel(new File(root,"vi_VN-vais1000-medium.onnx").getAbsolutePath());
      v.setTokens(new File(root,"tokens.txt").getAbsolutePath());
      v.setDataDir(new File(root,"espeak-ng-data").getAbsolutePath());
      v.setNoiseScale(.667f);v.setNoiseScaleW(.8f);v.setLengthScale(1.08f);
      OfflineTtsModelConfig m=new OfflineTtsModelConfig();m.setVits(v);m.setNumThreads(2);m.setProvider("cpu");
      OfflineTtsConfig c=new OfflineTtsConfig();c.setModel(m);c.setMaxNumSentences(1);c.setSilenceScale(.25f);
      tts=new OfflineTts(null,c); return true;
    }catch(Throwable e){toast("离线语音初始化失败："+e.getClass().getSimpleName());return false;}finally{loading=false;}
  }

  private synchronized void play(GeneratedAudio a){
    if(track!=null){try{track.stop();track.release();}catch(Exception ignored){}}
    float[] x=a.getSamples(); short[] pcm=new short[x.length];
    for(int i=0;i<x.length;i++){float sample=Math.max(-1f,Math.min(1f,x[i]));pcm[i]=(short)(sample*32767f);}
    int bytes=Math.max(2,pcm.length*2);
    track=new AudioTrack.Builder().setAudioAttributes(new AudioAttributes.Builder().setUsage(AudioAttributes.USAGE_MEDIA).setContentType(AudioAttributes.CONTENT_TYPE_SPEECH).build()).setAudioFormat(new AudioFormat.Builder().setEncoding(AudioFormat.ENCODING_PCM_16BIT).setSampleRate(a.getSampleRate()).setChannelMask(AudioFormat.CHANNEL_OUT_MONO).build()).setBufferSizeInBytes(bytes).setTransferMode(AudioTrack.MODE_STATIC).build();
    int written=track.write(pcm,0,pcm.length,AudioTrack.WRITE_BLOCKING);if(written<=0)throw new IllegalStateException("AudioTrack write failed: "+written);
    track.play();
    while(track.getPlayState()==AudioTrack.PLAYSTATE_PLAYING&&track.getPlaybackHeadPosition()<written-1){try{Thread.sleep(40);}catch(InterruptedException e){Thread.currentThread().interrupt();break;}}
    try{track.stop();track.release();}catch(Exception ignored){}track=null;
  }

  private void setSpeechUi(boolean busy,String label){
    if(webView==null)return;String safe=JSONObject.quote(label);
    runOnUiThread(()->webView.evaluateJavascript("window.setSpeechState&&window.setSpeechState("+busy+","+safe+")",null));
  }

  private String localizeForeignPronunciation(String text){
    return text
      .replaceAll("(?i)\\bCOVID(?:-?19)?\\b","cô vít mười chín")
      .replaceAll("(?i)\\bUSD\\b","đô la Mỹ")
      .replaceAll("(?i)\\bEU\\b","ê u")
      .replaceAll("(?i)\\bUSA\\b","Hoa Kỳ")
      .replaceAll("(?i)\\bUK\\b","Anh Quốc")
      .replaceAll("(?i)\\bUN\\b","Liên Hiệp Quốc")
      .replaceAll("(?i)\\bWHO\\b","Tổ chức Y tế Thế giới")
      .replaceAll("(?i)\\bAI\\b","ây ai")
      .replaceAll("(?i)\\bTV\\b","ti vi")
      .replaceAll("(?i)\\bGDP\\b","gi đi pi")
      .replaceAll("(?i)\\bUSB\\b","u ét bê");
  }

  public class SpeechBridge {
    @JavascriptInterface public void speakSegments(String json){
      if(!speaking.compareAndSet(false,true)){toast("正在朗读，请稍候");return;}
      setSpeechUi(true,tts==null?"正在准备语音…":"正在朗读…");
      speechExecutor.execute(()->{try{JSONArray segments=new JSONArray(json);for(int i=0;i<segments.length();i++){JSONObject segment=segments.getJSONObject(i);String text=segment.getString("text");if(text.trim().isEmpty())continue;if("foreign".equals(segment.getString("type"))&&systemTtsReady){String id="foreign-"+System.nanoTime();CountDownLatch latch=new CountDownLatch(1);speechWaiters.put(id,latch);runOnUiThread(()->systemTts.speak(text,TextToSpeech.QUEUE_FLUSH,null,id));latch.await(30,TimeUnit.SECONDS);}else{if(!ensureTts())return;setSpeechUi(true,"正在朗读…");GeneratedAudio audio=tts.generate(text,0,.93f);play(audio);}}}catch(Throwable e){toast("语音生成失败："+e.getClass().getSimpleName());}finally{speaking.set(false);setSpeechUi(false,"▶ 朗读越南语");}});
    }
    @JavascriptInterface public boolean isAvailable(){return true;}
  }

  @Override public void onBackPressed(){if(webView.canGoBack())webView.goBack();else super.onBackPressed();}
  @Override protected void onDestroy(){speechExecutor.shutdownNow();if(track!=null){try{track.stop();track.release();}catch(Exception ignored){}}if(tts!=null)tts.release();if(systemTts!=null){systemTts.stop();systemTts.shutdown();}webView.destroy();super.onDestroy();}
}

