package com.mobilebanking.transferservice.controllers;

import com.mobilebanking.transferservice.components.TransferComponent;
import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.dtos.AccountBody;
import com.mobilebanking.transferservice.dtos.TransactionBody;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.javalin.http.Context;
import io.vavr.collection.List;

import javax.inject.Inject;

public class JavalinControllerImpl implements Controller<Context> {
	private TransferComponent transferComponent;

	@Inject
	public JavalinControllerImpl(TransferComponent transferComponent) {
		this.transferComponent = transferComponent;
	}

	@Override
	public Account createAccount(Context context) {
		AccountBody body = context.bodyAsClass(AccountBody.class);

		Account account = transferComponent.createAccount(body.getBalance());

		context.status(201);
		context.json(account);

		return account;
	}

	@Override
	public Account closeAccount(Context context) throws BalanceIsNotZero, AccountIsNotAvailable {
		Long id = Long.valueOf(context.pathParam("id"));

		Account account = transferComponent.closeAccount(id);

		context.status(202);
		context.json(account);

		return account;
	}

	@Override
	public Account getAccount(Context context) throws AccountIsNotAvailable {
		Long id = Long.valueOf(context.pathParam("id"));

		Account account = transferComponent.getAccount(id);

		context.status(200);
		context.json(account);

		return account;
	}

	@Override
	public Transaction transfer(Context context) throws NotSufficientBalance, AccountIsNotAvailable {
		TransactionBody body = context.bodyAsClass(TransactionBody.class);

		Transaction transaction = transferComponent
				.transferMoney(body.getFromAccountId(), body.getToAccountId(), body.getAmount());

		context.status(201);
		context.json(transaction);

		return transaction;
	}

	@Override
	public List<Transaction> getTransactions(Context context) {
		Long id = Long.valueOf(context.pathParam("id"));

		List<Transaction> transactions = transferComponent.getTransactionsForAccount(id);

		context.status(200);
		context.json(transactions);

		return transactions;
	}
}
