package kr.co.yesco;

import android.app.Notification;
import android.app.NotificationChannel;
import android.app.NotificationManager;
import android.app.PendingIntent;
import android.content.Context;
import android.content.Intent;
import android.net.Uri;
import android.os.Build;
import android.os.Environment;
import android.os.Handler;
import android.util.Base64;
import android.util.Log;
import android.webkit.JavascriptInterface;
import android.webkit.MimeTypeMap;
import android.widget.Toast;

import androidx.core.app.NotificationCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import java.io.File;
import java.io.FileOutputStream;
import java.io.IOException;
import java.text.DateFormat;
import java.util.Date;
import java.util.UUID;

import kr.co.yesco.util.AES256Chiper;
import kr.co.yesco.util.PreferenceUtil;

public class JavaScriptInterface {
    private FragmentActivity mActivity;
    private Context mContext;
    private CustomHRWebview mWebview;
    public JavaScriptInterface(FragmentActivity activity, Context context,CustomHRWebview webview ) {
        this.mContext = context;
        this.mActivity = activity;
        this.mWebview = webview;
    }

    @JavascriptInterface
    public void setUserInfo(String id,String pw) throws IOException {
        Log.d("skyblue","id="+id+",pw="+pw);


        PreferenceUtil  pUtil = new PreferenceUtil(mContext);
        try{
            String encId = AES256Chiper.AES_Encode(id);
            String encPw = AES256Chiper.AES_Encode(pw);
            pUtil.setStringPreferences("encId",encId);
            pUtil.setStringPreferences("encPw",encPw);

        }catch (Exception e){
            Log.d("skyblue","encrypt fail::::::::::::::::::::::::::::::::"+e.getMessage());
        }

        /*
        Log.d("skyblue","encId="+pUtil.getStringPreferences("encId")+",encPw="+pUtil.getStringPreferences("encPw"));
        try{
            Log.d("skyblue","id="+AES256Chiper.AES_Decode(pUtil.getStringPreferences("encId"))+",encPw="+AES256Chiper.AES_Decode(pUtil.getStringPreferences("encPw")));
        }catch (Exception e){
        }
         */
    }

    @JavascriptInterface
    public void bioCall() throws IOException {



        mActivity.runOnUiThread(new Runnable() {
            @Override
            public void run() {
                Biometric bio = new Biometric(mActivity,mContext,mWebview);
                bio.isBioAvailable();
            }
        });

    }
    @JavascriptInterface
    public void getBase64FromBlobData(String base64Data,String fileext,String mimeType) throws IOException {
        convertBase64StringToPdfAndStoreIt(base64Data,fileext,mimeType);
    }
    public static String getBase64StringFromBlobUrl(String blobUrl,String fileext,String mimeType) {
        if(blobUrl.startsWith("blob")){
            return "javascript: var xhr = new XMLHttpRequest();" +
                    "xhr.open('GET', '"+ blobUrl +"', true);" +
                    "xhr.setRequestHeader('Content-type','"+mimeType+"');" +
                    "xhr.responseType = 'blob';" +
                    "xhr.onload = function(e) {" +
                    "    if (this.status == 200) {" +
                    "        var blobPdf = this.response;" +
                    "        var reader = new FileReader();" +
                    "        reader.readAsDataURL(blobPdf);" +
                    "        reader.onloadend = function() {" +
                    "            base64data = reader.result;" +
                    "            Android.getBase64FromBlobData(base64data,'"+fileext+"','"+mimeType+"');" +
                    "        }" +
                    "    }" +
                    "};" +
                    "xhr.send();";
        }
        return "javascript: console.log('It is not a Blob URL');";
    }
    private void convertBase64StringToPdfAndStoreIt(String base64PDf,String fileext,String mimeType) throws IOException {
        final int notificationId = 1;
        String currentDateTime = DateFormat.getDateTimeInstance().format(new Date());

        String filename = UUID.randomUUID().toString()+fileext;
        final File dwldsPath = new File(Environment.getExternalStoragePublicDirectory(
                Environment.DIRECTORY_DOWNLOADS) + "/"+filename);
        byte[] pdfAsBytes = Base64.decode(base64PDf.replaceFirst("^data:"+mimeType+";base64,", ""), 0);
        FileOutputStream os;
        os = new FileOutputStream(dwldsPath, false);
        os.write(pdfAsBytes);
        os.flush();

        if (dwldsPath.exists()) {
            Intent intent = new Intent();
            intent.setAction(Intent.ACTION_VIEW);

            Uri apkURI;
            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.N)
            {// API 24 이상 일경우..
                String strpa = mContext.getApplicationContext().getPackageName();
                apkURI = FileProvider.getUriForFile(mContext,
                        mContext.getApplicationContext().getPackageName() + ".fileProvider", dwldsPath);
            }
            else
            {// API 24 미만 일경우..
                apkURI = Uri.fromFile(dwldsPath);
            }



//            Uri apkURI = FileProvider.getUriForFile(context,context.getApplicationContext().getPackageName() + ".provider", dwldsPath);
            intent.setDataAndType(apkURI, MimeTypeMap.getSingleton().getMimeTypeFromExtension(fileext.replace(".","")));
            intent.addFlags(Intent.FLAG_GRANT_READ_URI_PERMISSION);
            PendingIntent pendingIntent = PendingIntent.getActivity(mContext,1, intent, PendingIntent.FLAG_CANCEL_CURRENT);
            String CHANNEL_ID = "MYCHANNEL";
            final NotificationManager notificationManager = (NotificationManager) mContext.getSystemService(Context.NOTIFICATION_SERVICE);

            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.O) {
                NotificationChannel notificationChannel= new NotificationChannel(CHANNEL_ID,"name", NotificationManager.IMPORTANCE_LOW);
                Notification notification = new Notification.Builder(mContext,CHANNEL_ID)
                        .setContentText(filename+"파일이 다운로드 완료되었습니다.")
                        .setContentTitle("파일다운로드")
                        .setContentIntent(pendingIntent)
                        .setChannelId(CHANNEL_ID)
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        .build();
                if (notificationManager != null) {
                    notificationManager.createNotificationChannel(notificationChannel);
                    notificationManager.notify(notificationId, notification);
                }

            } else {
                NotificationCompat.Builder b = new NotificationCompat.Builder(mContext, CHANNEL_ID)
                        .setDefaults(NotificationCompat.DEFAULT_ALL)
                        .setWhen(System.currentTimeMillis())
                        .setSmallIcon(android.R.drawable.sym_action_chat)
                        //.setContentIntent(pendingIntent)
                        .setContentTitle("MY TITLE")
                        .setContentText("MY TEXT CONTENT");

                if (notificationManager != null) {
                    notificationManager.notify(notificationId, b.build());
                    Handler h = new Handler();
                    long delayInMilliseconds = 1000;
                    h.postDelayed(new Runnable() {
                        public void run() {
                            notificationManager.cancel(notificationId);
                        }
                    }, delayInMilliseconds);
                }
            }
        }
        Toast.makeText(mContext, "파일다운로드가 완료되었습니다.", Toast.LENGTH_SHORT).show();
    }
}
