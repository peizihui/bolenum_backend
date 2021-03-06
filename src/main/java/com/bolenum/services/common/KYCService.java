package com.bolenum.services.common;

import java.io.IOException;
import java.util.List;

import org.springframework.data.domain.Page;
import org.springframework.web.multipart.MultipartFile;

import com.bolenum.enums.DocumentType;
import com.bolenum.exceptions.MaxSizeExceedException;
import com.bolenum.exceptions.MobileNotVerifiedException;
import com.bolenum.exceptions.PersistenceException;
import com.bolenum.model.User;
import com.bolenum.model.UserKyc;

/**
 * 
 * @author Vishal Kumar
 * @date 19-sep-2017
 *
 */

public interface KYCService {

	/**
	 * This method is use to upload KycDocument
	 * @param multipartFile
	 * @param userId
	 * @return User
	 * @throws IOException
	 * @throws MaxSizeExceedException
	 */
	UserKyc uploadKycDocument(MultipartFile multipartFile, Long userId, DocumentType documentType)
			throws IOException, PersistenceException, MaxSizeExceedException, MobileNotVerifiedException;

	/**
	 * This method is use to approve KycDocument
	 * @param userId
	 * @returnn User
	 */
	UserKyc approveKycDocument(Long kycId);

	/**
	 * This method is use to disApproved KycDocument
	 * @param userId
	 * @param rejectionMessage
	 * @return User
	 */
	UserKyc disApprovedKycDocument(Long kycId, String rejectionMessage);

	/**
	 * This method is use to get UserKyc By Id
	 * @param kycId
	 * @return UserKyc
	 */
	UserKyc getUserKycById(Long kycId);

	/**
	 * This method is use to validate Document Type
	 * @param documentType
	 * @return
	 */

	DocumentType validateDocumentType(String documentType);

	/**
	 * This method is use to get ListOfKyc
	 * @param pageNumber
	 * @param pageSize
	 * @param sortBy
	 * @param sortOrder
	 * @param searchData
	 * @return
	 */
	public Page<UserKyc> getListOfKyc(int pageNumber, int pageSize, String sortBy, String sortOrder, String searchData);

	/**
	 * This method is use to get List Of Kyc ByUser
	 * @param user
	 * @return
	 */
	List<UserKyc> getListOfKycByUser(User user);

}
