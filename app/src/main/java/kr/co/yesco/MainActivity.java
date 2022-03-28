package kr.co.yesco;

import android.Manifest;
import android.app.Activity;
import android.content.Context;
import android.content.Intent;
import android.content.SharedPreferences;
import android.content.pm.PackageManager;
import android.net.Uri;
import android.os.Build;
import android.os.Bundle;

import com.google.android.gms.tasks.OnCompleteListener;
import com.google.android.gms.tasks.Task;
import com.google.firebase.messaging.FirebaseMessaging;

import androidx.annotation.NonNull;

import android.os.Environment;
import android.os.Handler;
import android.preference.Preference;
import android.provider.MediaStore;
import android.text.TextUtils;
import android.util.Log;

import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.app.ActivityCompat;
import androidx.core.content.ContextCompat;
import androidx.core.content.FileProvider;
import androidx.fragment.app.FragmentActivity;

import android.view.WindowManager;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.DownloadListener;
import android.webkit.ValueCallback;
import android.webkit.WebChromeClient;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.widget.Toast;

import java.io.File;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.concurrent.Executor;

import kr.co.yesco.util.PreferenceUtil;


public class MainActivity extends FragmentActivity {
    private static final String TAG = MainActivity.class.getSimpleName();
    private CustomHRWebview customHRWebview;
    private SharedPreferences mPreferences;
    private Context mContext;

    private FragmentActivity mActivity;

    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricPrompt biometricPrompt;
    private Executor executor;


    // Storage Permissions variables
    private static final int REQUEST_EXTERNAL_STORAGE = 1;
    private static String[] PERMISSIONS_STORAGE = {
            Manifest.permission.READ_EXTERNAL_STORAGE,
            Manifest.permission.WRITE_EXTERNAL_STORAGE,
            Manifest.permission.CAMERA
    };

    private static final String TYPE_IMAGE = "image/*";
    private static final int INPUT_FILE_REQUEST_CODE = 1;
    private Uri cameraImageUri = null;

    private ValueCallback<Uri> mUploadMessage;
    private ValueCallback<Uri[]> mFilePathCallback;
    private String mCameraPhotoPath;
    private boolean isReload = true;

