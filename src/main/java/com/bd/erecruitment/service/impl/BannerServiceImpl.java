package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.BannerReqDto;
import com.bd.erecruitment.dto.res.BannerResDTO;
import com.bd.erecruitment.entity.Banner;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.repository.BannerRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.ImageUtils;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.Date;
import java.util.Map;

@Service
public class BannerServiceImpl extends AbstractBaseService<Banner> implements BaseService<BannerResDTO, BannerReqDto> {

	private static final int MAX_BANNER_BYTES = 5 * 1024 * 1024;

	private final BannerRepo bannerRepo;
	private final StorageService storageService;

	public BannerServiceImpl(BannerRepo bannerRepo, StorageService storageService) {
		super(bannerRepo);
		this.bannerRepo = bannerRepo;
		this.storageService = storageService;
	}

	@Override
	public Response<BannerResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		return getSuccessResponse("Banner found", new BannerResDTO(findByIdOrThrow(id, "Banner not found")));
	}

	/** Creates the banner row and its StoredFile in one call; GIFs stored untouched. */
	@Transactional
	@Override
	public Response<BannerResDTO> save(BannerReqDto reqDto) {
		validateForm(reqDto);
		if (StringUtils.isBlank(reqDto.getImageBase64())) returnErrorException("Banner image required");
		byte[] data = decodeImage(reqDto.getImageBase64());
		String mimeType = ImageUtils.sniffImageMimeType(data, true);
		if (mimeType == null) throw new BadRequestException("Banner must be a JPEG, PNG, WebP or GIF image");

		StoredFile file = storageService.store("banner-" + StringUtils.trim(reqDto.getName()) + "-" + System.currentTimeMillis(), mimeType, data);
		boolean active = Boolean.TRUE.equals(reqDto.getActive()) || bannerRepo.countByDeletedFalse() == 0;
		if (active) bannerRepo.deactivateAll();
		Banner banner = reqDto.getBean()
			.setFileId(file.getId())
			.setActive(active);
		return getCreatedResponse("Banner saved successfully", new BannerResDTO(createEntity(banner)));
	}

	@Transactional
	@Override
	public Response<BannerResDTO> update(BannerReqDto reqDto) {
		validateForm(reqDto);
		Banner existing = findByIdOrThrow(reqDto.getId(), "Banner not found");
		existing.setName(reqDto.getName())
			.setVisibleFrom(reqDto.getVisibleFrom())
			.setVisibleTo(reqDto.getVisibleTo());
		if (reqDto.getActive() != null && reqDto.getActive() != existing.isActive()) {
			if (reqDto.getActive()) bannerRepo.deactivateAll();
			existing.setActive(reqDto.getActive());
		}
		return getSuccessResponse("Banner updated successfully", new BannerResDTO(updateEntity(existing)));
	}

	/** Hard delete also removes the image (no snapshot references). */
	@Transactional
	@Override
	public Response<BannerResDTO> delete(Long id) {
		Banner banner = findByIdOrThrow(id, "Banner not found");
		Long fileId = banner.getFileId();
		deleteEntity(banner);
		if (fileId != null) storageService.delete(fileId);
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<BannerResDTO> remove(Long id) {
		removeEntity(findByIdOrThrow(id, "Banner not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<BannerResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		return genericFilter(filters, pageable, isPageable, BannerResDTO.class);
	}

	/** Active banner inside its visibility window; obj is null when none. */
	@Transactional
	public Response<BannerResDTO> active() {
		Date now = new Date();
		BannerResDTO banner = bannerRepo.findFirstByActiveTrueAndDeletedFalse()
			.filter(b -> (b.getVisibleFrom() == null || !b.getVisibleFrom().after(now))
				&& (b.getVisibleTo() == null || !b.getVisibleTo().before(now)))
			.map(BannerResDTO::new)
			.orElse(null);
		return getSuccessResponse(banner == null ? "No active banner" : "Active banner found", banner);
	}

	/** Serves a banner image only while referenced by a banner row. */
	@Transactional
	public StoredFile publicImage(Long fileId) {
		return storageService.retrieve(fileId);
	}

	private byte[] decodeImage(String encoded) {
		try {
			byte[] data = Base64.getDecoder().decode(encoded.substring(encoded.indexOf(',') + 1).trim());
			if (data.length > MAX_BANNER_BYTES) throw new BadRequestException("Banner must be 3 MB or smaller");
			return data;
		} catch (IllegalArgumentException e) {
			throw new BadRequestException("Banner is not a valid image");
		}
	}

	private void validateForm(BannerReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Banner name required");
	}
}
