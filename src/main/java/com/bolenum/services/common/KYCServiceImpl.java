package com.bolenum.services.common;

import java.io.IOException;
import java.util.Date;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort.Direction;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bolenum.constant.EmailTemplate;
import com.bolenum.enums.DocumentStatus;
import com.bolenum.enums.DocumentType;
import com.bolenum.enums.NotificationType;
import com.bolenum.exceptions.MaxSizeExceedException;
import com.bolenum.exceptions.MobileNotVerifiedException;
import com.bolenum.exceptions.PersistenceException;
import com.bolenum.model.User;
import com.bolenum.model.UserKyc;
import com.bolenum.repo.common.KYCRepo;
import com.bolenum.repo.user.UserRepository;
import com.bolenum.services.user.FileUploadService;
import com.bolenum.services.user.notification.NotificationService;
import com.bolenum.util.MailService;
import com.bolenum.util.SMSService;

/**
 * 
 * @author Vishal Kumar
 * @date 19-sep-2017
 *
 */

@Service
public class KYCServiceImpl implements KYCService {

	@Autowired
	private UserRepository userRepository;

	@Autowired
	private KYCRepo kycRepo;

	@Autowired
	private FileUploadService fileUploadService;

	@Autowired
	private MailService mailService;

	@Autowired
	private LocaleService localeService;

	@Autowired
	private SMSService smsServiceUtil;

	@Autowired
	private KYCService kycService;

	@Autowired
	private NotificationService notificationService;

	@Value("${bolenum.document.location}")
	private String uploadedFileLocation;

	/**
	 * to upload kyc documents
	 */
	@Override
	public UserKyc uploadKycDocument(MultipartFile file, Long userId, DocumentType documentType)
			throws IOException, PersistenceException, MaxSizeExceedException, MobileNotVerifiedException {
		long sizeLimit = 1024 * 1024 * 10L;
		User user = userRepository.findOne(userId);
		User admin = userRepository.findByEmailId("admin@bolenum.com");
		/*
		 * if (user.getMobileNumber() == null || !user.getIsMobileVerified()) { throw
		 * new MobileNotVerifiedException(localeService.getMessage(
		 * "mobile.number.not.verified")); }
		 */

		UserKyc savedKyc = null;
		if (file != null) { // 1
			String[] validExtentions = { "jpg", "jpeg", "png", "pdf" };
			String updatedFileName = fileUploadService.uploadFile(file, uploadedFileLocation, user, documentType,
					validExtentions, sizeLimit);

			List<UserKyc> listOfUserKyc = kycService.getListOfKycByUser(user);

			if (listOfUserKyc.isEmpty()) { // 2
				UserKyc kyc = new UserKyc();
				kyc.setDocument(updatedFileName);
				kyc.setDocumentType(documentType);
				listOfUserKyc.add(kyc);
				kyc.setUser(user);
				savedKyc = kycRepo.save(kyc);

			} else if (listOfUserKyc.size() <= 2) { // 2
				boolean added = false;
				Iterator<UserKyc> iterator = listOfUserKyc.iterator();
				while (iterator.hasNext()) { // 3
					UserKyc userKyc = iterator.next();
					if (documentType.equals(userKyc.getDocumentType())) { // 4
						userKyc.setDocument(updatedFileName);
						userKyc.setDocumentType(documentType);
						userKyc.setIsVerified(false);
						userKyc.setDocumentStatus(DocumentStatus.SUBMITTED);
						userKyc.setVerifiedDate(null);
						userKyc.setRejectionMessage(null);
						added = true;
						savedKyc = kycRepo.save(userKyc);
					}
				}
				if (!added) { // 3
					UserKyc kyc = new UserKyc();
					kyc.setDocument(updatedFileName);
					kyc.setDocumentType(documentType);
					kyc.setUser(user);
					listOfUserKyc.add(kyc);
					savedKyc = kycRepo.save(kyc);
				}
			}
		}
		if (savedKyc != null) {
			notificationService.saveNotification(user, admin,
					"KYC as " + savedKyc.getDocumentType() + " uploaded by " + user.getFullName(), savedKyc.getId(),
					NotificationType.KYC_NOTIFICATION);
		}
		return savedKyc;
	}

