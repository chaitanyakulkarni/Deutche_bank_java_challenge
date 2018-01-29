package com.db.awmd.challenge.domain;

import java.math.BigDecimal;

import javax.validation.constraints.NotNull;

import org.hibernate.validator.constraints.NotEmpty;

import com.fasterxml.jackson.annotation.JsonCreator;
import com.fasterxml.jackson.annotation.JsonProperty;

import lombok.Data;

@Data
public class TransferRequest {
	@NotNull
	@NotEmpty
	private String fromAccountId;

	@NotNull
	@NotEmpty
	private String toAccountId;

	@NotNull
	private BigDecimal amountToTransfer;

	@JsonCreator
	public TransferRequest(@JsonProperty("fromAccountId") String fromAccountId,
			@JsonProperty("toAccountId") String toAccountId,
			@JsonProperty("amountToTransfer") BigDecimal amountToTransfer) {
		this.fromAccountId = fromAccountId;
		this.toAccountId = toAccountId;
		this.amountToTransfer = amountToTransfer;
	}

}
