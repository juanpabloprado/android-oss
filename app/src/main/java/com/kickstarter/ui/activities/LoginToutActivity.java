package com.kickstarter.ui.activities;

import android.app.Activity;
import android.content.Intent;
import android.os.Bundle;
import android.support.annotation.NonNull;
import android.support.annotation.Nullable;
import android.widget.Button;
import android.widget.TextView;

import com.facebook.AccessToken;
import com.kickstarter.R;
import com.kickstarter.libs.ActivityRequestCodes;
import com.kickstarter.libs.BaseActivity;
import com.kickstarter.libs.qualifiers.RequiresViewModel;
import com.kickstarter.libs.utils.ObjectUtils;
import com.kickstarter.libs.utils.TransitionUtils;
import com.kickstarter.libs.utils.ViewUtils;
import com.kickstarter.ui.IntentKey;
import com.kickstarter.ui.data.ActivityResult;
import com.kickstarter.ui.data.LoginReason;
import com.kickstarter.viewmodels.LoginToutViewModel;
import com.kickstarter.services.apiresponses.ErrorEnvelope;
import com.kickstarter.ui.toolbars.LoginToolbar;
import com.kickstarter.ui.views.LoginPopupMenu;

import java.util.Arrays;

import butterknife.Bind;
import butterknife.BindString;
import butterknife.ButterKnife;
import butterknife.OnClick;
import rx.Observable;
import rx.android.schedulers.AndroidSchedulers;

@RequiresViewModel(LoginToutViewModel.class)
public final class LoginToutActivity extends BaseActivity<LoginToutViewModel> {
  @Bind(R.id.disclaimer_text_view) TextView disclaimerTextView;
  @Bind(R.id.login_button) Button loginButton;
  @Bind(R.id.facebook_login_button) Button facebookButton;
  @Bind(R.id.sign_up_button) Button signupButton;
  @Bind(R.id.help_button) TextView helpButton;
  @Bind(R.id.login_toolbar) LoginToolbar loginToolbar;

  @BindString(R.string.login_tout_navbar_title) String loginOrSignUpString;
  @BindString(R.string.login_errors_unable_to_log_in) String unableToLoginString;
  @BindString(R.string.general_error_oops) String errorTitleString;
  @BindString(R.string.login_tout_errors_facebook_authorization_exception_message) String troubleLoggingInString;
  @BindString(R.string.login_tout_errors_facebook_authorization_exception_button) String tryAgainString;

  @Override
  protected void onCreate(@Nullable final Bundle savedInstanceState) {
    super.onCreate(savedInstanceState);

    setContentView(R.layout.login_tout_layout);
    ButterKnife.bind(this);
    loginToolbar.setTitle(loginOrSignUpString);

    viewModel.outputs.finishWithSuccessfulResult()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(__ -> finishWithSuccessfulResult());

    viewModel.outputs.startLogin()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::startLogin);

    viewModel.outputs.startSignup()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(__ -> startSignup());

    viewModel.errors.confirmFacebookSignupError()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(ur -> this.startFacebookConfirmationActivity(ur.first, ur.second));

    viewModel.errors.facebookAuthorizationError()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(__ -> ViewUtils.showDialog(this, errorTitleString, troubleLoggingInString, tryAgainString));

    errorMessages()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(ViewUtils.showToast(this));

    viewModel.errors.startTwoFactorChallenge()
      .compose(bindToLifecycle())
      .observeOn(AndroidSchedulers.mainThread())
      .subscribe(this::startTwoFactorChallenge);
  }

  private Observable<String> errorMessages() {
    return viewModel.errors.missingFacebookEmailError()
      .map(ObjectUtils.coalesceWith(unableToLoginString))
      .mergeWith(
        viewModel.errors.facebookInvalidAccessTokenError()
          .map(ObjectUtils.coalesceWith(unableToLoginString))
      );
  }

  @OnClick({R.id.disclaimer_text_view})
  public void disclaimerTextViewClick() {
    new LoginPopupMenu(this, helpButton).show();
  }

  @OnClick(R.id.facebook_login_button)
  public void facebookLoginClick() {
    viewModel.inputs.facebookLoginClick(this,
      Arrays.asList(getResources().getStringArray(R.array.facebook_permissions_array))
    );
  }

  @OnClick(R.id.login_button)
  public void loginButtonClick() {
    viewModel.inputs.loginClick();
  }

  @OnClick(R.id.sign_up_button)
  public void signupButtonClick() {
    viewModel.inputs.signupClick();
  }

  @Override
  protected void onActivityResult(final int requestCode, final int resultCode, final @Nullable Intent intent) {
    super.onActivityResult(requestCode, resultCode, intent);

    if (requestCode != ActivityRequestCodes.LOGIN_FLOW) {
      return;
    }

    setResult(resultCode, intent);
    finish();
  }

  private void finishWithSuccessfulResult() {
    setResult(Activity.RESULT_OK);
    finish();
  }

  public void startFacebookConfirmationActivity(final @NonNull ErrorEnvelope.FacebookUser facebookUser,
    final @NonNull LoginReason loginReason) {

    final Intent intent = new Intent(this, FacebookConfirmationActivity.class)
      .putExtra(IntentKey.LOGIN_REASON, loginReason)
      .putExtra(IntentKey.FACEBOOK_USER, facebookUser);

    startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW);
    TransitionUtils.slideInFromRight(this);
  }

  private void startLogin(final @NonNull LoginReason loginReason) {
    startActivityForLoginFlow(LoginActivity.class, loginReason);
  }

  private void startSignup() {
    final Intent intent = new Intent(this, SignupActivity.class);
    startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW);
    TransitionUtils.slideInFromRight(this);
  }

  private void startActivityForLoginFlow(final Class<? extends Activity> cls, final @NonNull LoginReason loginReason) {
    final Intent intent = new Intent(this, cls)
      .putExtra(IntentKey.LOGIN_REASON, loginReason);
    startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW);
    TransitionUtils.slideInFromRight(this);
  }

  public void startTwoFactorChallenge(final @NonNull LoginReason loginReason) {
    final Intent intent = new Intent(this, TwoFactorActivity.class)
      .putExtra(IntentKey.FACEBOOK_LOGIN, true)
      .putExtra(IntentKey.FACEBOOK_TOKEN, AccessToken.getCurrentAccessToken().getToken())
      .putExtra(IntentKey.LOGIN_REASON, loginReason);

    startActivityForResult(intent, ActivityRequestCodes.LOGIN_FLOW);
    TransitionUtils.slideInFromRight(this);
  }
}
