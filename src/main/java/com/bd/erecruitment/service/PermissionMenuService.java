package com.bd.erecruitment.service;

import com.asl.issuetrack.dto.res.PermissionMenuResDTO;
import com.asl.issuetrack.model.DashboardModel;
import com.asl.issuetrack.model.DropdownOption;
import com.asl.issuetrack.util.Response;

import java.util.List;

public interface PermissionMenuService<R, E> extends BaseService<R, E> {
	
	Response<PermissionMenuResDTO> getLoggedInUserPermissionsBasedOnRole();
	List<DropdownOption> getUserRole();	
	DashboardModel getDashboard();
	
}
