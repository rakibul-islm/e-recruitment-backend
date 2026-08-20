package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.entity.UserSession;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class UserSessionResDTO extends BaseResponseDTO<UserSession> {

	private Long userId;
	private String userEmail;
	private String userFullName;
	private String jti;
	private Date issuedAt;
	private Date expiresAt;
	private String ipAddress;
	private String userAgent;
	private boolean revoked;
	private Date revokedAt;
	private String revokedBy;

	public UserSessionResDTO(UserSession session) {
		this.setId(session.getId());
		this.jti = session.getJti();
		this.issuedAt = session.getIssuedAt();
		this.expiresAt = session.getExpiresAt();
		this.ipAddress = session.getIpAddress();
		this.userAgent = session.getUserAgent();
		this.revoked = session.isRevoked();
		this.revokedAt = session.getRevokedAt();
		this.revokedBy = session.getRevokedBy();
		if (session.getUser() != null) {
			this.userId = session.getUser().getId();
			this.userEmail = session.getUser().getEmail();
			this.userFullName = session.getUser().getFullName();
		}
	}
}
