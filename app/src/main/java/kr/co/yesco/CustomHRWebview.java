package kr.co.yesco;

import android.content.Context;
import android.content.Intent;
import android.content.pm.PackageInfo;
import android.content.pm.PackageManager;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.Build;
import android.util.AttributeSet;
import android.util.Log;
import android.webkit.CookieManager;
import android.webkit.CookieSyncManager;
import android.webkit.SslErrorHandler;
import android.webkit.URLUtil;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;

public class CustomHRWebview extends WebView {

    private static final String TAG = CustomHRWebview.class.getSimpleName();

    private static int WEBVIEW_RESOLUTION = 100;

    public CustomHRWebview(Context context) {
        super(context);
        init(context);
    }

    public CustomHRWebview(Context context, AttributeSet attr) {
        super(context, attr);
    }

    private void init(Context context) {
        // 자바스크립트 사용 허용
        this.getSettings().setJavaScriptEnabled(true);
        this.getSettings().setSupportMultipleWindows(true);
        this.getSettings().setJavaScriptCanOpenWindowsAutomatically(true);
        this.getSettings().setLoadsImagesAutomatically(true);
        this.getSettings().setLoadWithOverviewMode(true);
        // Zoom 설정
        this.getSettings().setTextZoom(WEBVIEW_RESOLUTION);
        this.getSettings().setUseWideViewPort(true);
        this.getSettings().setSupportZoom(true);
        this.getSettings().setBuiltInZoomControls(true);
        // Cache 설정
        this.getSettings().setCacheMode(WebSettings.LOAD_NO_CACHE);
        // 로컬스토리지 사용여부
        this.getSettings().setDomStorageEnabled(true);
        // 파일 접근 허용여부
        this.getSettings().setAllowFileAccess(true);
        // 사용자 문자열 설정
//        this.getSettings().setUserAgentString("App-android");
        // 인코딩 설정
        this.getSettings().setDefaultTextEncodingName("UTF-8");
        // 웹뷰 컨텐츠 url 접근
        this.getSettings().setAllowContentAccess(true);
        // 쿠키설정
        if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
            CookieSyncManager.createInstance(context);
        } else {
            CookieManager cookieManager = CookieManager.getInstance();
            cookieManager.setAcceptCookie(true);
            cookieManager.setAcceptThirdPartyCookies(this, true);
        }

        if (Build.VERSION.SDK_INT >= Build.VERSION_CODES.KITKAT) {
            this.setWebContentsDebuggingEnabled(false);
        }
    }

    public void loadWebview(String myurl) {
        this.setWebViewClient(new WebViewClient() {
            @Override
            public boolean shouldOverrideUrlLoading(WebView view, String url) {
                if( URLUtil.isNetworkUrl(url) ) {
                    return false;
                }

                Context context = view.getContext();
                PackageManager pm = context.getPackageManager();
                Intent intent;
                if (url.contains("bizx://")) {
                    try {
                        PackageInfo pi = pm.getPackageInfo("com.successfactors.successfactors", PackageManager.GET_ACTIVITIES);
                        intent = pm.getLaunchIntentForPackage("com.successfactors.successfactors");

                        intent = new Intent(Intent.ACTION_VIEW);
                        intent.setData(Uri.parse(url));
                        start(intent, view.getContext());
                    } catch(PackageManager.NameNotFoundException e) {
                        String marketUrl = "market://details?id=com.successfactors.successfactors";
                        Intent i = new Intent(Intent.ACTION_VIEW, Uri.parse(marketUrl));
                        start(i, view.getContext());
                    }

                    return true;
                }
                return true;
            }

            private boolean appInstalledOrNot(String uri, Context context) {
                PackageManager pm = context.getPackageManager();
                try {
                    pm.getPackageInfo("com.successfactors.successfactors", PackageManager.GET_ACTIVITIES);
                    return true;
                } catch (PackageManager.NameNotFoundException e) {
                }

                return false;
            }

            private boolean start(Intent intent, Context context) {
                context.startActivity(intent);
                return true;
            }

            private boolean gotoMarket(Intent intent, Context context) {
                Log.d(TAG, "Bizx package : " + intent.getPackage());
                context.startActivity(new Intent(Intent.ACTION_VIEW, Uri.parse("market://details?id=com.successfactors.successfactors")));
                return true;
            }

            @Override
            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
                handler.proceed();
            }

            @Override
            public void onPageFinished(WebView view, String url) {
                view.getSettings().setTextZoom(WEBVIEW_RESOLUTION);
                view.getSettings().setUseWideViewPort(true);
                view.getSettings().setSupportZoom(true);
                view.getSettings().setBuiltInZoomControls(true);
                view.getSettings().setJavaScriptEnabled(true);
                if (Build.VERSION.SDK_INT < Build.VERSION_CODES.LOLLIPOP) {
                    CookieSyncManager.getInstance().sync();
                } else {
                    CookieManager.getInstance().flush();
                }
                super.onPageFinished(view, url);
            }

            @Override
            public void onPageStarted(WebView view, String url, Bitmap favicon) {
                super.onPageStarted(view, url, favicon);
            }
        });

        Log.d(TAG, "load url is " + myurl);
        this.loadUrl(myurl);
    }
}
