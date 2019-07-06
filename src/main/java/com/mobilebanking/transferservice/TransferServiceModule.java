package com.mobilebanking.transferservice;

import com.mobilebanking.transferservice.components.TransferComponent;
import com.mobilebanking.transferservice.components.TransferComponentImpl;
import com.mobilebanking.transferservice.controllers.Controller;
import com.mobilebanking.transferservice.controllers.JavalinControllerImpl;
import com.mobilebanking.transferservice.services.SimpleInMemoryStorageImpl;
import com.mobilebanking.transferservice.services.Storage;
import dagger.Module;
import dagger.Provides;
import io.javalin.http.Context;


@Module
public class TransferServiceModule {
	@Provides
	static Controller<Context> provideController(TransferComponent transferComponent) {
		return new JavalinControllerImpl(transferComponent);
	}

	@Provides
	static TransferComponent provideTransferComponent(Storage storage) {
		return new TransferComponentImpl(storage);
	}

	@Provides
	static Storage provideStorage() {
		return new SimpleInMemoryStorageImpl();
	}
}
