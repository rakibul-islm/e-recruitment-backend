package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.Notification;
import com.bd.erecruitment.enums.NotificationType;
import com.fasterxml.jackson.annotation.JsonFormat;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;
import java.util.Map;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class NotificationResDTO extends BaseResponseDTO<Notification> {

	private NotificationType type;
	private String actionRoute;
	private Map<String, Object> params;
	private boolean read;

	@JsonFormat(shape = JsonFormat.Shape.STRING, pattern = "yyyy-MM-dd'T'HH:mm:ss.SSS'Z'", timezone = "UTC")
	private Date createdOn;
}
