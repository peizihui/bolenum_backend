/*@Description Of class
 * 
 * AuthServiceImpl class is responsible for below listed task: 
 *  	
 *  	LogOut
 *      Login
 *      Validate user
 *      Send token to reset password
 *      Reset password
 *      Verify token for reset password
 *      Login response
 *      
 * **/



package com.bolenum.services.common;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bolenum.constant.EmailTemplate;
import com.bolenum.dto.common.ResetPasswordForm;
import com.bolenum.enums.TokenType;
import com.bolenum.exceptions.InvalidPasswordException;
import com.bolenum.model.AuthenticationToken;
import com.bolenum.model.User;
import com.bolenum.model.UserActivity;
import com.bolenum.repo.common.AuthenticationTokenRepo;
import com.bolenum.repo.user.UserActivityRepository;
import com.bolenum.repo.user.UserRepository;
import com.bolenum.services.user.AuthenticationTokenService;
import com.bolenum.util.MailService;
import com.bolenum.util.PasswordEncoderUtil;
import com.bolenum.util.TokenGenerator;

/**
 * @author Himanshu
 * @date 13-Sep-2017
 */
@Service
public class AuthServiceImpl implements AuthService {
	@Autowired
	private UserRepository userRepository;

	@Autowired
	private PasswordEncoderUtil passwordEncoder;

	@Autowired
	private AuthenticationTokenRepo authenticationTokenRepo;

	@Autowired
	private MailService emailService;

	@Autowired
	private UserActivityRepository userActivityRepository;

	@Autowired
	private AuthenticationTokenService authenticationTokenService;

	@Autowired
	private LocaleService localeService;

	@Value("${bolenum.url}")
	private String serverUrl;

	@Value("${bolenum.api.reset}")
	private String urlForResetPassword;

	public static final Logger logger = LoggerFactory.getLogger(AuthServiceImpl.class);

	/**@description use to login activity of user
	 * @param password
	 * @param user
	 * @param ipAddress
	 * @param browserName
	 * @return AuthenticationToken
	 */
	@Override
	public AuthenticationToken login(String password, User user, String ipAddress, String browserName,
			String clientOSName) {
		if (passwordEncoder.matches(password, user.getPassword())) {
			// Generate Token and Save it for the logged in user
			AuthenticationToken authToken = new AuthenticationToken(TokenGenerator.generateToken(), user);
			authToken.setTokentype(TokenType.AUTHENTICATION);

			AuthenticationToken savedAuthToken = authenticationTokenRepo.save(authToken);
			UserActivity userActivity = new UserActivity(ipAddress, browserName, clientOSName, savedAuthToken);
			userActivityRepository.save(userActivity);
			return savedAuthToken;
		} else {
			throw new InvalidPasswordException(localeService.getMessage("invalid.credential"));
		}
	}

	/**@description use to send response after login
	 * @param token
	 * @return Map
	 */
	@Override
	public Map<String, Object> loginResponse(AuthenticationToken token) {
		Map<String, Object> map = new HashMap<>();
		map.put("userId", token.getUser().getUserId());
		map.put("fName", token.getUser().getFirstName());
		map.put("mName", token.getUser().getMiddleName());
		map.put("lName", token.getUser().getLastName());
		map.put("name", token.getUser().getFullName());
		map.put("profilePic", token.getUser().getProfileImage());
		map.put("email", token.getUser().getEmailId());
		map.put("role", token.getUser().getRole().getName());
		map.put("token", token.getToken());
		return map;
	}

	/**@description use to get user validate
	 * @param email
	 * @return user
	 */
	@Override
	public User validateUser(String email) {

		return userRepository.findByEmailId(email);
	}

	/**@description use to get user logout
	 * @param token
	 * @return boolean
	 */

	@Override
	public boolean logOut(String token) {
		AuthenticationToken authToken = authenticationTokenRepo.findByToken(token);
		if (authToken != null && authToken.getDeletedOn() == null) {
			UserActivity userActivity = userActivityRepository.findByAuthenticationToken(authToken);
			userActivity.setIsDeleted(true);
			userActivityRepository.save(userActivity);
			authToken.setDeletedOn(new Date());
			authenticationTokenRepo.save(authToken);
			return true;
		} else {
			return false;
		}
	}

	/**@description use to send token so that user can reset password
	 * @param user
	 * @return AuthenticationToken
	 */

	public AuthenticationToken sendTokenToResetPassword(User user) {

		List<AuthenticationToken> previousToken = authenticationTokenRepo.findByUserAndTokentype(user,
				TokenType.FORGOT_PASSWORD);

		for (AuthenticationToken token : previousToken) {
			if (token.getTokentype() == TokenType.FORGOT_PASSWORD) {
				authenticationTokenRepo.delete(token);
			}
		}

		String token = TokenGenerator.generateToken();

		AuthenticationToken authenticationToken = new AuthenticationToken(token, user);

		String url = serverUrl + urlForResetPassword + token;
		Map<String, Object> map = new HashMap<>();
		map.put("url", url);
		emailService.mailSend(user.getEmailId(), localeService.getMessage("message.subject.forget.password"), map,EmailTemplate.RESET_PASSWORD_TEMPLATE);
		authenticationToken.setTokentype(TokenType.FORGOT_PASSWORD);
		return authenticationTokenRepo.saveAndFlush(authenticationToken);

	}

	/**@description use to verify token for reset password
	 * @param token
	 * @return User
	 */
	@Override
	public User verifyTokenForResetPassword(String token) {
		AuthenticationToken authenticationToken = authenticationTokenRepo.findByToken(token);
		if (authenticationToken != null) {
			User user = authenticationToken.getUser();
			boolean isExpired = authenticationTokenService.isTokenExpired(authenticationToken);
			if (user != null && user.getIsEnabled() && !isExpired) {
				return user;
			}
		}
		return null;
	}

	/**@description use to send token so that user can reset password
	 * @param user    
	 * @param resetPasswordForm
	 */
	@Override
	public void resetPassword(User user, ResetPasswordForm resetPasswordForm) {
		user.setPassword(passwordEncoder.encode(resetPasswordForm.getNewPassword()));
		userRepository.save(user);
		List<AuthenticationToken> previousToken = authenticationTokenRepo.findByUserAndTokentype(user,
				TokenType.FORGOT_PASSWORD);

		for (AuthenticationToken token : previousToken) {
			if (token.getTokentype() == TokenType.FORGOT_PASSWORD) {
				authenticationTokenRepo.delete(token);
			}
		}
	}

}
