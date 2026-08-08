package com.bd.erecruitment.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
public abstract class BaseRequestDTO<E> {

	protected Long id;

	@JsonIgnore
	public abstract E getBean();

}
