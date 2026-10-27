package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.Banner;
import com.bd.erecruitment.util.ModelMapperUtils;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class BannerReqDto extends BaseRequestDTO<Banner> {

	private String name;
	private Boolean active;

	// Optional special-day window; null clears the bound.
	private java.util.Date visibleFrom;
	private java.util.Date visibleTo;

	/** Create-only base64 image; ignored on update. */
	private String imageBase64;

	@JsonIgnore
	@Override
	public Banner getBean() {
		Banner banner = new Banner();
		ModelMapperUtils.MAPPER.map(this, banner);
		return banner;
	}
}
