package com.bd.erecruitment.dto.res;

import com.bd.erecruitment.dto.BaseDTO;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;

@Data
@MappedSuperclass
public class BaseResponseDTO<E> implements BaseDTO<E> {

	private Long id;
	protected boolean deleted;
}
