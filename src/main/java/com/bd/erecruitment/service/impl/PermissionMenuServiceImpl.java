package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.PermissionMenuReqDTO;
import com.asl.issuetrack.dto.res.PermissionMenuResDTO;
import com.asl.issuetrack.entity.PermissionMenu;
import com.asl.issuetrack.entity.User;
import com.asl.issuetrack.enums.UserRole;
import com.asl.issuetrack.model.DashboardModel;
import com.asl.issuetrack.model.DropdownOption;
import com.asl.issuetrack.repository.PermissionMenuRepo;
import com.asl.issuetrack.service.PermissionMenuService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class PermissionMenuServiceImpl extends AbstractBaseService<PermissionMenu, PermissionMenuResDTO, PermissionMenuReqDTO> implements PermissionMenuService<PermissionMenuResDTO, PermissionMenuReqDTO> {

	private PermissionMenuRepo permissionMenuRepo;

	PermissionMenuServiceImpl(PermissionMenuRepo permissionMenuRepo){
		super(permissionMenuRepo);
		this.permissionMenuRepo = permissionMenuRepo;
	}

	@Override
	public Response<PermissionMenuResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<PermissionMenu> o = permissionMenuRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("PermissionMenu found", new PermissionMenuResDTO(o.get())) : getErrorResponse("PermissionMenu not found");
	}

	@Transactional
	@Override
	public Response<PermissionMenuResDTO> save(PermissionMenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getRole() == null) return getErrorResponse("Roll should not be empty");
		if (reqDto.getSequence() == 0) return getErrorResponse("Sequence should not be empty");
		
		PermissionMenu z = reqDto.getBean();
		z.setMenu(reqDto.getMenuReqDTO().getBean());
		
		List<PermissionMenu> list = permissionMenuRepo.findAllByDeleted(false);
		for (PermissionMenu permissionMenu : list) {
			if(permissionMenu.getRole().equals(z.getRole()) && (permissionMenu.getMenu().getId().equals(z.getMenu().getId()))) {
				return getErrorResponse("Already Exist! ");
			}
		}
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new PermissionMenuResDTO(z));
	}

	@Transactional
	@Override
	public Response<PermissionMenuResDTO> update(PermissionMenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("PermissionMenu ID required");
		if (reqDto.getRole() == null) return getErrorResponse("Roll should not be empty");
		if (reqDto.getSequence() == 0) return getErrorResponse("Sequence should not be empty");
		
		PermissionMenu z = reqDto.getBean();
		
		PermissionMenu exist = null;
		Optional<PermissionMenu> o = permissionMenuRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found PermissionMenu");
		exist = o.get();
		
		BeanUtils.copyProperties(reqDto, exist);
		
		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new PermissionMenuResDTO(exist));
	}

	@Transactional
	@Override
	public Response<PermissionMenuResDTO> delete(PermissionMenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("PermissionMenu ID required");

		PermissionMenu z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<PermissionMenuResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("PermissionMenu ID required");

		PermissionMenu exist = null;
		Optional<PermissionMenu> o = permissionMenuRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found PermissionMenu");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<PermissionMenuResDTO> getAll() throws ServiceException{
		List<PermissionMenu> list = permissionMenuRepo.findAllByDeleted(false, Sort.by(Sort.Direction.ASC, "sequence"));
		if (list == null || list.isEmpty()) return getErrorResponse("PermissionMenu list not found");
		return getSuccessResponse("Found PermissionMenu", list.stream().map(data -> new ModelMapper().map(data, PermissionMenuResDTO.class)).collect(Collectors.toList()));
	}

	@Override
	public Response<PermissionMenuResDTO> getLoggedInUserPermissionsBasedOnRole() {
		List<PermissionMenu> list = permissionMenuRepo.findAllByRoleAndDeleted(UserRole.valueOf(getLoggedInUser().getRoles()), false, Sort.by(Sort.Direction.ASC, "sequence"));
		if (list == null || list.isEmpty()) return getErrorResponse("PermissionMenu list not found");
		return getSuccessResponse("Found PermissionMenu", list.stream().map(data -> new ModelMapper().map(data, PermissionMenuResDTO.class)).collect(Collectors.toList()));
	}

	@Override
	public List<DropdownOption> getUserRole() {		
		List<DropdownOption> options = new ArrayList<>();
		options.add(new DropdownOption(UserRole.ROLE_SUPER_ADMIN.name(), "Super Admin"));
		options.add(new DropdownOption(UserRole.ROLE_SYSTEM_ADMIN.name(), "Admin"));
		options.add(new DropdownOption(UserRole.ROLE_SND_USER.name(), "S&D User"));
		options.add(new DropdownOption(UserRole.ROLE_NORMAL_USER.name(), "Normal User"));

		return options;		
	}

	@Override
	public DashboardModel getDashboard() {
		DashboardModel data = new DashboardModel();
		User user = getLoggedInUser();
		
		if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SUPER_ADMIN.name())) {
			data.setTotalIssue(permissionMenuRepo.countIssuesByDeleted(false));
			data.setTotalPendingIssue(permissionMenuRepo.countIssuesByStatusAndDeleted("Open", false));
			data.setTotalInProgressIssue(permissionMenuRepo.countIssuesByStatusAndDeleted("In Progress", false));
			data.setTotalDoneIssue(permissionMenuRepo.countIssuesByStatusAndDeleted("Done", false));
			data.setTotalComplain(permissionMenuRepo.countComplainByDeleted(false));
			data.setTodaysComplain(permissionMenuRepo.countTodaysComplainByDeleted(false));
			data.setTotalFeedback(permissionMenuRepo.countFeedbackByDeleted(false));
			data.setTodaysFeedback(permissionMenuRepo.countTodaysFeedbackByDeleted(false));
		}else if(user.getRoles().equalsIgnoreCase(UserRole.ROLE_SYSTEM_ADMIN.name())){
			data.setTotalIssue(permissionMenuRepo.countIssuesByAdminAndDeleted(user.getId(),false));
			data.setTotalPendingIssue(permissionMenuRepo.countIssuesByAdminAndStatusAndDeleted(user.getId(),"Open", false));
			data.setTotalInProgressIssue(permissionMenuRepo.countIssuesByAdminAndStatusAndDeleted(user.getId(),"In Progress", false));
			data.setTotalDoneIssue(permissionMenuRepo.countIssuesByAdminAndStatusAndDeleted(user.getId(),"Done", false));
			data.setTotalComplain(permissionMenuRepo.countComplainByDeleted(false));
			data.setTodaysComplain(permissionMenuRepo.countTodaysComplainByDeleted(false));
			data.setTotalFeedback(permissionMenuRepo.countFeedbackByDeleted(false));
			data.setTodaysFeedback(permissionMenuRepo.countTodaysFeedbackByDeleted(false));
		}else {
			data.setTotalIssue(permissionMenuRepo.countIssuesByUserAndDeleted(user.getId(),false));
			data.setTotalPendingIssue(permissionMenuRepo.countIssuesByUserAndStatusAndDeleted(user.getId(),"Open", false));
			data.setTotalInProgressIssue(permissionMenuRepo.countIssuesByUserAndStatusAndDeleted(user.getId(),"In Progress", false));
			data.setTotalDoneIssue(permissionMenuRepo.countIssuesByUserAndStatusAndDeleted(user.getId(),"Done", false));
			data.setTotalComplain(permissionMenuRepo.countComplainByUserAndDeleted(user.getId(),false));
			data.setTodaysComplain(permissionMenuRepo.countTodaysComplainByUserAndDeleted(user.getId(),false));
			data.setTotalFeedback(permissionMenuRepo.countFeedbackByUserAndDeleted(user.getId(),false));
			data.setTodaysFeedback(permissionMenuRepo.countTodaysFeedbackByUserAndDeleted(user.getId(),false));
		}
		 
		return data;
	}
	
}
