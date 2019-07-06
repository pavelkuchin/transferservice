package com.mobilebanking.transferservice.components;

import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.ImmutableAccount;
import com.mobilebanking.transferservice.dtos.ImmutableTransaction;
import com.mobilebanking.transferservice.dtos.Transaction;
import com.mobilebanking.transferservice.services.Storage;
import io.vavr.collection.List;
import io.vavr.control.Option;
import org.assertj.core.api.JUnitSoftAssertions;
import org.junit.Rule;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Strict.class)
public class TransferComponentTests {
	@Rule
	public final JUnitSoftAssertions softly = new JUnitSoftAssertions();

	@Test
	public void shouldCreateNewAccountWithInitialBalance() {
		BigDecimal expectedBalance = BigDecimal.valueOf(24.22);
		Long expectedId = 1L;

		Account expectedAccount = ImmutableAccount
				.builder()
				.id(expectedId)
				.balance(expectedBalance)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent testes = new TransferComponentImpl(mockStorage);

		when(mockStorage.createAccount(expectedBalance)).thenReturn(expectedAccount);


		Account actualAccount = testes.createAccount(expectedBalance);


		verify(mockStorage).createAccount(expectedBalance);
		assertThat(expectedAccount).isEqualTo(actualAccount);
	}

	@Test
	public void shouldCloseAccountWhenBalanceIsZero() throws BalanceIsNotZero, AccountIsNotAvailable {
		Long expectedId = 1L;
		ImmutableAccount accountWithZeroBalance = ImmutableAccount
				.builder()
				.id(1L)
				.balance(BigDecimal.ZERO)
				.status(Account.AccountStatus.ACTIVE)
				.build();


		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(expectedId))
				.thenReturn(Option.of(accountWithZeroBalance));
		when(mockStorage.closeAccount(expectedId))
				.thenReturn(Option.of(accountWithZeroBalance.withStatus(Account.AccountStatus.CLOSED)));


		Account actualAccount = tested.closeAccount(expectedId);


		verify(mockStorage).getAccount(expectedId);
		verify(mockStorage).closeAccount(expectedId);
		verify(mockStorage, never()).rollbackDbTransaction();