	/**
	 * to approve kyc document
	 */
	@Override
	public UserKyc approveKycDocument(Long kycId) {
		UserKyc userKyc = kycRepo.findOne(kycId);
		userKyc.setVerifiedDate(new Date());
		userKyc.setIsVerified(true);
		userKyc.setDocumentStatus(DocumentStatus.APPROVED);
		userKyc.setRejectionMessage(null);
		User user = userKyc.getUser();
		User admin = userRepository.findByEmailId("admin@bolenum.com");
		Map<String, Object> map = new HashMap<>();
		map.put("to", user.getEmailId());
		map.put("name", user.getFirstName());
		if (DocumentType.NATIONAL_ID.equals(userKyc.getDocumentType())) {
			smsServiceUtil.sendMessage(user.getMobileNumber(), user.getCountryCode(),
					localeService.getMessage("email.text.approve.user.kyc.nationalId"));

			map.put("message", localeService.getMessage("email.text.approve.user.kyc.nationalId"));

			mailService.mailSend(user.getEmailId(), localeService.getMessage("email.subject.approve.user.kyc"), map,
					EmailTemplate.KYC_APPROVE_MAIL_TEMPLATE);
			notificationService.saveNotification(admin, user, "Your KYC as NATIONAL_ID has been approved", kycId,
					NotificationType.KYC_NOTIFICATION);

		} else {
			smsServiceUtil.sendMessage(user.getMobileNumber(), user.getCountryCode(),
					localeService.getMessage("email.text.approve.user.kyc.addressproof"));

			map.put("message", localeService.getMessage("email.text.approve.user.kyc.addressproof"));
			mailService.mailSend(user.getEmailId(), localeService.getMessage("email.subject.approve.user.kyc"), map,
					EmailTemplate.KYC_APPROVE_MAIL_TEMPLATE);
			notificationService.saveNotification(admin, user, "Your KYC as RESIDENCE_PROOF has been approved", kycId,
					NotificationType.KYC_NOTIFICATION);

		}
		return kycRepo.save(userKyc);
	}

	/**
	 * to disapprove kyc document
	 */
	@Override
	public UserKyc disApprovedKycDocument(Long kycId, String rejectionMessage) {

		UserKyc userKyc = kycRepo.findOne(kycId);
		userKyc.setVerifiedDate(null);
		userKyc.setIsVerified(false);
		userKyc.setDocumentStatus(DocumentStatus.DISAPPROVED);
		userKyc.setRejectionMessage(rejectionMessage);
		User user = userKyc.getUser();

		Map<String, Object> map = new HashMap<>();
		map.put("to", user.getEmailId());
		map.put("name", user.getFirstName());
		map.put("rejectionMessage", rejectionMessage);
		map.put("documentType", userKyc.getDocumentType());
       
		User admin = userRepository.findByEmailId("admin@bolenum.com");
		
		if (DocumentType.NATIONAL_ID.equals(userKyc.getDocumentType())) {
			smsServiceUtil.sendMessage(user.getMobileNumber(), user.getCountryCode(),
					localeService.getMessage("email.text.disapprove.user.kyc.nationalId"));

			mailService.mailSend(user.getEmailId(), localeService.getMessage("email.subject.disapprove.user.kyc"), map,
					EmailTemplate.KYC_DISAPPROVE_MAIL_TEMPLATE);

			notificationService.saveNotification(admin, user, "Your KYC as NATIONAL_ID has been disapproved", kycId,
					NotificationType.KYC_NOTIFICATION);
		} else {
			smsServiceUtil.sendMessage(user.getMobileNumber(), user.getCountryCode(),
					localeService.getMessage("email.text.disapprove.user.kyc.addressproof"));
			mailService.mailSend(user.getEmailId(), localeService.getMessage("email.subject.disapprove.user.kyc"), map,
					EmailTemplate.KYC_DISAPPROVE_MAIL_TEMPLATE);

			notificationService.saveNotification(admin, user, "Your KYC as as RESIDENCE_PROOF has been disapproved",
					kycId, NotificationType.KYC_NOTIFICATION);
		}
		return kycRepo.save(userKyc);
	}

	/**
	 * 
	 */
	@Override
	public UserKyc getUserKycById(Long kycId) {
		return kycRepo.findOne(kycId);
	}

	/**
	 * 
	 */
	@Override
	public DocumentType validateDocumentType(String documentType) {
		for (DocumentType documentTypeToMatch : DocumentType.values()) {
			if (documentType.equals(documentTypeToMatch.toString())) {
				return documentTypeToMatch;
			}
		}
		return null;
	}

	@Override
	public Page<UserKyc> getListOfKyc(int pageNumber, int pageSize, String sortBy, String sortOrder,
			String searchData) {
		Direction sort;
		if (sortOrder.equals("desc")) {
			sort = Direction.DESC;
		} else {
			sort = Direction.ASC;
		}
		Pageable pageRequest = new PageRequest(pageNumber, pageSize, sort, sortBy);
		return kycRepo.findByDocumentStatus(DocumentStatus.SUBMITTED, searchData, pageRequest);

	}

	@Override
	public List<UserKyc> getListOfKycByUser(User user) {
		return kycRepo.findByUser(user);
	}

}
