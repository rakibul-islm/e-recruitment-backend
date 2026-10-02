package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class GuestSessionResDTO {
	private String guestId;
	private String ipAddress;
	private String city;
	private String country;
	private String deviceType;
	private String os;
	private String browser;
	private Date connectedAt;
}
