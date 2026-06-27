package com.bd.erecruitment.dto.res;

import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public class BaseResponseDTO<E> {

	private Long id;
	protected boolean deleted;
}
