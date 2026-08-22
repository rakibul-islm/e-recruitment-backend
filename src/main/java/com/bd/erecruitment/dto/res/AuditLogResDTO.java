package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLogResDTO extends BaseResponseDTO<AuditLog> {

	private AuditCategory category;
	private String action;
	private String entityType;
	private Long entityId;
	private AuditOutcome outcome;
	private String ipAddress;
	private String userAgent;
	private String correlationId;
	private String requestUri;
	private String httpMethod;
	private String changedFields;
	private String createdBy;
	private Date createdOn;

	public AuditLogResDTO(AuditLog log) {
		new ModelMapper().map(log, this);
	}
}
