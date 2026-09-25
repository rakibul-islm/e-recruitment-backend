package com.bd.erecruitment.util;

import org.modelmapper.ModelMapper;

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
