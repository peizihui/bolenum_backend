package com.bolenum.services.user;

import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Paths;
import java.nio.file.attribute.PosixFilePermission;
import java.util.Arrays;
import java.util.Base64;
import java.util.HashSet;
import java.util.Set;

import javax.imageio.ImageIO;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.web.multipart.MultipartFile;

import com.bolenum.exceptions.MaxSizeExceedException;
import com.bolenum.exceptions.PersistenceException;
import com.bolenum.model.User;
import com.bolenum.services.common.LocaleService;

/**
 * 
 * @Author Vishal Kumar
 * @Date 25-Sep-2017
 */
@Service
public class FileUploadServiceImpl implements FileUploadService {

	@Autowired
	private LocaleService localeService;

	private static final Logger logger = LoggerFactory.getLogger(FileUploadServiceImpl.class);

	@Override
	public String uploadFile(MultipartFile multipartFile, String storageLocation, User user, String[] validExtentions,
			long maxSize) throws IOException, PersistenceException, MaxSizeExceedException {
		if (multipartFile.getSize() > maxSize) {
			throw new MaxSizeExceedException(localeService.getMessage("max.file.size.exceeds"));
		}
		String originalFileName = multipartFile.getOriginalFilename();
		int dot = originalFileName.lastIndexOf('.');
		String extension = (dot == -1) ? "" : originalFileName.substring(dot + 1);
		if (!Arrays.asList(validExtentions).contains(extension.toLowerCase())) {
			throw new PersistenceException(localeService.getMessage("valid.image.extention.error"));
		}
		String updatedFileName = user.getFirstName() + "_" + user.getUserId() + "." + extension;
		InputStream inputStream = multipartFile.getInputStream();
		BufferedImage imageFromConvert = ImageIO.read(inputStream);
		File file = new File(storageLocation + updatedFileName);
		ImageIO.write(imageFromConvert, extension, file);
		logger.debug("user uploaded file name: {}",String.valueOf(file));
		//using PosixFilePermission to set file permissions 777
        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
        //add owners permission
        perms.add(PosixFilePermission.OWNER_READ);
        perms.add(PosixFilePermission.OWNER_WRITE);
        //perms.add(PosixFilePermission.OWNER_EXECUTE);
        //add group permissions
        perms.add(PosixFilePermission.GROUP_READ);
        perms.add(PosixFilePermission.GROUP_WRITE);
        //perms.add(PosixFilePermission.GROUP_EXECUTE);
        //add others permissions
        perms.add(PosixFilePermission.OTHERS_READ);
        //perms.add(PosixFilePermission.OTHERS_WRITE);
        //perms.add(PosixFilePermission.OTHERS_EXECUTE);
        
        Files.setPosixFilePermissions(Paths.get(file.toString()), perms);
        logger.info("kyc doc uploaded success");
		return updatedFileName;
	}
	
	@Override
	public String updateUserImage(String imageBase64, String storageLocation, User user, String[] validExtentions,
			long maxSize) throws IOException, MaxSizeExceedException, PersistenceException {
		logger.debug("Profile pic size: {}",imageBase64.length());
		logger.debug("Allowed maximum size: {}", maxSize);

		if (imageBase64.length() > maxSize) {
			throw new MaxSizeExceedException(localeService.getMessage("max.file.size.exceeds"));
		}
		String partSeparator = ",";
		if (imageBase64.contains(partSeparator)) {
			String encodedImg = imageBase64.split(partSeparator)[1];
			String extension = imageBase64.split(partSeparator)[0].split(";")[0].split("/")[1];
			if (!Arrays.asList(validExtentions).contains(extension.toLowerCase())) {
				throw new PersistenceException(localeService.getMessage("valid.image.extention.error"));
			}
			byte[] decodedImg = Base64.getDecoder().decode(encodedImg.getBytes(StandardCharsets.UTF_8));
			InputStream in = new ByteArrayInputStream(decodedImg);
			String updatedFileName = user.getFirstName() + "_" + user.getUserId() + "." + extension;
			BufferedImage imageFromConvert = ImageIO.read(in);
			File file = new File(storageLocation + updatedFileName);
			ImageIO.write(imageFromConvert, extension, file);
			logger.debug("user uploaded file name: {}",String.valueOf(file));
			//using PosixFilePermission to set file permissions 777
	        Set<PosixFilePermission> perms = new HashSet<PosixFilePermission>();
	        //add owners permission
	        perms.add(PosixFilePermission.OWNER_READ);
	        perms.add(PosixFilePermission.OWNER_WRITE);
	        //perms.add(PosixFilePermission.OWNER_EXECUTE);
	        //add group permissions
	        perms.add(PosixFilePermission.GROUP_READ);
	        perms.add(PosixFilePermission.GROUP_WRITE);
	        //perms.add(PosixFilePermission.GROUP_EXECUTE);
	        //add others permissions
	        perms.add(PosixFilePermission.OTHERS_READ);
	        //perms.add(PosixFilePermission.OTHERS_WRITE);
	        //perms.add(PosixFilePermission.OTHERS_EXECUTE);
	        
	        Files.setPosixFilePermissions(Paths.get(file.toString()), perms);
	        logger.info("profile pic uploaded success");
			return updatedFileName;
		}
		return null;
	}
}
