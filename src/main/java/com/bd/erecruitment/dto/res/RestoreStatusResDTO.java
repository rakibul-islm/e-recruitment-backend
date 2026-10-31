package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.backup.RestoreState;
import lombok.Data;
import lombok.EqualsAndHashCode;
import lombok.NoArgsConstructor;

import java.util.Date;

@Data
@NoArgsConstructor
@EqualsAndHashCode(callSuper = true)
public class RestoreStatusResDTO extends BaseResponseDTO<RestoreState.Snapshot> {

	private String phase;
	private String fileName;
	private String startedBy;
	private Date startedOn;
	private Date finishedOn;
	private String message;

	public RestoreStatusResDTO(RestoreState.Snapshot snapshot) {
		this.phase = snapshot.phase().name();
		this.fileName = snapshot.fileName();
		this.startedBy = snapshot.startedBy();
		this.startedOn = snapshot.startedOn();
		this.finishedOn = snapshot.finishedOn();
		this.message = snapshot.message();
	}
}
