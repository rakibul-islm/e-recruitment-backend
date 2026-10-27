package com.bd.erecruitment.controller;

import com.bd.erecruitment.annotation.RestApiController;
import com.bd.erecruitment.dto.req.BannerReqDto;
import com.bd.erecruitment.dto.res.BannerResDTO;
import com.bd.erecruitment.service.impl.BannerServiceImpl;
import com.bd.erecruitment.util.Response;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;

@RestApiController
@RequestMapping("/banner")
@Tag(name = "13.1 Banner", description = "Dashboard banner management")
public class BannerController extends AbstractBaseController<BannerResDTO, BannerReqDto> {

	private final BannerServiceImpl bannerService;

	public BannerController(BannerServiceImpl bannerService) {
		super(bannerService);
		this.bannerService = bannerService;
	}

	@Operation(summary = "The active dashboard banner (obj null when none)")
	@GetMapping("/active")
	public Response<BannerResDTO> active() {
		return bannerService.active();
	}
}
