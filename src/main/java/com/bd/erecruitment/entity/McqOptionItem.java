package com.bd.erecruitment.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Embeddable;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.experimental.Accessors;

@Data
@Embeddable
@NoArgsConstructor
@Accessors(chain = true)
public class McqOptionItem {

	@Column(name = "option_key", length = 5)
	private String optionKey;

	@Column(name = "option_text", length = 500)
	private String optionText;

	private boolean correct;

	@Column(name = "display_order")
	private int displayOrder;
}
