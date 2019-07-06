package com.mobilebanking.transferservice.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigDecimal;
import java.time.Instant;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonSerialize(as = ImmutableTransaction.class)
@JsonDeserialize(as = ImmutableTransaction.class)
public interface Transaction {
	@JsonFormat(pattern = "yyyy-MM-dd HH:mm:ss", timezone = "UTC")
	Instant getTimeStamp();

	Long getFromAccountId();

	Long getToAccountId();

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	BigDecimal getAmount();
}
