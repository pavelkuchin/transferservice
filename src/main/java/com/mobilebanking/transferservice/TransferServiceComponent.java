package com.mobilebanking.transferservice;

import com.mobilebanking.transferservice.controllers.Controller;
import dagger.Component;
import io.javalin.http.Context;


@Component(modules = TransferServiceModule.class)
public interface TransferServiceComponent {
	Controller<Context> controller();
}
