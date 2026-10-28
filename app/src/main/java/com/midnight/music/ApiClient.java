package com.midnight.music;

import android.content.Context;
import android.net.Uri;
import android.provider.OpenableColumns;
import org.json.JSONArray;
import org.json.JSONObject;
import java.io.*;
import java.net.*;
import java.nio.charset.StandardCharsets;

public class ApiClient {
    private final Context context;
    private String base;
    private String cookie;
    private String csrf;

    public ApiClient(Context c) {
        context=c.getApplicationContext();
        android.content.SharedPreferences p=context.getSharedPreferences("midnight",Context.MODE_PRIVATE);
        base=p.getString("base","http://177.3.223.91:4533");
        cookie=p.getString("cookie",""); csrf=p.getString("csrf","");
    }
    public String base(){return base;}
    public void setBase(String s){base=normalizeBase(s);save();}
    public boolean hasSession(){return !cookie.isEmpty()&&!csrf.isEmpty();}
    public void clear(){cookie="";csrf="";save();}
    private String normalizeBase(String s){s=(s==null?"":s.trim());while(s.endsWith("/"))s=s.substring(0,s.length()-1);return s;}
    private void save(){context.getSharedPreferences("midnight",Context.MODE_PRIVATE).edit().putString("base",base).putString("cookie",cookie).putString("csrf",csrf).apply();}
    private HttpURLConnection open(String path,String method) throws Exception {
        URL u=new URL(path.startsWith("http")?path:base+path);
        HttpURLConnection c=(HttpURLConnection)u.openConnection();c.setRequestMethod(method);c.setConnectTimeout(12000);c.setReadTimeout(120000);c.setUseCaches(false);
        c.setRequestProperty("Accept","application/json");if(!cookie.isEmpty())c.setRequestProperty("Cookie",cookie);if(!csrf.isEmpty()&&!method.equals("GET"))c.setRequestProperty("X-CSRF-Token",csrf);return c;
    }
    private String read(HttpURLConnection c) throws Exception {InputStream in=(c.getResponseCode()>=400?c.getErrorStream():c.getInputStream());if(in==null)return "";try(in;ByteArrayOutputStream out=new ByteArrayOutputStream()){in.transferTo(out);return out.toString(StandardCharsets.UTF_8);}}
    public JSONObject login(String server,String username,String password) throws Exception {
        setBase(server);HttpURLConnection c=open("/api/login","POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");JSONObject b=new JSONObject().put("username",username).put("password",password);try(OutputStream o=c.getOutputStream()){o.write(b.toString().getBytes(StandardCharsets.UTF_8));}
        String txt=read(c);if(c.getResponseCode()>=400)throw new IOException(error(txt));String set=c.getHeaderField("Set-Cookie");if(set!=null)cookie=set.split(";",2)[0];JSONObject r=new JSONObject(txt);csrf=r.optString("csrf","");save();return r;
    }
    public JSONObject me() throws Exception {return json("/api/me","GET",null);}
    public JSONObject library() throws Exception {return json("/api/library","GET",null);}
    public JSONObject json(String path,String method,JSONObject body) throws Exception {
        HttpURLConnection c=open(path,method);if(body!=null){c.setDoOutput(true);c.setRequestProperty("Content-Type","application/json");try(OutputStream o=c.getOutputStream()){o.write(body.toString().getBytes(StandardCharsets.UTF_8));}}
        String txt=read(c);if(c.getResponseCode()==401){clear();throw new SecurityException("Сессия истекла");}if(c.getResponseCode()>=400)throw new IOException(error(txt));return txt.isBlank()?new JSONObject():new JSONObject(txt);
    }
    public void upload(Uri uri) throws Exception {
        String name=fileName(uri);HttpURLConnection c=open("/api/upload","POST");c.setDoOutput(true);c.setRequestProperty("Content-Type","application/octet-stream");JSONObject meta=new JSONObject().put("name",name);c.setRequestProperty("X-Track-Meta",URLEncoder.encode(meta.toString(),StandardCharsets.UTF_8));try(InputStream in=context.getContentResolver().openInputStream(uri);OutputStream out=c.getOutputStream()){if(in==null)throw new IOException("Не удалось открыть файл");in.transferTo(out);}String txt=read(c);if(c.getResponseCode()>=400)throw new IOException(error(txt));
    }
    private String fileName(Uri uri){String n="track";try(android.database.Cursor cur=context.getContentResolver().query(uri,new String[]{OpenableColumns.DISPLAY_NAME},null,null,null)){if(cur!=null&&cur.moveToFirst())n=cur.getString(0);}catch(Exception ignored){}return n==null?"track":n;}
    private String error(String txt){try{return new JSONObject(txt).optString("error","Ошибка сервера");}catch(Exception e){return "Ошибка сервера";}}
    public String absolute(String p){if(p==null||p.isBlank())return "";return p.startsWith("http")?p:base+p;}
    public String cookie(){return cookie;}
}
