package com.bd.erecruitment.dto.res;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@AllArgsConstructor
@NoArgsConstructor
public class SessionSummaryResDTO {
	private long activeSessions;
	private long distinctActiveUsers;
	private long activeGuests;
}
