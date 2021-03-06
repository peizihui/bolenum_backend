/*@Description Of class
 * 
 * UserServiceImpl class is responsible for below listed task: 
 *   
 *     Save user
 *     Find by email
 *     Re Register
 *     Change password
 *     Update user profile
 *     Add mobile number
 *     Re send OTP
 *     Verify OTP
 *     Mail verification
 *     Find by user id
 *     Upload image
 *     KYC Verified
 *     Save user coin
 **/

package com.bolenum.services.user;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import javax.transaction.Transactional;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import com.bolenum.dto.common.EditUserForm;
import com.bolenum.dto.common.PasswordForm;
import com.bolenum.dto.common.UserSignupForm;
import com.bolenum.enums.CurrencyType;
import com.bolenum.enums.TokenType;
import com.bolenum.exceptions.InvalidOtpException;
import com.bolenum.exceptions.InvalidPasswordException;
import com.bolenum.exceptions.MaxSizeExceedException;
import com.bolenum.exceptions.PersistenceException;
import com.bolenum.model.AuthenticationToken;
import com.bolenum.model.OTP;
import com.bolenum.model.User;
import com.bolenum.model.UserKyc;
import com.bolenum.model.coin.UserCoin;
import com.bolenum.repo.common.AuthenticationTokenRepo;
import com.bolenum.repo.common.RoleRepo;
import com.bolenum.repo.common.coin.UserCoinRepository;
import com.bolenum.repo.user.OTPRepository;
import com.bolenum.repo.user.UserRepository;
import com.bolenum.services.common.KYCService;
import com.bolenum.services.common.LocaleService;
import com.bolenum.util.MailService;
import com.bolenum.util.PasswordEncoderUtil;
import com.bolenum.util.SMSService;
import com.bolenum.util.TokenGenerator;

/**
 * 
 * @Author Himanshu
 * @Date 12-Sep-2017
 */
