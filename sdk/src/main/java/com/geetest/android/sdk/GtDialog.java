package com.geetest.android.sdk;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;
import java.util.Timer;
import java.util.TimerTask;
import java.lang.Runnable;
import java.net.Socket;
import java.net.InetSocketAddress;
import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;

import android.annotation.TargetApi;
import android.app.Activity;
import android.app.Dialog;
import android.content.Context;
import android.content.Intent;
import android.graphics.Bitmap;
import android.net.Uri;
import android.net.http.SslError;
import android.os.AsyncTask;
import android.os.Build;
import android.os.Bundle;
import android.util.Log;
import android.view.View;
import android.view.ViewGroup;
import android.view.ViewGroup.LayoutParams;
import android.view.Window;
import android.webkit.JavascriptInterface;
import android.webkit.SslErrorHandler;
import android.webkit.WebResourceError;
import android.webkit.WebResourceRequest;
import android.webkit.WebResourceResponse;
import android.webkit.WebSettings;
import android.webkit.WebView;
import android.webkit.WebViewClient;
import android.telephony.TelephonyManager;
import android.widget.Toast;

import org.json.JSONException;
import org.json.JSONObject;

/**
 * 验证对话框
 * 
 * @author dreamzsm@gmail.com
 * 
 */
public class GtDialog extends Dialog {

    private String baseURL = "https://static.geetest.com/static/appweb/app-index.html";
//    private String baseURL = "http://192.168.1.158:8721";

    protected static final String ACTIVITY_TAG="GtDialog";
    private  TelephonyManager tm;
    private  Context mContext;

    private String mParamsString;
    private String product = "embed";
    private String language = "zh-cn";
    private String mTitle = "";
    private Boolean debug = false;

    private Dialog mDialog = this;
    private int mWidth;
    private int mHeight;
    private int mTimeout = 10000;//默认10000ms
    private GTWebView webView;

    public Boolean isShowing = false;

    public void setBaseURL(String url) {
        this.baseURL = url;
    }

    public void setDebug(Boolean debug) {
        this.debug = debug;
    }

    public void setProduct(String product) {
        this.product = product;
    }

    //支持"zh-cn","zh-hk","zh-tw","ko-kr","ja-jp","en-us".默认"zh-cn".
    public void setLanguage(String lang) {
        this.language = lang;
    }

    //验证标题, 默认无标题, 不宜过长.
    public void setGTTitle(String title) { this.mTitle = title;}

    public void setTimeout(int timeout) {
        this.mTimeout = timeout;
    }

    @Override
    public void onDetachedFromWindow() {

        super.onDetachedFromWindow();
    }

    public void stopLoading() {
        webView.stopLoading();
    }

    public GtDialog (Context context, JSONObject params) {
        super(context);
        mContext = context;
        tm = (TelephonyManager)context.getSystemService(Context.TELEPHONY_SERVICE);
        mParamsString = unwrappingParameters(params);
        init(context);
    }

    private void init(Context context) {

        webView = new GTWebView(context);

        webView.setGtWebViewListener(new GTWebView.GtWebViewListener() {
            @Override
            public void gtCallReady(Boolean status) {
                if (gtListener != null) {
                    gtListener.gtCallReady(status);
                }
            }

            @Override
            public void gtError() {
                if (gtListener != null) {
                    gtError();
                }
            }
        });

        webView.addJavascriptInterface(new JSInterface(), "JSInterface");

        String pathUrl = getPathUrl(mParamsString);
        String gt_mobile_req_url = baseURL + pathUrl;
        Log.i("GtDialog", "url: " + gt_mobile_req_url);
        webView.loadUrl(gt_mobile_req_url);

        webView.buildLayer();
    }

    private String unwrappingParameters(JSONObject params) {
        Iterator<String> iter = params.keys();
        String paramString = "";
        while (iter.hasNext()) {
            String key = iter.next();
            try {
                if (iter.hasNext()) {
                    String comp = key + "=" + params.getString(key) + "&";
                    paramString = paramString + comp;
                }
                else {
                    String comp = key + "=" + params.getString(key);
                    paramString = paramString + comp;
                }
            } catch (JSONException e) {
                e.printStackTrace();
            }
        }
        return paramString;
    }

