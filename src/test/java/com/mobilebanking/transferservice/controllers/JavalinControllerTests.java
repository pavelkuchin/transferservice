package com.mobilebanking.transferservice.controllers;

import com.mobilebanking.transferservice.components.TransferComponent;
import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.dtos.AccountBody;
import com.mobilebanking.transferservice.dtos.ImmutableAccountBody;
import com.mobilebanking.transferservice.dtos.ImmutableTransactionBody;
import com.mobilebanking.transferservice.dtos.TransactionBody;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.ImmutableAccount;
import com.mobilebanking.transferservice.dtos.ImmutableTransaction;
import com.mobilebanking.transferservice.dtos.Transaction;
import io.javalin.http.Context;
import io.vavr.collection.List;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.mockito.junit.MockitoJUnitRunner;

import java.math.BigDecimal;
import java.time.Instant;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@RunWith(MockitoJUnitRunner.Strict.class)
public class JavalinControllerTests {

	@Test
	public void shouldCreateAccountWithInitialBalance() {
		TransferComponent mockTransferComponent = mock(TransferComponent.class);
		Context mockContext = mock(Context.class);

		Long id = 1L;
		BigDecimal initialBalance = BigDecimal.valueOf(100.00);

		AccountBody accountBody = ImmutableAccountBody
				.builder()
				.balance(initialBalance)
				.build();

		when(mockContext.bodyAsClass(AccountBody.class)).thenReturn(accountBody);

		Account expectedAccount = ImmutableAccount
				.builder()
				.id(id)
				.balance(initialBalance)
				.status(Account.AccountStatus.ACTIVE)
				.build();

		when(mockTransferComponent.createAccount(initialBalance)).thenReturn(expectedAccount);

		Controller<Context> tested = new JavalinControllerImpl(mockTransferComponent);

		Account actualAccount = tested.createAccount(mockContext);

		assertThat(actualAccount).isEqualTo(expectedAccount);
		verify(mockContext).json(expectedAccount);
		verify(mockContext).status(201);
	}

	@Test
	public void shouldCloseCorrectAccount() throws BalanceIsNotZero, AccountIsNotAvailable {
		TransferComponent mockTransferComponent = mock(TransferComponent.class);
		Context mockContext = mock(Context.class);

		Long id = 1L;

		Account closedAccount = ImmutableAccount
				.builder()
				.id(id)
				.balance(BigDecimal.ZERO)
				.status(Account.AccountStatus.CLOSED)
				.build();

		when(mockContext.pathParam("id")).thenReturn(id.toString());
		when(mockTransferComponent.closeAccount(id)).thenReturn(closedAccount);

		Controller<Context> tested = new JavalinControllerImpl(mockTransferComponent);

		Account actualAccount = tested.closeAccount(mockContext);

		assertThat(actualAccount).isEqualTo(closedAccount);
		verify(mockContext).status(202);
		verify(mockContext).json(closedAccount);
	}

	@Test
	public void shouldReturnCorrectAccount() throws AccountIsNotAvailable {
		TransferComponent mockTransferComponent = mock(TransferComponent.class);
		Context mockContext = mock(Context.class);

		Long id = 1L;

		Account account = ImmutableAccount
				.builder()
				.id(id)
				.balance(BigDecimal.valueOf(100.00))
				.status(Account.AccountStatus.ACTIVE)
				.build();

		when(mockContext.pathParam("id")).thenReturn(id.toString());
		when(mockTransferComponent.getAccount(id)).thenReturn(account);

		Controller<Context> tested = new JavalinControllerImpl(mockTransferComponent);

		Account actualAccount = tested.getAccount(mockContext);

		assertThat(actualAccount).isEqualTo(account);
		verify(mockContext).status(200);
		verify(mockContext).json(account);
	}

	@Test
	public void shouldPerformTransfer() throws NotSufficientBalance, AccountIsNotAvailable {
		TransferComponent mockTransferComponent = mock(TransferComponent.class);
		Context mockContext = mock(Context.class);

		Long fromAccount = 1L;
		Long toAccount = 2L;
		BigDecimal amount = BigDecimal.valueOf(100.00);

		Transaction transaction = ImmutableTransaction
				.builder()
				.fromAccountId(fromAccount)
				.toAccountId(toAccount)
				.timeStamp(Instant.now())
				.amount(amount)
				.build();

		TransactionBody transactionBody = ImmutableTransactionBody
				.builder()
				.fromAccountId(fromAccount)
				.toAccountId(toAccount)
				.amount(amount)
				.build();

		when(mockContext.bodyAsClass(TransactionBody.class)).thenReturn(transactionBody);
		when(mockTransferComponent.transferMoney(fromAccount, toAccount, amount)).thenReturn(transaction);

		Controller<Context> tested = new JavalinControllerImpl(mockTransferComponent);

		Transaction actualTransaction = tested.transfer(mockContext);

		assertThat(actualTransaction).isEqualTo(transaction);
		verify(mockContext).status(201);
		verify(mockContext).json(transaction);
	}

	@Test
	public void shouldReturnAllTransactions() {
		TransferComponent mockTransferComponent = mock(TransferComponent.class);
		Context mockContext = mock(Context.class);

		Long id = 1L;

		Transaction transaction1 = ImmutableTransaction
				.builder()
				.fromAccountId(id)
				.toAccountId(2L)
				.amount(BigDecimal.valueOf(100.00))
				.timeStamp(Instant.now())
				.build();

		Transaction transaction2 = ImmutableTransaction
				.builder()
				.fromAccountId(2L)
				.toAccountId(id)
				.amount(BigDecimal.valueOf(100.00))
				.timeStamp(Instant.now())
				.build();

		when(mockContext.pathParam("id")).thenReturn(id.toString());
		when(mockTransferComponent.getTransactionsForAccount(id))
				.thenReturn(List.of(transaction1, transaction2));

		Controller<Context> tested = new JavalinControllerImpl(mockTransferComponent);

		List<Transaction> actualList = tested.getTransactions(mockContext);

		assertThat(actualList).isEqualTo(List.of(transaction1, transaction2));
		verify(mockContext).status(200);
		verify(mockContext).json(List.of(transaction1, transaction2));
	}
}
