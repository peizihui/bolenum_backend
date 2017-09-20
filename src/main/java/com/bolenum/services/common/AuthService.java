/**
 * 
 */
package com.bolenum.services.common;

import com.bolenum.dto.common.ResetPasswordForm;
import com.bolenum.exceptions.InvalidPasswordException;
import com.bolenum.model.AuthenticationToken;
import com.bolenum.model.User;

/**
 * @author chandan kumar singh
 * @date 13-Sep-2017
 */
public interface AuthService {

	/**
	 * 
	 * @param token
	 * @return boolean
	 */

	boolean logOut(String token);

	AuthenticationToken login(String email, User user, String ipAddress, String browserName)
			throws InvalidPasswordException;

	public boolean validateUser(String email);
	
	public void sendTokenToResetPassword(String email);

	User verifyToken(String token);

	void resetPassword(User verifiedUser, ResetPasswordForm resetPasswordForm);

}