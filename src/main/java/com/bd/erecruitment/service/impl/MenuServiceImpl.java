package com.bd.erecruitment.service.impl;

import com.asl.issuetrack.dto.req.MenuReqDTO;
import com.asl.issuetrack.dto.res.MenuResDTO;
import com.asl.issuetrack.entity.Menu;
import com.asl.issuetrack.repository.MenuRepo;
import com.asl.issuetrack.service.MenuService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.data.domain.Sort;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

@Service
public class MenuServiceImpl extends AbstractBaseService<Menu, MenuResDTO, MenuReqDTO> implements MenuService<MenuResDTO, MenuReqDTO> {

	private MenuRepo menuRepo;

	MenuServiceImpl(MenuRepo menuRepo){
		super(menuRepo);
		this.menuRepo = menuRepo;
	}

	@Override
	public Response<MenuResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<Menu> o = menuRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Menu found", new MenuResDTO(o.get())) : getErrorResponse("Menu not found");
	}

	@Transactional
	@Override
	public Response<MenuResDTO> save(MenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getMSequence() == null) reqDto.setMSequence(0L);
		
		Menu z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new MenuResDTO(z));
	}

	@Transactional
	@Override
	public Response<MenuResDTO> update(MenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Menu ID required");
		
		Menu z = reqDto.getBean();
		Menu exist = null;
		Optional<Menu> o = menuRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Menu");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new MenuResDTO(exist));
	}

	@Transactional
	@Override
	public Response<MenuResDTO> delete(MenuReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Menu ID required");

		Menu z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<MenuResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Menu ID required");

		Menu exist = null;
		Optional<Menu> o = menuRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Menu");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<MenuResDTO> getAll() throws ServiceException{
		List<Menu> list = menuRepo.findAllByDeleted(false, Sort.by(Sort.Direction.ASC, "mSequence"));
		if (list == null || list.isEmpty()) return getErrorResponse("Menu list not found");
		return getSuccessResponse("Found Menu", list.stream().map(data -> new ModelMapper().map(data, MenuResDTO.class)).collect(Collectors.toList()));
	}

}
