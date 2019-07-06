package com.mobilebanking.transferservice;

import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.mobilebanking.transferservice.components.exceptions.AccountIsNotAvailable;
import com.mobilebanking.transferservice.components.exceptions.BalanceIsNotZero;
import com.mobilebanking.transferservice.components.exceptions.NotSufficientBalance;
import com.mobilebanking.transferservice.controllers.Controller;
import io.javalin.Javalin;
import io.javalin.http.Context;
import io.javalin.plugin.json.JavalinJackson;
import io.vavr.jackson.datatype.VavrModule;

public class App {
	public static void main(String[] args) {
		initApp().start(7000);
	}

	protected static Javalin initApp() {
		TransferServiceComponent transferServiceComponent = DaggerTransferServiceComponent.create();

		Controller<Context> controller = transferServiceComponent.controller();

		ObjectMapper objectMapper = new ObjectMapper()
				.findAndRegisterModules()
				.registerModule(new VavrModule())
				.registerModule(new JavaTimeModule());

		JavalinJackson.configure(objectMapper);

		Javalin app = Javalin.create();
		app.post("/v1/account", controller::createAccount);
		app.delete("/v1/account/:id", controller::closeAccount);
		app.get("/v1/account/:id", controller::getAccount);
		app.post("/v1/transaction/", controller::transfer);
		app.get("/v1/account/:id/transactions", controller::getTransactions);

		app.exception(AccountIsNotAvailable.class, (e, ctx) -> {
			ctx.status(404);
			ctx.result("Account is not available");
		});

		app.exception(BalanceIsNotZero.class, (e, ctx) -> {
			ctx.status(400);
			ctx.result("Balance isn't zero, can't close the account");
		});

		app.exception(NotSufficientBalance.class, (e, ctx) -> {
			ctx.status(400);
			ctx.result("Not sufficient balance");
		});

		return app;
	}
}