    //private final String initURL = "https://yeshrsapdev.yescoholdings.com:8443/sap/bc/ui5_ui5/sap/zui5_yescohr/index.html?sap-client=100&saml2=disabled";
    //private final String initURL = "https://devhrportal.yescoholdings.com:44300/hey/index.html?sap-client=100&saml2=disabled#/mobile";
//    private final String initURL = "http://tosky.co.kr:8080/test";
    //private final String initURL = "https://m.naver.com";
    private final String initURL = "https://devhrportal.yescoholdings.com:44300/hey/index.html?sap-client=300&saml2=disabled#/mobile";

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        setTheme(R.style.Theme_AppCompat);
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_main);
        mContext = MainActivity.this;
        mActivity = this;

        if(CellphoneRoutingCheck.checkSuperUser()) {
            android.os.Process.killProcess(android.os.Process.myPid());
        }

        verifyStoragePermissions(this);

        this.getToken();

        FirebaseMessaging.getInstance().subscribeToTopic("all")
                .addOnCompleteListener(new OnCompleteListener<Void>() {

                    @Override
                    public void onComplete(@NonNull Task<Void> task) {
                        String msg = "[구독 성공]";
                        if(!task.isSuccessful()) {
                            msg = "[구독 실패]";
                        }

                        Log.d(TAG, msg);
                    }
                });

        SharedPreferences sharedPreferences = getSharedPreferences("sFile1", MODE_PRIVATE);
        String token = sharedPreferences.getString("Token1", "");


        customHRWebview = (CustomHRWebview) findViewById(R.id.webview);

        customHRWebview.getSettings().setJavaScriptEnabled(true);
        customHRWebview.getSettings().setLoadWithOverviewMode(true);
        customHRWebview.getSettings().setUseWideViewPort(true);
        customHRWebview.getSettings().setBuiltInZoomControls(true);
        customHRWebview.getSettings().setDisplayZoomControls(false);

        customHRWebview.setWebChromeClient(new PQChromeClient());
        customHRWebview.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        //  customHRWebview.clearCache(true);

        customHRWebview.getSettings().setRenderPriority(WebSettings.RenderPriority.HIGH);
        customHRWebview.getSettings().setLayoutAlgorithm(WebSettings.LayoutAlgorithm.SINGLE_COLUMN);
        customHRWebview.getSettings().setEnableSmoothTransition(true);
        customHRWebview.setDownloadListener(new DownloadListener() {
            @Override
            public void onDownloadStart(String url, String userAgent, String contentDisposition, String mimeType, long contentLength) {
                String fileext="";
                if(mimeType.contains("application/pdf")){
                    fileext =".pdf";
                }else if (mimeType.contains("application/vnd.openxmlformats-officedocument.presentationml.presentation")){
                    fileext =".pptx";
                }else if (mimeType.contains("application/vnd.ms-powerpoint")){
                    fileext =".pptx";
                }else if (mimeType.contains("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet")){
                    fileext =".xlsx";
                }else if (mimeType.contains("application/vnd.ms-excel")){
                    fileext =".xls";
                }else if (mimeType.contains("image/jpeg")){
                    fileext =".jpg";
                }
/*
                try {
                    contentDisposition = URLDecoder.decode(contentDisposition,"UTF-8"); //디코딩
                } catch (UnsupportedEncodingException e) {
                }
                */
                String FileName = contentDisposition.replace("attachment; filename=", "");


                customHRWebview.loadUrl(JavaScriptInterface.getBase64StringFromBlobUrl(url,fileext,mimeType));
            }
        });

        customHRWebview.addJavascriptInterface(new JavaScriptInterface(mActivity,mContext,customHRWebview), "YescoApp");
        customHRWebview.getSettings().setPluginState(WebSettings.PluginState.ON);
        customHRWebview.clearCache(true);
        customHRWebview.clearHistory();

        clearCookies(mContext);

        //customHRWebview.loadWebview(initURL + "?token=" + token);

        //getWindow().setFlags(WindowManager.LayoutParams.FLAG_SECURE, WindowManager.LayoutParams.FLAG_SECURE);
    }

    private  void clearCookies(Context context)
    {

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP_MR1) {
            Log.d(TAG, "Using clearCookies code for API >=" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieManager.getInstance().removeAllCookies(null);
            CookieManager.getInstance().flush();
        } else
        {
            Log.d(TAG, "Using clearCookies code for API <" + String.valueOf(Build.VERSION_CODES.LOLLIPOP_MR1));
            CookieSyncManager cookieSyncMngr=CookieSyncManager.createInstance(context);
            cookieSyncMngr.startSync();
            CookieManager cookieManager=CookieManager.getInstance();
            cookieManager.removeAllCookie();
            cookieManager.removeSessionCookie();
            cookieSyncMngr.stopSync();
            cookieSyncMngr.sync();
        }
    }

    /**
     * More info this method can be found at
     * http://developer.android.com/training/camera/photobasics.html
     *
     * @return
     * @throws IOException
     */
    private File createImageFile() throws IOException {
        // Create an image file name
        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String imageFileName = "CAMERA_" + timeStamp;
        File storageDir = getExternalFilesDir(Environment.DIRECTORY_PICTURES);
        File imageFile = File.createTempFile(
                imageFileName,  /* prefix */
                ".jpg",  /* suffix */
                storageDir      /* directory */
        );

        return imageFile;
    }

    @Override
    protected void onActivityResult(int requestCode, int resultCode, Intent data) {
        Log.d("onActivityResult() ","requestCode = " + Integer.toString(requestCode) + ", resultCode = " + Integer.toString(resultCode));

        if (requestCode == INPUT_FILE_REQUEST_CODE && resultCode == RESULT_OK) {
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                if (mFilePathCallback == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri[] results = new Uri[]{getResultUri(data)};

                mFilePathCallback.onReceiveValue(results);
                mFilePathCallback = null;
            } else {
                if (mUploadMessage == null) {
                    super.onActivityResult(requestCode, resultCode, data);
                    return;
                }
                Uri result = getResultUri(data);

                Log.d(getClass().getName(), "openFileChooser : "+result);
                mUploadMessage.onReceiveValue(result);
                mUploadMessage = null;
            }
        } else {
            if (mFilePathCallback != null) mFilePathCallback.onReceiveValue(null);
            if (mUploadMessage != null) mUploadMessage.onReceiveValue(null);
            mFilePathCallback = null;
            mUploadMessage = null;
            super.onActivityResult(requestCode, resultCode, data);
        }
    }

    private Uri getResultUri(Intent data) {
        Uri result = null;
        if(data == null || TextUtils.isEmpty(data.getDataString())) {
            // If there is not data, then we may have taken a photo
            if(mCameraPhotoPath != null) {
                result = Uri.parse(mCameraPhotoPath);
            }
        } else {
            String filePath = "";
            if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.LOLLIPOP) {
                filePath = data.getDataString();
            } else {
                filePath = "file:" + RealPathUtil.getRealPath(this, data.getData());
            }
            result = Uri.parse(filePath);
        }

        return result;
    }

    public static void verifyStoragePermissions(Activity activity) {
        // Check if we have read or write permission
        int writePermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.WRITE_EXTERNAL_STORAGE);
        int readPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.READ_EXTERNAL_STORAGE);
        int cameraPermission = ActivityCompat.checkSelfPermission(activity, Manifest.permission.CAMERA);

        if (writePermission != PackageManager.PERMISSION_GRANTED || readPermission != PackageManager.PERMISSION_GRANTED || cameraPermission != PackageManager.PERMISSION_GRANTED) {
            // We don't have permission so prompt the user
            ActivityCompat.requestPermissions(
                    activity,
                    PERMISSIONS_STORAGE,
                    REQUEST_EXTERNAL_STORAGE
            );
        }
    }

    public void getToken() {
        FirebaseMessaging.getInstance().getToken()
                .addOnCompleteListener(new OnCompleteListener<String>() {
                    @Override
                    public void onComplete(@NonNull Task<String> task) {
                        if(!task.isSuccessful()) {
                            Log.w(TAG, "Fetching FCM registration token failed", task.getException());
                            return;
                        }

                        mPreferences = getSharedPreferences("sFile1", MODE_PRIVATE);
                        SharedPreferences.Editor editor = mPreferences.edit();

                        // Get new FCM registration token
                        String token = task.getResult();

                        PreferenceUtil pUtil = new PreferenceUtil(mContext);

                        pUtil.setStringPreferences("token",token);

                        // Log and toast
                        String msg = "[onCreate]Current Token is " + token;
                        Log.d(TAG, msg);
                    }
                });
    }

    @Override
    protected void onResume() {
        if(isReload) {
            customHRWebview.getSettings().setJavaScriptEnabled(true);
            customHRWebview.getSettings().setLoadWithOverviewMode(true);
            customHRWebview.getSettings().setUseWideViewPort(true);
            customHRWebview.getSettings().setBuiltInZoomControls(true);
            customHRWebview.getSettings().setDisplayZoomControls(false);
            customHRWebview.loadWebview(initURL);
            if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                CookieSyncManager.getInstance().startSync();
            }

            /*
            Handler handler = new Handler();
            handler.postDelayed(new Runnable() {
                @Override
                public void run() {
                    //생체 인증 테스트
                    Biometric bio = new Biometric(mActivity,mContext);
                    bio.isBioAvailable();
                }
            }, 3000); //딜레이 타임 조절
            */
          isReload = false;
        }

        super.onResume();
    }


    @Override
    protected void onPause() {
        super.onPause();

        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.getInstance().stopSync();
        }
    }

    @Override
    public void onBackPressed() {
        customHRWebview.loadUrl(initURL);
    }

    public class PQChromeClient extends WebChromeClient {
        @Override
        public void onCloseWindow(WebView w) {
            super.onCloseWindow(w);
            finish();
        }

        // For Android Version < 3.0
        public void openFileChooser(ValueCallback<Uri> uploadMsg) {
            //System.out.println("WebViewActivity OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU), n=1");
            mUploadMessage = uploadMsg;
            Intent intent = new Intent(Intent.ACTION_GET_CONTENT);
            intent.addCategory(Intent.CATEGORY_OPENABLE);
            intent.setType(TYPE_IMAGE);
            startActivityForResult(intent, INPUT_FILE_REQUEST_CODE);
        }

        // For 3.0 <= Android Version < 4.1
        public void openFileChooser(ValueCallback<Uri> uploadMsg, String acceptType) {
            //System.out.println("WebViewActivity 3<A<4.1, OS Version : " + Build.VERSION.SDK_INT + "\t openFC(VCU,aT), n=2");
            openFileChooser(uploadMsg, acceptType, "");
        }

        // For 4.1 <= Android Version < 5.0
        public void openFileChooser(ValueCallback<Uri> uploadFile, String acceptType, String capture) {
            Log.d(getClass().getName(), "openFileChooser : "+acceptType+"/"+capture);
            mUploadMessage = uploadFile;
            imageChooser();
        }

        // For Android Version 5.0+
        // Ref: https://github.com/GoogleChrome/chromium-webview-samples/blob/master/input-file-example/app/src/main/java/inputfilesample/android/chrome/google/com/inputfilesample/MainFragment.java
        public boolean onShowFileChooser(WebView webView,
                                         ValueCallback<Uri[]> filePathCallback, FileChooserParams fileChooserParams) {
            System.out.println("WebViewActivity A>5, OS Version : " + Build.VERSION.SDK_INT + "\t onSFC(WV,VCUB,FCP), n=3");
            if (mFilePathCallback != null) {
                mFilePathCallback.onReceiveValue(null);
            }
            mFilePathCallback = filePathCallback;
            imageChooser();
            return true;
        }

        private void imageChooser() {
            Intent takePictureIntent = new Intent(MediaStore.ACTION_IMAGE_CAPTURE);
            if (takePictureIntent.resolveActivity(MainActivity.this.getPackageManager()) != null) {
                // Create the File where the photo should go
                File photoFile = null;
                try {
                    photoFile = createImageFile();
                    takePictureIntent.putExtra("PhotoPath", mCameraPhotoPath);
                } catch (IOException ex) {
                    // Error occurred while creating the File
                    Log.e(getClass().getName(), "Unable to create Image File", ex);
                }

                // Continue only if the File was successfully created
                if (photoFile != null) {
                    mCameraPhotoPath = "file:"+photoFile.getAbsolutePath();
                    takePictureIntent.putExtra(MediaStore.EXTRA_OUTPUT, FileProvider.getUriForFile(getApplicationContext(), "kr.co.yesco.fileProvider", photoFile));
                } else {
                    takePictureIntent = null;
                }
            }

            Intent contentSelectionIntent = new Intent(Intent.ACTION_OPEN_DOCUMENT);
            contentSelectionIntent.addCategory(Intent.CATEGORY_OPENABLE);
            contentSelectionIntent.setType("*/*");
            String[] mimeTypes =
                    {"application/msword","application/vnd.openxmlformats-officedocument.wordprocessingml.document", // .doc & .docx
                            "application/vnd.ms-powerpoint","application/vnd.openxmlformats-officedocument.presentationml.presentation", // .ppt & .pptx
                            "application/vnd.ms-excel","application/vnd.openxmlformats-officedocument.spreadsheetml.sheet", // .xls & .xlsx
                            "text/plain",
                            "application/pdf",
                            "application/zip",
                            "image/*"
                    };
            contentSelectionIntent.putExtra(Intent.EXTRA_MIME_TYPES, mimeTypes);

            Intent[] intentArray;
            if(takePictureIntent != null) {
                intentArray = new Intent[]{takePictureIntent};
            } else {
                intentArray = new Intent[0];
            }

            Intent chooserIntent = new Intent(Intent.ACTION_CHOOSER);
            chooserIntent.putExtra(Intent.EXTRA_INTENT, contentSelectionIntent);
            chooserIntent.putExtra(Intent.EXTRA_TITLE, "File");
            chooserIntent.putExtra(Intent.EXTRA_INITIAL_INTENTS, intentArray);

            startActivityForResult(chooserIntent, INPUT_FILE_REQUEST_CODE);
        }

    }
}