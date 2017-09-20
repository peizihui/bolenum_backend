package com.bolenum.controller.common;

import java.io.IOException;
import java.util.Map;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestMethod;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

import com.bolenum.constant.UrlConstant;
import com.bolenum.model.User;
import com.bolenum.services.common.KYCService;
import com.bolenum.services.common.LocaleService;
import com.bolenum.util.GenericUtils;
import com.bolenum.util.ResponseHandler;

@RestController
@RequestMapping(value = UrlConstant.BASE_USER_URI_V1)
public class KYCController {

	@Autowired
	private KYCService kycService;
	@Autowired
	private LocaleService localeService;
	
	@RequestMapping(value = UrlConstant.UPLOAD_DOCUMENT, method = RequestMethod.POST)
	public ResponseEntity<Object> uploadKycDocument(@RequestParam MultipartFile multipartFile) {
		User user = GenericUtils.getLoggedInUser();
		try {
			User response = kycService.uploadKycDocument(multipartFile, user.getUserId());
			if (response != null) {
				return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("user.document.uploaded.success"), user);
			} else {
				return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("user.document.uploaded.failed"), null);
			}
		} catch (IOException e) {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, e.getMessage(), null);
		}
	}
	
	@RequestMapping(value = UrlConstant.APPROVE_DOCUMENT, method = RequestMethod.PUT)
	public ResponseEntity<Object> approveKycDocument(@PathVariable Long userId) {
		User user = kycService.approveKycDocument(userId);
		if (user != null) {
			return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("user.document.disapprove.success"), user);
		}
		else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("user.document.disapprove.failed"), null);
		}
	}
	
	@RequestMapping(value = UrlConstant.DISAPPROVE_DOCUMENT, method = RequestMethod.PUT)
	public ResponseEntity<Object> disApproveKycDocument(@RequestBody Map<String, String> data) {
		User user = kycService.disApprovedKycDocument(Long.parseLong(data.get("userId")), data.get("rejectionMessage"));
		if (user != null) {
			return ResponseHandler.response(HttpStatus.OK, false, localeService.getMessage("user.document.approve.success"), user);
		}
		else {
			return ResponseHandler.response(HttpStatus.BAD_REQUEST, true, localeService.getMessage("user.document.approve.failed"), null);
		}
	}
	
}