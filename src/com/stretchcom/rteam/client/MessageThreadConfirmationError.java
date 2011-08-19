package com.stretchcom.rteam.client;

import com.google.gwt.core.client.GWT;
import com.google.gwt.dom.client.SpanElement;
import com.google.gwt.event.dom.client.ClickEvent;
import com.google.gwt.uibinder.client.UiBinder;
import com.google.gwt.uibinder.client.UiField;
import com.google.gwt.uibinder.client.UiHandler;
import com.google.gwt.user.client.Window;
import com.google.gwt.user.client.ui.Button;
import com.google.gwt.user.client.ui.Composite;
import com.google.gwt.user.client.ui.Label;
import com.google.gwt.user.client.ui.Widget;

public class MessageThreadConfirmationError extends Composite {

	private static MessageThreadConfirmationErrorUiBinder uiBinder = GWT.create(MessageThreadConfirmationErrorUiBinder.class);

	interface MessageThreadConfirmationErrorUiBinder extends UiBinder<Widget, MessageThreadConfirmationError> {}

	@UiField
	SpanElement errorMessage;

	public MessageThreadConfirmationError() {
		initWidget(uiBinder.createAndBindUi(this));
	}
	
	public void setErrorMessage(String theErrorMessage) { errorMessage.setInnerText(theErrorMessage); }
}
