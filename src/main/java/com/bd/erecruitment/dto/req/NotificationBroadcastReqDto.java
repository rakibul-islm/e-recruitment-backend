package com.bd.erecruitment.dto.req;

import lombok.Data;

import java.util.List;

@Data
public class NotificationBroadcastReqDto {

	public enum TargetType { ALL, ROLE, USER }

	private String title;
	private String message;
	private TargetType targetType;
	private Long roleId;
	private List<Long> userIds;
	private boolean sendEmail;
}
