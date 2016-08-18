package com.modong.service.login;

import android.animation.ObjectAnimator;
import android.content.Intent;
import android.databinding.DataBindingUtil;
import android.os.Bundle;
import android.os.Handler;
import android.support.v7.app.AppCompatActivity;
import android.view.View;
import android.view.animation.Animation;
import android.view.animation.AnimationUtils;

import com.bumptech.glide.Glide;
import com.modong.service.BaseApplication;
import com.modong.service.MainActivity;
import com.modong.service.R;
import com.modong.service.databinding.ActivitySplashBinding;
import com.modong.service.db.WeiboDbExecutor;
import com.modong.service.model.AccessToken;
import com.modong.service.model.AccountBean;
import com.modong.service.model.SplashData;
import com.modong.service.model.WeiboUser;
import com.modong.service.retrofit.WeiboApiFactory;

import rx.Observable;
import rx.Subscriber;
import rx.Subscription;
import rx.android.schedulers.AndroidSchedulers;
import rx.functions.Action1;
import rx.schedulers.Schedulers;

public class SplashActivity extends AppCompatActivity implements View.OnClickListener {

    private ActivitySplashBinding mBinding;
    private Subscription mSubscribe;
    private int startTime = 3;

    private Handler mHandler = new Handler();
    private Runnable mRunnable = new Runnable() {
        @Override
        public void run() {
            startTime--;
            mBinding.countDownText.setText("Close: " + startTime);
            if (startTime == 1) {
                mHandler.postDelayed(new Runnable() {
                    @Override
                    public void run() {
                        ObjectAnimator.ofFloat(mBinding.countDownText, "alpha", 1.0f, 0.0f).setDuration(500).start();
                    }
                }, 500);
            }
            if (startTime == 0) {
                startMainActivity();
                return;
            }
            mHandler.postDelayed(this, 1000);
        }
    };

    @Override
    protected void onCreate(Bundle savedInstanceState) {
        super.onCreate(savedInstanceState);
        setContentView(R.layout.activity_splash);
        mBinding = DataBindingUtil.setContentView(this, R.layout.activity_splash);

        initEvents();
        fetchSplashImage();
    }

    private void initEvents() {
        mBinding.countDownText.setOnClickListener(this);
        mBinding.bottomLl.setAlpha(1.0f);
        mBinding.rootRl.setBackgroundResource(R.color.colorTransparentBlackBg);
        Animation animation = AnimationUtils.loadAnimation(SplashActivity.this, R.anim.anim_splash_bottom);
        mBinding.bottomLl.startAnimation(animation);
        mHandler.postDelayed(new Runnable() {
            @Override
            public void run() {
                hiddenNavigationBar();
            }
        }, 800);
        mHandler.postDelayed(mRunnable, 1000);
    }

    private void hiddenNavigationBar() {
        final View decorView = getWindow().getDecorView();
        int uiOptions = View.SYSTEM_UI_FLAG_LAYOUT_FULLSCREEN | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION;
        decorView.setSystemUiVisibility(uiOptions);

        decorView.setOnSystemUiVisibilityChangeListener (new View.OnSystemUiVisibilityChangeListener() {
            @Override
            public void onSystemUiVisibilityChange(int visibility) {
                if ((visibility & View.SYSTEM_UI_FLAG_FULLSCREEN) == 0) {
                    decorView.setSystemUiVisibility(View.SYSTEM_UI_FLAG_LAYOUT_STABLE
                            | View.SYSTEM_UI_FLAG_LAYOUT_HIDE_NAVIGATION | View.SYSTEM_UI_FLAG_HIDE_NAVIGATION
                            | View.SYSTEM_UI_FLAG_IMMERSIVE_STICKY);
                }
            }
        });
    }

    private void fetchSplashImage() {
        mSubscribe = WeiboApiFactory.createSplashApi().getSplashData()
                .subscribeOn(Schedulers.io())
                .observeOn(AndroidSchedulers.mainThread())
                .subscribe(new Action1<SplashData>() {
                    @Override
                    public void call(SplashData splashData) {
                        if (splashData != null) {
                            String image = splashData.getImg();
                            Glide.with(getApplicationContext()).load(image).crossFade().into(mBinding.splashImage);
                        }
                    }
                });
    }

    @Override
    public void onClick(View view) {
        switch (view.getId()) {
            case R.id.count_down_text:
                startMainActivity();
                break;
        }
    }

    private void startMainActivity() {
        stopTimer();
        Observable.create(new Observable.OnSubscribe<AccountBean>() {
            @Override
            public void call(Subscriber<? super AccountBean> subscriber) {
                AccountBean accountBean = null;
                AccessToken token = WeiboDbExecutor.getInstance().getTokenInfoFromDB();
                if (token != null) {
                    WeiboUser weiboUser = WeiboDbExecutor.getInstance().getWeiboUserFromDB(token.getUid());
                    if (weiboUser != null) {
                        accountBean = new AccountBean();
                        accountBean.setAccessToken(token);
                        accountBean.setUser(weiboUser);
                    }
                }
                subscriber.onNext(accountBean);
                subscriber.onCompleted();
            }
        })
        .subscribeOn(Schedulers.io())
        .observeOn(AndroidSchedulers.mainThread())
        .subscribe(new Action1<AccountBean>() {
        @Override
        public void call(AccountBean accountBean) {
                Intent intent = null;
                if (accountBean != null) {
                    BaseApplication.getInstance().setAccountBean(accountBean);
                    intent = new Intent(SplashActivity.this, MainActivity.class);
                } else {
                    intent = new Intent(SplashActivity.this, AuthorizeActivity.class);
                }
                startActivity(intent);
                finish();
            }
        });
    }

    private void stopTimer() {
        if (mHandler != null) {
            mHandler.removeCallbacks(mRunnable);
        }
    }

    @Override
    protected void onDestroy() {
        super.onDestroy();
        stopTimer();
        if (mSubscribe != null) {
            mSubscribe.unsubscribe();
        }
    }

}