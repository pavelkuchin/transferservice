package com.mobilebanking.transferservice.components;

import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import com.mobilebanking.transferservice.services.Storage;
import io.vavr.collection.List;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.math.BigDecimal;

public class TransferComponentImpl implements TransferComponent {
	private Storage storage;

	@Inject
	public TransferComponentImpl(Storage storage) {
		this.storage = storage;
	}

	@Override
	public Transaction transferMoney(Long fromAccountId, Long toAccountId, BigDecimal amount)
			throws NotSufficientBalance, AccountIsNotAvailable {
		storage.startDbTransaction();

		Option<Account> fromAccount = storage.getAccount(fromAccountId);
		Option<Account> toAccount = storage.getAccount(toAccountId);

		BigDecimal newBalanceOnSource = fromAccount
				.filter(account -> account.getStatus().equals(Account.AccountStatus.ACTIVE))
				.map(Account::getBalance)
				.filter(balance -> hasEnoughBalance(balance, amount))
				.map(balance -> balance.subtract(amount))
				.onEmpty(storage::rollbackDbTransaction)
				.getOrElseThrow(NotSufficientBalance::new);

		BigDecimal newBalanceOnTarget = toAccount
				.filter(account -> account.getStatus().equals(Account.AccountStatus.ACTIVE))
				.map(Account::getBalance)
				.map(balance -> balance.add(amount))
				.onEmpty(storage::rollbackDbTransaction)
				.getOrElseThrow(AccountIsNotAvailable::new);

		Transaction transaction = storage.createTransaction(fromAccountId, toAccountId, amount);
		storage.setAccount(fromAccountId, newBalanceOnSource);
		storage.setAccount(toAccountId, newBalanceOnTarget);

		storage.commitDbTransaction();

		return transaction;
	}

	private boolean hasEnoughBalance(BigDecimal balance, BigDecimal transactionAmount) {
		return balance.compareTo(transactionAmount) >= 0;
	}

	@Override
	public Account getAccount(Long accountId) throws AccountIsNotAvailable {
		return storage.getAccount(accountId).getOrElseThrow(AccountIsNotAvailable::new);
	}

	@Override
	public List<Transaction> getTransactionsForAccount(Long accountId) {
		return storage.getTransactionsForAccount(accountId);
	}

	@Override
	public Account createAccount(BigDecimal initialBalance) {
		return storage.createAccount(initialBalance);
	}

	@Override
	public Account closeAccount(Long accountId) throws AccountIsNotAvailable, BalanceIsNotZero {
		storage.startDbTransaction();

		BigDecimal currentBalance = storage
				.getAccount(accountId)
				.filter(account -> account.getStatus().equals(Account.AccountStatus.ACTIVE))
				.map(Account::getBalance)
				.onEmpty(storage::rollbackDbTransaction)
				.getOrElseThrow(AccountIsNotAvailable::new);

		Account closedAccount;

		if(currentBalance.equals(BigDecimal.ZERO)) {
			closedAccount = storage.closeAccount(accountId).getOrElseThrow(AccountIsNotAvailable::new);
		} else {
			storage.rollbackDbTransaction();
			throw new BalanceIsNotZero();
		}

		storage.commitDbTransaction();

		return closedAccount;
	}
}
