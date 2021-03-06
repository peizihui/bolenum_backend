/*@Description Of Class
 * 
 * AuthController class is responsible for below listed task: 
 *    
 *   login user
 *   logout user
 *   forget password
 *   verify authentication link
 *   
 */
package com.bolenum.controller.common;

import javax.validation.Valid;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.annotation.Secured;
import org.springframework.security.core.userdetails.UsernameNotFoundException;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestHeader;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

import com.bolenum.constant.UrlConstant;
import com.bolenum.dto.common.LoginForm;
import com.bolenum.dto.common.ResetPasswordForm;
import com.bolenum.enums.TwoFactorAuthOption;
import com.bolenum.exceptions.InvalidPasswordException;
import com.bolenum.model.AuthenticationToken;
import com.bolenum.model.User;
import com.bolenum.services.common.AuthService;
import com.bolenum.services.common.LocaleService;
import com.bolenum.services.user.AuthenticationTokenService;
import com.bolenum.services.user.TwoFactorAuthService;
import com.bolenum.services.user.UserService;
import com.bolenum.util.ErrorCollectionUtil;
import com.bolenum.util.GenericUtils;
import com.bolenum.util.ResponseHandler;

import io.swagger.annotations.Api;

/**
 * @author chandan kumar singh
 * @date 13-Sep-2017
 * 
 *       Auth controller contains all functionality which requires
 *       authentication like login, logout, and functionality used for reset
 *       password
 */

@RestController
@Api(value = "Authentication Controller")
public class AuthController {
	@Autowired
	private AuthService authService;

	@Autowired
	private UserService userService;

	@Autowired
	private TwoFactorAuthService twoFactorAuthService;

	@Autowired
	private LocaleService localeService;

	@Autowired
	private AuthenticationTokenService authenticationTokenService;

	public static final Logger logger = LoggerFactory.getLogger(AuthController.class);

	private static final String LOGIN_SUCCESS = "login.success";

	/**
	 * @Descripton Validate user for login
	 * @param loginForm
	 * @param bindingResult
	 * @return user 
	 * @return token
	 */

	@RequestMapping(value = UrlConstant.USER_LOGIN, method = RequestMethod.POST)
	ResponseEntity<Object> login(@Valid @RequestBody LoginForm loginForm, BindingResult bindingResult) {
		if (bindingResult.hasErrors()) {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, ErrorCollectionUtil.getError(bindingResult),
					null);
		}
		logger.debug("login email id: {}", loginForm.getEmailId());
		User user = userService.findByEmail(loginForm.getEmailId());
		if (user == null) {
			return ResponseHandler.response(HttpStatus.UNAUTHORIZED, true, localeService.getMessage("user.not.found"),
					null);
		} else if (!user.getIsEnabled()) {
			return ResponseHandler.response(HttpStatus.UNAUTHORIZED, true,
					localeService.getMessage("user.mail.verify.error"), null);
		} else {
			AuthenticationToken token;
			if (user.getRole().getName().equals(loginForm.getRole())) {
				try {
					token = authService.login(loginForm.getPassword(), user, loginForm.getIpAddress(),
							loginForm.getBrowserName(), loginForm.getClientOsName());
				} catch (UsernameNotFoundException | InvalidPasswordException e) {
					return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, e.getMessage(), null);
				}

				if (user.getTwoFactorAuthOption().equals(TwoFactorAuthOption.NONE)) {
					return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage(LOGIN_SUCCESS),
							authService.loginResponse(token));
				} else if (user.getTwoFactorAuthOption().equals(TwoFactorAuthOption.MOBILE)) {
					twoFactorAuthService.sendOtpForTwoFactorAuth(user);
					return ResponseHandler.response(HttpStatus.ACCEPTED, false, localeService.getMessage(LOGIN_SUCCESS),
							user.getTwoFactorAuthOption());
				} else {
					return ResponseHandler.response(HttpStatus.ACCEPTED, false, localeService.getMessage(LOGIN_SUCCESS),
							user.getTwoFactorAuthOption());
				}
			} else {
				return ResponseHandler.response(HttpStatus.UNAUTHORIZED, true,
						localeService.getMessage("user.not.authorized.error"), null);
			}

		}
	}

	/**
	 *@Description controller that respond when hit comes for logout activity of user
	 * 
	 * @param token
	 * @return null
	 */
	@Secured({ "ROLE_USER", "ROLE_ADMIN" })
	@RequestMapping(value = UrlConstant.USER_LOOUT, method = RequestMethod.DELETE)
	ResponseEntity<Object> logout(@RequestHeader("Authorization") String token) {
		boolean response = authService.logOut(token);
		if (response) {
			return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("logout.success"), null);
		} else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("logout.failure"),
					null);
		}
	}

	/**
	 * @description used for forget password
	 * 
	 * @param email
	 * @return verification link
	 */
	@RequestMapping(value = UrlConstant.FORGET_PASS, method = RequestMethod.GET)
	public ResponseEntity<Object> forgetPassword(@RequestParam String email) {
		email = email.trim();
		email = email.replace(' ', '+');
		boolean isValid = GenericUtils.isValidMail(email);
		logger.debug("isValid: {}", isValid);
		if (isValid) {
			User user = userService.findByEmail(email);
			if (user != null && user.getIsEnabled()) {
				AuthenticationToken authenticationToken = authService.sendTokenToResetPassword(user);
				logger.debug(authenticationToken.getToken());
				return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("mail.sent.success"),
						email);
			}
		}
		return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("invalid.email"), null);
	}

	/**
	 * @description  verify authentication link sent at the time of forget password
	 * 
	 * @param token
	 * @param resetPasswordForm
	 * @param result
	 * @return verified user email id
	 * 
	 */
	@RequestMapping(value = UrlConstant.FORGET_PASS_VERIFY, method = RequestMethod.PUT)
	public ResponseEntity<Object> resetPassword(@RequestParam String token,
			@Valid @RequestBody ResetPasswordForm resetPasswordForm, BindingResult result) {
		logger.debug("user mail verify token: {}", token);
		if (token == null || token.isEmpty()) {
			throw new IllegalArgumentException(localeService.getMessage("token.invalid"));
		}
		User verifiedUser = authService.verifyTokenForResetPassword(token);
		if (verifiedUser == null) {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("token.invalid"),
					null);
		}
		AuthenticationToken authenticationToken = authenticationTokenService.findByToken(token);
		boolean isExpired = authenticationTokenService.isTokenExpired(authenticationToken);
		logger.debug("user mail verify token expired: {}", isExpired);
		if (isExpired) {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, false, localeService.getMessage("token.expired"),
					null);
		}
		if (result.hasErrors()) {
			return ResponseHandler.response(HttpStatus.CONFLICT, true,
					localeService.getMessage("user.password.not.proper"), verifiedUser.getEmailId());
		} else if (!result.hasErrors()) {
			authService.resetPassword(verifiedUser, resetPasswordForm);
			return ResponseHandler.response(HttpStatus.OK, false,
					localeService.getMessage("user.password.change.success"), verifiedUser.getEmailId());
		} else {
			return ResponseHandler.response(HttpStatus.CONFLICT, true,
					localeService.getMessage("user.password.not.matched"), null);
		}
	}

}
