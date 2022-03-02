package kr.co.yesco;

import android.content.Context;
import android.os.Build;
import android.os.Handler;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

import kr.co.yesco.util.AES256Chiper;
import kr.co.yesco.util.PreferenceUtil;

public class Biometric {

    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricPrompt biometricPrompt;
    private Executor executor;
    private Context mContext;
    private FragmentActivity mActivity;
    private CustomHRWebview mWebview;
    public Biometric(FragmentActivity a,Context c,CustomHRWebview webview){
        mContext = c;
        mActivity = a;
        mWebview = webview;
        this.setBioCallback();
        this.BioRegist();
    }

    public void BioRegist(){
    }

    public void BioRun(){
        try{
            BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder();

            promptBuilder.setTitle("생체 인증");
            promptBuilder.setSubtitle("생체인증을 통하여 로그인합니다.");
            //promptBuilder.setNegativeButtonText("대체수단을 사용해주세요.");

            if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){ //  안면인식 ap사용 android 11부터 지원
                promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_WEAK | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
                //  promptBuilder.setNegativeButtonText("생체인증 취소");
            }else{
                promptBuilder.setNegativeButtonText("생체인증 취소");

            }

            promptInfo = promptBuilder.build();
            biometricPrompt.authenticate(promptInfo);

        }catch (Exception e){

            Log.e("error","skyblue",e);
        }
    }


    public void setBioCallback(){

        try{
            executor = ContextCompat.getMainExecutor(mContext);
            biometricPrompt = new BiometricPrompt(mActivity,
                    executor, new BiometricPrompt.AuthenticationCallback() {
                @Override
                public void onAuthenticationError(int errorCode,
                                                  @NonNull CharSequence errString) {
                    super.onAuthenticationError(errorCode, errString);
                    Toast.makeText(mContext,
                            "생체 인증을 실행하던중 오류가 발생하였습니다..", Toast.LENGTH_SHORT)
                            .show();
                }
                @Override
                public void onAuthenticationSucceeded(
                        @NonNull BiometricPrompt.AuthenticationResult result) {
                    super.onAuthenticationSucceeded(result);

                    PreferenceUtil pUtil = new PreferenceUtil(mContext);
                    try{
                        String id = AES256Chiper.AES_Decode(pUtil.getStringPreferences("encId"));
                        String pw = AES256Chiper.AES_Decode(pUtil.getStringPreferences("encPw"));
                        Handler handler = new Handler();
                        handler.post(new Runnable() {
                            @Override
                            public void run() {
                                mWebview.loadUrl("javascript:BioCallback('"+id+"','"+pw+"');");
                            }
                        });


                    }catch (Exception e){
                        Log.e("skyblue",e.getMessage());
                    }

                    Toast.makeText(mContext,
                            "생체 인증에 성공하였습니다.", Toast.LENGTH_SHORT).show();
                }

                @Override
                public void onAuthenticationFailed() {
                    super.onAuthenticationFailed();
                    Toast.makeText(mContext, "생체 인증에 실패하였습니다.",
                            Toast.LENGTH_SHORT)
                            .show();
                }
            });
        }catch (Exception e){

            Log.e("error","skyblue",e);
        }


    }
    public boolean isBioAvailable(){
        BiometricManager manager = BiometricManager.from(mContext);

        boolean isAvailable = false;
        switch (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS: {   //  생체 인증 가능
                Log.d("MainActivity", "Application can authenticate with biometrics.");
                isAvailable = true;
                this.BioRun();
                break;
            }
            case BiometricManager.BIOMETRIC_ERROR_NO_HARDWARE: { //  기기에서 생체 인증을 지원하지 않는 경우
                Log.d("MainActivity", "Biometric facility is not available in this device.");
                Toast.makeText(mContext,"생체인증을 사용할수없습니다.",Toast.LENGTH_SHORT).show();
                break;
            }
            case BiometricManager.BIOMETRIC_ERROR_HW_UNAVAILABLE: {
                Log.d("MainActivity", "Biometric facility is currently not available");
                Toast.makeText(mContext,"생체인증을 사용할수없습니다.",Toast.LENGTH_SHORT).show();
                break;
            }
            case BiometricManager.BIOMETRIC_ERROR_NONE_ENROLLED: {   //  생체 인식 정보가 등록되지 않은 경우
                Log.d("MainActivity", "Any biometric credential is not added in this device.");
                Toast.makeText(mContext,"생체정보 등록 후 사용이 가능합니다.",Toast.LENGTH_SHORT).show();
                break;
            }
            default: {   //   기타 실패
                Log.d("MainActivity", "Fail Biometric facility");
                Toast.makeText(mContext,"생체인증을 사용할수없습니다.",Toast.LENGTH_SHORT).show();
                break;
            }
        }
        return isAvailable;
    }
}
