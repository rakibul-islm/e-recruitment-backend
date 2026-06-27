package com.bd.erecruitment.dto.req;

import com.fasterxml.jackson.annotation.JsonIgnore;
import jakarta.persistence.MappedSuperclass;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.SuperBuilder;

@Data
@SuperBuilder
@NoArgsConstructor
@MappedSuperclass
public abstract class BaseRequestDTO<E> {

	protected Long id;
	protected boolean deleted;

	@JsonIgnore
	public abstract E getBean();

}
