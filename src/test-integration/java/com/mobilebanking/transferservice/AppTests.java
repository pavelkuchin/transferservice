package com.mobilebanking.transferservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mobilebanking.transferservice.dtos.AccountBody;
import com.mobilebanking.transferservice.dtos.Account;
import com.mobilebanking.transferservice.dtos.ImmutableAccountBody;
import io.javalin.Javalin;
import io.vavr.jackson.datatype.VavrModule;
import okhttp3.MediaType;
import okhttp3.OkHttpClient;
import okhttp3.Request;
import okhttp3.RequestBody;
import okhttp3.Response;
import org.testng.annotations.AfterTest;
import org.testng.annotations.BeforeTest;
import org.testng.annotations.Test;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;

public class AppTests {

	private Javalin app;

	private static final MediaType JSON
			= MediaType.get("application/json; charset=utf-8");

	private OkHttpClient client = new OkHttpClient();

	private final static String URL = "http://localhost:7000";

	private ObjectMapper objectMapper;

	private AtomicInteger counterForConcurrentTests = new AtomicInteger(0);

	private Long fromAccountForConcurrentTest;
	private Long toAccountForConcurrentTest;

	@BeforeTest
	public void init() throws IOException {
		objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.registerModule(new VavrModule())
				.registerModule(new JavaTimeModule());

		app = App.initApp().start(7000);

		fromAccountForConcurrentTest = createAccount(BigDecimal.valueOf(1000));
		toAccountForConcurrentTest = createAccount(BigDecimal.ZERO);
	}

	@AfterTest
	public void destroy() {
		app.stop();
	}

	@Test
	public void shouldCreateAccount() throws IOException {
		String body = "{\"balance\": \"10\"}";

		String expected = "\"balance\":\"10\",\"status\":\"ACTIVE\"";

		String result = post(URL + "/v1/account/", body);

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldReturnAccount() throws IOException {
		Long accountId = createAccount(BigDecimal.valueOf(1000));

		String expected = "{\"id\": " + accountId + ",\"balance\": \"1000\",\"status\": \"ACTIVE\"}";

		String result = get(URL + "/v1/account/" + accountId);

		assertThat(result).isEqualToIgnoringWhitespace(expected);
	}

	@Test
	public void shouldReturnErrorIfAccountNotFound() throws IOException {
		String expected = "Account is not available";

		String result = get(URL + "/v1/account/1000000");

		assertThat(result).isEqualToIgnoringWhitespace(expected);
	}

	@Test
	public void shouldDeleteAccount() throws IOException {
		String expected = "\"status\":\"CLOSED\"";

		Long id = createAccount(BigDecimal.ZERO);

		String result = delete(URL + "/v1/account/" + id);

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldReturnErrorWhenDeletingNonExistingAccount() throws IOException {
		String expected = "Account is not available";

		String result = delete(URL + "/v1/account/11111111");

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldReturnErrorWhenTryingToCloseAlreadyClosedAccount() throws IOException {
		String expected = "Account is not available";

		Long id = createAccount(BigDecimal.ZERO);

		delete(URL + "/v1/account/" + id);
		String result = delete(URL + "/v1/account/" + id);

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldReturnErrorWhenTryingToCloseAccountWithNonZeroBalance() throws IOException {
		String expected = "Balance isn't zero, can't close the account";

		Long id = createAccount(BigDecimal.valueOf(100));

		String result = delete(URL + "/v1/account/" + id);

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldPerformTransfer() throws IOException {
		Long fromAccountId = createAccount(BigDecimal.valueOf(1000));
		Long toAccountId = createAccount(BigDecimal.ZERO);

		String expected = "\"balance\":\"1000\"";

		String body = "{ \n" +
				"\t\"fromAccountId\": " + fromAccountId + ", \n" +
				"\t\"toAccountId\": " + toAccountId + ", \n" +
				"\t\"amount\": \"1000\" \n" +
				"}";

		post(URL + "/v1/transaction/", body);
		String result = get(URL + "/v1/account/" + toAccountId);

		assertThat(result).contains(expected);
	}

	@Test
	public void shouldReturnErrorIfPerformingTransferFromAccountWithZeroBalance() throws IOException {
		Long fromAccountId = createAccount(BigDecimal.ZERO);
		Long toAccountId = createAccount(BigDecimal.ZERO);

		String expected = "Not sufficient balance";

		String body = "{ \n" +
				"\t\"fromAccountId\": " + fromAccountId + ", \n" +
				"\t\"toAccountId\": " + toAccountId + ", \n" +
				"\t\"amount\": \"1000\" \n" +
				"}";

		String result = post(URL + "/v1/transaction/", body);

		assertThat(result).contains(expected);
	}

	@Test(threadPoolSize = 5, invocationCount = 20,  timeOut = 10000)
	public void shouldPerformTransferConcurrently() throws IOException, InterruptedException {
		String expected = "\"balance\":\"20\"";

		int numberOfInvocations = 20;

		int count = counterForConcurrentTests.addAndGet(1);

		String body = "{ \n" +
				"\t\"fromAccountId\": " + fromAccountForConcurrentTest + ", \n" +
				"\t\"toAccountId\": " + toAccountForConcurrentTest + ", \n" +
				"\t\"amount\": \"1\" \n" +
				"}";

		post(URL + "/v1/transaction/", body);

		// Making sure that once all transactions have been executed we have what we expected.
		if(count == numberOfInvocations) {
			Thread.sleep(100);
			String result = get(URL + "/v1/account/" + toAccountForConcurrentTest);
			assertThat(result).contains(expected);
		}
	}

	@Test
	public void shouldReturnEmptyListIfTryingToGetTransactionsForNonExistingAccount() throws IOException {
		String expected = "[]";

		String result = get(URL + "/v1/account/1111111111111/transactions/");

		assertThat(result).isEqualTo(expected);
	}

	@Test
	public void shouldReturnTransactions() throws IOException {
		Long fromAccountId = createAccount(BigDecimal.valueOf(1000));
		Long toAccountId = createAccount(BigDecimal.ZERO);

		String expected = "\"amount\":\"500\"";

		String body = "{ \n" +
				"\t\"fromAccountId\": " + fromAccountId + ", \n" +
				"\t\"toAccountId\": " + toAccountId + ", \n" +
				"\t\"amount\": \"500\" \n" +
				"}";

		post(URL + "/v1/transaction/", body);
		post(URL + "/v1/transaction/", body);

		String resultByFromId = get(URL + "/v1/account/" + fromAccountId + "/transactions/");
		String resultByToId = get(URL + "/v1/account/" + toAccountId + "/transactions/");

		assertThat(resultByFromId).isEqualTo(resultByToId);
		assertThat(resultByFromId).contains(expected);
	}

	private Long createAccount(BigDecimal initialBalance) throws IOException {
		AccountBody accountBody = ImmutableAccountBody
				.builder()
				.balance(initialBalance)
				.build();

		String body = objectMapper.writeValueAsString(accountBody);
		String response = post(URL + "/v1/account/", body);
		Account account = objectMapper.readValue(response, Account.class);

		return account.getId();
	}

	private String post(String url, String json) throws IOException {
		RequestBody body = RequestBody.create(JSON, json);
		Request request = new Request.Builder()
				.url(url)
				.post(body)
				.build();
		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}

	private String get(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}

	private String delete(String url) throws IOException {
		Request request = new Request.Builder()
				.url(url)
				.delete()
				.build();

		try (Response response = client.newCall(request).execute()) {
			return response.body().string();
		}
	}
}
