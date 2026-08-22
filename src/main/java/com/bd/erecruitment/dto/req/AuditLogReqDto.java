package com.bd.erecruitment.dto.req;

import com.bd.erecruitment.entity.AuditLog;
import com.bd.erecruitment.enums.AuditCategory;
import com.bd.erecruitment.enums.AuditOutcome;
import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;
import org.modelmapper.ModelMapper;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class AuditLogReqDto extends BaseRequestDTO<AuditLog> {

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

	@JsonIgnore
	@Override
	public AuditLog getBean() {
		AuditLog e = new AuditLog();
		new ModelMapper().map(this, e);
		return e;
	}
}
