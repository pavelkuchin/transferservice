package com.mobilebanking.transferservice.services;

import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.ImmutableAccount;
import com.mobilebanking.transferservice.dtos.ImmutableTransaction;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.vavr.collection.HashMap;
import io.vavr.collection.List;
import io.vavr.collection.Map;
import io.vavr.control.Option;

import javax.inject.Inject;
import java.math.BigDecimal;
import java.time.Instant;
import java.util.concurrent.locks.ReentrantLock;


public class SimpleInMemoryStorageImpl implements Storage {
	private Map<Long, Account> accounts;
	private List<Transaction> transactions;

	private Map<Long, Account> backupAccounts;
	private List<Transaction> backupTransactions;

	private ReentrantLock lock = new ReentrantLock();

	@Inject
	public SimpleInMemoryStorageImpl() {
		Account fakeAccount1 = ImmutableAccount
				.builder()
				.id(1L)
				.balance(BigDecimal.valueOf(1000.12))
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Account fakeAccount2 = ImmutableAccount
				.builder()
				.id(2L)
				.balance(BigDecimal.valueOf(0))
				.status(Account.AccountStatus.ACTIVE)
				.build();

		accounts = HashMap.of(1L, fakeAccount1,2L, fakeAccount2);
		transactions = List.empty();
	}

	@Override
	public Option<Account> getAccount(Long accountId) {
		return accounts
				.get(accountId);
	}

	private boolean isActive(Long id, Account account) {
		return account.getStatus().equals(Account.AccountStatus.ACTIVE);
	}

	@Override
	public Option<Account> setAccount(Long accountId, BigDecimal newBalance) {
		accounts = accounts
				.computeIfPresent(accountId, (Long key, Account value) ->
					ImmutableAccount.copyOf(value).withBalance(newBalance)
				)._2();

		return accounts.get(accountId);
	}

	@Override
	public Transaction createTransaction(Long fromId, Long toId, BigDecimal amount) {
		Transaction transaction = ImmutableTransaction
				.builder()
				.fromAccountId(fromId)
				.toAccountId(toId)
				.amount(amount)
				.timeStamp(Instant.now())
				.build();

		transactions = transactions.append(transaction);

		return transaction;
	}

	@Override
	public List<Transaction> getTransactionsForAccount(Long accountId) {
		return transactions.filter(transaction ->
				transaction.getFromAccountId().equals(accountId) || transaction.getToAccountId().equals(accountId)
		);
	}

	@Override
	public Account createAccount(BigDecimal initialBalance) {
		Long newId = accounts.last()._2().getId() + 1L;

		Account account = ImmutableAccount
				.builder()
				.id(newId)
				.balance(initialBalance)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		accounts = accounts.put(newId, account);

		return account;
	}

	@Override
	public Option<Account> closeAccount(Long accountId) {
		accounts = accounts.computeIfPresent(accountId,
				(Long key, Account account) ->
						ImmutableAccount.copyOf(account).withStatus(Account.AccountStatus.CLOSED)
		)._2();

		return accounts.get(accountId);
	}

	@Override
	public void startDbTransaction() {
		lock.lock();

		backupAccounts = copyHashMap(accounts);
		backupTransactions = copyList(transactions);
	}

	private <K, V> HashMap<K, V> copyHashMap(Map<K, V> map) {
		return HashMap.ofAll(map.toJavaMap());
	}

	private <V> List<V> copyList(List<V> list) {
		return List.ofAll(list.asJava());
	}

	@Override
	public void commitDbTransaction() {
		lock.unlock();
	}

	@Override
	public void rollbackDbTransaction() {
		accounts = backupAccounts;
		transactions = backupTransactions;

		lock.unlock();
	}
}
