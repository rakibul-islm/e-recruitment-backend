package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Banner;
import com.bd.erecruitment.util.ModelMapperUtils;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BannerResDTO extends BaseResponseDTO<Banner> {

	public BannerResDTO(Banner banner) {
		ModelMapperUtils.MAPPER.map(banner, this);
	}

	private String name;
	private Long fileId;
	private boolean active;
	private java.util.Date visibleFrom;
	private java.util.Date visibleTo;
}
