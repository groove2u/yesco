package kr.co.yesco;

import android.content.Context;
import android.os.Build;
import android.util.Log;
import android.widget.Toast;

import androidx.annotation.NonNull;
import androidx.biometric.BiometricManager;
import androidx.biometric.BiometricPrompt;
import androidx.core.content.ContextCompat;
import androidx.fragment.app.FragmentActivity;

import java.util.concurrent.Executor;

public class Biometric {

    private BiometricPrompt.PromptInfo promptInfo;
    private BiometricPrompt biometricPrompt;
    private Executor executor;
    private Context mContext;
    private FragmentActivity mActivity;

    public Biometric(FragmentActivity a,Context c){
        mContext = c;
        mActivity = a;
        this.BioRegist();
        this.setBioCallback();
    }

    public void BioRegist(){
        BiometricPrompt.PromptInfo.Builder promptBuilder = new BiometricPrompt.PromptInfo.Builder();

        promptBuilder.setTitle("생체 인증");
        promptBuilder.setSubtitle("생체인증을 통하여 로그인합니다.");
        promptBuilder.setNegativeButtonText("생체인증 불가능.");

        if(Build.VERSION.SDK_INT >= Build.VERSION_CODES.R){ //  안면인식 ap사용 android 11부터 지원
            promptBuilder.setAllowedAuthenticators(BiometricManager.Authenticators.BIOMETRIC_STRONG | BiometricManager.Authenticators.DEVICE_CREDENTIAL);
        }

        promptInfo = promptBuilder.build();
    }

    public void BioRun(){
            biometricPrompt.authenticate(promptInfo);
    }


    public void setBioCallback(){



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

    }
    public boolean isBioAvailable(){
        BiometricManager manager = BiometricManager.from(mContext);

        boolean isAvailable = false;
        switch (manager.canAuthenticate(BiometricManager.Authenticators.BIOMETRIC_WEAK)) {
            case BiometricManager.BIOMETRIC_SUCCESS: {   //  생체 인증 가능
                Log.d("MainActivity", "Application can authenticate with biometrics.");
                isAvailable = true;
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
