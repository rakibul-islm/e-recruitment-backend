package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.StoredFileRepo;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.RequestUtils;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.Authentication;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;

import java.util.Date;

@Service
@RequiredArgsConstructor
public class StorageServiceImpl implements StorageService {

	private final StoredFileRepo storedFileRepo;

	@Override
	public StoredFile store(String filename, String contentType, byte[] data) {
		String actor = currentUsername();
		Date now = new Date();
		StoredFile file = new StoredFile()
			.setFilename(filename)
			.setContentType(contentType)
			.setSize(data.length)
			.setData(data);
		file.setCreatedBy(actor).setCreatedOn(now).setCreatedTerminal(RequestUtils.getClientTerminal())
			.setCreatedLocation(RequestUtils.getClientLocation()).setCreatedDevice(RequestUtils.getClientDevice()).setCreatedUserAgent(RequestUtils.getClientUserAgent());
		markUpdated(file, actor, now);
		file.setDeleted(false);
		return storedFileRepo.save(file);
	}

	@Override
	public StoredFile replace(Long fileId, String filename, String contentType, byte[] data) {
		StoredFile file = retrieve(fileId)
			.setFilename(filename)
			.setContentType(contentType)
			.setSize(data.length)
			.setData(data);
		markUpdated(file, currentUsername(), new Date());
		return storedFileRepo.save(file);
	}

	private void markUpdated(StoredFile file, String actor, Date now) {
		file.setUpdatedBy(actor).setUpdatedOn(now).setUpdatedTerminal(RequestUtils.getClientTerminal())
			.setUpdatedLocation(RequestUtils.getClientLocation()).setUpdatedDevice(RequestUtils.getClientDevice()).setUpdatedUserAgent(RequestUtils.getClientUserAgent());
	}

	private String currentUsername() {
		Authentication auth = SecurityContextHolder.getContext().getAuthentication();
		if (auth != null && auth.isAuthenticated() && auth.getPrincipal() instanceof MyUserDetail mud) {
			return mud.getUsername();
		}
		return "system";
	}

	@Override
	public StoredFile retrieve(Long fileId) {
		return storedFileRepo.findByIdAndDeleted(fileId, false)
			.orElseThrow(() -> new NotFoundException("File not found"));
	}

	@Override
	public void delete(Long fileId) {
		storedFileRepo.findByIdAndDeleted(fileId, false).ifPresent(storedFileRepo::delete);
	}
}
