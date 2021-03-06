/*@Description Of Class
 * 
 * TwoFactorAuthController class is responsible for below listed task: 
 *    
 *    Generate Google Authentication  QR Code  
 *    Authenticate Google AuthKey
 *    Set two factor authentication via mobile
 *    Send OTP for two factor authentication
 *    Verifies two factor authentication vai OTP 
 *    Remove two factor authentication
 *    
 */

package com.bolenum.controller.user;

import java.io.IOException;
import java.net.URISyntaxException;
import java.util.Map;

import javax.validation.Valid;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bolenum.constant.UrlConstant;
import com.bolenum.dto.common.LoginForm;
import com.bolenum.enums.TwoFactorAuthOption;
import com.bolenum.exceptions.InvalidOtpException;
import com.bolenum.model.AuthenticationToken;
import com.bolenum.model.OTP;
import com.bolenum.model.User;
import com.bolenum.services.common.AuthService;
import com.bolenum.services.common.LocaleService;
import com.bolenum.services.user.TwoFactorAuthService;
import com.bolenum.services.user.UserService;
import com.bolenum.util.GenericUtils;
import com.bolenum.util.ResponseHandler;
import com.google.zxing.WriterException;

/**
 * 
 * @author Vishal Kumar
 * @date 26-sep-2017
 *
 */
@RestController
@RequestMapping(value = UrlConstant.BASE_USER_URI_V1)
public class TwoFactorAuthController {

	@Autowired
	private TwoFactorAuthService twoFactorAuthService;
	@Autowired
	private UserService userService;
	@Autowired
	private LocaleService localeService;
	@Autowired
	private AuthService authService;
	
	/**@Description Use generate Google Authentication  QR Code
	 * 
	 * @throws URISyntaxException
	 * @throws WriterException
	 * @throws IOException
	 * @returns QR code
	 */
	@Secured("ROLE_USER")
	@RequestMapping(value = UrlConstant.GEN_GOOGLE_AUTH_QR, method = RequestMethod.POST)
	ResponseEntity<Object> generateGoogleAuthQr() throws URISyntaxException, WriterException, IOException {
		User user = GenericUtils.getLoggedInUser();
		Map<String, String> response = twoFactorAuthService.qrCodeGeneration(user);
		if (response != null) {
			return ResponseHandler.response(HttpStatus.OK, false,
					localeService.getMessage("tfa.qr.code.generation.success"), response);
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.qr.code.generation.failure"), null);
		}
	}

	/**@Description use to authenticate Google AuthKey
	 * 
	 * @param secret
	 * @return authentication.success OR authentication.failure
	 */
	@Secured("ROLE_USER")
	@RequestMapping(value = UrlConstant.VERIFY_GOOGLE_AUTH_KEY, method = RequestMethod.PUT)
	ResponseEntity<Object> authenticateGoogleAuthKey(@RequestParam("secret") String secret) {
		User user = GenericUtils.getLoggedInUser();
		boolean authResponse = twoFactorAuthService.performAuthentication(secret, user);
		if (authResponse) {
			User updateResponse = twoFactorAuthService.setTwoFactorAuth(TwoFactorAuthOption.GOOGLE_AUTHENTICATOR, user);
			if (updateResponse != null) {
				return ResponseHandler.response(HttpStatus.OK, false,
						localeService.getMessage("tfa.set.to.google.authenticator.success"), null);
			} else {
				return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
						localeService.getMessage("tfa.set.to.google.authenticator.failure"), null);
			}
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.set.to.google.authenticator.failure"), null);
		}
	}


	/**
	 * @Description use to set two factor authentication via mobile
	 * @param Nothing
	 * @return authentication via.mobile success OR authentication via.mobile failure 
	 */
	@Secured("ROLE_USER")
	@RequestMapping(value = UrlConstant.TWO_FACTOR_AUTH_VIA_MOBILE, method = RequestMethod.PUT)
	ResponseEntity<Object> setTwoFactorAuthViaMobile() {
		User user = GenericUtils.getLoggedInUser();
		if (user.getMobileNumber() != null) {
			if (user.getIsMobileVerified()) {
				User updateResponse = twoFactorAuthService.setTwoFactorAuth(TwoFactorAuthOption.MOBILE, user);
				if (updateResponse != null) {
					return ResponseHandler.response(HttpStatus.OK, false,
							localeService.getMessage("tfa.set.to.via.mobile.success"), null);
				} else {
					return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
							localeService.getMessage("tfa.set.to.via.mobile.failure"), null);
				}
			} else {
				return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
						localeService.getMessage("tfa.please.verify.your.mobile"), null);
			}
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.please.add.mobile.number"), null);
		}
	}

	/**
	 * @Description use to send OTP for two factor authentication
	 * @param loginForm
	 * @param bindingResult
	 * @return otp.send.successfully
	 */
	@RequestMapping(value = UrlConstant.SEND_2FA_OTP, method = RequestMethod.PUT)
	ResponseEntity<Object> sendOtpForTwoFactorAuth(@Valid @RequestBody LoginForm loginForm, BindingResult bindingResult) {
		User user = userService.findByEmail(loginForm.getEmailId());
		OTP otp = twoFactorAuthService.sendOtpForTwoFactorAuth(user);
		if (otp != null) {
			return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("tfa.otp.send.successfully"),
					null);
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.unable.to.send.otp"), null);
		}
	}

	/**
	 * @Description  Use to verifies two factor authentication OTP 
	 * @param loginForm
	 * @param bindingResult
	 * @param otp
	 * @return otp.verification.successful OR otp.verification.failure
	 */
	@RequestMapping(value = UrlConstant.VERIFY_2FA_OTP, method = RequestMethod.PUT)
	ResponseEntity<Object> verify2faOtp(@Valid @RequestBody LoginForm loginForm, BindingResult bindingResult, @RequestParam("otp") int otp) throws InvalidOtpException {
		User user = userService.findByEmail(loginForm.getEmailId());
		boolean response;
		if (user.getTwoFactorAuthOption().equals(TwoFactorAuthOption.GOOGLE_AUTHENTICATOR)) {
			response = twoFactorAuthService.performAuthentication(String.valueOf(otp), user);
		}
		else {
			response = twoFactorAuthService.verify2faOtp(otp);
		}
		if (response) {
			AuthenticationToken token = authService.login(loginForm.getPassword(), user, loginForm.getIpAddress(),
					loginForm.getBrowserName(), loginForm.getClientOsName());
			return ResponseHandler.response(HttpStatus.OK, false,
					localeService.getMessage("tfa.otp.verification.successful"), authService.loginResponse(token));
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.otp.verification.failure"), null);
		}
	}

	/**
	 * @Description use to remove two  factor authentication
	 * @param Nothing
	 * @return  removed.success OR removed.fail
	 */
	@Secured("ROLE_USER")
	@RequestMapping(value = UrlConstant.REMOVE_TWO_FACTOR_AUTH, method = RequestMethod.DELETE)
	ResponseEntity<Object> removeTwoFactorAuth() {
		User user = GenericUtils.getLoggedInUser();
		User updateResponse = twoFactorAuthService.setTwoFactorAuth(TwoFactorAuthOption.NONE, user);
		if (updateResponse != null) {
			return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("tfa.removed.success"),
					updateResponse);
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true,
					localeService.getMessage("tfa.remove.failure"), null);
		}
	}

}