    private String getPathUrl(String paramsString) {

        mWidth = getDeviceWidth();
        mHeight = getDeviceHeight();
        float scale = getDeviceScale();

        return "?" + paramsString
//                + "&challenge=" + this.challenge
//                + "&success=" + (this.success ? 1 : 0)
                + "&imei=" + tm.getDeviceId()
                + "&mType=" + Build.MODEL
                + "&osType=" + "android"
                + "&osVerInt=" + Build.VERSION.RELEASE
                + "&gsdkVerCode=" + "2.16.11.11.2"
                + "&title=" + this.mTitle //验证标题，不宜过长
                + "&lang=" + this.language //支持"zh-cn","zh-hk","zh-tw","ko-kr","ja-jp","en-us".默认"zh-cn"
                + "&debug=" + this.debug
                + "&width=" + (int)(mWidth / scale + 1.5f);//1.5f: fix blank on the webview
    }

    private float getDeviceScale() {
        return getContext().getResources().getDisplayMetrics().density;
    }

    private int getDeviceWidth() {
        int height = DimenTool.getHeightPx(getContext());
        int width = DimenTool.getWidthPx(getContext());
        float scale = getDeviceScale();

        final int WIDTH = 290;

        if (height < width) {
            width = height * 3 / 4;
        }
        width = width * 4 / 5;
        if ((int)(width / scale + 0.5f) < WIDTH) {
            width = (int)((WIDTH - 0.5f) * scale);
        }
        return width;
    }

    private int getDeviceHeight() {
        int height = DimenTool.getHeightPx(getContext());
        int width = DimenTool.getWidthPx(getContext());
        float scale = getContext().getResources().getDisplayMetrics().density;

        final int HEIGHT = 500;

        return (int)(HEIGHT *  scale);
    }

    public interface GtListener {
        //通知native验证已准备完毕
        void gtCallReady(Boolean status); // true准备完成/false未准备完成
        //通知native关闭验证
        void gtCallClose();
        //通知javascript发生严重错误
        void  gtError();
        //通知native验证结果，并准备二次验证
        void gtResult(boolean success, String result);
    }

    private GtListener gtListener;

    public void setGtListener(GtListener listener) {
        gtListener = listener;
    }

    @Override
    protected void onCreate(Bundle savedInstanceState) {

        super.onCreate(savedInstanceState);
        requestWindowFeature(Window.FEATURE_NO_TITLE);

        setContentView(webView);

        final LayoutParams layoutParams = webView.getLayoutParams();

        layoutParams.width = mWidth;
        layoutParams.height = LayoutParams.WRAP_CONTENT;
        webView.setLayoutParams(layoutParams);
    }

    @Override
    public void show() {
        isShowing = true;
        super.show();
    }

