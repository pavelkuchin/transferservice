package com.mobilebanking.transferservice.services;

import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.vavr.collection.List;
import io.vavr.control.Option;

import java.math.BigDecimal;

public interface Storage {
	Option<Account> getAccount(Long accountId);
	Option<Account> setAccount(Long accountId, BigDecimal newBalance);
	Transaction createTransaction(Long fromId, Long toId, BigDecimal amount);
	List<Transaction> getTransactionsForAccount(Long accountId);
	Account createAccount(BigDecimal initialBalance);
	Option<Account> closeAccount(Long accountId);

	void startDbTransaction();
	void commitDbTransaction();
	void rollbackDbTransaction();
}
