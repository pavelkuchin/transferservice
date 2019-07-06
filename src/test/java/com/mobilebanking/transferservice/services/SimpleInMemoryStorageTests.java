package com.mobilebanking.transferservice.services;


import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.Transaction;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;


public class SimpleInMemoryStorageTests {
	private Storage tested = new SimpleInMemoryStorageImpl();
	private AtomicInteger counterForConcurrentTests = new AtomicInteger(0);

	@BeforeTest
	public void init() {
		tested = new SimpleInMemoryStorageImpl();
		counterForConcurrentTests = new AtomicInteger(0);
	}

	@Test
	public void shouldCreateAccount() {
		BigDecimal expectedBalance = BigDecimal.valueOf(100.00);

		Account actualAccount = tested.createAccount(expectedBalance);

		assertThat(actualAccount.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test
	public void shouldReturnAccount() {
		BigDecimal expectedBalance = BigDecimal.valueOf(100.00);

		Account account = tested.createAccount(expectedBalance);
		Account actualAccount = tested.getAccount(account.getId()).get();

		assertThat(actualAccount.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);
		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test
	public void shouldChangeBalanceForAccountIfActiveAtomic() {
		BigDecimal originalBalance = BigDecimal.valueOf(100.00);
		BigDecimal expectedBalance = BigDecimal.valueOf(200.00);

		Account account = tested.createAccount(originalBalance);
		tested.setAccount(account.getId(), expectedBalance);
		Account actualAccount = tested.getAccount(account.getId()).get();

		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test(threadPoolSize = 5, invocationCount = 20,  timeOut = 10000)
	public void shouldIncrementallyChangeBalanceForAccountWithDbTransaction() {
		BigDecimal increment = BigDecimal.valueOf(100.00);

		tested.startDbTransaction();

		int count = counterForConcurrentTests.addAndGet(1);
		BigDecimal expectedBalance = BigDecimal
				.valueOf(1000.12)
				.add(increment.multiply(BigDecimal.valueOf(count)));

		Account account = tested.getAccount(1L).get();
		tested.setAccount(1L, account.getBalance().add(increment));

		Account actualAccount = tested.getAccount(1L).get();

		tested.commitDbTransaction();

		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test(threadPoolSize = 5, invocationCount = 20,  timeOut = 10000)
	public void shouldPerformRollback() {
		BigDecimal increment = BigDecimal.valueOf(100.00);
		BigDecimal expectedBalance = BigDecimal.valueOf(1000.12);

		tested.setAccount(1L, expectedBalance);

		tested.startDbTransaction();

		Account account = tested.getAccount(1L).get();
		tested.setAccount(1L, account.getBalance().add(increment));

		tested.rollbackDbTransaction();

		Account actualAccount = tested.getAccount(1L).get();

		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test
	public void shouldCreateTransaction() {
		Long from = 123456789L;
		Long to = 987654321L;

		BigDecimal amount = BigDecimal.valueOf(100);

		Transaction transaction = tested.createTransaction(from, to, amount);

		assertThat(transaction.getAmount()).isEqualTo(amount);
		assertThat(transaction.getToAccountId()).isEqualTo(to);
		assertThat(transaction.getFromAccountId()).isEqualTo(from);
	}

	@Test
	public void shouldCloseAccount() {
		Account accountBefore = tested.getAccount(1L).get();

		assertThat(accountBefore.getStatus()).isEqualTo(Account.AccountStatus.ACTIVE);

		Account accountAfter = tested.closeAccount(1L).get();

		assertThat(accountAfter.getStatus()).isEqualTo(Account.AccountStatus.CLOSED);
	}
}
