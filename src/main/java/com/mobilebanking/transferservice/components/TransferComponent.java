package com.mobilebanking.transferservice.components;

import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.vavr.collection.List;

import java.math.BigDecimal;

public interface TransferComponent {
	Transaction transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount)
			throws NotSufficientBalance, AccountIsNotAvailable;

	Account getAccount(Long accountId) throws AccountIsNotAvailable;

	List<Transaction> getTransactionsForAccount(Long accountId);

	Account createAccount(BigDecimal initialBalance);

	Account closeAccount(Long accountToDelete) throws AccountIsNotAvailable, BalanceIsNotZero;
}
