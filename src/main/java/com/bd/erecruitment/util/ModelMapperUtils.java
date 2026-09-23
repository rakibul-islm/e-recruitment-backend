package com.bd.erecruitment.util;

import org.modelmapper.ModelMapper;

// Shared instance so DTO mappings reuse one ModelMapper instead of each building its own.
public final class ModelMapperUtils {

	public static final ModelMapper MAPPER = build();

	private ModelMapperUtils() {
	}

	private static ModelMapper build() {
		ModelMapper mapper = new ModelMapper();
		mapper.getConfiguration().setAmbiguityIgnored(true);
		return mapper;
	}
}
