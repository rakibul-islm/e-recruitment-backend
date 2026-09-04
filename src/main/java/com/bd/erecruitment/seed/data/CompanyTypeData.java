package com.bd.erecruitment.seed.data;

import java.util.List;

public class CompanyTypeData {

	public static List<String> get() {
		return List.of(
			"Private Limited",
			"Public Limited",
			"Multinational",
			"Startup",
			"Government",
			"Non-Government Organization (NGO)",
			"Sole Proprietorship",
			"Partnership"
		);
	}
}