@Transactional
@Service
public class UserServiceImpl implements UserService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private AuthenticationTokenRepo authenticationTokenRepo;

	@Autowired
	private MailService emailservice;

	@Autowired
	private PasswordEncoderUtil passwordEncoder;

	@Autowired
	private RoleRepo roleRepo;

	@Autowired
	private LocaleService localService;

	@Autowired
	private SMSService smsServiceUtil;

	@Autowired
	private OTPRepository otpRepository;

	@Autowired
	private FileUploadService fileUploadService;

	@Autowired
	private KYCService kycService;

	@Autowired
	private UserCoinRepository userCoinRepository;

	@Value("${bolenum.profile.image.location}")
	private String uploadedFileLocation;

	private static final Logger logger = LoggerFactory.getLogger(UserServiceImpl.class);

	/**
	 *@description use to register user if and only if when user details not present in database
	 *@param       user
	 *
	 */
	@Override
	public void registerUser(User user) {
		user.setPassword(passwordEncoder.encode(user.getPassword()));
		user.setRole(roleRepo.findByNameIgnoreCase("ROLE_USER"));
		userRepository.save(user);
		logger.debug("saved new user= {}", user.getEmailId());
		AuthenticationToken authenticationToken = mailVerification(user);
		authenticationToken.setTokentype(TokenType.REGISTRATION);
		authenticationTokenRepo.saveAndFlush(authenticationToken);
	}

	/**
	 * @description to send mail which contains verification link and Authentication token
	 * 
	 * @param  user
	 * @return authenticationToken
	 */
	private AuthenticationToken mailVerification(User user) {
		String token = TokenGenerator.generateToken();
		AuthenticationToken authenticationToken = new AuthenticationToken(token, user);
		emailservice.registrationMailSend(user.getEmailId(), token);
		return authenticationToken;
	}

	/**@description  find user with respect to email id
	 * @param        email
	 * @return       user
	 */
	@Override
	public User findByEmail(String email) {
		logger.debug("email in findByEmail = {}", email);
		return userRepository.findByEmailId(email);
	}

	/**@description  use to save user
	 * @param        user
	 * @return       user
	 */
	@Override
	public User saveUser(User user) {
		return userRepository.saveAndFlush(user);
	}
	
	
	/**@description  use to save user coin
	 * @param        walletAddress
	 * @param        user
	 * @param        tokenName
	 * @return       userCoin
	 */
	@Override
	public UserCoin saveUserCoin(String walletAddress, User user, String tokenName) {
		UserCoin userCoin = userCoinRepository.findByTokenNameAndUser(tokenName, user);
		if (userCoin == null) {
			userCoin = new UserCoin();
			userCoin.setWalletAddress(walletAddress);
			userCoin.setTokenName(tokenName);
			userCoin.setUser(user);
			userCoin.setCurrencyType(CurrencyType.CRYPTO);
			userCoin = userCoinRepository.save(userCoin);
		}
		return userCoin;
	}

	/**@description  use to re register user if already details present in user table
	 * @param        userSignupForm
	 */

	@Override
	public void reRegister(UserSignupForm userSignupForm) {
		User user = userRepository.findByEmailId(userSignupForm.getEmailId());
		List<AuthenticationToken> verificationToken = authenticationTokenRepo.findByUserAndTokentype(user,
				TokenType.REGISTRATION);

		for (AuthenticationToken token : verificationToken) {
			if (token.getTokentype() == TokenType.REGISTRATION) {
				authenticationTokenRepo.delete(token);
			}
		}
		User isUserExist = userRepository.findByEmailId(userSignupForm.getEmailId());
		isUserExist.setFirstName(userSignupForm.getFirstName());
		isUserExist.setLastName(userSignupForm.getLastName());
		isUserExist.setPassword(userSignupForm.getPassword());
		registerUser(isUserExist);
	}

	/**@description use to change password of user
	 * @param       user
	 * @param       passwordForm
	 * @return      boolean
	 */
	@Override
	public Boolean changePassword(User user, PasswordForm passwordForm) {
		if (passwordEncoder.matches(passwordForm.getOldPassword(), user.getPassword())) {
			user.setPassword(passwordEncoder.encode(passwordForm.getNewPassword()));
			userRepository.save(user);
			return true;
		} else {
			throw new InvalidPasswordException(localService.getMessage("invalid.credential"));
		}
	}

	/**@description use to update user profile
	 * @param       user
	 * @param       editUserForm
	 * @return      user
	 */
	@Override
	public User updateUserProfile(EditUserForm editUserForm, User user) {

		if (editUserForm.getFirstName() != null) {
			user.setFirstName(editUserForm.getFirstName());
		}

		if (editUserForm.getMiddleName() != null) {
			user.setMiddleName(editUserForm.getMiddleName());
		}

		user.setLastName(editUserForm.getLastName());

		if (editUserForm.getAddress() != null) {
			user.setAddress(editUserForm.getAddress());
		}

		if (editUserForm.getCity() != null) {
			user.setCity(editUserForm.getCity());
		}

		if (editUserForm.getState() != null) {
			user.setState(editUserForm.getState());
		}

		if (editUserForm.getCountry() != null) {
			user.setCountry(editUserForm.getCountry());
		}

		if (editUserForm.getGender() != null) {
			user.setGender(editUserForm.getGender());
		}

		if (editUserForm.getDob() != null) {

			user.setDob(new Date(editUserForm.getDob()));
		}

		return userRepository.saveAndFlush(user);

	}
	/**@description use to add mobile number
	 * @param       mobileNumber
	 * @param       countryCode
	 * @param       user
	 * @return      user
	 */
	@Override
	public User addMobileNumber(String mobileNumber, String countryCode, User user) throws PersistenceException {
		User existinguser = userRepository.findByMobileNumber(mobileNumber);
		Random r = new Random();
		int code = (100000 + r.nextInt(900000));
		logger.debug("Otp sent success: {}", code);
		if (existinguser == null) {
			smsServiceUtil.sendOtp(code, countryCode, mobileNumber);
			OTP otp = new OTP(mobileNumber, code, user);
			if (otpRepository.save(otp) != null) {
				logger.debug("OTP saved");
				user.setCountryCode(countryCode);
				user.setMobileNumber(mobileNumber);
				user.setIsMobileVerified(false);
				return userRepository.save(user);
			} else {
				return null;
			}
		} else {
			if (existinguser.getUserId().equals(user.getUserId()) && existinguser.getIsMobileVerified()) {
				throw new PersistenceException(localService.getMessage("mobile.number.already.verified.by.you"));
			} else if (existinguser.getUserId().equals(user.getUserId()) && !existinguser.getIsMobileVerified()) {
				logger.debug("user exist but mobile not verified");
				smsServiceUtil.sendOtp(code, countryCode, mobileNumber);
				OTP otp = new OTP(mobileNumber, code, user);
				otpRepository.save(otp);
				logger.debug("OTP saved");
				return existinguser;
			} else {
				throw new PersistenceException(localService.getMessage("mobile.number.already.added"));
			}
		}
	}
	/**@description use to verify otp
	 * @param       otp
	 * @param       user
	 * @return      boolean
	 */
	@Override
	public Boolean verifyOTP(Integer otp, User user) throws InvalidOtpException {
		OTP existingOtp = otpRepository.findByOtpNumber(otp);
		if (existingOtp != null) {
			if (!existingOtp.getIsDeleted() && existingOtp.getMobileNumber().equals(user.getMobileNumber())) {
				long timeDiffInSec = (new Date().getTime() - existingOtp.getCreatedDate().getTime()) / 1000;
				if (timeDiffInSec <= 300) {
					user.setIsMobileVerified(true);
					if (userRepository.save(user) != null) {
						existingOtp.setIsDeleted(true);
						otpRepository.save(existingOtp);
						return true;
					} else {
						return false;
					}
				} else {
					throw new InvalidOtpException(localService.getMessage("otp.expired"));
				}
			} else {
				throw new InvalidOtpException(localService.getMessage("otp.invalid"));
			}
		} else {
			throw new InvalidOtpException(localService.getMessage("otp.invalid"));
		}
	}
	/**@description use to resends otp
	 * @param       user
	 */
	@Override
	public void resendOTP(User user) {
		String mobileNumber = user.getMobileNumber();
		Random r = new Random();
		int code = (100000 + r.nextInt(900000));
		logger.debug("Otp sent success: {}", code);
		smsServiceUtil.sendOtp(code, user.getCountryCode(), mobileNumber);
		OTP otp = new OTP(mobileNumber, code, user);
		otpRepository.save(otp);
	}

	/**@description use to find user by id
	 * @param       id
	 * @return      user
	 */
	@Override
	public User findByUserId(Long id) {
		return userRepository.findByUserId(id);

	}

	/**
	 * @description use to upload profile image with all validation of image file.
	 * @param       imageBase64
	 * @param       userId
	 * @throws      IOException
	 * @throws      PersistenceException
	 * @throws      MaxSizeExceedException
	 * @return      user
	 */
	@Override
	public User uploadImage(String imageBase64, Long userId)
			throws IOException, PersistenceException, MaxSizeExceedException {

		long sizeLimit = 1024 * 1024 * 5L;
		User user = userRepository.findOne(userId);
		if (imageBase64 != null) {
			String[] validExtentions = { "jpg", "jpeg", "png" };
			String updatedFileName = fileUploadService.updateUserImage(imageBase64, uploadedFileLocation, user,
					validExtentions, sizeLimit);
			user.setProfileImage(updatedFileName);
			return userRepository.save(user);

		}
		return null;
	}

	/**@description  use to verify user KYC
	 * @return       boolean
	 * @param user
	 */
	@Override
	public boolean isKycVerified(User user) {
		List<UserKyc> kycList = kycService.getListOfKycByUser(user);
		if (kycList == null || kycList.size() < 2) {
			return false;

		} else {
			for (int i = 0; i < kycList.size(); i++) {
				UserKyc userKyc = kycList.get(i);
				if (!userKyc.getIsVerified()) {
					return false;
				}
			}
		}
		return true;
	}

}
