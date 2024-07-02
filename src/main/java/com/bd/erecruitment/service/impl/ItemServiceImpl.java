package com.bd.erecruitment.service.impl;

import java.util.List;
import java.util.Optional;
import java.util.stream.Collectors;

import org.apache.commons.lang3.StringUtils;
import org.modelmapper.ModelMapper;
import org.springframework.beans.BeanUtils;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import com.asl.issuetrack.dto.req.ItemReqDTO;
import com.asl.issuetrack.dto.res.ItemResDTO;
import com.asl.issuetrack.entity.Item;
import com.asl.issuetrack.repository.ItemRepo;
import com.asl.issuetrack.service.ItemService;
import com.asl.issuetrack.service.exception.ServiceException;
import com.asl.issuetrack.util.Response;

@Service
public class ItemServiceImpl extends AbstractBaseService<Item, ItemResDTO, ItemReqDTO> implements ItemService<ItemResDTO, ItemReqDTO> {

	private ItemRepo itemRepo;

	ItemServiceImpl(ItemRepo itemRepo){
		super(itemRepo);
		this.itemRepo = itemRepo;
	}

	@Override
	public Response<ItemResDTO> find(Long id) throws ServiceException{
		if (id == null) return getErrorResponse("ID required");

		Optional<Item> o = itemRepo.findByIdAndDeleted(id, false);
		return o.isPresent() ? getSuccessResponse("Item found", new ItemResDTO(o.get())) : getErrorResponse("Item not found");
	}

	@Transactional
	@Override
	public Response<ItemResDTO> save(ItemReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if(StringUtils.isBlank(reqDto.getName())) return getErrorResponse("Name required");
		
		Item z = reqDto.getBean();
	//	z.setUser(getLoggedInUser());
		z = createEntity(z);
		if (z == null || z.getId() == null) return getErrorResponse("Can't save");

		return getSuccessResponse("Saved successfully", new ItemResDTO(z));
	}

	@Transactional
	@Override
	public Response<ItemResDTO> update(ItemReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Item ID required");
		if(StringUtils.isBlank(reqDto.getName())) return getErrorResponse("Name required");
		
		Item z = reqDto.getBean();
		Item exist = null;
		Optional<Item> o = itemRepo.findByIdAndDeleted(z.getId(), false);
		if (!o.isPresent()) return getErrorResponse("Can't found Item");
		exist = o.get();

		BeanUtils.copyProperties(reqDto, exist);

		exist = updateEntity(exist);
		if (exist == null || exist.getId() == null) return getErrorResponse("Can't update");

		return getSuccessResponse("Update successfully", new ItemResDTO(exist));
	}

	@Transactional
	@Override
	public Response<ItemResDTO> delete(ItemReqDTO reqDto) throws ServiceException{
		if (reqDto == null) return getErrorResponse("Request data is empty");
		if (reqDto.getId() == null) return getErrorResponse("Item ID required");

		Item z = reqDto.getBean();
		return delete(z.getId());
	}

	@Transactional
	@Override
	public Response<ItemResDTO> delete(Long id) throws ServiceException {
		if (id == null) return getErrorResponse("Item ID required");

		Item exist = null;
		Optional<Item> o = itemRepo.findByIdAndDeleted(id, false);
		if (!o.isPresent()) return getErrorResponse("Can't found Item");
		exist = o.get();

		try {
			deleteEntity(exist);
		} catch (Exception e) {
			return getErrorResponse(e.getMessage());
		}

		return getSuccessResponse("Delete successfully");
	}

	@Override
	public Response<ItemResDTO> getAll() throws ServiceException{
		List<Item> list = itemRepo.findAllByDeleted(false);
		if (list == null || list.isEmpty()) return getErrorResponse("Item list not found");
		return getSuccessResponse("Found Item", list.stream().map(data -> new ModelMapper().map(data, ItemResDTO.class)).collect(Collectors.toList()));
	}

}