    @Override
    public void dismiss() {
        isShowing = false;
        webView.stopLoading();
        webView.removeJavascriptInterface("JSInterface");
//        webView.destroy();
        super.dismiss();
    }

//    private class GtWebview extends WebView {
//
//        public GtWebview(Context context) {
//            super(context);
//
//            init(context);
//        }
//
//        private void init(Context context) {
//            WebSettings webSettings = this.getSettings();
//            webSettings.setJavaScriptEnabled(true);
//            webSettings.setLoadWithOverviewMode(true);
//            webSettings.setDomStorageEnabled(true);
//            webSettings.setDatabaseEnabled(true);
////            webSettings.setSupportZoom(true);
//            webSettings.setUseWideViewPort(true);
//            this.setOverScrollMode(View.OVER_SCROLL_NEVER);
//            this.setHorizontalScrollBarEnabled(false);
//            this.setVerticalScrollBarEnabled(false);
//            this.setWebViewClient(mWebViewClientBase);
//            this.onResume();
//        }
//
//        @Override
//        public void loadUrl(String url) {
//            domainTimer = new Timer();
//            TimerTask timerTask = new TimerTask() {
//                @Override
//                public void run() {
//                    ((Activity)context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (webView.getProgress() < 100) {
//                                webView.stopLoading();
//                                new PingTask().execute();
//
//                                domainTimer.cancel();
//                                domainTimer.purge();
//                            }
//                        }
//                    });
//                }
//            };
//            domainTimer.schedule(timerTask, 5000);
//
//            super.loadUrl(url);
//        }
//
//        public void loadIPUrl(String aIP) {
//            String mobile_ip_request_url = "http://" + aIP +"/static/appweb/app-index.html" + getPathUrl(mParamsString);
//            Log.i(ACTIVITY_TAG, "load url: " + mobile_ip_request_url);
//            final Map<String, String> additionalHttpHeaders = new HashMap<String, String>();
//            additionalHttpHeaders.put("Host", baseDomain);
//            webView.loadUrl(mobile_ip_request_url, additionalHttpHeaders);
//            Log.i(ACTIVITY_TAG, "webview did load ip url");
//            ipTimer = new Timer();
//            TimerTask timerTask1 = new TimerTask() {
//                @Override
//                public void run() {
//                    ((Activity)context).runOnUiThread(new Runnable() {
//                        @Override
//                        public void run() {
//                            if (webView.getProgress() < 100) {
//                                webView.stopLoading();
//                                mDialog.dismiss();
//                                if (gtListener != null) {
//                                    gtListener.gtCallReady(false);
//                                }
//                                ipTimer.cancel();
//                                ipTimer.purge();
//                            }
//                        }
//                    });
//                }
//            };
//            ipTimer.schedule(timerTask1, 10000);
//        }
//
//        private WebViewClientBase mWebViewClientBase = new WebViewClientBase();
//
//        private class WebViewClientBase extends WebViewClient {
//
//            @Override
//            public boolean shouldOverrideUrlLoading(WebView view, String url) {
//                // TODO Auto-generated method stub
//
//                Intent intent = new Intent(Intent.ACTION_VIEW);
//                intent.setData(Uri.parse(url));
//                context.startActivity(intent);
//
//                return true;
//            }
//
//            @Override
//            public void onPageStarted(final WebView view, String url, Bitmap favicon) {
//                // TODO Auto-generated method stub
//                Log.i(ACTIVITY_TAG, "webview did start");
//                super.onPageStarted(view, url, favicon);
//            }
//
//            @Override
//            public void onPageFinished(WebView view, String url) {
//                if (debug) {
//                    //当验证无法访问, 可能展示PROXY ERROR
//                    if (gtListener != null) {
//                        gtListener.gtCallReady(false);
//                    }
//                    mDialog.show();
//                }
//                // TODO Auto-generated method stub
//                Log.i(ACTIVITY_TAG, "webview did finish");
//                super.onPageFinished(view, url);
//            }
//
//            @Override
//            public void onReceivedError(WebView view, WebResourceRequest request, WebResourceError error) {
//                //
//                if (gtListener != null) {
//                    gtListener.gtCallReady(false);
//                }
//                super.onReceivedError(view, request, error);
//            }
//
//            @Override
//            public void onReceivedError(WebView view, int errorCode,
//                                                  String description, String failingUrl) {
//                if (gtListener != null) {
//                    gtListener.gtCallReady(false);
//                }
//                super.onReceivedError(view, errorCode, description, failingUrl);
//            }
//
////            @TargetApi(23)
////            @Override
////            public void onReceivedHttpError(
////                    WebView view, WebResourceRequest request, WebResourceResponse errorResponse) {
////                //
////                if (gtListener != null) {
////                    gtListener.gtCallReady(false);
////                }
////                mDialog.show();
////                super.onReceivedHttpError(view, request, errorResponse);
////            }
//
//            @Override
//            public void onReceivedSslError(WebView view, SslErrorHandler handler, SslError error) {
//                //handler.cancel(); 默认的处理方式，WebView变成空白页
//                //handler.process();接受证书
//                //handleMessage(Message msg); 其他处理
//                if (gtListener != null) {
//                    gtListener.gtError();
//                }
//            }
//
//        }
//
//    }

    public class JSInterface {

        @JavascriptInterface
        public void gtCallBack(String code, String result, String message) {
            int codeInt;
            try {
                codeInt = Integer.parseInt(code);
                if (codeInt == 1) {
                    dismiss();

                    if (gtListener != null) {
                        gtListener.gtResult(true, result);
                    }

                } else {
                    if (gtListener != null) {
                        gtListener.gtResult(false, result);
                    }
                    Toast.makeText(getContext(), "message:" + message,
                            Toast.LENGTH_LONG).show();
                }
            } catch (NumberFormatException e) {
                // TODO Auto-generated catch block
                e.printStackTrace();
            }

        }

        @JavascriptInterface
        public void gtCloseWindow() {
            dismiss();
            if (gtListener != null) {
                gtListener.gtCallClose();
            }
        }

        @JavascriptInterface
        public void gtReady() {

            ((Activity)mContext).runOnUiThread(new Runnable() {

                @Override
                public void run() {
                    mDialog.show();
                }
            });

            if (gtListener != null) {
                gtListener.gtCallReady(true);
            }
        }

        @JavascriptInterface
        public void gtError() {
            if (gtListener != null) {
                gtListener.gtError();
            }
        }

    }

}
