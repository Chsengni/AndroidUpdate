package com.ch.androidupdate;

import android.annotation.SuppressLint;
import android.app.ProgressDialog;
import android.content.ComponentName;
import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Color;
import android.net.Uri;
import android.os.Environment;
import android.os.Handler;
import android.os.Message;
import android.os.Parcelable;
import android.support.v7.app.AlertDialog;
import android.support.v7.app.AppCompatActivity;
import android.os.Bundle;
import android.util.Log;
import android.util.Xml;
import android.view.View;
import android.widget.Button;
import android.widget.TextView;
import android.widget.Toast;

import com.github.johnpersano.supertoasts.library.Style;
import com.github.johnpersano.supertoasts.library.SuperActivityToast;
import com.github.johnpersano.supertoasts.library.utils.PaletteUtils;

import org.xmlpull.v1.XmlPullParser;

import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.net.URL;
import okhttp3.Call;
import okhttp3.Callback;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.Response;

/**
 * author xiaolin
 * date 2019-3-31
 **/
public class MainActivity extends AppCompatActivity {

    private static final int UPDATA_CLIENT = 0;
    private static final int GET_UNDATAINFO_ERROR =1 ;
    private static final int DOWN_ERROR = 2;
    private static final int LATESED = 3;
    private String TAG="MainActivity";
    private UpdateInfo info;
    private Button btn;
    private TextView version;
    private ProgressDialog progressDialog;

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        info = new UpdateInfo();
        version =findViewById(R.id.version);
        version.setText(getVersionName());
        findViewById(R.id.btn).setOnClickListener(new View.OnClickListener() {
            @Override
            public void onClick(View view) {
                new CheckVersionTask().run();
            }
        });

    }

    /*
     * 获取当前程序的版本号
     */
    public String getVersionName(){
        //获取packagemanager的实例
        PackageManager packageManager = getPackageManager();
        //getPackageName()是你当前类的包名，0代表是获取版本信息
        PackageInfo packInfo = null;
        try {
            packInfo = packageManager.getPackageInfo(getPackageName(), 0);

        } catch (PackageManager.NameNotFoundException e) {
            e.printStackTrace();
        }
        return packInfo.versionName;
    }

    /*
     * 用pull解析器解析服务器返回的xml文件 (xml封装了版本号)
     */
    public static UpdateInfo getUpdateInfo(InputStream is) throws Exception{
        XmlPullParser parser = Xml.newPullParser();
        parser.setInput(is, "utf-8");//设置解析的数据源
        int type = parser.getEventType();
        UpdateInfo info = new UpdateInfo();//实体
        while(type != XmlPullParser.END_DOCUMENT ){
            switch (type) {
                case XmlPullParser.START_TAG:
                    if("version".equals(parser.getName())){
                        info.setVersion(parser.nextText());	//获取版本号
                    }else if ("url".equals(parser.getName())){
                        info.setAPKurl(parser.nextText());	//获取要升级的APK文件
                    }else if ("description".equals(parser.getName())){
                        info.setDesc(parser.nextText());	//获取该文件的信息
                    }
                    break;
            }
            type = parser.next();
        }
        return info;
    }

    /*
     * 从服务器获取xml解析并进行比对版本号
     */
    public class CheckVersionTask implements Runnable{

        public void run() {
            try {
                //从资源文件获取服务器 地址
                String path = getResources().getString(R.string.serverurl);
                //包装成url的对象
                URL url = new URL(path);
                OkHttpClient okHttpClient = new OkHttpClient();
                final Request request = new Request.Builder()
                        .url(url)
                        .build();
                Call call = okHttpClient.newCall(request);
                call.enqueue(new Callback() {
                    @Override
                    public void onFailure(Call call, IOException e) {
                        Log.d(TAG, "onFailure: ");
                    }

                    @Override
                    public void onResponse(Call call, Response response) throws IOException {

                        InputStream is =response.body().byteStream();
                        try {
                            info =getUpdateInfo(is);
                        } catch (Exception e) {
                            e.printStackTrace();
                        }
                        if(info.getVersion().equals(getVersionName())){
                            Message msg = new Message();
                            msg.what = LATESED;
                            handler.sendMessage(msg);
                            Log.i(TAG,"版本号已经是最新的！");
                        }else{
                            Log.i(TAG,"发现新的版本,立即更新！");
                            Message msg = new Message();
                            msg.what = UPDATA_CLIENT;
                            Bundle bundle =new Bundle();
                            bundle.putString("APKurl",info.getAPKurl());
                            msg.setData(bundle);
                            handler.sendMessage(msg);
                        }
                    }
                });


            } catch (Exception e) {
                // 待处理
                Message msg = new Message();
                msg.what = GET_UNDATAINFO_ERROR;
                handler.sendMessage(msg);
                e.printStackTrace();

            }
        }
    }

    @SuppressLint("HandlerLeak")
    Handler handler = new Handler(){

        @Override
        public void handleMessage(Message msg) {
            // TODO Auto-generated method stub
            super.handleMessage(msg);
            ChooseMessage(msg);
        }
    };

    private void ChooseMessage(Message msg){
        switch (msg.what) {
            case LATESED:
                Toast.makeText(getApplicationContext(),"已经是最新的版本了！",Toast.LENGTH_SHORT).show();
                break;
            case UPDATA_CLIENT:
                //对话框通知用户升级程序
                final String APKUrl =msg.getData().getString("APKurl");
                SuperActivityToast.create(this,new Style(),Style.TYPE_BUTTON)
                        .setButtonText("更新")
                        .setButtonIconResource(R.mipmap.ic_launcher)
                        .setOnButtonClickListener("更新", null, new SuperActivityToast.OnButtonClickListener() {
                            @Override
                            public void onClick(View view, Parcelable token) {
                                openBrowser(info.getAPKurl());
                            }
                        })
                        .setProgressBarColor(Color.YELLOW)
                        .setText(info.getDesc())
                        .setDuration(Style.DURATION_LONG)
                        .setFrame(Style.FRAME_LOLLIPOP)
                        .setColor(PaletteUtils.getSolidColor(PaletteUtils.MATERIAL_BLUE))
                        .setAnimations(Style.ANIMATIONS_POP).show();
                break;
            case GET_UNDATAINFO_ERROR:
                //服务器超时
                Toast.makeText(getApplicationContext(), "获取服务器更新信息失败", Toast.LENGTH_SHORT).show();
                //LoginMain();
                break;
            case DOWN_ERROR:
                //下载apk失败
                Toast.makeText(getApplicationContext(), "下载新版本失败", Toast.LENGTH_SHORT).show();
                // LoginMain();
                break;
        }
    }

    /**
     * 调用第三方浏览器打开
     * @param url 要浏览的资源地址
     */
    public  void openBrowser(String url){
        Uri uri = Uri.parse(url);
        Intent intent = new Intent(Intent.ACTION_VIEW);
        intent.addCategory(Intent.CATEGORY_BROWSABLE);
        intent.setData(uri);
        startActivity(intent);
    }

}

