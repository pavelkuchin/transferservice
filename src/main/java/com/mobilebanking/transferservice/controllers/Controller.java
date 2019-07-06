package com.mobilebanking.transferservice.controllers;

import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.vavr.collection.List;

public interface Controller<T> {
	Account createAccount(T context);
	Account closeAccount(T context) throws BalanceIsNotZero, AccountIsNotAvailable;
	Account getAccount(T context) throws AccountIsNotAvailable;

	Transaction transfer(T context) throws NotSufficientBalance, AccountIsNotAvailable;
	List<Transaction> getTransactions(T context);
}
