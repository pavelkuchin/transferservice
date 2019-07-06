package com.mobilebanking.transferservice.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonSerialize(as = ImmutableTransactionBody.class)
@JsonDeserialize(as = ImmutableTransactionBody.class)
public interface TransactionBody {
	Long getFromAccountId();

	Long getToAccountId();

	@JsonFormat(shape = JsonFormat.Shape.STRING)
	BigDecimal getAmount();
}
