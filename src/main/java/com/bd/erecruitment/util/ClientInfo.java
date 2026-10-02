package com.bd.erecruitment.util;

import java.util.stream.Collectors;
import java.util.stream.Stream;

public record ClientInfo(String city, String country, String deviceType, String os, String browser) {

	public String location() {
		return join(", ", city, country);
	}

	public String device() {
		return join(" / ", browser, os, deviceType);
	}

	private static String join(String separator, String... parts) {
		String joined = Stream.of(parts).filter(p -> p != null && !p.isBlank()).collect(Collectors.joining(separator));
		return joined.isEmpty() ? null : joined;
	}
}