		assertThat(actualAccount).isEqualTo(accountWithZeroBalance.withStatus(Account.AccountStatus.CLOSED));
	}

	@Test
	public void shouldThrowExceptionOnAttemptToCloseAccountWithPositiveBalance() throws AccountIsNotAvailable {
		Long expectedId = 1L;
		ImmutableAccount accountWithNotZeroBalance = ImmutableAccount
				.builder()
				.id(1L)
				.balance(BigDecimal.valueOf(100.00))
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(expectedId))
				.thenReturn(Option.of(accountWithNotZeroBalance));

		try {
			tested.closeAccount(expectedId);
		} catch (BalanceIsNotZero balanceIsNotZero) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldThrowExceptionOnAttemptToCloseNonExistingAccount() throws BalanceIsNotZero {
		Long expectedId = 1L;

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(expectedId))
				.thenReturn(Option.none());

		try {
			tested.closeAccount(expectedId);
		} catch (AccountIsNotAvailable accountIsNotAvailable) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldThrowExceptionOnAttemptToCloseClosedAccount() throws BalanceIsNotZero {
		Long expectedId = 1L;

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		Account closedAccount = ImmutableAccount
				.builder()
				.id(1L)
				.balance(BigDecimal.ZERO)
				.status(Account.AccountStatus.CLOSED)
				.build();

		when(mockStorage.getAccount(expectedId))
				.thenReturn(Option.of(closedAccount));

		try {
			tested.closeAccount(expectedId);
		} catch (AccountIsNotAvailable accountIsNotAvailable) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldReturnTransactionsForAccount() {
		Long accountId = 987654321L;

		Long expectedFromId1 = 987654321L;
		Long expectedToId1 = 122555324L;
		BigDecimal expectedAmount1 = BigDecimal.valueOf(100.00);
		Instant expectedTime1 = Instant.now();

		Transaction expectedTransactions1 = ImmutableTransaction
				.builder()
				.fromAccountId(expectedFromId1)
				.toAccountId(expectedToId1)
				.timeStamp(expectedTime1)
				.amount(expectedAmount1)
				.build();

		Long expectedFromId2 = 123456789L;
		Long expectedToId2 = 987654321L;
		BigDecimal expectedAmount2 = BigDecimal.valueOf(50.00);
		Instant expectedTime2 = Instant.now();

		Transaction expectedTransactions2 = ImmutableTransaction
				.builder()
				.fromAccountId(expectedFromId2)
				.toAccountId(expectedToId2)
				.timeStamp(expectedTime2)
				.amount(expectedAmount2)
				.build();

		List<Transaction> expectedList = List.of(expectedTransactions1, expectedTransactions2);

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getTransactionsForAccount(accountId)).thenReturn(expectedList);


		List<Transaction> actualList = tested.getTransactionsForAccount(accountId);

		verify(mockStorage).getTransactionsForAccount(accountId);

		assertThat(actualList).containsExactly(expectedTransactions1, expectedTransactions2);
	}

	@Test
	public void shouldReturnBalanceForAccount() throws AccountIsNotAvailable {
		Long accountId = 123456789L;
		BigDecimal expectedBalance = BigDecimal.valueOf(100.00);

		Account account = ImmutableAccount
				.builder()
				.id(accountId)
				.balance(expectedBalance)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(accountId)).thenReturn(Option.of(account));

		Account actualAccount = tested.getAccount(accountId);

		verify(mockStorage).getAccount(accountId);
		assertThat(actualAccount.getBalance()).isEqualTo(expectedBalance);
	}

	@Test(expected = AccountIsNotAvailable.class)
	public void shouldThrowExceptionOnAttemptToGetBalanceForNonExistingAccount() throws AccountIsNotAvailable {
		Long accountId = 123456789L;

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(accountId)).thenReturn(Option.none());

		tested.getAccount(accountId);
	}

	@Test
	public void shouldTransferMoneyBetweenAccounts() throws NotSufficientBalance, AccountIsNotAvailable {
		Long transferFrom = 123456789L;
		Long transferTo = 987654321L;

		BigDecimal initialAmountFrom = BigDecimal.valueOf(200.00);
		BigDecimal initialAmountTo = BigDecimal.ZERO;

		Account fromAccount = ImmutableAccount
				.builder()
				.id(transferFrom)
				.balance(initialAmountFrom)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Account toAccount = ImmutableAccount
				.builder()
				.id(transferTo)
				.balance(initialAmountTo)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		BigDecimal amount = BigDecimal.valueOf(100.00);

		Transaction transaction = ImmutableTransaction
				.builder()
				.fromAccountId(transferFrom)
				.toAccountId(transferTo)
				.amount(amount)
				.timeStamp(Instant.now())
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(transferFrom))
				.thenReturn(Option.of(fromAccount));

		when(mockStorage.getAccount(transferTo))
				.thenReturn(Option.of(toAccount));

		when(mockStorage.createTransaction(transferFrom, transferTo, amount))
				.thenReturn(transaction);

		Transaction actualTransaction = tested.transferMoney(transferFrom, transferTo, amount);


		verify(mockStorage).startDbTransaction();
		verify(mockStorage).getAccount(transferFrom);
		verify(mockStorage).getAccount(transferTo);
		verify(mockStorage).setAccount(transferFrom, initialAmountFrom.subtract(amount));
		verify(mockStorage).setAccount(transferTo, initialAmountTo.add(amount));
		verify(mockStorage).createTransaction(transferFrom, transferTo, amount);
		verify(mockStorage).commitDbTransaction();
		verify(mockStorage, never()).rollbackDbTransaction();

		softly.assertThat(actualTransaction.getFromAccountId()).isEqualTo(transferFrom);
		softly.assertThat(actualTransaction.getToAccountId()).isEqualTo(transferTo);
		softly.assertThat(actualTransaction.getAmount()).isEqualTo(amount);
	}

	@Test
	public void shouldThrowExceptionIfNotEnoughBalanceOnFromAccount() throws AccountIsNotAvailable {
		Long transferFrom = 123456789L;
		Long transferTo = 987654321L;

		BigDecimal initialAmountFrom = BigDecimal.valueOf(99.99);
		BigDecimal initialAmountTo = BigDecimal.ZERO;
		BigDecimal amount = BigDecimal.valueOf(100.00);

		Account fromAccount = ImmutableAccount
				.builder()
				.id(transferFrom)
				.balance(initialAmountFrom)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Account toAccount = ImmutableAccount
				.builder()
				.id(transferTo)
				.balance(initialAmountTo)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(transferFrom))
				.thenReturn(Option.of(fromAccount));

		when(mockStorage.getAccount(transferTo))
				.thenReturn(Option.of(toAccount));

		try {
			tested.transferMoney(transferFrom, transferTo, amount);
		} catch (NotSufficientBalance notSufficientBalance) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldThrowExceptionIfToAccountDoesNotExist() throws NotSufficientBalance {
		Long transferFrom = 123456789L;
		Long transferTo = 987654321L;

		BigDecimal initialAmountFrom = BigDecimal.valueOf(1000.00);
		BigDecimal amount = BigDecimal.valueOf(100.00);

		Account fromAccount = ImmutableAccount
				.builder()
				.id(transferFrom)
				.balance(initialAmountFrom)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(transferFrom))
				.thenReturn(Option.of(fromAccount));

		when(mockStorage.getAccount(transferTo))
				.thenReturn(Option.none());

		try {
			tested.transferMoney(transferFrom, transferTo, amount);
		} catch (AccountIsNotAvailable accountIsNotAvailable) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldThrowExceptionIfFromAccountClosed() throws AccountIsNotAvailable {
		Long transferFrom = 123456789L;
		Long transferTo = 987654321L;

		BigDecimal initialAmountFrom = BigDecimal.ZERO;
		BigDecimal initialAmountTo = BigDecimal.ZERO;
		BigDecimal amount = BigDecimal.valueOf(100.00);

		Account fromAccount = ImmutableAccount
				.builder()
				.id(transferFrom)
				.balance(initialAmountFrom)
				.status(Account.AccountStatus.CLOSED)
				.build();

		Account toAccount = ImmutableAccount
				.builder()
				.id(transferTo)
				.balance(initialAmountTo)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(transferFrom))
				.thenReturn(Option.of(fromAccount));

		when(mockStorage.getAccount(transferTo))
				.thenReturn(Option.of(toAccount));

		try {
			tested.transferMoney(transferFrom, transferTo, amount);
		} catch (NotSufficientBalance notSufficientBalance) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}

	@Test
	public void shouldThrowExceptionIfToAccountClosed() throws NotSufficientBalance {
		Long transferFrom = 123456789L;
		Long transferTo = 987654321L;

		BigDecimal initialAmountFrom = BigDecimal.valueOf(300.00);
		BigDecimal initialAmountTo = BigDecimal.ZERO;
		BigDecimal amount = BigDecimal.valueOf(100.00);

		Account fromAccount = ImmutableAccount
				.builder()
				.id(transferFrom)
				.balance(initialAmountFrom)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		Account toAccount = ImmutableAccount
				.builder()
				.id(transferTo)
				.balance(initialAmountTo)
				.status(Account.AccountStatus.CLOSED)
				.build();

		Storage mockStorage = mock(Storage.class);
		TransferComponent tested = new TransferComponentImpl(mockStorage);

		when(mockStorage.getAccount(transferFrom))
				.thenReturn(Option.of(fromAccount));

		when(mockStorage.getAccount(transferTo))
				.thenReturn(Option.of(toAccount));

		try {
			tested.transferMoney(transferFrom, transferTo, amount);
		} catch (AccountIsNotAvailable accountIsNotAvailable) {
			verify(mockStorage).rollbackDbTransaction();
		}
	}
}
