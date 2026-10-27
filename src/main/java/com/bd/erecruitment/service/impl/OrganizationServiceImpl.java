package com.bd.erecruitment.service.impl;

import com.bd.erecruitment.dto.req.LogoImageReqDto;
import com.bd.erecruitment.dto.req.OrganizationReqDto;
import com.bd.erecruitment.dto.res.OrganizationResDTO;
import com.bd.erecruitment.entity.Organization;
import com.bd.erecruitment.entity.StoredFile;
import com.bd.erecruitment.exception.BadRequestException;
import com.bd.erecruitment.exception.ForbiddenException;
import com.bd.erecruitment.exception.NotFoundException;
import com.bd.erecruitment.model.MyUserDetail;
import com.bd.erecruitment.repository.JobCircularRepo;
import com.bd.erecruitment.repository.OrganizationRepo;
import com.bd.erecruitment.service.BaseService;
import com.bd.erecruitment.service.StorageService;
import com.bd.erecruitment.util.ImageUtils;
import com.bd.erecruitment.util.Response;
import jakarta.transaction.Transactional;
import org.apache.commons.lang3.StringUtils;
import org.springframework.data.domain.Pageable;
import org.springframework.stereotype.Service;

import java.util.Base64;
import java.util.HashMap;
import java.util.Map;

@Service
public class OrganizationServiceImpl extends AbstractBaseService<Organization> implements BaseService<OrganizationResDTO, OrganizationReqDto> {

	private static final int MAX_LOGO_BYTES = 1024 * 1024;

	private final OrganizationRepo organizationRepo;
	private final JobCircularRepo jobCircularRepo;
	private final StorageService storageService;

	public OrganizationServiceImpl(OrganizationRepo organizationRepo, JobCircularRepo jobCircularRepo, StorageService storageService) {
		super(organizationRepo);
		this.organizationRepo = organizationRepo;
		this.jobCircularRepo = jobCircularRepo;
		this.storageService = storageService;
	}

	@Override
	public Response<OrganizationResDTO> find(Long id) {
		if (id == null) returnErrorException("Id required");
		Organization organization = findByIdOrThrow(id, "Organization not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(organization.getId())) returnNotFoundException("Organization not found");
		return getSuccessResponse("Organization found", new OrganizationResDTO(organization));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> save(OrganizationReqDto reqDto) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot create organizations");
		validateForm(reqDto);
		Organization organization = createEntity(reqDto.getBean());
		return getCreatedResponse("Organization saved successfully", new OrganizationResDTO(organization));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> update(OrganizationReqDto reqDto) {
		if (isScopedRecruiter() && !organizationMatchesCaller(reqDto.getId()))
			throw new ForbiddenException("You may only edit your own organization");
		validateForm(reqDto);
		Organization existing = findByIdOrThrow(reqDto.getId(), "Organization not found");
		modelMapper.map(reqDto, existing);
		existing = updateEntity(existing);
		return getSuccessResponse("Organization updated successfully", new OrganizationResDTO(existing));
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> delete(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete organizations");
		deleteEntity(findByIdOrThrow(id, "Organization not found"));
		return getSuccessResponse("Deleted successfully");
	}

	@Transactional
	@Override
	public Response<OrganizationResDTO> remove(Long id) {
		if (isScopedRecruiter()) throw new ForbiddenException("Recruiters cannot delete organizations");
		removeEntity(findByIdOrThrow(id, "Organization not found"));
		return getSuccessResponse("Removed successfully");
	}

	@Override
	public Response<OrganizationResDTO> filter(Map<String, String> filters, Pageable pageable, Boolean isPageable) {
		if (isScopedRecruiter()) {
			MyUserDetail me = getLoggedInUserDetails();
			filters = new HashMap<>(filters);
			filters.put("id", me.getOrganizationId() == null ? "-1" : String.valueOf(me.getOrganizationId()));
		}
		return genericFilter(filters, pageable, isPageable, OrganizationResDTO.class);
	}

	/** Uploads or replaces the logo; a new StoredFile per upload keeps the public URL immutable-cacheable. */
	@Transactional
	public Response<OrganizationResDTO> updateLogo(Long id, LogoImageReqDto reqDto) {
		Organization organization = findByIdOrThrow(id, "Organization not found");
		if (isScopedRecruiter() && !organizationMatchesCaller(organization.getId()))
			throw new ForbiddenException("You may only edit your own organization");

		String encoded = reqDto == null ? null : reqDto.getImageBase64();
		if (StringUtils.isBlank(encoded)) {
			organization.setLogoFileId(null);
		} else {
			byte[] data;
			try {
				data = Base64.getDecoder().decode(encoded.substring(encoded.indexOf(',') + 1).trim());
			} catch (IllegalArgumentException e) {
				throw new BadRequestException("Logo is not a valid image");
			}
			if (data.length > MAX_LOGO_BYTES) throw new BadRequestException("Logo must be 1 MB or smaller");
			String mimeType = ImageUtils.sniffImageMimeType(data);
			if (mimeType == null) throw new BadRequestException("Logo must be a JPEG, PNG or WebP image");
			StoredFile file = storageService.store("organization-" + organization.getId() + "-logo", mimeType, data);
			organization.setLogoFileId(file.getId());
		}
		organization = updateEntity(organization);
		if (organization.getLogoFileId() != null)
			jobCircularRepo.backfillOrganizationLogo(organization.getId(), organization.getLogoFileId());
		return getSuccessResponse("Logo updated successfully", new OrganizationResDTO(organization));
	}

	/** Serves a file only if referenced as an organization logo, keeping CVs private. */
	@Transactional
	public StoredFile publicLogo(Long fileId) {
		if (fileId == null || !organizationRepo.existsByLogoFileIdAndDeletedFalse(fileId))
			throw new NotFoundException("Logo not found");
		return storageService.retrieve(fileId);
	}

	private boolean organizationMatchesCaller(Long organizationId) {
		MyUserDetail me = getLoggedInUserDetails();
		return me != null && me.getOrganizationId() != null && me.getOrganizationId().equals(organizationId);
	}

	private void validateForm(OrganizationReqDto reqDto) {
		if (StringUtils.isBlank(reqDto.getName())) returnErrorException("Organization name required");
	}
}
