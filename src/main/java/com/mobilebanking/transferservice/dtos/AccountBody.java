package com.mobilebanking.transferservice.dtos;

import com.fasterxml.jackson.annotation.JsonFormat;
import com.fasterxml.jackson.databind.annotation.JsonDeserialize;
import com.fasterxml.jackson.databind.annotation.JsonSerialize;
import org.immutables.value.Value;

import java.math.BigDecimal;

@Value.Style(jdkOnly = true)
@Value.Immutable
@JsonSerialize(as = ImmutableAccountBody.class)
@JsonDeserialize(as = ImmutableAccountBody.class)
public interface AccountBody {
	@JsonFormat(shape = JsonFormat.Shape.STRING)
	BigDecimal getBalance();
}
